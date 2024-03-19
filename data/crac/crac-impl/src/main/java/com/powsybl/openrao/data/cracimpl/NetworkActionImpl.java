/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.iidm.network.*;
import com.powsybl.openrao.data.cracapi.networkaction.*;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.usagerule.UsageRule;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashSet;
import java.util.Set;

/**
 * Group of simple elementary remedial actions (setpoint, open/close, ...).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NetworkActionImpl extends AbstractRemedialAction<NetworkAction> implements NetworkAction {

    private static final double EPSILON = 0.1;
    private final Set<ElementaryAction> elementaryActions;
    private final Set<NetworkElement> networkElements;

    NetworkActionImpl(String id, String name, String operator, Set<UsageRule> usageRules,
                             Set<ElementaryAction> elementaryNetworkActions, Integer speed, Set<NetworkElement> networkElements) {
        super(id, name, operator, usageRules, speed);
        this.elementaryActions = new HashSet<>(elementaryNetworkActions);
        this.networkElements = new HashSet<>(networkElements);
    }

    public Set<ElementaryAction> getElementaryActions() {
        return elementaryActions;
    }

    private int getNormalizedSetpoint(PhaseTapChanger phaseTapChanger, int setpoint) {
        return ((phaseTapChanger.getLowTapPosition() + phaseTapChanger.getHighTapPosition()) / 2) + setpoint;
    }

    @Override
    public boolean hasImpactOnNetwork(Network network) {
        return elementaryActions.stream().anyMatch(elementaryAction -> {
            if (elementaryAction instanceof InjectionSetpoint injectionSetPoint) {
                return injectionImpact(injectionSetPoint, network);
            } else if (elementaryAction instanceof PstSetpoint pstSetPoint) {
                PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(pstSetPoint.getNetworkElement().getId()).getPhaseTapChanger();
                return getNormalizedSetpoint(phaseTapChanger, pstSetPoint.getSetpoint()) != phaseTapChanger.getTapPosition();
            } else if (elementaryAction instanceof SwitchPair switchPair) {
                return !network.getSwitch(switchPair.getSwitchToOpen().getId()).isOpen() || network.getSwitch(switchPair.getSwitchToClose().getId()).isOpen();
            } else if (elementaryAction instanceof TopologicalAction topologicalAction) {
                return topologicalImpact(topologicalAction, network);
            } else {
                throw new NotImplementedException();
            }
        });
    }

    private static boolean topologicalImpact(TopologicalAction topologicalAction, Network network) {
        Identifiable<?> identifiable = network.getIdentifiable(topologicalAction.getNetworkElement().getId());
        if (identifiable instanceof Branch<?> branch) {
            if (topologicalAction.getActionType() == ActionType.OPEN) {
                // Line is considered closed if both terminal are connected
                return branch.getTerminal1().isConnected() && branch.getTerminal2().isConnected();
            } else {
                // Line is already considered opened if one of the terminals is disconnected
                return !branch.getTerminal1().isConnected() || !branch.getTerminal2().isConnected();
            }
        } else if (identifiable instanceof Switch sw) {
            return sw.isOpen() == (topologicalAction.getActionType() == ActionType.CLOSE);
        } else {
            throw new NotImplementedException("Topological actions are only on branches or switches for now");
        }
    }

    private static boolean injectionImpact(InjectionSetpoint injectionSetPoint, Network network) {
        Identifiable<?> identifiable = network.getIdentifiable(injectionSetPoint.getNetworkElement().getId());
        if (identifiable instanceof Generator generator) {
            return Math.abs(generator.getTargetP() - injectionSetPoint.getSetpoint()) >= EPSILON;
        } else if (identifiable instanceof Load load) {
            return Math.abs(load.getP0() - injectionSetPoint.getSetpoint()) >= EPSILON;
        } else if (identifiable instanceof DanglingLine danglingLine) {
            return Math.abs(danglingLine.getP0() - injectionSetPoint.getSetpoint()) >= EPSILON;
        } else if (identifiable instanceof ShuntCompensator shuntCompensator) {
            return Math.abs(shuntCompensator.getSectionCount() - injectionSetPoint.getSetpoint()) >= EPSILON;
        } else {
            throw new NotImplementedException("Injection setpoint only handled for generators, loads, dangling lines or shunt compensator");
        }
    }

    @Override
    public boolean apply(Network network) {
        if (!canBeApplied(network)) {
            return false;
        } else {
            elementaryActions.forEach(action -> action.apply(network));
            return true;
        }
    }

    @Override
    public boolean canBeApplied(Network network) {
        return elementaryActions.stream().allMatch(elementaryAction -> {
            // can be applied:
            if (elementaryAction instanceof InjectionSetpoint injectionSetPoint) {
                Identifiable<?> identifiable = network.getIdentifiable(injectionSetPoint.getNetworkElement().getId());
                if (identifiable instanceof ShuntCompensator shuntCompensator) {
                    return injectionSetPoint.getSetpoint() <= shuntCompensator.getMaximumSectionCount();
                }
                return true;
            } else if (elementaryAction instanceof PstSetpoint) {
                // TODO : setpoint out of range ?
                return true;
            } else if (elementaryAction instanceof SwitchPair switchPair) {
                // It is only applicable if, initially, one switch was closed and the other was open.
                return network.getSwitch(switchPair.getSwitchToOpen().getId()).isOpen() != network.getSwitch(switchPair.getSwitchToClose().getId()).isOpen();
            } else if (elementaryAction instanceof TopologicalAction) {
                // TODO : always true ?
                return true;
            } else {
                throw new NotImplementedException();
            }
        });
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return this.networkElements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NetworkActionImpl otherNetworkActionImpl = (NetworkActionImpl) o;
        return super.equals(otherNetworkActionImpl)
            && new HashSet<>(elementaryActions).equals(new HashSet<>(otherNetworkActionImpl.elementaryActions));
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

}
