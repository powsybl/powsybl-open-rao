/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.glsk.import_.GlskProvider;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey <sebastien.murgey at rte-france.com>
 */
public class FlowbasedComputationTest {

    private Network network;

    private Crac crac;

    private GlskProvider glsk;

    @Before
    public void setUp() {
        network = Mockito.mock(Network.class);
        crac = Mockito.mock(Crac.class);
        glsk = Mockito.mock(GlskProvider.class);
    }

    @Test
    public void testDefaultProvider() {
        // case with only one provider, no need for config
        FlowbasedComputation.Runner defaultFlowBased = FlowbasedComputation.find();
        assertEquals("FlowBasedComputationMock", defaultFlowBased.getName());
        assertEquals("1.0", defaultFlowBased.getVersion());
        FlowbasedComputationResult result = FlowbasedComputation.run(network, crac, glsk);
        assertNotNull(result);
        FlowbasedComputationResult resultAsync = FlowbasedComputation.runAsync(network, crac, glsk).join();
        assertNotNull(resultAsync);
    }
}
