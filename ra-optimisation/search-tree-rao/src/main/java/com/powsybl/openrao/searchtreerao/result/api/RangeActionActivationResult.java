/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.api;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;

import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface RangeActionActivationResult {

    Set<RangeAction<?>> getRangeActions();

    Set<RangeAction<?>> getActivatedRangeActions(State state);

    Map<State, Set<RangeAction<?>>> getActivatedRangeActionsPerState();

    double getOptimizedSetpoint(RangeAction<?> rangeAction, State state);

    Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state);

    double getSetPointVariation(RangeAction<?> rangeAction, State state);

    int getOptimizedTap(PstRangeAction pstRangeAction, State state);

    Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state);

    int getTapVariation(PstRangeAction pstRangeAction, State state);
}
