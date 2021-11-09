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
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.farao_community.farao.rao_commons.result_api.SensitivityResult;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.powsybl.iidm.network.Network;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LeafProblem extends SearchTreeProblem {
    private final Set<RangeAction> rangeActions;

    public LeafProblem(FlowResult initialFlowResult,
                       FlowResult prePerimeterFlowResult,
                       RangeActionResult prePerimeterSetPoints,
                       Set<FlowCnec> flowCnecs,
                       Set<FlowCnec> loopFlowCnecs,
                       LinearOptimizerParameters linearOptimizerParameters,
                       Set<RangeAction> rangeActions) {
        super(initialFlowResult, prePerimeterFlowResult, prePerimeterSetPoints, flowCnecs, loopFlowCnecs, linearOptimizerParameters);
        this.rangeActions = rangeActions;
    }

    public LinearProblem getLinearProblem(Network network, FlowResult preOptimFlowResult, SensitivityResult preOptimSensitivityResult) {
        LinearProblem.LinearProblemBuilder linearProblemBuilder =  LinearProblem.create()
                .withProblemFiller(createCoreProblemFiller(network, flowCnecs, rangeActions));

        if (linearOptimizerParameters.getObjectiveFunction().relativePositiveMargins()) {
            linearProblemBuilder.withProblemFiller(createMaxMinRelativeMarginFiller(flowCnecs, rangeActions, preOptimFlowResult));
        } else {
            linearProblemBuilder.withProblemFiller(createMaxMinMarginFiller(flowCnecs, rangeActions));
        }

        if (linearOptimizerParameters.isRaoWithMnecLimitation()) {
            linearProblemBuilder.withProblemFiller(createMnecFiller(flowCnecs));
        }

        if (linearOptimizerParameters.isRaoWithLoopFlowLimitation()) {
            linearProblemBuilder.withProblemFiller(createLoopFlowFiller(loopFlowCnecs));
        }
        if (!Objects.isNull(linearOptimizerParameters.getUnoptimizedCnecParameters())) {
            linearProblemBuilder.withProblemFiller(createUnoptimizedCnecFiller(flowCnecs));
        }

        if (linearOptimizerParameters.getPstOptimizationApproximation().equals(RaoParameters.PstOptimizationApproximation.APPROXIMATED_INTEGERS)) {
            linearProblemBuilder.withProblemFiller(createIntegerPstTapFiller(network, rangeActions));
            linearProblemBuilder.withProblemFiller(createDiscretePstGroupFiller(network, rangeActions.stream().filter(PstRangeAction.class::isInstance).map(PstRangeAction.class::cast).collect(Collectors.toSet())));
            linearProblemBuilder.withProblemFiller(createContinuousRangeActionGroupFiller(rangeActions.stream().filter(ra -> !(ra instanceof PstRangeAction)).collect(Collectors.toSet())));
        } else {
            linearProblemBuilder.withProblemFiller(createContinuousRangeActionGroupFiller(rangeActions));
        }

        linearProblemBuilder.withBranchResult(preOptimFlowResult)
                .withSensitivityResult(preOptimSensitivityResult)
                .withSolver(linearOptimizerParameters.getSolver())
                .withRelativeMipGap(linearOptimizerParameters.getRelativeMipGap())
                .withSolverSpecificParameters(linearOptimizerParameters.getSolverSpecificParameters());

        return linearProblemBuilder.build();
    }
}
