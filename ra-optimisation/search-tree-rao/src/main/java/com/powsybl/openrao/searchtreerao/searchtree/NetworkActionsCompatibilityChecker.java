/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.ElementaryAction;
import com.powsybl.openrao.data.cracapi.networkaction.InjectionSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.PstSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.SwitchPair;
import com.powsybl.openrao.data.cracapi.networkaction.TopologicalAction;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class NetworkActionsCompatibilityChecker {
    private NetworkActionsCompatibilityChecker() { }

    public static boolean areNetworkActionsCompatible(NetworkAction networkAction1, NetworkAction networkAction2) {
        for (ElementaryAction elementaryAction1 : networkAction1.getElementaryActions()) {
            for (ElementaryAction elementaryAction2 : networkAction2.getElementaryActions()) {
                if (!areElementaryActionsCompatible(elementaryAction1, elementaryAction2)) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean areElementaryActionsCompatible(ElementaryAction elementaryAction1, ElementaryAction elementaryAction2) {
        if (elementaryAction1 instanceof TopologicalAction && elementaryAction2 instanceof SwitchPair) {
            return areTopologicalActionAndSwitchPairCompatible((TopologicalAction) elementaryAction1, (SwitchPair) elementaryAction2);
        } else if (elementaryAction1 instanceof SwitchPair && elementaryAction2 instanceof TopologicalAction) {
            return areTopologicalActionAndSwitchPairCompatible((TopologicalAction) elementaryAction2, (SwitchPair) elementaryAction1);
        } else if (elementaryAction1.getClass() != elementaryAction2.getClass()) {
            return true;
        } else if (elementaryAction1 instanceof TopologicalAction) {
            return areTopologicalActionsCompatible((TopologicalAction) elementaryAction1, (TopologicalAction) elementaryAction2);
        } else if (elementaryAction1 instanceof PstSetpoint) {
            return arePstSetpointsCompatible((PstSetpoint) elementaryAction1, (PstSetpoint) elementaryAction2);
        } else if (elementaryAction1 instanceof InjectionSetpoint) {
            return areInjectionSetpointsCompatible((InjectionSetpoint) elementaryAction1, (InjectionSetpoint) elementaryAction2);
        } else if (elementaryAction1 instanceof SwitchPair) {
            return areSwitchPairsCompatible((SwitchPair) elementaryAction1, (SwitchPair) elementaryAction2);
        } else {
            throw new OpenRaoException("Unsupported network action type: " + elementaryAction1.getClass().getName());
        }
    }

    private static boolean areTopologicalActionAndSwitchPairCompatible(TopologicalAction topologicalAction, SwitchPair switchPair) {
        return ActionType.OPEN.equals(topologicalAction.getActionType()) && topologicalAction.getNetworkElement().equals(switchPair.getSwitchToOpen())
            || ActionType.CLOSE.equals(topologicalAction.getActionType()) && topologicalAction.getNetworkElement().equals(switchPair.getSwitchToClose());
    }

    private static boolean areTopologicalActionsCompatible(TopologicalAction elementaryAction1, TopologicalAction elementaryAction2) {
        NetworkElement switch1 = elementaryAction1.getNetworkElement();
        NetworkElement switch2 = elementaryAction2.getNetworkElement();
        ActionType actionType1 = elementaryAction1.getActionType();
        ActionType actionType2 = elementaryAction2.getActionType();
        return !switch1.equals(switch2) || actionType1.equals(actionType2);
    }

    private static boolean arePstSetpointsCompatible(PstSetpoint elementaryAction1, PstSetpoint elementaryAction2) {
        NetworkElement pst1 = elementaryAction1.getNetworkElement();
        NetworkElement pst2 = elementaryAction2.getNetworkElement();
        double setpoint1 = elementaryAction1.getSetpoint();
        double setpoint2 = elementaryAction2.getSetpoint();
        return !pst1.equals(pst2) || setpoint1 == setpoint2;
    }

    private static boolean areInjectionSetpointsCompatible(InjectionSetpoint elementaryAction1, InjectionSetpoint elementaryAction2) {
        NetworkElement networkElement1 = elementaryAction1.getNetworkElement();
        NetworkElement networkElement2 = elementaryAction2.getNetworkElement();
        double setpoint1 = elementaryAction1.getSetpoint();
        double setpoint2 = elementaryAction2.getSetpoint();
        return !networkElement1.equals(networkElement2) || setpoint1 == setpoint2;
    }

    private static boolean areSwitchPairsCompatible(SwitchPair elementaryAction1, SwitchPair elementaryAction2) {
        NetworkElement switchToOpen1 = elementaryAction1.getSwitchToOpen();
        NetworkElement switchToClose1 = elementaryAction1.getSwitchToClose();
        NetworkElement switchToOpen2 = elementaryAction2.getSwitchToOpen();
        NetworkElement switchToClose2 = elementaryAction2.getSwitchToClose();
        return !switchToOpen1.equals(switchToOpen2) && !switchToOpen1.equals(switchToClose2) && !switchToClose1.equals(switchToClose2) && !switchToClose1.equals(switchToOpen2)
            || switchToOpen1.equals(switchToOpen2) && switchToClose1.equals(switchToClose2);
    }

    public static Set<NetworkAction> filterOutIncompatibleRemedialActions(Set<NetworkAction> appliedNetworkActions, Set<NetworkAction> availableRemedialActions) {
        Set<NetworkAction> compatibleNetworkActions = new HashSet<>();
        for (NetworkAction availableRemedialAction : availableRemedialActions) {
            if (appliedNetworkActions.stream().allMatch(networkAction -> areNetworkActionsCompatible(networkAction, availableRemedialAction))) {
                compatibleNetworkActions.add(availableRemedialAction);
            }
        }
        return compatibleNetworkActions;
    }
}
