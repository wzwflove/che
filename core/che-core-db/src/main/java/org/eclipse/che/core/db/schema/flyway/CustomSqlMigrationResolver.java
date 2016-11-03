/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.core.db.schema.flyway;

import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.flywaydb.core.api.MigrationType;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FlywayConfiguration;
import org.flywaydb.core.api.resolver.BaseMigrationResolver;
import org.flywaydb.core.api.resolver.ResolvedMigration;
import org.flywaydb.core.internal.dbsupport.DbSupportFactory;
import org.flywaydb.core.internal.resolver.ResolvedMigrationImpl;
import org.flywaydb.core.internal.resolver.sql.SqlMigrationExecutor;
import org.flywaydb.core.internal.util.PlaceholderReplacer;
import org.flywaydb.core.internal.util.scanner.filesystem.FileSystemResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Resolves SQL migrations from the configured directory,
 * allows overriding of default scripts with database specific ones.
 *
 * <ul>Migrations script must follow the next rules:
 * <li>It must be placed in the project version directory e.g. <i>5.0.1</i></li>
 * <li>Project version directory must be placed in dedicated directory e.g. <i>resources/sql</i></li>
 * <li>Migration/Initialization script name must start with a number e.g <i>1.init.sql</i>,
 * this number indicates the subversion of the database migration, e.g. for version <i>5.0.0</i>
 * and migration script <i>1.init.sql</i> database migration version will be <i>5.0.0.1</i></li>
 * <li>If a directory is not a version directory but it has to be present in migrations root,
 * then it should be included to ignored dirs list</li>
 * <li>If a file is not a part of migration it shouldn't end with migration prefix e.g. <i>.sql</i>
 * then resolver will ignore it</li>
 * </ul>
 *
 * <p>From the structure:
 * <pre>
 *   resources/
 *     /sql
 *       /5.0.0
 *         1.init.sql
 *       /5.0.0-M1
 *         1.rename_fields.sql
 *         2.add_workspace_constraint.sql
 *         /postgresql
 *           2.add_workspace_constraint.sql
 *       /5.0.1
 *         1.stacks_migration.sql
 * </pre>
 *
 * <ul>4 database migrations will be resolved
 * <li>5.0.0.1 - initialization script based on file <i>sql/5.0.0/1.init.sql</i></li>
 * <li>5.0.0.1.1 - modification script based on file <i>sql/5.0.0-M1/1.rename_fields.sql</i></li>
 * <li>5.0.0.1.2 - modification script(if postgresql is current provider) based on file
 * <i>sql/5.0.0-M1/postgresql/2.add_workspace_constraint.sql</li>
 * <li>5.0.1.1 - modification script based on file <i>sql/5.0.1/1.stacks_migrations.sql</i></li>
 * </ul>
 *
 * @author Yevhenii Voevodin
 */
public class CustomSqlMigrationResolver extends BaseMigrationResolver {

    private static final Pattern NOT_VERSION_CHARS_PATTERN = Pattern.compile("[^0-9.]");

    @Inject
    @Named("db.sql.scripts_dir")
    private String scriptsDirPath;

    @Inject
    @Named("db.provider.name")
    private String dbProviderName;

    @Inject(optional = true)
    @Named("db.sql.ignore_dirs")
    private String[] ignoredDirs = new String[0];

    @Override
    public Collection<ResolvedMigration> resolveMigrations() {
        try {
            return resolveSqlMigrations();
        } catch (IOException | SQLException x) {
            throw new RuntimeException(x.getLocalizedMessage(), x);
        }
    }

