/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.farao_community.farao.rao_commons.result_api.SensitivityResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.powsybl.iidm.network.Network;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class DiscretePstGroupFiller implements ProblemFiller {

    private final Set<PstRangeAction> pstRangeActions;
    private final Network network;

    public DiscretePstGroupFiller(Network network, Set<PstRangeAction> pstRangeActions) {
        this.pstRangeActions = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.pstRangeActions.addAll(pstRangeActions);
        this.network = network;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        pstRangeActions.forEach(rangeAction -> buildRangeActionGroupConstraint(linearProblem, rangeAction));
    }

    @Override
    public void update(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionResult rangeActionResult) {
        pstRangeActions.forEach(rangeAction -> updateRangeActionGroupConstraint(linearProblem, rangeAction, rangeActionResult));
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
        groupSetPointConstraint.setCoefficient(linearProblem.getPstTapVariationVariable(pstRangeAction, LinearProblem.VariationExtension.UPWARD), -1);
        groupSetPointConstraint.setCoefficient(linearProblem.getPstTapVariationVariable(pstRangeAction, LinearProblem.VariationExtension.DOWNWARD), 1);
        groupSetPointConstraint.setCoefficient(linearProblem.getPstGroupTapVariable(groupId), 1);
    }

    private void updateRangeActionGroupConstraint(LinearProblem linearProblem, PstRangeAction pstRangeAction, RangeActionResult rangeActionResult) {
        Optional<String> optGroupId = pstRangeAction.getGroupId();
        if (optGroupId.isPresent()) {
            double newTap = rangeActionResult.getOptimizedTap(pstRangeAction);
            MPConstraint groupSetPointConstraint = linearProblem.getPstGroupTapConstraint(pstRangeAction);
            groupSetPointConstraint.setLb(newTap);
            groupSetPointConstraint.setUb(newTap);
        }
    }
}
