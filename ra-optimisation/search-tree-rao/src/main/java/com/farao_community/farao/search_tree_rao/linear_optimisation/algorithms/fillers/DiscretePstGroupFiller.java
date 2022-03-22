/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class DiscretePstGroupFiller {

    /*
    private final State optimizedState;
    private final Set<PstRangeAction> pstRangeActions;
    private final Network network;

    public DiscretePstGroupFiller(Network network, State optimizedState, Map<State, Set<PstRangeAction>> pstRangeActions) {
        this.pstRangeActions = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.pstRangeActions.addAll(pstRangeActions);
        this.network = network;
        this.optimizedState = optimizedState;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        pstRangeActions.forEach(rangeAction -> buildRangeActionGroupConstraint(linearProblem, rangeAction));
    }

    @Override
    public void update(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        pstRangeActions.forEach(rangeAction -> updateRangeActionGroupConstraint(linearProblem, rangeAction, rangeActionActivationResult));
    }

    private void buildRangeActionGroupConstraint(LinearProblem linearProblem, PstRangeAction pstRangeAction) {
        Optional<String> optGroupId = pstRangeAction.getGroupId();
        if (optGroupId.isPresent()) {
            String groupId = optGroupId.get();
            // For the first time the group ID is encountered a common variable for the tap has to be created
            if (linearProblem.getPstGroupTapVariable(groupId) == null) {
                linearProblem.addPstGroupTapVariable(-LinearProblem.infinity(), LinearProblem.infinity(), groupId);
            }
            addRangeActionGroupConstraint(linearProblem, pstRangeAction, groupId);
        }
    }

    private void addRangeActionGroupConstraint(LinearProblem linearProblem, PstRangeAction pstRangeAction, String groupId) {
        double currentTap = pstRangeAction.getCurrentTapPosition(network);
        MPConstraint groupSetPointConstraint = linearProblem.addPstGroupTapConstraint(currentTap, currentTap, pstRangeAction);
        groupSetPointConstraint.setCoefficient(linearProblem.getPstTapVariationVariable(pstRangeAction, LinearProblem.VariationDirectionExtension.UPWARD), -1);
        groupSetPointConstraint.setCoefficient(linearProblem.getPstTapVariationVariable(pstRangeAction, LinearProblem.VariationDirectionExtension.DOWNWARD), 1);
        groupSetPointConstraint.setCoefficient(linearProblem.getPstGroupTapVariable(groupId), 1);
    }

    private void updateRangeActionGroupConstraint(LinearProblem linearProblem, PstRangeAction pstRangeAction, RangeActionActivationResult rangeActionActivationResult) {
        Optional<String> optGroupId = pstRangeAction.getGroupId();
        if (optGroupId.isPresent()) {
            double newTap = rangeActionActivationResult.getOptimizedTap(pstRangeAction, optimizedState);
            MPConstraint groupSetPointConstraint = linearProblem.getPstGroupTapConstraint(pstRangeAction);
            groupSetPointConstraint.setLb(newTap);
            groupSetPointConstraint.setUb(newTap);
        }
    }
     */
}
