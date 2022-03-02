/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.search_tree.algorithms;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionResult;
import com.farao_community.farao.search_tree_rao.search_tree.parameters.TreeParameters;

import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SearchTreeProblem {
    protected final FlowResult initialFlowResult;
    protected final FlowResult prePerimeterFlowResult;
    protected final RangeActionResult prePerimeterSetPoints;
    protected final Set<FlowCnec> flowCnecs;
    protected final Set<FlowCnec> loopFlowCnecs;
    protected final LinearOptimizerParameters linearOptimizerParameters;
    protected final TreeParameters treeParameters;

    public SearchTreeProblem(FlowResult initialFlowResult,
                             FlowResult prePerimeterFlowResult,
                             RangeActionResult prePerimeterSetPoints,
                             Set<FlowCnec> flowCnecs,
                             Set<FlowCnec> loopFlowCnecs,
                             LinearOptimizerParameters linearOptimizerParameters,
                             TreeParameters treeParameters) {
        this.initialFlowResult = initialFlowResult;
        this.prePerimeterFlowResult = prePerimeterFlowResult;
        this.prePerimeterSetPoints = prePerimeterSetPoints;
        this.flowCnecs = flowCnecs;
        this.loopFlowCnecs = loopFlowCnecs;
        this.linearOptimizerParameters = linearOptimizerParameters;
        this.treeParameters = treeParameters;
    }

    public LeafProblem getLeafProblem(Set<RangeAction<?>> rangeActions, Set<NetworkAction> activatedNetworkActions) {
        return new LeafProblem(
                initialFlowResult,
                prePerimeterFlowResult,
                prePerimeterSetPoints,
                flowCnecs,
                loopFlowCnecs,
                linearOptimizerParameters,
                treeParameters,
                rangeActions,
                activatedNetworkActions
        );
    }
}
