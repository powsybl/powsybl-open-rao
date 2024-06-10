/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.*;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.data.cracapi.networkaction.*;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.usagerule.UsageRule;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashSet;
import java.util.Set;

/**
 * Group of simple elementary remedial actions.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NetworkActionImpl extends AbstractRemedialAction<NetworkAction> implements NetworkAction {

    private static final double EPSILON = 0.1;
    private final Set<Action> elementaryActions;
    private final Set<NetworkElement> networkElements;

    NetworkActionImpl(String id, String name, String operator, Set<UsageRule> usageRules,
                      Set<Action> elementaryNetworkActions, Integer speed, Set<NetworkElement> networkElements) {
        super(id, name, operator, usageRules, speed);
        this.elementaryActions = new HashSet<>(elementaryNetworkActions);
        this.networkElements = new HashSet<>(networkElements);
    }

    @Override
    public Set<Action> getElementaryActions() {
        return elementaryActions;
    }

    @Override
    public boolean hasImpactOnNetwork(Network network) {
        return elementaryActions.stream().anyMatch(elementaryAction -> {
            if (elementaryAction instanceof GeneratorAction generatorAction) {
                Generator generator = network.getGenerator(generatorAction.getGeneratorId());
                return Math.abs(generator.getTargetP() - generatorAction.getActivePowerValue().getAsDouble()) >= EPSILON;
            } else if (elementaryAction instanceof LoadAction loadAction) {
                Load load = network.getLoad(loadAction.getLoadId());
                return Math.abs(load.getP0() - loadAction.getActivePowerValue().getAsDouble()) >= EPSILON;
            } else if (elementaryAction instanceof DanglingLineAction danglingLineAction) {
                DanglingLine danglingLine = network.getDanglingLine(danglingLineAction.getDanglingLineId());
                return Math.abs(danglingLine.getP0() - danglingLineAction.getActivePowerValue().getAsDouble()) >= EPSILON;
            } else if (elementaryAction instanceof ShuntCompensatorPositionAction shuntCompensatorPositionAction) {
                ShuntCompensator shuntCompensator = network.getShuntCompensator(shuntCompensatorPositionAction.getShuntCompensatorId());
                return Math.abs(shuntCompensator.getSectionCount() - shuntCompensatorPositionAction.getSectionCount()) > 0;
            } else if (elementaryAction instanceof PhaseTapChangerTapPositionAction phaseTapChangerTapPositionAction) {
                PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(phaseTapChangerTapPositionAction.getTransformerId()).getPhaseTapChanger();
                return phaseTapChangerTapPositionAction.getTapPosition() != phaseTapChanger.getTapPosition();
            } else if (elementaryAction instanceof SwitchPair switchPair) {
                return !network.getSwitch(switchPair.getSwitchToOpen().getId()).isOpen() || network.getSwitch(switchPair.getSwitchToClose().getId()).isOpen();
            } else if (elementaryAction instanceof TerminalsConnectionAction terminalsConnectionAction) {
                Connectable<?> connectable = network.getConnectable(terminalsConnectionAction.getElementId());
                if (terminalsConnectionAction.isOpen()) {
                    // Connectable is considered closed if all terminals are connected
                    return connectable.getTerminals().stream().allMatch(Terminal::isConnected);
                } else {
                    // Connectable is already considered opened if one of the terminals is disconnected
                    return !connectable.getTerminals().stream().allMatch(Terminal::isConnected);
                }
            } else if (elementaryAction instanceof SwitchAction switchAction) {
                Switch aSwitch = network.getSwitch(switchAction.getSwitchId());
                return aSwitch.isOpen() != switchAction.isOpen();
            } else {
                throw new NotImplementedException();
            }
        });
    }

    @Override
    public boolean apply(Network network) {
        if (!canBeApplied(network)) {
            return false;
        } else {
            elementaryActions.forEach(action -> action.toModification().apply(network, true, ReportNode.NO_OP));
            return true;
        }
    }

    @Override
    public boolean canBeApplied(Network network) {
        // TODO: To implement on powsybl-core Action
        return elementaryActions.stream().allMatch(elementaryAction -> {
            if (elementaryAction instanceof ShuntCompensatorPositionAction shuntCompensatorPositionAction) {
                ShuntCompensator shuntCompensator = network.getShuntCompensator(shuntCompensatorPositionAction.getShuntCompensatorId());
                return shuntCompensatorPositionAction.getSectionCount() <= shuntCompensator.getMaximumSectionCount();
            } else if (elementaryAction instanceof GeneratorAction || elementaryAction instanceof LoadAction || elementaryAction instanceof DanglingLineAction) {
                return true;
            } else if (elementaryAction instanceof PhaseTapChangerTapPositionAction) {
                // TODO : setpoint out of range ?
                return true;
            } else if (elementaryAction instanceof SwitchPair switchPair) {
                // It is only applicable if, initially, one switch was closed and the other was open.
                return network.getSwitch(switchPair.getSwitchToOpen().getId()).isOpen() != network.getSwitch(switchPair.getSwitchToClose().getId()).isOpen();
            } else if (elementaryAction instanceof TerminalsConnectionAction || elementaryAction instanceof SwitchAction) {
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
