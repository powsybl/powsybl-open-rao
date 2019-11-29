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
import com.powsybl.sensitivity.SensitivityComputation;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityFactorsProvider;
import com.farao_community.farao.commons.FaraoException;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class SensitivityComputationServiceTest {

    @Test
    public void testSensitivityComputationService() {
        SensitivityComputationFactory factory = Mockito.mock(SensitivityComputationFactory.class);
        SensitivityComputation sensitivityComputation = Mockito.mock(SensitivityComputation.class);
        SensitivityComputationResults sensitivityComputationResults = Mockito.mock(SensitivityComputationResults.class);

        when(factory.create(any(Network.class), any(ComputationManager.class), anyInt())).thenReturn(sensitivityComputation);
        when(sensitivityComputation.run(any(SensitivityFactorsProvider.class), anyString(), any(SensitivityComputationParameters.class))).thenReturn(CompletableFuture.completedFuture(sensitivityComputationResults));

        SensitivityComputationService service = new SensitivityComputationService(factory, Mockito.mock(ComputationManager.class));
        SensitivityComputationResults result = service.compute(Mockito.mock(SensitivityFactorsProvider.class), Mockito.mock(Network.class), "", Mockito.mock(FullLineDecompositionParameters.class, RETURNS_DEEP_STUBS));
        assertNotNull(result);
        assertEquals(sensitivityComputationResults, result);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionInSensitivityComputationService() {
        SensitivityComputationFactory factory = Mockito.mock(SensitivityComputationFactory.class);
        SensitivityComputation sensitivityComputation = Mockito.mock(SensitivityComputation.class);

        when(factory.create(any(Network.class), any(ComputationManager.class), anyInt())).thenReturn(sensitivityComputation);
        when(sensitivityComputation.run(any(SensitivityFactorsProvider.class), anyString(), any(SensitivityComputationParameters.class))).thenThrow(PowsyblException.class);

        SensitivityComputationService service = new SensitivityComputationService(factory, Mockito.mock(ComputationManager.class));
        service.compute(Mockito.mock(SensitivityFactorsProvider.class), Mockito.mock(Network.class), "", Mockito.mock(FullLineDecompositionParameters.class, RETURNS_DEEP_STUBS));
    }
}
