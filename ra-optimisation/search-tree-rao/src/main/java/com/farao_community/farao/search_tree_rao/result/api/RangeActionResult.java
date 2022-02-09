/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;

import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface RangeActionResult {

    Set<RangeAction<?>> getRangeActions();

    /**
     * It gives the tap position of the PST on which the {@link PstRangeAction} is pointing at after it is optimized.
     *
     * @param pstRangeAction: The PST range action to be studied.
     * @return The tap of the PST defined in the PST range action after its optimization.
     */
    int getOptimizedTap(PstRangeAction pstRangeAction);

    /**
     * It gives the set point position of the network element on which the {@link RangeAction} is pointing at
     * after it is optimized.
     *
     * @param rangeAction: The range action to be studied.
     * @return The set point of the network element defined in the range action after its optimization.
     */
    double getOptimizedSetPoint(RangeAction<?> rangeAction);

    /**
     * It gives a summary of all the optimized taps of the {@link PstRangeAction} present in the {@link Crac}.
     *
     * @return The map of the PST range actions associated to their optimized tap.
     */
    Map<PstRangeAction, Integer> getOptimizedTaps();

    /**
     * It gives a summary of all the optimized set points of the {@link RangeAction} present in the {@link Crac}.
     *
     * @return The map of the range actions associated to their optimized set points.
     */
    Map<RangeAction<?>, Double> getOptimizedSetPoints();
}
