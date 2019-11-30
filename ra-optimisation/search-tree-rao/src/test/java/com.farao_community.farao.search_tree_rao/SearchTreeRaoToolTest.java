package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.AbstractToolTest;
import com.powsybl.tools.Tool;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

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

        Assert.assertEquals(tool.getCommand().getTheme(), "Computation");
        Assert.assertEquals(tool.getCommand().getDescription(), "Run SearchTreeRao Computation");
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
