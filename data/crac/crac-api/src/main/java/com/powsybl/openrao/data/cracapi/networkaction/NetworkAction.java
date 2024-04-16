/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi.networkaction;

import com.powsybl.action.*;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Set;

/**
 * Remedial action interface specifying a direct action on the network.
 * <p>
 * The Network Action is completely defined by itself.
 * It involves a Set of {@link Action}.
 * When the apply method is called, a {@link com.powsybl.iidm.modification.NetworkModification}
 * is triggered on each of these elementary remedial actions.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface NetworkAction extends RemedialAction<NetworkAction> {

    /**
     * States if the remedial action would change the current state of the network. It has no impact on the network.
     *
     * @param network: Network that serves as reference for the impact.
     * @return True if the remedial action would have an impact on the network.
     */
    boolean hasImpactOnNetwork(final Network network);

    /**
     * Apply the action on a given network.
     *
     * @param network the Network to apply the network action upon
     * @return true if the network action was applied, false if not (eg if it was already applied)
     */
    boolean apply(Network network);

    /**
     * Get the set of the elementary actions constituting then network action
     */
    Set<Action> getElementaryActions();

    /**
     * States if the network action can be applied without infringing on another network action's scope.
     *
     * @param otherNetworkAction the other network action to check compatibility with
     * @return true if both network actions can be applied without any conflictual behaviour
     */
    default boolean isCompatibleWith(NetworkAction otherNetworkAction) {
        return getElementaryActions().stream().allMatch(elementaryAction -> {
            if (elementaryAction instanceof GeneratorAction generatorAction) {
                return otherNetworkAction.getElementaryActions().stream().allMatch(otherElementaryAction -> {
                    if (otherElementaryAction instanceof GeneratorAction otherGeneratorAction) {
                        return !generatorAction.getGeneratorId().equals(otherGeneratorAction.getGeneratorId()) || generatorAction.getActivePowerValue().equals(otherGeneratorAction.getActivePowerValue());
                    }
                    return true;
                });
            } else if (elementaryAction instanceof LoadAction loadAction) {
                return otherNetworkAction.getElementaryActions().stream().allMatch(otherElementaryAction -> {
                    if (otherElementaryAction instanceof LoadAction otherLoadAction) {
                        return !loadAction.getLoadId().equals(otherLoadAction.getLoadId()) || loadAction.getActivePowerValue().equals(otherLoadAction.getActivePowerValue());
                    }
                    return true;
                });
            } else if (elementaryAction instanceof DanglingLineAction danglingLineAction) {
                return otherNetworkAction.getElementaryActions().stream().allMatch(otherElementaryAction -> {
                    if (otherElementaryAction instanceof DanglingLineAction otherDanglingLineAction) {
                        return !danglingLineAction.getDanglingLineId().equals(otherDanglingLineAction.getDanglingLineId()) || danglingLineAction.getActivePowerValue().equals(otherDanglingLineAction.getActivePowerValue());
                    }
                    return true;
                });
            } else if (elementaryAction instanceof ShuntCompensatorPositionAction shuntCompensatorPositionAction) {
                return otherNetworkAction.getElementaryActions().stream().allMatch(otherElementaryAction -> {
                    if (otherElementaryAction instanceof ShuntCompensatorPositionAction otherShuntCompensatorPositionAction) {
                        return !shuntCompensatorPositionAction.getShuntCompensatorId().equals(otherShuntCompensatorPositionAction.getShuntCompensatorId()) || shuntCompensatorPositionAction.getSectionCount() == otherShuntCompensatorPositionAction.getSectionCount();
                    }
                    return true;
                });
            } else if (elementaryAction instanceof PhaseTapChangerTapPositionAction phaseTapChangerTapPositionAction) {
                return otherNetworkAction.getElementaryActions().stream().allMatch(otherElementaryAction -> {
                    if (otherElementaryAction instanceof PhaseTapChangerTapPositionAction otherPhaseTapChangerTapPositionAction) {
                        return !phaseTapChangerTapPositionAction.getTransformerId().equals(otherPhaseTapChangerTapPositionAction.getTransformerId()) || phaseTapChangerTapPositionAction.getTapPosition() == otherPhaseTapChangerTapPositionAction.getTapPosition();
                    }
                    return true;
                });
            } else if (elementaryAction instanceof SwitchPair switchPair) {
                return otherNetworkAction.getElementaryActions().stream().allMatch(switchPair::isCompatibleWith);
            } else if (elementaryAction instanceof TerminalsConnectionAction terminalsConnectionAction) {
                return otherNetworkAction.getElementaryActions().stream().allMatch(otherElementaryAction -> {
                    if (otherElementaryAction instanceof TerminalsConnectionAction otherTerminalsConnectionAction) {
                        return !terminalsConnectionAction.getElementId().equals(otherTerminalsConnectionAction.getElementId()) || terminalsConnectionAction.isOpen() == otherTerminalsConnectionAction.isOpen();
                    }
                    return true;
                });
            } else if (elementaryAction instanceof SwitchAction switchAction) {
                return otherNetworkAction.getElementaryActions().stream().allMatch(otherElementaryAction -> {
                    if (otherElementaryAction instanceof SwitchAction otherSwitchAction) {
                        return !switchAction.getSwitchId().equals(otherSwitchAction.getSwitchId()) || switchAction.isOpen() == otherSwitchAction.isOpen();
                    }
                    return true;
                });
            } else {
                throw new NotImplementedException();
            }
        });
    }

    /**
     * Returns true if all the elementary actions can be applied to the given network
     */
    boolean canBeApplied(Network network);
}
