/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.result;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.powsybl.iidm.network.Network;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionResultImpl implements RangeActionResult {

    private Map<RangeAction, Double> setPoints;

    public RangeActionResultImpl(Map<RangeAction, Double> setPoints) {
        this.setPoints = setPoints;
    }

    public RangeActionResultImpl(Network network, Set<RangeAction> rangeActions) {
        this(rangeActions.stream()
                .collect(Collectors.toMap(Function.identity(), rangeAction -> rangeAction.getCurrentSetpoint(network))));
    }

    @Override
    public Set<RangeAction> getRangeActions() {
        return setPoints.keySet();
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        if (!setPoints.containsKey(pstRangeAction)) {
            throw new FaraoException(format("PST range action %s is not present in the result", pstRangeAction.getName()));
        }
        return pstRangeAction.convertAngleToTap(getOptimizedSetPoint(pstRangeAction));
    }

    @Override
    public double getOptimizedSetPoint(RangeAction rangeAction) {
        if (!setPoints.containsKey(rangeAction)) {
            throw new FaraoException(format("PST range action %s is not present in the result", rangeAction.getName()));
        }
        return setPoints.get(rangeAction);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        Map<PstRangeAction, Integer> optimizedTaps = new HashMap<>();
        setPoints.keySet().stream()
                .filter(ra -> ra instanceof PstRangeAction)
                .forEach(ra -> optimizedTaps.put((PstRangeAction) ra, getOptimizedTap((PstRangeAction) ra)));
        return optimizedTaps;
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPoints() {
        return setPoints;
    }
}
