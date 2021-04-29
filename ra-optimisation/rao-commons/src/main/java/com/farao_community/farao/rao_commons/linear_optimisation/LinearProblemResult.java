/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.rao_api.results.LinearProblemStatus;
import com.farao_community.farao.rao_api.results.RangeActionResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearProblemResult implements RangeActionResult {
    private final Map<RangeAction, Double> results = new HashMap<>();

    public LinearProblemResult(LinearProblem linearProblem) {
        if (linearProblem.getStatus() != LinearProblemStatus.OPTIMAL) {
            throw new FaraoException("Impossible to define results on non-optimal Linear problem.");
        }
        linearProblem.getRangeActions().forEach(rangeAction ->
                results.put(rangeAction, linearProblem.getRangeActionSetPointVariable(rangeAction).solutionValue()));
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        return pstRangeAction.computeTapPosition(results.get(pstRangeAction));
    }

    @Override
    public double getOptimizedSetPoint(RangeAction rangeAction) {
        return results.get(rangeAction);
    }

    @Override
    public Set<RangeAction> getActivatedRangeActions() {
        //TODO : checker par rapport à la variable de variation
        return results.keySet();
    }

    @Override
    public final Map<PstRangeAction, Integer> getOptimizedTaps() {
        return results.keySet().stream()
                .filter(rangeAction -> rangeAction instanceof PstRangeAction)
                .collect(Collectors.toMap(
                        rangeAction -> (PstRangeAction) rangeAction,
                        rangeAction -> getOptimizedTap((PstRangeAction) rangeAction)
                ));
    }

    @Override
    public final Map<RangeAction, Double> getOptimizedSetPoints() {
        return Collections.unmodifiableMap(results);
    }
}
