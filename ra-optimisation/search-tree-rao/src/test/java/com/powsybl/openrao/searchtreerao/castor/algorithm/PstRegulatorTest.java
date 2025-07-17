/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class PstRegulatorTest {
    private final Network network = Network.read("/network/2Nodes2ParallelLinesPST.uct", PstRegulatorTest.class.getResourceAsStream("/network/2Nodes2ParallelLinesPST.uct"));
    private final LoadFlowParameters loadFlowParameters = new LoadFlowParameters();

    @Test
    void testRegulationWithOnePst() {
        // TODO: add CRAC
        PstRangeAction pstRangeAction = null;
        Map<PstRangeAction, Integer> regulatedTapPerPst = PstRegulator.regulatePsts(network, Set.of(pstRangeAction), loadFlowParameters);
        assertEquals(Map.of(pstRangeAction, 1), regulatedTapPerPst);
    }
}
