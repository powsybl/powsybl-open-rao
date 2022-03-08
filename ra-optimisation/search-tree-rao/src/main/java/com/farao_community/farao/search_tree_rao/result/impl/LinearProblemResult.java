/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.search_tree_rao.result.api.LinearProblemStatus;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.LinearProblem;

import java.util.*;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearProblemResult implements RangeActionActivationResult {

    private final Map<RangeAction<?>, Double> setPointPerRangeAction = new HashMap<>();

    public LinearProblemResult(LinearProblem linearProblem) {
        if (linearProblem.getStatus() != LinearProblemStatus.OPTIMAL && linearProblem.getStatus() != LinearProblemStatus.FEASIBLE) {
            throw new FaraoException("Impossible to define results on non-optimal and non-feasible Linear problem.");
        }

        //todo : adapt constructor to new multi-state approach
        linearProblem.getRangeActions().forEach(rangeAction ->
                setPointPerRangeAction.put(rangeAction, linearProblem.getRangeActionSetpointVariable(rangeAction).solutionValue()));
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return setPointPerRangeAction.keySet();
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions() {
        //todo, write method
        return new HashSet<>();
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        Double setPoint = setPointPerRangeAction.get(rangeAction);
        if (setPoint != null && !Double.isNaN(setPoint)) {
            return setPoint;
        }
        throw new FaraoException(format("The range action %s is not available in linear problem result", rangeAction.getName()));
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        //todo: implement
        return null;
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        return pstRangeAction.convertAngleToTap(getOptimizedSetpoint(pstRangeAction, state));
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        //todo: implement
        return null;
    }

}
