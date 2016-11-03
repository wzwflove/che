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
package org.eclipse.che.plugin.docker.compose.yaml;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.EnvironmentRecipe;
import org.eclipse.che.api.environment.server.model.CheServicesEnvironmentImpl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Mario Loriedo
 */
@Listeners(MockitoTestNGListener.class)
public class BuildContextTest {

    @Mock
    private EnvironmentRecipe recipe;

    @InjectMocks
    private ComposeEnvironmentParser parser;

    @Test
    public void shouldParseBuildArgsWhenProvided() throws ServerException {
        // given
        String recipeContent = "services:\n" +
                               " dev-machine:\n" +
                               "  build:\n" +
                               "   context: .\n" +
                               "   args:\n" +
                               "    buildno: 1\n" +
                               "    password: secret\n";
        setUpRecipe(recipeContent);

        Map<String, String> expected = new HashMap<String, String>() {
            {
                put("buildno", "1");
                put("password", "secret");
            }
        };

        // when
        CheServicesEnvironmentImpl cheServicesEnvironment = parser.parse(recipe);


        // then
        assertEquals(cheServicesEnvironment.getServices().get("dev-machine").getBuild().getArgs(), expected);
    }

    @Test
    public void shouldNotParseBuildArgsWhenNotProvided() throws ServerException {
        // given
        String recipeContent = "services:\n" +
                               " dev-machine:\n" +
                               "  build:\n" +
                               "   context: .\n";
        setUpRecipe(recipeContent);

        // when
        CheServicesEnvironmentImpl cheServicesEnvironment = parser.parse(recipe);

        // then
        assertEquals(Collections.emptyMap(), cheServicesEnvironment.getServices().get("dev-machine").getBuild().getArgs());
    }

    private void setUpRecipe(String content) {
        when(recipe.getContent()).thenReturn(content);
        when(recipe.getContentType()).thenReturn("application/x-yaml");
    }
}
