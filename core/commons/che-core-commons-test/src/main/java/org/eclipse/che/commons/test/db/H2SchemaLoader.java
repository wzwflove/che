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
package org.eclipse.che.commons.test.db;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Yevhenii Voevodin
 */
public class H2SchemaLoader {

    private final String     scriptsDir;
    private final DataSource dataSource;

    public H2SchemaLoader() {
        final URL resource = Thread.currentThread().getContextClassLoader().getResource(".");
        if (resource == null) {
            throw new IllegalStateException();
        }
        final URI uri;
        try {
            uri = resource.toURI();
        } catch (URISyntaxException x) {
            throw new IllegalStateException(x);
        }
        final Path target = Paths.get(uri).getParent();
        if (target == null) {
            throw new IllegalStateException();
        }
        // target/sql
        this.scriptsDir = target.resolve("sql").toAbsolutePath().toString();
        final JdbcDataSource ds = new JdbcDataSource();
        ds.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        this.dataSource = ds;
    }

    public H2SchemaLoader(String scriptsDir, DataSource dataSource) {
        this.scriptsDir = scriptsDir;
        this.dataSource = dataSource;
    }

    public void load() throws IOException, SQLException {
        final ScriptBuilder scriptBuilder = new ScriptBuilder();
        Files.walkFileTree(Paths.get(scriptsDir), scriptBuilder);
        if (!scriptBuilder.isOk()) {
            throw scriptBuilder.getException();
        }
        try (Connection conn = dataSource.getConnection()) {
            RunScript.execute(conn, new StringReader(scriptBuilder.build()));
        }
    }

    public void loadSilently(){
        try {
            load();
        } catch (Exception x) {
            throw new RuntimeException(x.getLocalizedMessage(), x);
        }
    }

    private static class ScriptBuilder implements FileVisitor<Path> {

        private final StringBuilder sb = new StringBuilder();

        private IOException exception;

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            final Path fileName = file.getFileName();
            if (fileName != null && fileName.toString().endsWith(".sql")) {
                sb.append(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            this.exception = exc;
            return FileVisitResult.TERMINATE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        public boolean isOk() {
            return exception == null;
        }

        public IOException getException() {
            return exception;
        }

        public String build() {
            return sb.toString();
        }
    }
}
