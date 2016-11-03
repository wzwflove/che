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
package org.eclipse.che.api.environment.server;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.EnvironmentRecipe;
import org.eclipse.che.api.environment.server.model.CheServicesEnvironmentImpl;

/**
 * Parser for creating {@link CheServicesEnvironmentImpl} with parameters
 * parsed from {@link EnvironmentRecipe} content.
 *
 * @author Alexander Andrienko
 */
public interface EnvironmentRecipeParser {
    /**
     * Returns {@link CheServicesEnvironmentImpl} with parameters parsed
     * from from {@link EnvironmentRecipe} content.
     *
     * @param environmentRecipe
     *         recipe with environment definition
     * @throws IllegalArgumentException
     *         in case invalid argument in the recipe content of the {@link EnvironmentRecipe}
     * @throws ServerException
     *         when any error occurs
     */
    CheServicesEnvironmentImpl parse(EnvironmentRecipe environmentRecipe) throws IllegalArgumentException,
                                                                                 ServerException;
}
