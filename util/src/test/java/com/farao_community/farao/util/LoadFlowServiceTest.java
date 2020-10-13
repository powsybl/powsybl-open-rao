/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.*;
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
        assertTrue(LoadFlowService.runLoadFlow(Mockito.mock(Network.class), "").isOk());
    }

    @AutoService(LoadFlowProvider.class)
    public static final class MockLoadFlow implements LoadFlowProvider {
        @Override
        public CompletableFuture<LoadFlowResult> run(Network network, ComputationManager computationManager, String s, LoadFlowParameters loadFlowParameters) {
            return CompletableFuture.completedFuture(new LoadFlowResultImpl(true, Collections.emptyMap(), ""));
        }

        @Override
        public String getName() {
            return "MockLoadFlow";
        }

        @Override
        public String getVersion() {
            return "0";
        }
    }
}
