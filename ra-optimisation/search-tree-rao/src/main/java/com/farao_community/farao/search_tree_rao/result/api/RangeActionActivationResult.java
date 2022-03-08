/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.api;

import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;

import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface RangeActionActivationResult {

    Set<RangeAction<?>> getRangeActions();

    Set<RangeAction<?>> getActivatedRangeActions();

    double getOptimizedSetpoint(RangeAction<?> rangeAction, State state);

    Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state);

    int getOptimizedTap(PstRangeAction pstRangeAction, State state);

    Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state);
}
