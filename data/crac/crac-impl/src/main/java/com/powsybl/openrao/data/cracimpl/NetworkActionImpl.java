/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.*;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.modification.NetworkModification;
import com.powsybl.iidm.modification.NetworkModificationList;
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
                Identifiable<?> element = network.getIdentifiable(terminalsConnectionAction.getElementId());
                if (element instanceof Branch<?> branch) {
                    if (terminalsConnectionAction.isOpen()) {
                        // Line is considered closed if both terminal are connected
                        return branch.getTerminal1().isConnected() && branch.getTerminal2().isConnected();
                    } else {
                        // Line is already considered opened if one of the terminals is disconnected
                        return !branch.getTerminal1().isConnected() || !branch.getTerminal2().isConnected();
                    }
                } else {
                    throw new NotImplementedException("TerminalsConnectionAction are only on branches for now");
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
            } else if (elementaryAction instanceof PhaseTapChangerTapPositionAction phaseTapChangerTapPositionAction) {
                // hypothesis: transformer is a two windings transformer
                PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(phaseTapChangerTapPositionAction.getTransformerId()).getPhaseTapChanger();
                int tapPosition = phaseTapChangerTapPositionAction.getTapPosition();
                return tapPosition >= phaseTapChanger.getLowTapPosition() && tapPosition <= phaseTapChanger.getHighTapPosition();
            } else if (elementaryAction instanceof SwitchPair switchPair) {
                // It is only applicable if, initially, one switch was closed and the other was open.
                return network.getSwitch(switchPair.getSwitchToOpen().getId()).isOpen() != network.getSwitch(switchPair.getSwitchToClose().getId()).isOpen();
            } else if (elementaryAction instanceof TerminalsConnectionAction || elementaryAction instanceof SwitchAction) {
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
    public NetworkModification getRollbackModification(Network network) {
        return new NetworkModificationList(
            elementaryActions.stream().map(ea -> rollback(ea, network)).toList()
        );
    }

    private NetworkModification rollback(Action elementaryAction, Network network) {
        if (elementaryAction instanceof GeneratorAction generatorAction) {
            return new GeneratorActionBuilder()
                .withId(String.format("rollback_%s", elementaryAction.getId()))
                .withNetworkElementId(generatorAction.getGeneratorId())
                .withActivePowerRelativeValue(false)
                .withActivePowerValue(network.getGenerator(generatorAction.getGeneratorId()).getTargetP())
                .build().toModification();
        } else if (elementaryAction instanceof LoadAction loadAction) {
            return new LoadActionBuilder()
                .withId(String.format("rollback_%s", elementaryAction.getId()))
                .withNetworkElementId(loadAction.getLoadId())
                .withRelativeValue(false)
                .withActivePowerValue(network.getLoad(loadAction.getLoadId()).getP0())
                .build().toModification();
        } else if (elementaryAction instanceof DanglingLineAction danglingLineAction) {
            return new DanglingLineActionBuilder()
                .withId(String.format("rollback_%s", elementaryAction.getId()))
                .withNetworkElementId(danglingLineAction.getDanglingLineId())
                .withRelativeValue(false)
                .withActivePowerValue(network.getDanglingLine(danglingLineAction.getDanglingLineId()).getP0())
                .build().toModification();
        } else if (elementaryAction instanceof ShuntCompensatorPositionAction shuntCompensatorPositionAction) {
            return new ShuntCompensatorPositionActionBuilder()
                .withId(String.format("rollback_%s", elementaryAction.getId()))
                .withNetworkElementId(shuntCompensatorPositionAction.getShuntCompensatorId())
                .withSectionCount(network.getShuntCompensator(shuntCompensatorPositionAction.getShuntCompensatorId()).getSectionCount())
                .build().toModification();
        } else if (elementaryAction instanceof PhaseTapChangerTapPositionAction phaseTapChangerTapPositionAction) {
            return new PhaseTapChangerTapPositionActionBuilder()
                .withId(String.format("rollback_%s", elementaryAction.getId()))
                .withNetworkElementId(phaseTapChangerTapPositionAction.getTransformerId())
                .withTapPosition(network.getTwoWindingsTransformer(phaseTapChangerTapPositionAction.getTransformerId()).getPhaseTapChanger().getTapPosition())
                .withRelativeValue(false)
                .build().toModification();
        } else if (elementaryAction instanceof SwitchPair switchPair) {
            return new NetworkModificationList(
                new SwitchActionBuilder()
                    .withId(String.format("rollback_%s_open", elementaryAction.getId()))
                    .withNetworkElementId(switchPair.getSwitchToOpen().getId())
                    .withOpen(network.getSwitch(switchPair.getSwitchToOpen().getId()).isOpen())
                    .build()
                    .toModification(),
                new SwitchActionBuilder()
                    .withId(String.format("rollback_%s_close", switchPair.getId()))
                    .withNetworkElementId(switchPair.getSwitchToClose().getId())
                    .withOpen(network.getSwitch(switchPair.getSwitchToClose().getId()).isOpen())
                    .build()
                    .toModification()
            );
        } else if (elementaryAction instanceof TerminalsConnectionAction terminalsConnectionAction) {
            Identifiable<?> element = network.getIdentifiable(terminalsConnectionAction.getElementId());
            if (element instanceof Branch<?> branch) {
                return new NetworkModificationList(
                    new TerminalsConnectionActionBuilder()
                        .withId(String.format("rollback_%s_side1", elementaryAction.getId()))
                        .withNetworkElementId(terminalsConnectionAction.getElementId())
                        .withSide(ThreeSides.ONE)
                        .withOpen(!branch.getTerminal1().isConnected())
                        .build().toModification(),
                    new TerminalsConnectionActionBuilder()
                        .withId(String.format("rollback_%s_side2", elementaryAction.getId()))
                        .withNetworkElementId(terminalsConnectionAction.getElementId())
                        .withSide(ThreeSides.TWO)
                        .withOpen(!branch.getTerminal2().isConnected())
                        .build().toModification()
                );
            } else {
                throw new NotImplementedException("TerminalsConnectionAction are only on branches for now");
            }
        } else if (elementaryAction instanceof SwitchAction switchAction) {
            return new SwitchActionBuilder()
                .withId(String.format("rollback_%s", elementaryAction.getId()))
                .withNetworkElementId(switchAction.getSwitchId())
                .withOpen(network.getSwitch(switchAction.getSwitchId()).isOpen())
                .build().toModification();
        } else {
            throw new NotImplementedException();
        }
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
