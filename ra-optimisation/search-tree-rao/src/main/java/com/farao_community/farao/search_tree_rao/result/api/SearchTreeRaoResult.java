/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.result.api;

import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface SearchTreeRaoResult extends RaoResult {

    /**
     * It enables to access to a {@link PerimeterResult} which is a sub-representation of the {@link RaoResult}. Be
     * careful because some combinations of {@code optimizationState} and {@code state} can be quite tricky to
     * analyze.
     *
     * @param optimizationState: The state of optimization to be studied.
     * @param state: The state of the state tree to be studied.
     * @return The full perimeter result to be studied with comprehensive data.
     */
    PerimeterResult getPerimeterResult(OptimizationState optimizationState, State state);

    /**
     * It enables to access to the preventive {@link PerimeterResult} after PRA which is a sub-representation of the
     * {@link RaoResult}.
     *
     * @return The full preventive perimeter result to be studied with comprehensive data.
     */
    PerimeterResult getPostPreventivePerimeterResult();

    /**
     * It enables to access to the initial {@link PerimeterResult} which is a sub-representation of the {@link RaoResult}.
     *
     * @return The full initial perimeter result to be studied with comprehensive data.
     */
    PrePerimeterResult getInitialResult();

}
