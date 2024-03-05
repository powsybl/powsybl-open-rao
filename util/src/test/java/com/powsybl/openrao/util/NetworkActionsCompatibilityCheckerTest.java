/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.util;

import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.InjectionSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.PstSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.SwitchPair;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class NetworkActionsCompatibilityCheckerTest {

    private final NetworkElement switch1 = Mockito.mock(NetworkElement.class);
    private final NetworkElement switch2 = Mockito.mock(NetworkElement.class);
    private final NetworkElement switch3 = Mockito.mock(NetworkElement.class);
    private final NetworkElement switch4 = Mockito.mock(NetworkElement.class);
    private final NetworkElement pst1 = Mockito.mock(NetworkElement.class);
    private final NetworkElement pst2 = Mockito.mock(NetworkElement.class);
    private final NetworkElement generator1 = Mockito.mock(NetworkElement.class);
    private final NetworkElement generator2 = Mockito.mock(NetworkElement.class);

    @BeforeEach
    void setUp() {
        Mockito.when(switch1.getId()).thenReturn("switch-1");
        Mockito.when(switch2.getId()).thenReturn("switch-2");
        Mockito.when(switch3.getId()).thenReturn("switch-3");
        Mockito.when(switch4.getId()).thenReturn("switch-4");
        Mockito.when(pst1.getId()).thenReturn("pst-1");
        Mockito.when(pst2.getId()).thenReturn("pst-2");
        Mockito.when(generator1.getId()).thenReturn("generator-1");
        Mockito.when(generator2.getId()).thenReturn("generator-2");
    }

    @Test
    void topologicalActionsCompatibility() {
        TopologicalAction topologicalAction1 = Mockito.mock(TopologicalAction.class);
        TopologicalAction topologicalAction2 = Mockito.mock(TopologicalAction.class);
        // Different switches
        Mockito.when(topologicalAction1.getNetworkElement()).thenReturn(switch1);
        Mockito.when(topologicalAction1.getActionType()).thenReturn(ActionType.OPEN);
        Mockito.when(topologicalAction2.getNetworkElement()).thenReturn(switch2);
        Mockito.when(topologicalAction2.getActionType()).thenReturn(ActionType.CLOSE);
        assertTrue(NetworkActionsCompatibilityChecker.areElementaryActionsCompatible(topologicalAction1, topologicalAction2));
        // Same switch, different action
        Mockito.when(topologicalAction1.getNetworkElement()).thenReturn(switch1);
        Mockito.when(topologicalAction1.getActionType()).thenReturn(ActionType.OPEN);
        Mockito.when(topologicalAction2.getNetworkElement()).thenReturn(switch1);
        Mockito.when(topologicalAction2.getActionType()).thenReturn(ActionType.CLOSE);
        assertFalse(NetworkActionsCompatibilityChecker.areElementaryActionsCompatible(topologicalAction1, topologicalAction2));
        // Same switch, same action type
        Mockito.when(topologicalAction1.getNetworkElement()).thenReturn(switch1);
        Mockito.when(topologicalAction1.getActionType()).thenReturn(ActionType.OPEN);
        Mockito.when(topologicalAction2.getNetworkElement()).thenReturn(switch1);
        Mockito.when(topologicalAction2.getActionType()).thenReturn(ActionType.OPEN);
        assertTrue(NetworkActionsCompatibilityChecker.areElementaryActionsCompatible(topologicalAction1, topologicalAction2));
    }

    @Test
    void pstSetpointsCompatibility() {
        PstSetpoint pstSetpoint1 = Mockito.mock(PstSetpoint.class);
        PstSetpoint pstSetpoint2 = Mockito.mock(PstSetpoint.class);
        // Different PSTs
        Mockito.when(pstSetpoint1.getNetworkElement()).thenReturn(pst1);
        Mockito.when(pstSetpoint1.getSetpoint()).thenReturn(1);
        Mockito.when(pstSetpoint2.getNetworkElement()).thenReturn(pst2);
        Mockito.when(pstSetpoint2.getSetpoint()).thenReturn(2);
        assertTrue(NetworkActionsCompatibilityChecker.areElementaryActionsCompatible(pstSetpoint1, pstSetpoint2));
        // Same PST, different setpoints
        Mockito.when(pstSetpoint1.getNetworkElement()).thenReturn(pst1);
        Mockito.when(pstSetpoint1.getSetpoint()).thenReturn(1);
        Mockito.when(pstSetpoint2.getNetworkElement()).thenReturn(pst1);
        Mockito.when(pstSetpoint2.getSetpoint()).thenReturn(2);
        assertFalse(NetworkActionsCompatibilityChecker.areElementaryActionsCompatible(pstSetpoint1, pstSetpoint2));
        // Same PST, same setpoint
        Mockito.when(pstSetpoint1.getNetworkElement()).thenReturn(pst1);
        Mockito.when(pstSetpoint1.getSetpoint()).thenReturn(1);
        Mockito.when(pstSetpoint2.getNetworkElement()).thenReturn(pst1);
        Mockito.when(pstSetpoint2.getSetpoint()).thenReturn(1);
        assertTrue(NetworkActionsCompatibilityChecker.areElementaryActionsCompatible(pstSetpoint1, pstSetpoint2));
    }

    @Test
    void injectionSetpointsCompatibility() {
        InjectionSetpoint injectionSetpoint1 = Mockito.mock(InjectionSetpoint.class);
        InjectionSetpoint injectionSetpoint2 = Mockito.mock(InjectionSetpoint.class);
        // Different generators
        Mockito.when(injectionSetpoint1.getNetworkElement()).thenReturn(generator1);
        Mockito.when(injectionSetpoint1.getSetpoint()).thenReturn(100d);
        Mockito.when(injectionSetpoint2.getNetworkElement()).thenReturn(generator2);
        Mockito.when(injectionSetpoint2.getSetpoint()).thenReturn(75d);
        assertTrue(NetworkActionsCompatibilityChecker.areElementaryActionsCompatible(injectionSetpoint1, injectionSetpoint2));
        // Same generator, different setpoints
        Mockito.when(injectionSetpoint1.getNetworkElement()).thenReturn(generator1);
        Mockito.when(injectionSetpoint1.getSetpoint()).thenReturn(100d);
        Mockito.when(injectionSetpoint2.getNetworkElement()).thenReturn(generator1);
        Mockito.when(injectionSetpoint2.getSetpoint()).thenReturn(75d);
        assertFalse(NetworkActionsCompatibilityChecker.areElementaryActionsCompatible(injectionSetpoint1, injectionSetpoint2));
        // Same generator, same setpoint
        Mockito.when(injectionSetpoint1.getNetworkElement()).thenReturn(generator1);
        Mockito.when(injectionSetpoint1.getSetpoint()).thenReturn(100d);
        Mockito.when(injectionSetpoint2.getNetworkElement()).thenReturn(generator1);
        Mockito.when(injectionSetpoint2.getSetpoint()).thenReturn(100d);
        assertTrue(NetworkActionsCompatibilityChecker.areElementaryActionsCompatible(injectionSetpoint1, injectionSetpoint2));
    }

    @Test
    void switchPairsCompatibility() {
        SwitchPair switchPair1 = Mockito.mock(SwitchPair.class);
        SwitchPair switchPair2 = Mockito.mock(SwitchPair.class);
        // Completely different switches
        Mockito.when(switchPair1.getSwitchToOpen()).thenReturn(switch1);
        Mockito.when(switchPair1.getSwitchToClose()).thenReturn(switch2);
        Mockito.when(switchPair2.getSwitchToOpen()).thenReturn(switch3);
        Mockito.when(switchPair2.getSwitchToClose()).thenReturn(switch4);
        assertTrue(NetworkActionsCompatibilityChecker.areElementaryActionsCompatible(switchPair1, switchPair2));
        // Common switches, different roles
        Mockito.when(switchPair1.getSwitchToOpen()).thenReturn(switch1);
        Mockito.when(switchPair1.getSwitchToClose()).thenReturn(switch2);
        Mockito.when(switchPair2.getSwitchToOpen()).thenReturn(switch3);
        Mockito.when(switchPair2.getSwitchToClose()).thenReturn(switch1);
        assertFalse(NetworkActionsCompatibilityChecker.areElementaryActionsCompatible(switchPair1, switchPair2));
        // Same switches, same roles
        Mockito.when(switchPair1.getSwitchToOpen()).thenReturn(switch1);
        Mockito.when(switchPair1.getSwitchToClose()).thenReturn(switch2);
        Mockito.when(switchPair2.getSwitchToOpen()).thenReturn(switch1);
        Mockito.when(switchPair2.getSwitchToClose()).thenReturn(switch2);
        assertTrue(NetworkActionsCompatibilityChecker.areElementaryActionsCompatible(switchPair1, switchPair2));
    }

    @Test
    void topologicalActionAndSwitchCompatibility() {
        TopologicalAction topologicalAction = Mockito.mock(TopologicalAction.class);
        SwitchPair switchPair = Mockito.mock(SwitchPair.class);
        // Compatible closing switches
        Mockito.when(topologicalAction.getNetworkElement()).thenReturn(switch2);
        Mockito.when(topologicalAction.getActionType()).thenReturn(ActionType.CLOSE);
        Mockito.when(switchPair.getSwitchToOpen()).thenReturn(switch1);
        Mockito.when(switchPair.getSwitchToClose()).thenReturn(switch2);
        assertTrue(NetworkActionsCompatibilityChecker.areElementaryActionsCompatible(topologicalAction, switchPair));
        // Incompatible closing switches
        Mockito.when(topologicalAction.getNetworkElement()).thenReturn(switch1);
        Mockito.when(topologicalAction.getActionType()).thenReturn(ActionType.CLOSE);
        Mockito.when(switchPair.getSwitchToOpen()).thenReturn(switch1);
        Mockito.when(switchPair.getSwitchToClose()).thenReturn(switch2);
        assertFalse(NetworkActionsCompatibilityChecker.areElementaryActionsCompatible(topologicalAction, switchPair));
        // Compatible opening switches
        Mockito.when(topologicalAction.getNetworkElement()).thenReturn(switch1);
        Mockito.when(topologicalAction.getActionType()).thenReturn(ActionType.OPEN);
        Mockito.when(switchPair.getSwitchToOpen()).thenReturn(switch1);
        Mockito.when(switchPair.getSwitchToClose()).thenReturn(switch2);
        assertTrue(NetworkActionsCompatibilityChecker.areElementaryActionsCompatible(topologicalAction, switchPair));
        // Incompatible opening switches
        Mockito.when(topologicalAction.getNetworkElement()).thenReturn(switch1);
        Mockito.when(topologicalAction.getActionType()).thenReturn(ActionType.OPEN);
        Mockito.when(switchPair.getSwitchToOpen()).thenReturn(switch2);
        Mockito.when(switchPair.getSwitchToClose()).thenReturn(switch1);
        assertFalse(NetworkActionsCompatibilityChecker.areElementaryActionsCompatible(topologicalAction, switchPair));
    }

    @Test
    void compatibleNetworkActions() {
        NetworkAction networkAction1 = Mockito.mock(NetworkAction.class);
        NetworkAction networkAction2 = Mockito.mock(NetworkAction.class);

        TopologicalAction topologicalAction1 = Mockito.mock(TopologicalAction.class);
        Mockito.when(topologicalAction1.getNetworkElement()).thenReturn(switch1);
        Mockito.when(topologicalAction1.getActionType()).thenReturn(ActionType.OPEN);

        TopologicalAction topologicalAction2 = Mockito.mock(TopologicalAction.class);
        Mockito.when(topologicalAction2.getNetworkElement()).thenReturn(switch2);
        Mockito.when(topologicalAction2.getActionType()).thenReturn(ActionType.CLOSE);

        TopologicalAction topologicalAction3 = Mockito.mock(TopologicalAction.class);
        Mockito.when(topologicalAction3.getNetworkElement()).thenReturn(switch2);
        Mockito.when(topologicalAction3.getActionType()).thenReturn(ActionType.OPEN);

        InjectionSetpoint injectionSetpoint1 = Mockito.mock(InjectionSetpoint.class);
        Mockito.when(injectionSetpoint1.getNetworkElement()).thenReturn(generator1);
        Mockito.when(injectionSetpoint1.getSetpoint()).thenReturn(100d);

        InjectionSetpoint injectionSetpoint2 = Mockito.mock(InjectionSetpoint.class);
        Mockito.when(injectionSetpoint2.getNetworkElement()).thenReturn(generator2);
        Mockito.when(injectionSetpoint2.getSetpoint()).thenReturn(75d);

        // Completely separated network actions
        Mockito.when(networkAction1.getElementaryActions()).thenReturn(Set.of(topologicalAction1, injectionSetpoint1));
        Mockito.when(networkAction2.getElementaryActions()).thenReturn(Set.of(topologicalAction2, injectionSetpoint2));
        assertTrue(NetworkActionsCompatibilityChecker.areNetworkActionsCompatible(networkAction1, networkAction2));
        // One common elementary action
        Mockito.when(networkAction1.getElementaryActions()).thenReturn(Set.of(topologicalAction1, topologicalAction2, injectionSetpoint1));
        Mockito.when(networkAction2.getElementaryActions()).thenReturn(Set.of(topologicalAction2, injectionSetpoint2));
        assertTrue(NetworkActionsCompatibilityChecker.areNetworkActionsCompatible(networkAction1, networkAction2));
        // Conflictual elementary actions
        Mockito.when(networkAction1.getElementaryActions()).thenReturn(Set.of(injectionSetpoint1, topologicalAction2));
        Mockito.when(networkAction2.getElementaryActions()).thenReturn(Set.of(topologicalAction3, injectionSetpoint2));
        assertFalse(NetworkActionsCompatibilityChecker.areNetworkActionsCompatible(networkAction1, networkAction2));
    }

    @Test
    void filterOutIncompatibleRemedialActions() {
        TopologicalAction topologicalAction1 = Mockito.mock(TopologicalAction.class);
        Mockito.when(topologicalAction1.getNetworkElement()).thenReturn(switch1);
        Mockito.when(topologicalAction1.getActionType()).thenReturn(ActionType.OPEN);

        TopologicalAction topologicalAction2 = Mockito.mock(TopologicalAction.class);
        Mockito.when(topologicalAction2.getNetworkElement()).thenReturn(switch2);
        Mockito.when(topologicalAction2.getActionType()).thenReturn(ActionType.CLOSE);

        TopologicalAction topologicalAction3 = Mockito.mock(TopologicalAction.class);
        Mockito.when(topologicalAction3.getNetworkElement()).thenReturn(switch2);
        Mockito.when(topologicalAction3.getActionType()).thenReturn(ActionType.OPEN);

        TopologicalAction topologicalAction4 = Mockito.mock(TopologicalAction.class);
        Mockito.when(topologicalAction4.getNetworkElement()).thenReturn(switch3);
        Mockito.when(topologicalAction4.getActionType()).thenReturn(ActionType.CLOSE);

        InjectionSetpoint injectionSetpoint1 = Mockito.mock(InjectionSetpoint.class);
        Mockito.when(injectionSetpoint1.getNetworkElement()).thenReturn(generator1);
        Mockito.when(injectionSetpoint1.getSetpoint()).thenReturn(100d);

        InjectionSetpoint injectionSetpoint2 = Mockito.mock(InjectionSetpoint.class);
        Mockito.when(injectionSetpoint2.getNetworkElement()).thenReturn(generator2);
        Mockito.when(injectionSetpoint2.getSetpoint()).thenReturn(75d);

        PstSetpoint pstSetpoint = Mockito.mock(PstSetpoint.class);
        Mockito.when(pstSetpoint.getNetworkElement()).thenReturn(pst1);
        Mockito.when(pstSetpoint.getSetpoint()).thenReturn(1);

        SwitchPair switchPair = Mockito.mock(SwitchPair.class);
        Mockito.when(switchPair.getSwitchToOpen()).thenReturn(switch2);
        Mockito.when(switchPair.getSwitchToClose()).thenReturn(switch1);

        NetworkAction appliedRemedialAction1 = Mockito.mock(NetworkAction.class);
        Mockito.when(appliedRemedialAction1.getElementaryActions()).thenReturn(Set.of(topologicalAction1));

        NetworkAction appliedRemedialAction2 = Mockito.mock(NetworkAction.class);
        Mockito.when(appliedRemedialAction2.getElementaryActions()).thenReturn(Set.of(topologicalAction2, injectionSetpoint1));

        NetworkAction availableRemedialAction1 = Mockito.mock(NetworkAction.class);
        Mockito.when(availableRemedialAction1.getElementaryActions()).thenReturn(Set.of(topologicalAction3, pstSetpoint));

        NetworkAction availableRemedialAction2 = Mockito.mock(NetworkAction.class);
        Mockito.when(availableRemedialAction2.getElementaryActions()).thenReturn(Set.of(injectionSetpoint2));

        NetworkAction availableRemedialAction3 = Mockito.mock(NetworkAction.class);
        Mockito.when(availableRemedialAction3.getElementaryActions()).thenReturn(Set.of(injectionSetpoint1, topologicalAction4));

        NetworkAction availableRemedialAction4 = Mockito.mock(NetworkAction.class);
        Mockito.when(availableRemedialAction4.getElementaryActions()).thenReturn(Set.of(switchPair));

        assertEquals(
            Set.of(availableRemedialAction2, availableRemedialAction3),
            NetworkActionsCompatibilityChecker.filterOutIncompatibleRemedialActions(
                Set.of(appliedRemedialAction1, appliedRemedialAction2),
                Set.of(availableRemedialAction1, availableRemedialAction2, availableRemedialAction3, availableRemedialAction4)
            )
        );
    }
}
