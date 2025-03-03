/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.flowbasedcomputation;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class FlowbasedComputationTest {

    private Network network;

    private Crac crac;

    private ZonalData<SensitivityVariableSet> glsk;

    @BeforeEach
    public void setUp() {
        network = Mockito.mock(Network.class);
        crac = Mockito.mock(Crac.class);
        glsk = glskProvider();
    }

    @Test
    void testDefaultProvider() {
        // case with only one provider, no need for config
        FlowbasedComputation.Runner defaultFlowBased = FlowbasedComputation.find();
        assertEquals("FlowBasedComputationMock", defaultFlowBased.getName());
        assertEquals("1.0", defaultFlowBased.getVersion());
        FlowbasedComputationResult result = FlowbasedComputation.run(network, crac, glsk, null);
        assertNotNull(result);
        FlowbasedComputationResult resultAsync = FlowbasedComputation.runAsync(network, crac, glsk).join();
        assertNotNull(resultAsync);
    }

    static ZonalData<SensitivityVariableSet> glskProvider() {
        return new ZonalDataImpl<>(new HashMap<>());
    }
}
