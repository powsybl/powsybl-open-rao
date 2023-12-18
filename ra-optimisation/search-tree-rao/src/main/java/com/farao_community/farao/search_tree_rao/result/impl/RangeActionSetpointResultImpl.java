/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.search_tree_rao.result.impl;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.data.crac_api.State;
import com.powsybl.open_rao.data.crac_api.range_action.PstRangeAction;
import com.powsybl.open_rao.data.crac_api.range_action.RangeAction;
import com.powsybl.open_rao.search_tree_rao.result.api.RangeActionActivationResult;
import com.powsybl.open_rao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.powsybl.iidm.network.Network;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Baptiste Seguinot {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionSetpointResultImpl implements RangeActionSetpointResult {

    private final Map<RangeAction<?>, Double> setPoints;

    public RangeActionSetpointResultImpl(Map<RangeAction<?>, Double> setPoints) {
        this.setPoints = setPoints;
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return setPoints.keySet();
    }

    @Override
    public double getSetpoint(RangeAction<?> rangeAction) {
        if (!setPoints.containsKey(rangeAction)) {
            throw new FaraoException(format("range action %s is not present in the result", rangeAction.getName()));
        }
        return setPoints.get(rangeAction);
    }

    @Override
    public int getTap(PstRangeAction pstRangeAction) {
        return pstRangeAction.convertAngleToTap(getSetpoint(pstRangeAction));
    }

    public static RangeActionSetpointResult buildWithSetpointsFromNetwork(Network network, Set<RangeAction<?>> rangeActions) {
        return new RangeActionSetpointResultImpl(rangeActions.stream()
            .collect(Collectors.toMap(Function.identity(), rangeAction -> rangeAction.getCurrentSetpoint(network))));
    }

    public static RangeActionSetpointResult buildFromActivationOfRangeActionAtState(RangeActionActivationResult raar, State state) {
        Map<RangeAction<?>, Double> setPoints = new HashMap<>();
        raar.getRangeActions().forEach(ra -> setPoints.put(ra, raar.getOptimizedSetpoint(ra, state)));
        return new RangeActionSetpointResultImpl(setPoints);
    }
}
