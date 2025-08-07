/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class ContinuousRangeActionGroupFiller implements ProblemFiller {

    private final Map<State, Set<RangeAction<?>>> rangeActionsPerState;

    public ContinuousRangeActionGroupFiller(Map<State, Set<RangeAction<?>>> rangeActionsPerState) {
        this.rangeActionsPerState = rangeActionsPerState;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        buildRangeActionGroupConstraint(linearProblem);
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do
    }

    private void buildRangeActionGroupConstraint(LinearProblem linearProblem) {

        rangeActionsPerState.forEach((state, rangeActions) -> rangeActions.forEach(ra -> {
            Optional<String> optGroupId = ra.getGroupId();
            // if range action belongs to a group
            if (optGroupId.isPresent()) {
                String groupId = optGroupId.get();
                // For the first time the group ID is encountered a common variable for set point has to be created
                try {
                    linearProblem.getRangeActionGroupSetpointVariable(groupId, state);
                } catch (OpenRaoException ignored) {
                    linearProblem.addRangeActionGroupSetpointVariable(-linearProblem.infinity(), linearProblem.infinity(), groupId, state);
                }
                addRangeActionGroupConstraint(linearProblem, ra, groupId, state);
            }

        }));

    }

    private void addRangeActionGroupConstraint(LinearProblem linearProblem, RangeAction<?> rangeAction, String groupId, State state) {
        OpenRaoMPConstraint groupSetPointConstraint = linearProblem.addRangeActionGroupSetpointConstraint(0, 0, rangeAction, state);
        groupSetPointConstraint.setCoefficient(linearProblem.getRangeActionSetpointVariable(rangeAction, state), 1);
        groupSetPointConstraint.setCoefficient(linearProblem.getRangeActionGroupSetpointVariable(groupId, state), -1);
    }
}
