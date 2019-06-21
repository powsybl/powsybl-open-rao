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

    private Network network;
    private CracFile cracFile;
    private Instant instant;
    private FlowBasedGlskValuesProvider flowBasedGlskValuesProvider;
    private ComputationManager computationManager;

    @Before
    public void setup() {
        network = Mockito.mock(Network.class);
        cracFile = Mockito.mock(CracFile.class);
        instant = Instant.parse("2018-08-28T22:00:00Z");
        flowBasedGlskValuesProvider = Mockito.mock(FlowBasedGlskValuesProvider.class);
        computationManager = Mockito.mock(ComputationManager.class);
    }

    @Test (expected = com.powsybl.commons.PowsyblException.class)
    public void runTest() {
        ComputationManager computationManager = Mockito.mock(ComputationManager.class);
        new FlowBasedComputationFactoryImpl().create(network, cracFile, flowBasedGlskValuesProvider, instant, computationManager, 0);
    }

    @Test (expected = com.powsybl.commons.PowsyblException.class)
    public void runTestBis() {
        new FlowBasedComputationFactoryImpl().create(network, cracFile, computationManager, 0);
    }

}
