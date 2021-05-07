/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.result;

import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.rao_api.results.RangeActionResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionResultImpl implements RangeActionResult {

    private Map<RangeAction, Double> setPoints;

    public RangeActionResultImpl(Map<RangeAction, Double> setPoints) {
        this.setPoints = setPoints;
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        return pstRangeAction.computeTapPosition(getOptimizedSetPoint(pstRangeAction));
    }

    @Override
    public double getOptimizedSetPoint(RangeAction rangeAction) {
        return setPoints.get(rangeAction);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        Map<PstRangeAction, Integer> optimizedTaps = new HashMap<>();
        setPoints.keySet().stream().filter(ra -> ra instanceof PstRangeAction).forEach(ra -> optimizedTaps.put((PstRangeAction) ra, getOptimizedTap((PstRangeAction) ra)));
        return optimizedTaps;
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPoints() {
        return setPoints;
    }
}
