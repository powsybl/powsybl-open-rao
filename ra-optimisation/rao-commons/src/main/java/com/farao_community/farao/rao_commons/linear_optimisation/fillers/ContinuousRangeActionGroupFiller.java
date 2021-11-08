/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.farao_community.farao.rao_commons.result_api.SensitivityResult;
import com.google.ortools.linearsolver.MPConstraint;

import java.util.Optional;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class ContinuousRangeActionGroupFiller implements ProblemFiller {

    private final Set<RangeAction> rangeActions;

    public ContinuousRangeActionGroupFiller(Set<RangeAction> rangeActions) {
        this.rangeActions = rangeActions;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        rangeActions.forEach(rangeAction -> buildRangeActionGroupConstraint(linearProblem, rangeAction));
    }

    @Override
    public void update(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionResult rangeActionResult) {
        // nothing to do
    }

    private void buildRangeActionGroupConstraint(LinearProblem linearProblem, RangeAction rangeAction) {
        Optional<String> optGroupId = rangeAction.getGroupId();
        if (optGroupId.isPresent()) {
            String groupId = optGroupId.get();
            // For the first time the group ID is encountered a common variable for set point has to be created
            if (linearProblem.getRangeActionGroupSetpointVariable(groupId) == null) {
                linearProblem.addRangeActionGroupSetpointVariable(-LinearProblem.infinity(), LinearProblem.infinity(), groupId);
            }
            addRangeActionGroupConstraint(linearProblem, rangeAction, groupId);
        }
    }

    private void addRangeActionGroupConstraint(LinearProblem linearProblem, RangeAction rangeAction, String groupId) {
        MPConstraint groupSetPointConstraint = linearProblem.addRangeActionGroupSetpointConstraint(0, 0, rangeAction);
        groupSetPointConstraint.setCoefficient(linearProblem.getRangeActionSetpointVariable(rangeAction), 1);
        groupSetPointConstraint.setCoefficient(linearProblem.getRangeActionGroupSetpointVariable(groupId), -1);
    }
}
