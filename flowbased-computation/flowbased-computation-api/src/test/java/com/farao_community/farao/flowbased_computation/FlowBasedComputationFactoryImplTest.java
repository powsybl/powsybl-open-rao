/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.data.crac_file.CracFile;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Instant;

/**
 * FlowBased Computation Factory Impl Test
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public class FlowBasedComputationFactoryImplTest {

    private FlowBasedComputationFactoryImpl flowBasedComputationFactoryMock;
    private Network network;
    private CracFile cracFile;
    private FlowBasedGlskValuesProvider flowBasedGlskValuesProvider;
    private ComputationManager computationManager;

    @Before
    public void setup() {
        flowBasedComputationFactoryMock = Mockito.mock(FlowBasedComputationFactoryImpl.class);
        network = Mockito.mock(Network.class);
        cracFile = Mockito.mock(CracFile.class);
        flowBasedGlskValuesProvider = Mockito.mock(FlowBasedGlskValuesProvider.class);
        computationManager = Mockito.mock(ComputationManager.class);
    }

    @Test
    public void runTest() {
        Instant instant = Instant.parse("2018-08-28T22:00:00Z");
        ComputationManager computationManager = Mockito.mock(ComputationManager.class);
        flowBasedComputationFactoryMock.create(network, cracFile, flowBasedGlskValuesProvider, instant, computationManager, 0);
    }

    @Test
    public void runTestBis() {
        flowBasedComputationFactoryMock.create(network, cracFile, computationManager, 0);
    }

}
