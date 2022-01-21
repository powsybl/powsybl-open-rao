/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.rao_api.parameters.*;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.*;
import com.powsybl.iidm.network.Network;

import java.util.Set;
import java.util.stream.Collectors;

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

    public SearchTreeProblem(FlowResult initialFlowResult,
                             FlowResult prePerimeterFlowResult,
                             RangeActionResult prePerimeterSetPoints,
                             Set<FlowCnec> flowCnecs,
                             Set<FlowCnec> loopFlowCnecs,
                             LinearOptimizerParameters linearOptimizerParameters) {
        this.initialFlowResult = initialFlowResult;
        this.prePerimeterFlowResult = prePerimeterFlowResult;
        this.prePerimeterSetPoints = prePerimeterSetPoints;
        this.flowCnecs = flowCnecs;
        this.loopFlowCnecs = loopFlowCnecs;
        this.linearOptimizerParameters = linearOptimizerParameters;
    }

    public LeafProblem getLeafProblem(Set<RangeAction<?>> rangeActions) {
        return new LeafProblem(
                initialFlowResult,
                prePerimeterFlowResult,
                prePerimeterSetPoints,
                flowCnecs,
                loopFlowCnecs,
                linearOptimizerParameters,
                rangeActions
        );
    }

    protected ProblemFiller createCoreProblemFiller(Network network, Set<FlowCnec> flowCnecs, Set<RangeAction<?>> rangeActions) {
        return new CoreProblemFiller(
                network,
                flowCnecs,
                rangeActions,
                prePerimeterSetPoints,
                linearOptimizerParameters.getPstSensitivityThreshold(),
                linearOptimizerParameters.getHvdcSensitivityThreshold(),
                linearOptimizerParameters.getInjectionSensitivityThreshold(),
                linearOptimizerParameters.getObjectiveFunction().relativePositiveMargins()
        );
    }

    protected ProblemFiller createMaxMinRelativeMarginFiller(Set<FlowCnec> flowCnecs, Set<RangeAction<?>> rangeActions, FlowResult preOptimFlowResult) {
        return new MaxMinRelativeMarginFiller(
                flowCnecs.stream().filter(Cnec::isOptimized).collect(Collectors.toSet()),
                preOptimFlowResult,
                rangeActions,
                linearOptimizerParameters.getObjectiveFunction().getUnit(),
                linearOptimizerParameters.getMaxMinRelativeMarginParameters()
        );
    }

    protected ProblemFiller createMaxMinMarginFiller(Set<FlowCnec> flowCnecs, Set<RangeAction<?>> rangeActions) {
        return new MaxMinMarginFiller(
                flowCnecs.stream().filter(Cnec::isOptimized).collect(Collectors.toSet()),
                rangeActions,
                linearOptimizerParameters.getUnit(),
                linearOptimizerParameters.getMaxMinMarginParameters()
        );
    }

    protected ProblemFiller createMnecFiller(Set<FlowCnec> flowCnecs) {
        return new MnecFiller(
                initialFlowResult,
                flowCnecs.stream().filter(Cnec::isMonitored).collect(Collectors.toSet()),
                linearOptimizerParameters.getUnit(),
                linearOptimizerParameters.getMnecParameters()
        );
    }

    protected ProblemFiller createLoopFlowFiller(Set<FlowCnec> loopFlowCnecs) {
        return new MaxLoopFlowFiller(
                loopFlowCnecs,
                initialFlowResult,
                linearOptimizerParameters.getLoopFlowParameters()
        );
    }

    protected ProblemFiller createUnoptimizedCnecFiller(Set<FlowCnec> flowCnecs) {
        return new UnoptimizedCnecFiller(
                flowCnecs,
                prePerimeterFlowResult,
                linearOptimizerParameters.getUnoptimizedCnecParameters()
        );
    }

    protected ProblemFiller createContinuousRangeActionGroupFiller(Set<RangeAction<?>> rangeActions) {
        return new ContinuousRangeActionGroupFiller(
                rangeActions
        );
    }

    protected ProblemFiller createIntegerPstTapFiller(Network network, Set<RangeAction<?>> rangeActions) {
        return new DiscretePstTapFiller(
                network,
                rangeActions,
                prePerimeterSetPoints
        );
    }

    protected ProblemFiller createDiscretePstGroupFiller(Network network, Set<PstRangeAction> pstRangeActions) {
        return new DiscretePstGroupFiller(
                network,
                pstRangeActions
        );
    }
}
