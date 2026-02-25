/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.powsybl.action.*;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.SwitchPair;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class NetworkActionTest {
    @Test
    void compatibility() {
        NetworkAction hvdcFrEs200Mw = mockHvdcAction(-200d);
        NetworkAction hvdcEsFr200Mw = mockHvdcAction(200d);
        NetworkAction alignedPsts = mockNetworkAction(
            mockPhaseTapChangerTapPositionAction("pst-fr-1", 4),
            mockPhaseTapChangerTapPositionAction("pst-fr-2", 4),
            mockPhaseTapChangerTapPositionAction("pst-fr-3", 4)
        );
        NetworkAction switchPairAndPst = mockNetworkAction(mockPhaseTapChangerTapPositionAction("pst-fr-2", -2), mockSwitchPair());

        assertTrue(hvdcFrEs200Mw.isCompatibleWith(hvdcFrEs200Mw));
        assertFalse(hvdcFrEs200Mw.isCompatibleWith(hvdcEsFr200Mw));
        assertTrue(hvdcFrEs200Mw.isCompatibleWith(alignedPsts));
        assertTrue(hvdcFrEs200Mw.isCompatibleWith(switchPairAndPst));
        assertTrue(hvdcEsFr200Mw.isCompatibleWith(hvdcEsFr200Mw));
        assertTrue(hvdcEsFr200Mw.isCompatibleWith(alignedPsts));
        assertTrue(hvdcEsFr200Mw.isCompatibleWith(switchPairAndPst));
        assertTrue(alignedPsts.isCompatibleWith(alignedPsts));
        assertFalse(alignedPsts.isCompatibleWith(switchPairAndPst));
        assertTrue(switchPairAndPst.isCompatibleWith(switchPairAndPst));
    }

    private NetworkAction mockHvdcAction(double setpoint) {
        return new NetworkActionUtils.NetworkActionImplTest(Set.of(
            mockSwitchActionOpen("switch-fr"),
            mockSwitchActionOpen("switch-es"),
            mockGeneratorAction("generator-fr-1", setpoint / 2d),
            mockGeneratorAction("generator-fr-2", setpoint / 2d),
            mockGeneratorAction("generator-es-1", -setpoint / 2d),
            mockGeneratorAction("generator-es-2", -setpoint / 2d))
        );
    }

    private NetworkAction mockNetworkAction(Action... elementaryActions) {
        return new NetworkActionUtils.NetworkActionImplTest(new HashSet<>(List.of(elementaryActions)));
    }

    private SwitchAction mockSwitchActionOpen(String switchId) {
        return new SwitchActionBuilder().withId("id").withNetworkElementId(switchId).withOpen(true).build();
    }

    private GeneratorAction mockGeneratorAction(String networkElementId, double setpoint) {
        return new GeneratorActionBuilder().withId("id").withGeneratorId(networkElementId).withActivePowerValue(setpoint).withActivePowerRelativeValue(false).build();
    }

    private PhaseTapChangerTapPositionAction mockPhaseTapChangerTapPositionAction(String pstId, int setpoint) {
        return new PhaseTapChangerTapPositionActionBuilder().withId("id").withNetworkElementId(pstId).withTapPosition(setpoint).withRelativeValue(false).build();
    }

    private SwitchPair mockSwitchPair() {
        return new NetworkActionUtils.SwitchPairImplTest(NetworkActionUtils.createNetworkElement("switch-fr"), NetworkActionUtils.createNetworkElement("switch-es"));
    }
}
