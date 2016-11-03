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
package org.eclipse.che.plugin.docker.compose;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import org.eclipse.che.api.environment.server.EnvironmentRecipeParser;
import org.eclipse.che.plugin.docker.compose.yaml.ComposeEnvironmentParser;

/**
 * @author Alexander Andrienko
 */
public class ComposeModule extends AbstractModule {
    @Override
    protected void configure() {
        MapBinder<String, EnvironmentRecipeParser> mapBinder = MapBinder.newMapBinder(binder(),
                                                                                      String.class,
                                                                                      EnvironmentRecipeParser.class);
        mapBinder.addBinding("compose").to(ComposeEnvironmentParser.class);
    }
}
