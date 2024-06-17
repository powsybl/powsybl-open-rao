/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.searchtreerao.result.impl.MultiStateRemedialActionResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PerimeterResultWithCnecs;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class DiscretePstGroupFiller implements ProblemFiller {

    private final State optimizedState;
    private final Map<State, Set<PstRangeAction>> pstRangeActions;
    private final Network network;

    public DiscretePstGroupFiller(Network network, State optimizedState, Map<State, Set<PstRangeAction>> pstRangeActions) {
        this.pstRangeActions = pstRangeActions;
        this.network = network;
        this.optimizedState = optimizedState;
    }

    @Override
    public void fill(LinearProblem linearProblem, PerimeterResultWithCnecs flowAndSensiResult) {
        pstRangeActions.forEach((state, rangeActionSet) -> rangeActionSet.forEach(rangeAction ->
            buildRangeActionGroupConstraint(linearProblem, rangeAction, state)
        ));
    }

    @Override
    public void updateBetweenSensiIteration(LinearProblem linearProblem, PerimeterResultWithCnecs flowAndSensiResult, MultiStateRemedialActionResultImpl rangeActionResult) {
        pstRangeActions.forEach((state, rangeActionSet) -> rangeActionSet.forEach(rangeAction ->
            updateRangeActionGroupConstraint(linearProblem, rangeAction, state, rangeActionResult)
        ));
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, MultiStateRemedialActionResultImpl rangeActionResult) {
        pstRangeActions.forEach((state, rangeActionSet) -> rangeActionSet.forEach(rangeAction ->
            updateRangeActionGroupConstraint(linearProblem, rangeAction, state, rangeActionResult)
        ));
    }

    private void buildRangeActionGroupConstraint(LinearProblem linearProblem, PstRangeAction pstRangeAction, State state) {
        Optional<String> optGroupId = pstRangeAction.getGroupId();
        if (optGroupId.isPresent()) {
            String groupId = optGroupId.get();
            // For the first time the group ID is encountered a common variable for the tap has to be created
            try {
                linearProblem.getPstGroupTapVariable(groupId, state);
            } catch (OpenRaoException ignored) {
                linearProblem.addPstGroupTapVariable(-LinearProblem.infinity(), LinearProblem.infinity(), groupId, state);
            }
            addRangeActionGroupConstraint(linearProblem, pstRangeAction, groupId, state);
        }
    }

    private void addRangeActionGroupConstraint(LinearProblem linearProblem, PstRangeAction pstRangeAction, String groupId, State state) {
        double currentTap = pstRangeAction.getCurrentTapPosition(network);
        OpenRaoMPConstraint groupSetPointConstraint = linearProblem.addPstGroupTapConstraint(currentTap, currentTap, pstRangeAction, state);
        groupSetPointConstraint.setCoefficient(linearProblem.getPstTapVariationVariable(pstRangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD), -1);
        groupSetPointConstraint.setCoefficient(linearProblem.getPstTapVariationVariable(pstRangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD), 1);
        groupSetPointConstraint.setCoefficient(linearProblem.getPstGroupTapVariable(groupId, state), 1);
    }

    private void updateRangeActionGroupConstraint(LinearProblem linearProblem, PstRangeAction pstRangeAction, State state, MultiStateRemedialActionResultImpl rangeActionResult) {
        Optional<String> optGroupId = pstRangeAction.getGroupId();
        if (optGroupId.isPresent()) {
            int newTap = rangeActionResult.getOptimizedTapOnState(pstRangeAction, optimizedState);
            OpenRaoMPConstraint groupSetPointConstraint = linearProblem.getPstGroupTapConstraint(pstRangeAction, state);
            groupSetPointConstraint.setLb(newTap);
            groupSetPointConstraint.setUb(newTap);
        }
    }
}
