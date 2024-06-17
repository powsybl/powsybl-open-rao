/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.ElementaryAction;
import com.powsybl.openrao.data.cracapi.networkaction.InjectionSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.PstSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.SwitchPair;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalAction;
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
        NetworkAction alignedPsts = mockNetworkAction(mockPstSetpoint("pst-fr-1", 4), mockPstSetpoint("pst-fr-2", 4), mockPstSetpoint("pst-fr-3", 4));
        NetworkAction switchPairAndPst = mockNetworkAction(mockPstSetpoint("pst-fr-2", -2), mockSwitchPair());

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
        return new NetworkActionUtils.NetworkActionImplTest(Set.of(mockTopologicalAction("switch-fr"), mockTopologicalAction("switch-es"), mockInjectionSetpoint("generator-fr-1", setpoint / 2d), mockInjectionSetpoint("generator-fr-2", setpoint / 2d), mockInjectionSetpoint("generator-es-1", -setpoint / 2d), mockInjectionSetpoint("generator-es-2", -setpoint / 2d)));
    }

    private NetworkAction mockNetworkAction(ElementaryAction... elementaryActions) {
        return new NetworkActionUtils.NetworkActionImplTest(new HashSet<>(List.of(elementaryActions)));
    }

    private TopologicalAction mockTopologicalAction(String switchId) {
        return new NetworkActionUtils.TopologicalActionImplTest(NetworkActionUtils.createNetworkElement(switchId), ActionType.OPEN);
    }

    private InjectionSetpoint mockInjectionSetpoint(String networkElementId, double setpoint) {
        return new NetworkActionUtils.InjectionSetpointImplTest(NetworkActionUtils.createNetworkElement(networkElementId), setpoint, Unit.MEGAWATT);
    }

    private PstSetpoint mockPstSetpoint(String pstId, int setpoint) {
        return new NetworkActionUtils.PstSetpointImplTest(NetworkActionUtils.createNetworkElement(pstId), setpoint);
    }

    private SwitchPair mockSwitchPair() {
        return new NetworkActionUtils.SwitchPairImplTest(NetworkActionUtils.createNetworkElement("switch-fr"), NetworkActionUtils.createNetworkElement("switch-es"));
    }
}
