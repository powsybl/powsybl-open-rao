/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputation;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityFactorsProvider;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class SensitivityComputationServiceTest {
    @Test
    public void testLoadflowServiceInitialisation() {
        SensitivityComputationFactory sensitivityComputationFactory = Mockito.mock(SensitivityComputationFactory.class);
        SensitivityComputation sensitivityComputation = Mockito.mock(SensitivityComputation.class);
        ComputationManager computationManager = Mockito.mock(ComputationManager.class);

        Mockito.when(sensitivityComputationFactory.create(Mockito.any(), Mockito.any(), Mockito.anyInt())).thenReturn(sensitivityComputation);
        Mockito.when(sensitivityComputation.run(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(CompletableFuture.completedFuture(new SensitivityComputationResults(true, Collections.emptyMap(), "", Collections.emptyList())));

        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);
        assertTrue(SensitivityComputationService.runSensitivity(Mockito.mock(Network.class), "", Mockito.mock(SensitivityFactorsProvider.class)).isOk());
    }
}
