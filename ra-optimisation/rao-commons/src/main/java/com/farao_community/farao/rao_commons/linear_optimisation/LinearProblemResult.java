/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.results.LinearProblemStatus;
import com.farao_community.farao.rao_api.results.RangeActionResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearProblemResult implements RangeActionResult {
    private final Map<RangeAction, Double> setPointVariationPerRangeAction = new HashMap<>();
    private final Map<RangeAction, Double> setPointPerRangeAction = new HashMap<>();

    public LinearProblemResult(LinearProblem linearProblem) {
        if (linearProblem.getStatus() != LinearProblemStatus.OPTIMAL) {
            throw new FaraoException("Impossible to define results on non-optimal Linear problem.");
        }

        linearProblem.getRangeActions().forEach(rangeAction -> {
            setPointPerRangeAction.put(rangeAction, linearProblem.getRangeActionSetPointVariable(rangeAction).solutionValue());
            setPointVariationPerRangeAction.put(rangeAction, linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction).solutionValue());
        });
    }

    @Override
    public Set<RangeAction> getRangeActions() {
        return setPointPerRangeAction.keySet();
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        return pstRangeAction.convertAngleToTap(getOptimizedSetPoint(pstRangeAction));
    }

    @Override
    public double getOptimizedSetPoint(RangeAction rangeAction) {
        Double setPoint = setPointPerRangeAction.get(rangeAction);
        if (setPoint != null && !Double.isNaN(setPoint)) {
            return setPoint;
        }
        throw new FaraoException(format("The range action %s is not available in linear problem result", rangeAction.getName()));
    }

    @Override
    public final Map<PstRangeAction, Integer> getOptimizedTaps() {
        return setPointPerRangeAction.keySet().stream()
                .filter(rangeAction -> rangeAction instanceof PstRangeAction)
                .map(rangeAction -> (PstRangeAction) rangeAction)
                .collect(Collectors.toUnmodifiableMap(
                        Function.identity(),
                        this::getOptimizedTap
                ));
    }

    @Override
    public final Map<RangeAction, Double> getOptimizedSetPoints() {
        return Collections.unmodifiableMap(setPointPerRangeAction);
    }
}
