/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.results;

import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * This interface gives a perimeter result. It represents a unique state optimization with full network and range actions.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface PerimeterResult extends OptimizationResult {

    /**
     * It gathers the {@link RangeAction} that are activated.
     *
     * @return The set of activated range actions.
     */
    Set<RangeAction> getActivatedRangeActions();

    default Set<PstRangeAction> getActivatedPstRangeActions() {
        return getActivatedRangeActions().stream()
                .filter(rangeAction -> rangeAction instanceof PstRangeAction)
                .map(rangeAction -> (PstRangeAction) rangeAction)
                .collect(Collectors.toSet());
    }
}
