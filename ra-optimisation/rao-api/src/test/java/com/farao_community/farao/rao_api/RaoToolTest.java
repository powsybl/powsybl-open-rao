/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api;

import com.farao_community.farao.commons.AbstractToolTest;

import com.powsybl.tools.Tool;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class RaoToolTest extends AbstractToolTest {

    private static final String COMMAND_NAME = "rao";
    private static RaoTool tool = new RaoTool();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        createFile("testcase.xiidm", "");
        createFile("testcrac.json", "");
        createFile("testoutputfile.json", "");
    }

    protected Iterable<Tool> getTools() {
        return Collections.singleton(tool);
    }

    @Override
    public void assertCommand() {
        assertCommand(tool.getCommand(), COMMAND_NAME, 4, 4);
        assertOption(tool.getCommand().getOptions(), "case-file", true, true);
        assertOption(tool.getCommand().getOptions(), "crac-file", true, true);
        assertOption(tool.getCommand().getOptions(), "output-file", true, true);
        assertOption(tool.getCommand().getOptions(), "output-format", true, true);

        Assert.assertEquals("Computation", tool.getCommand().getTheme());
        Assert.assertEquals("Run a RAO computation", tool.getCommand().getDescription());
        Assert.assertEquals("Rao computation returns RaoComputationResult using the Rao implementation given in the config", tool.getCommand().getUsageFooter());
    }

    @Test
    public void run() throws IOException {
        assertCommand(new String[] {
            COMMAND_NAME,
            "--case-file", "testCase.xiidm",
            "--crac-file", "crac.json",
            "--output-file", "output.json",
            "--output-format", "Json"
        }, 3, "", "");
    }
}
