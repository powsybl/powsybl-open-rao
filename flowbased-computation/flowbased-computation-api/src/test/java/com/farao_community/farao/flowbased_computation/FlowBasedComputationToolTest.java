/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * FlowBased Computation Tool Test
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public class FlowBasedComputationToolTest {

    private CommandLine line;
    private ToolRunningContext context;
    private FlowBasedComputationTool flowBasedComputationTool;

    @Before
    public void setup() {
        line = Mockito.mock(CommandLine.class);
        context = Mockito.mock(ToolRunningContext.class);
        flowBasedComputationTool = Mockito.mock(FlowBasedComputationTool.class);
    }

    @Test
    public void runBis() throws Exception {
        flowBasedComputationTool.getCommand();
        flowBasedComputationTool.run(line, context);
    }
}
