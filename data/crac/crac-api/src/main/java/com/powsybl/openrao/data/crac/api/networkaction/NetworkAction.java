/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.networkaction;

import com.powsybl.action.*;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Set;
import java.util.stream.Collectors;

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
     * @param network Network that serves as reference for the impact.
     * @return True if the remedial action would have an impact on the network.
     */
    boolean hasImpactOnNetwork(Network network);

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
        Set<String> myElements = getElementaryActions().stream().flatMap(action -> getNetworkElementIds(action).stream()).collect(Collectors.toSet());
        Set<String> otherElements = otherNetworkAction.getElementaryActions().stream().flatMap(action -> getNetworkElementIds(action).stream()).collect(Collectors.toSet());
        return myElements.stream().noneMatch(otherElements::contains);
    }

    /**
     * Returns true if all the elementary actions can be applied to the given network
     */
    boolean canBeApplied(Network network);

    // FIXME: to remove after getElementId is added to core Action interface
    static Set<String> getNetworkElementIds(Action elementaryAction) {
        return switch (elementaryAction) {
            case GeneratorAction generatorAction -> Set.of(generatorAction.getGeneratorId());
            case LoadAction loadAction -> Set.of(loadAction.getLoadId());
            case BoundaryLineAction boundaryLineAction -> Set.of(boundaryLineAction.getBoundaryLineId());
            case ShuntCompensatorPositionAction shuntCompensatorPositionAction -> Set.of(shuntCompensatorPositionAction.getShuntCompensatorId());
            case PhaseTapChangerTapPositionAction phaseTapChangerTapPositionAction -> Set.of(phaseTapChangerTapPositionAction.getTransformerId());
            case TerminalsConnectionAction terminalsConnectionAction -> Set.of(terminalsConnectionAction.getElementId());
            case SwitchAction switchAction -> Set.of(switchAction.getSwitchId());
            case HvdcAction hvdcAction -> Set.of(hvdcAction.getHvdcId());
            case MultipleActionsAction multipleActionsAction -> multipleActionsAction.getActions().stream().flatMap(action -> getNetworkElementIds(action).stream()).collect(Collectors.toSet());
            default -> throw new NotImplementedException();
        };
    }

    void addUsageRule(UsageRule usageRule);
}
