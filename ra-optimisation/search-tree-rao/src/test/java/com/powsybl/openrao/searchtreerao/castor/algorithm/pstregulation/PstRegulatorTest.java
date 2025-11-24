/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm.pstregulation;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class PstRegulatorTest {
    private Network network;
    private final LoadFlowParameters loadFlowParameters = new LoadFlowParameters();

    @BeforeEach
    void setUp() {
        network = Network.read("/network/2Nodes2ParallelLinesPST.uct", PstRegulatorTest.class.getResourceAsStream("/network/2Nodes2ParallelLinesPST.uct"));
        loadFlowParameters.setPhaseShifterRegulationOn(true);
        OpenLoadFlowParameters openLoadFlowParameters = new OpenLoadFlowParameters();
        openLoadFlowParameters.setMaxOuterLoopIterations(1000);
        loadFlowParameters.addExtension(OpenLoadFlowParameters.class, openLoadFlowParameters);
    }

    @Test
    void testRegulationWithOnePst() {
        NetworkElement networkElement = Mockito.mock(NetworkElement.class);
        Mockito.when(networkElement.getId()).thenReturn("BBE1AA1  FFR1AA1  2");
        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        Mockito.when(pstRangeAction.getNetworkElement()).thenReturn(networkElement);
        Map<PstRangeAction, Integer> regulatedTapPerPst = PstRegulator.regulatePsts(Set.of(new ElementaryPstRegulationInput(pstRangeAction, TwoSides.ONE, 500.0)), network, loadFlowParameters);
        // PATL of PST is 500 A; tap must be in range [3; 15]
        assertEquals(Map.of(pstRangeAction, 3), regulatedTapPerPst);
    }

    @Test
    void testRegulationWithOneAlreadySecurePst() {
        // PST is moved to tap 8 and is thus secure
        network.getTwoWindingsTransformer("BBE1AA1  FFR1AA1  2").getPhaseTapChanger().setTapPosition(8);
        NetworkElement networkElement = Mockito.mock(NetworkElement.class);
        Mockito.when(networkElement.getId()).thenReturn("BBE1AA1  FFR1AA1  2");
        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        Mockito.when(pstRangeAction.getNetworkElement()).thenReturn(networkElement);
        Map<PstRangeAction, Integer> regulatedTapPerPst = PstRegulator.regulatePsts(Set.of(new ElementaryPstRegulationInput(pstRangeAction, TwoSides.ONE, 500.0)), network, loadFlowParameters);
        // PATL of PST is 500 A; tap must be in range [3; 15]
        assertEquals(Map.of(pstRangeAction, 8), regulatedTapPerPst);
    }
}
