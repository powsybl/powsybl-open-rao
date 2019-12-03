/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.AbstractToolTest;
import com.powsybl.tools.Tool;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class SearchTreeRaoToolTest extends AbstractToolTest {

    private static final String COMMAND_NAME = "search-tree-rao";
    private final SearchTreeRaoTool tool = new SearchTreeRaoTool();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        createFile("testCase.xiidm", "");
        createFile("crac.json", "");
        createFile("output.json", "");
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
        Assert.assertEquals("Run SearchTreeRao Computation", tool.getCommand().getDescription());
        Assert.assertEquals("SearchTreeRao computation returns RaoComputation result with SearchTreeRao extension", tool.getCommand().getUsageFooter());
    }

    @Test
    public void checkCommandOK() throws IOException {
        assertCommand(new String[] {
            COMMAND_NAME,
            "--case-file", "testCase.xiidm",
            "--crac-file", "crac.json",
            "--output-file", "output.json",
            "--output-format", "Json"
        }, 3, "", "");
    }
}
