/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.flowbased_computation.glsk_provider.CimGlskValuesProvider;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * FlowBased Computation Factory Impl Test
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public class FlowBasedComputationFactoryImplTest {

    private Network network;
    private CracFile cracFile;
    private Instant instant;
    private CimGlskValuesProvider cimGlskValuesProvider;
    private ComputationManager computationManager;

    private FlowBasedComputationFactoryImpl flowBasedComputationFactoryImpl;
    private FlowBasedComputation flowBasedComputation;

    @Before
    public void setup() {
        network = Mockito.mock(Network.class);
        cracFile = Mockito.mock(CracFile.class);
        instant = Instant.parse("2018-08-28T22:00:00Z");
        cimGlskValuesProvider = Mockito.mock(CimGlskValuesProvider.class);
        computationManager = Mockito.mock(ComputationManager.class);

        flowBasedComputationFactoryImpl = Mockito.mock(FlowBasedComputationFactoryImpl.class);
        flowBasedComputation = Mockito.mock(FlowBasedComputation.class);
        when(flowBasedComputationFactoryImpl.create(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(flowBasedComputation);
    }

    @Test (expected = com.powsybl.commons.PowsyblException.class)
    public void runTest() {
        ComputationManager computationManager = Mockito.mock(ComputationManager.class);
        new FlowBasedComputationFactoryImpl().create(network, cracFile, cimGlskValuesProvider, instant, computationManager, 0);
    }

    @Test (expected = com.powsybl.commons.PowsyblException.class)
    public void runTestBis() {
        new FlowBasedComputationFactoryImpl().create(network, cracFile, computationManager, 0);
    }

    @Test
    public void runTestTre() {
        flowBasedComputation = flowBasedComputationFactoryImpl.create(network, cracFile, computationManager, 0);
    }

}