    private List<ResolvedMigration> resolveSqlMigrations() throws IOException, SQLException {
        final Set<String> ignoreDirs = Sets.newHashSet(ignoredDirs);
        final Path scriptsDir = Paths.get(scriptsDirPath);
        final List<Path> versionDirs = Files.list(scriptsDir)
                                            .filter(dir -> Files.isDirectory(dir))
                                            .filter(dir -> !ignoreDirs.contains(dir.getFileName().toString()))
                                            .collect(Collectors.toList());
        final List<ResolvedMigration> migrations = new ArrayList<>(versionDirs.size());
        for (Path versionDir : versionDirs) {
            final String versionDirName = versionDir.getFileName().toString();
            final TreeMap<Integer, Path> scripts = findScripts(versionDir);
            for (Map.Entry<Integer, Path> entry : scripts.entrySet()) {
                final int scriptVersion = entry.getKey();
                final Path scriptPath = entry.getValue();
                // 5.0.0-M1 becomes -> 5.0.0.1
                // 6.0.0    becomes -> 6.0.0
                final String version = NOT_VERSION_CHARS_PATTERN.matcher(versionDirName.replaceAll("-", ".")).replaceAll("");
                final ResolvedMigrationImpl migration = new ResolvedMigrationImpl();
                // 6.0.0    becomes -> 6.0.0.1 e.g. for 1.init.sql
                // 6.0.0    becomes -> 6.0.0.2 e.g. for 2.rename_fields.sql
                migration.setVersion(MigrationVersion.fromVersion(version + "." + scriptVersion));
                migration.setPhysicalLocation(scriptPath.toAbsolutePath().toString());
                migration.setScript(migration.getPhysicalLocation());
                migration.setType(MigrationType.SQL);
                migration.setDescription(versionDirName);
                migration.setChecksum(com.google.common.io.Files.hash(scriptPath.toFile(), Hashing.crc32()).asInt());
                final Connection connection = flywayConfiguration.getDataSource().getConnection();
                migration.setExecutor(new SqlMigrationExecutor(DbSupportFactory.createDbSupport(connection, true),
                                                               new FileSystemResource(migration.getPhysicalLocation()),
                                                               PlaceholderReplacer.NO_PLACEHOLDERS,
                                                               flywayConfiguration.getEncoding()));
                migrations.add(migration);
            }
        }
        return migrations;
    }

    /**
     * Searches for all the scripts available in given directory,
     * overrides those scripts which are redefined in provider's folder.
     */
    private TreeMap<Integer, Path> findScripts(Path versionDir) throws IOException {
        final TreeMap<Integer, Path> scripts = new TreeMap<>();
        indexScripts(versionDir, scripts);
        final Path providerScriptsDir = versionDir.resolve(dbProviderName);
        if (Files.exists(providerScriptsDir) && Files.isDirectory(providerScriptsDir)) {
            indexScripts(providerScriptsDir, scripts);
        }
        return scripts;
    }

    /**
     * Searches for migrations scripts and fills the given {@code scripts} map
     * with those which are reliable, if script starts with numeric symbol and
     * contains {@link FlywayConfiguration#getSqlMigrationSeparator() separator} character
     * then it will be added to the map as {@code filename.substring(0, sepChar) -> file},
     * with a key parsed as integer.
     *
     * <p>For example:
     * <pre>
     *     file: ~/sql/1.0.0/1.init.sql          =produces mapping=>  1 -> "~/sql/1.0.0/1.init.sql"
     *     file: ~/sql/1.0.0/2.rename_fields.sql =produces mapping=>  2 -> "~/sql/1.0.0/2.rename_fields.sql"
     *     file: ~/sql/1.0.0/description.sql     ={@link IllegalStateException} is thrown 'description' is not integer
     *     file: ~/sql/1.0.0/README.md           =ignored extension is different from .sql(or configured suffix)
     *     dir:  ~/sql/1.0.0/additional-scripts  =ignored
     * </pre>
     *
     * @throws IllegalStateException
     *         when script file name doesn't start with a numeric version
     * @throws IOException
     *         when any error occurs during call to list files
     */
    private void indexScripts(Path dir, TreeMap<Integer, Path> scripts) throws IOException {
        final String separator = flywayConfiguration.getSqlMigrationSeparator();
        final String suffix = flywayConfiguration.getSqlMigrationSuffix();
        Files.list(dir).forEach(path -> {
            final String fileName = path.getFileName().toString();
            final int separatorIdx = fileName.indexOf(separator);
            if (!Files.isDirectory(path) && separatorIdx > 0 && fileName.endsWith(suffix)) {
                final Integer scriptVersion = Ints.tryParse(fileName.substring(0, separatorIdx));
                if (scriptVersion == null) {
                    throw new IllegalStateException(format("Expected script '%s' to provide numeric version", fileName));
                }
                scripts.put(scriptVersion, path);
            }
        });
    }
}
