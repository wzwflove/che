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

import org.eclipse.che.core.db.schema.SchemaInitializationException;
import org.eclipse.che.core.db.schema.SchemaInitializer;
import org.flywaydb.core.Flyway;

import javax.inject.Inject;
import javax.sql.DataSource;

/**
 * <a href="https://flywaydb.org/">Flyway</a> based schema initializer.
 *
 * @author Yevhenii Voevodin
 */
public class FlywaySchemaInitializer implements SchemaInitializer {

    @Inject
    private CustomSqlMigrationResolver customResolver;

    @Inject
    private DataSource dataSource;

    @Override
    public void init() throws SchemaInitializationException {
        final Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setLocations("sql/");
        flyway.setResolvers(customResolver);
        flyway.setSkipDefaultResolvers(true);
        flyway.setBaselineOnMigrate(true);
        flyway.setBaselineVersionAsString("");
        flyway.setSqlMigrationSeparator(".");
        flyway.setSqlMigrationSuffix(".sql");
        flyway.setSqlMigrationPrefix("");
        try {
            flyway.migrate();
        } catch (RuntimeException x) {
            throw new SchemaInitializationException(x.getLocalizedMessage(), x);
        }
    }
}
