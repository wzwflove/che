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

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.reader.ReaderException;

import org.eclipse.che.api.core.model.workspace.EnvironmentRecipe;
import org.eclipse.che.api.environment.server.model.CheServiceImpl;
import org.eclipse.che.api.environment.server.model.CheServicesEnvironmentImpl;
import org.eclipse.che.plugin.docker.compose.ComposeServiceImpl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test deserialization field {@link ComposeServiceImpl#command}.
 *
 * @author Alexander Andrienko
 */
@Listeners(MockitoTestNGListener.class)
public class ComposeServiceCommandContextTest {

    @Mock
    private EnvironmentRecipe recipe;

    @InjectMocks
    private ComposeEnvironmentParser composeEnvironmentParser;

    private static final String RECIPE_WITHOUT_COMMAND_VALUE = "services:\n" +
                                                               " machine1:\n" +
                                                               "  image: codenvy/mysql\n" +
                                                               "  environment:\n" +
                                                               "   MYSQL_USER: petclinic\n" +
                                                               "   MYSQL_PASSWORD: password\n" +
                                                               "  mem_limit: 2147483648\n" +
                                                               "  command: %s\n" + //<- test target
                                                               "  expose: [4403, 5502]";

    @Test(dataProvider = "validCommand")
    public void composeServiceCommandShouldBeParsedSuccessfully(String command,
                                                                List<String> commandWords,
                                                                int commandNumberOfWords) throws Exception {
        setUpRecipe(command);

        CheServicesEnvironmentImpl cheServicesEnvironment = composeEnvironmentParser.parse(recipe);

        assertEquals(cheServicesEnvironment.getServices().size(), 1);
        CheServiceImpl service = cheServicesEnvironment.getServices().get("machine1");
        assertEquals(service.getImage(), "codenvy/mysql");
        assertEquals(service.getMemLimit().longValue(), 2147483648L);
        Map<String, String> environment = service.getEnvironment();
        assertEquals(environment.size(), 2);
        assertEquals(environment.get("MYSQL_USER"), "petclinic");
        assertEquals(environment.get("MYSQL_PASSWORD"), "password");
        assertTrue(service.getExpose().containsAll(asList("4403", "5502")));

        assertTrue(service.getCommand().containsAll(commandWords));
        assertEquals(service.getCommand().size(), commandNumberOfWords);
    }

    @DataProvider(name = "validCommand")
    private Object[][] validCommand() {
        return new Object[][] {
                //allow command in one line
                {"service mysql start", asList("service", "mysql", "start"), 3},
                {"service mysql start && tail -f /dev/null", asList("service", "mysql", "start", "&&", "tail", "-f", "/dev/null"), 7},
                {"service mysql              start", asList("service", "mysql", "start"), 3},
                {"service mysql start         ", asList("service", "mysql", "start"), 3},
                {"service mysql start         ", asList("service", "mysql", "start"), 3},

                //allow break line feature
                {"| \n" +
                 "   service mysql\n" +
                 "   restart", asList("service", "mysql", "restart"), 3},

                {"| \r" +
                 "   service mysql\r" +
                 "   restart", asList("service", "mysql", "restart"), 3},

                {"| \r\n" +
                 "   service mysql\r\n" +
                 "   restart", asList("service", "mysql", "restart"), 3},

                {"| \n\n" +
                 "   service mysql\n\n" +
                 "   restart", asList("service", "mysql", "restart"), 3},

                {"| \n \n" +
                 "   service mysql\n \n" +
                 "   restart", asList("service", "mysql", "restart"), 3},

                {"> \n \n" +
                 "   service mysql\n \n" +
                 "   restart", asList("service", "mysql", "restart"), 3},

                {"> \n \n" +
                 "   ls -a\n \n" +
                 "   -i -p", asList("ls", "-a", "-i", "-p"), 4},

                //allow list command words
                    //first form
                {"[service, mysql, start]", asList("service", "mysql", "start"), 3},
                {"[service, mysql, start, '&&', tail, -f, /dev/null]",
                 asList("service", "mysql", "start", "&&", "tail", "-f", "/dev/null"), 7},
                    //second form
                {"\n" +
                 "   - tail\n" +
                 "   - -f\n" +
                 "   - /dev/null", asList("tail", "-f", "/dev/null"), 3},
                {"\n" +
                 "   - service\n" +
                 "   - mysql\n" +
                 "   - start\n" +
                 "   - '&&'\n" +
                 "   - tail\n" +
                 "   - -f\n" +
                 "   - /dev/null\n",
                 asList("service", "mysql", "start", "&&", "tail", "-f", "/dev/null"), 7},


                //Some special symbol should be accessible in case line was wrapped by quotes
                {"\"echo ${PWD}\"", asList("echo", "${PWD}"), 2},
                {"\"(Test)\"", singletonList("(Test)"), 1},

                {"", singletonList(""), 1},
        };
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          dataProvider = "inValidCommand")
    public void composeServiceCommandShouldBeParsedFailed(String command) throws Exception {
        setUpRecipe(command);

        try {
            composeEnvironmentParser.parse(recipe);
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            throw e;
        }
    }

    @DataProvider(name = "inValidCommand")
    private Object[][] inValidCommand() {
        return new Object[][] {
                {"\n |" +
                 "  - tail\n" +
                 "   - -f\n" +
                 "   - /dev/null"},
                {"\n |" +
                 "   - tail\n" +
                 "    -f\n" +
                 "   - /dev/null"},
                {"\n >" +
                 "   - tail\n" +
                 "    -f\n" +
                 "   - /dev/null"},
                {"{service mysql start}"},
                {"[service, mysql, {start : test}]"},
                {"[service, mysql, [start : test]]"},
                {"service mysql \nstart"},
                {"test : value"},
                {"[service mysql start"},
        };
    }

    @Test(expectedExceptions = ReaderException.class,
          expectedExceptionsMessageRegExp = "special characters are not allowed",
          dataProvider = "inValidSymbols")
    public void symbolsShouldBeInvalidForYaml(String command) throws Exception {
        setUpRecipe(command);

        try {
            composeEnvironmentParser.parse(recipe);
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            throw e;
        }
    }

    /**
     * Valid yaml Regex by specification Yaml 1.2 (for two bytes - UTF-16):
     * (\\x09|\\x0A|\\x0D|[\\x20-\\x7E]|\\x85|[\\xA0-\\xD7FF]|[\\xE000-\\xFFFD])+
     */
    @DataProvider(name = "inValidSymbols")
    private Object[][] inValidSymbols() {
        return new Object[][] {
            {"service mysql start\uFFFE"},
            {"service mysql start\uDFFF"},
            {"service mysql start\uD800"},
            {"service mysql start\u009F"},
            {"service mysql start\u0086"},
            {"service mysql start\u0084"},
            {"service mysql start\u0084"},
            {"service mysql start\u007F"},
            {"service mysql start\u001F"},
            {"service mysql start\u000E"},
            {"service mysql start\u000C"},
            {"service mysql start\u000B"},
            {"service mysql start\u0008"},
        };
    }

    private void setUpRecipe(String command) {
        when(recipe.getContentType()).thenReturn("text/x-yaml");

        String content = format(RECIPE_WITHOUT_COMMAND_VALUE, command);
        when(recipe.getContent()).thenReturn(content);
    }
}
