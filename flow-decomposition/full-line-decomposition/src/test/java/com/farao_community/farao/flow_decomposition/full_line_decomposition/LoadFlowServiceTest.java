/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResult;
import com.farao_community.farao.commons.FaraoException;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class LoadFlowServiceTest {

    @Test
    public void testLoadFlowService() {
        LoadFlow.Runner runner = Mockito.mock(LoadFlow.Runner.class);
        LoadFlowResult loadFlowResult = Mockito.mock(LoadFlowResult.class);

        when(runner.run(any(), any(), any(), any())).thenReturn(loadFlowResult);

        LoadFlowService service = new LoadFlowService(runner, Mockito.mock(ComputationManager.class));
        LoadFlowResult result = service.compute(Mockito.mock(Network.class), "", Mockito.mock(FullLineDecompositionParameters.class, RETURNS_DEEP_STUBS));
        assertNotNull(result);
        assertEquals(loadFlowResult, result);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionInLoadFlowService() {
        LoadFlow.Runner runner = Mockito.mock(LoadFlow.Runner.class);
        when(runner.run(any(), any(), any(), any())).thenThrow(PowsyblException.class);

        LoadFlowService service = new LoadFlowService(runner, Mockito.mock(ComputationManager.class));
        service.compute(Mockito.mock(Network.class), "", Mockito.mock(FullLineDecompositionParameters.class, RETURNS_DEEP_STUBS));
    }
}
