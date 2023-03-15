/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPConstraint;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import com.powsybl.iidm.network.Network;

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
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        pstRangeActions.forEach((state, rangeActionSet) -> rangeActionSet.forEach(rangeAction ->
            buildRangeActionGroupConstraint(linearProblem, rangeAction, state)
        ));
    }

    @Override
    public void updateBetweenSensiIteration(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        pstRangeActions.forEach((state, rangeActionSet) -> rangeActionSet.forEach(rangeAction ->
            updateRangeActionGroupConstraint(linearProblem, rangeAction, state, rangeActionActivationResult)
        ));
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        pstRangeActions.forEach((state, rangeActionSet) -> rangeActionSet.forEach(rangeAction ->
            updateRangeActionGroupConstraint(linearProblem, rangeAction, state, rangeActionActivationResult)
        ));
    }

    private void buildRangeActionGroupConstraint(LinearProblem linearProblem, PstRangeAction pstRangeAction, State state) {
        Optional<String> optGroupId = pstRangeAction.getGroupId();
        if (optGroupId.isPresent()) {
            String groupId = optGroupId.get();
            // For the first time the group ID is encountered a common variable for the tap has to be created
            if (linearProblem.getPstGroupTapVariable(groupId, state) == null) {
                linearProblem.addPstGroupTapVariable(-LinearProblem.infinity(), LinearProblem.infinity(), groupId, state);
            }
            addRangeActionGroupConstraint(linearProblem, pstRangeAction, groupId, state);
        }
    }

    private void addRangeActionGroupConstraint(LinearProblem linearProblem, PstRangeAction pstRangeAction, String groupId, State state) {
        double currentTap = pstRangeAction.getCurrentTapPosition(network);
        FaraoMPConstraint groupSetPointConstraint = linearProblem.addPstGroupTapConstraint(currentTap, currentTap, pstRangeAction, state);
        groupSetPointConstraint.setCoefficient(linearProblem.getPstTapVariationVariable(pstRangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD), -1);
        groupSetPointConstraint.setCoefficient(linearProblem.getPstTapVariationVariable(pstRangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD), 1);
        groupSetPointConstraint.setCoefficient(linearProblem.getPstGroupTapVariable(groupId, state), 1);
    }

    private void updateRangeActionGroupConstraint(LinearProblem linearProblem, PstRangeAction pstRangeAction, State state, RangeActionActivationResult rangeActionActivationResult) {
        Optional<String> optGroupId = pstRangeAction.getGroupId();
        if (optGroupId.isPresent()) {
            double newTap = rangeActionActivationResult.getOptimizedTap(pstRangeAction, optimizedState);
            FaraoMPConstraint groupSetPointConstraint = linearProblem.getPstGroupTapConstraint(pstRangeAction, state);
            groupSetPointConstraint.setLb(newTap);
            groupSetPointConstraint.setUb(newTap);
        }
    }
}
