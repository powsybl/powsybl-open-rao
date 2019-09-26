/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.loadflow.LoadFlowResultImpl;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class LoadFlowServiceTest {
    @Test
    public void testLoadflowServiceInitialisation() {
        LoadFlowFactory loadFlowFactory = Mockito.mock(LoadFlowFactory.class);
        LoadFlow loadFlow = Mockito.mock(LoadFlow.class);
        ComputationManager computationManager = Mockito.mock(ComputationManager.class);

        Mockito.when(loadFlowFactory.create(Mockito.any(), Mockito.any(), Mockito.anyInt())).thenReturn(loadFlow);
        Mockito.when(loadFlow.run(Mockito.any(), Mockito.any())).thenReturn(CompletableFuture.completedFuture(new LoadFlowResultImpl(true, Collections.emptyMap(), "")));

        LoadFlowService.init(loadFlowFactory, computationManager);
        assertTrue(LoadFlowService.runLoadFlow(Mockito.mock(Network.class), "").isOk());
    }
}
