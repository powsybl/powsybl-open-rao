/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;

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
            throw new OpenRaoException(format("range action %s is not present in the result", rangeAction.getName()));
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
