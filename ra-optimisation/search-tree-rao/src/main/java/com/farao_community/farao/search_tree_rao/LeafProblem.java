/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.RangeActionResult;
import com.farao_community.farao.rao_api.results.SensitivityResult;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.powsybl.iidm.network.Network;

import java.util.Objects;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LeafProblem extends SearchTreeProblem {
    private final Set<RangeAction> rangeActions;

    public LeafProblem(BranchResult initialBranchResult,
                       BranchResult prePerimeterBranchResult,
                       RangeActionResult prePerimeterSetPoints,
                       Set<BranchCnec> cnecs,
                       Set<BranchCnec> loopFlowCnecs,
                       LinearOptimizerParameters linearOptimizerParameters,
                       Set<RangeAction> rangeActions) {
        super(initialBranchResult, prePerimeterBranchResult, prePerimeterSetPoints, cnecs, loopFlowCnecs, linearOptimizerParameters);
        this.rangeActions = rangeActions;
    }

    public LinearProblem getLinearProblem(Network network, BranchResult preOptimBranchResult, SensitivityResult preOptimSensitivityResult) {
        LinearProblem.LinearProblemBuilder linearProblemBuilder =  LinearProblem.create()
                .withProblemFiller(createCoreProblemFiller(network, cnecs, rangeActions));

        if (linearOptimizerParameters.getObjectiveFunction().relativePositiveMargins()) {
            linearProblemBuilder.withProblemFiller(createMaxMinRelativeMarginFiller(cnecs, rangeActions, preOptimBranchResult));
        } else {
            linearProblemBuilder.withProblemFiller(createMaxMinMarginFiller(cnecs, rangeActions));
        }

        linearProblemBuilder.withProblemFiller(createMnecFiller(cnecs));

        if (linearOptimizerParameters.isRaoWithLoopFlowLimitation()) {
            linearProblemBuilder.withProblemFiller(createLoopFlowFiller(loopFlowCnecs));
        }
        if (!Objects.isNull(linearOptimizerParameters.getUnoptimizedCnecParameters())) {
            linearProblemBuilder.withProblemFiller(createUnoptimizedCnecFiller(cnecs));
        }
        linearProblemBuilder.withBranchResult(preOptimBranchResult);
        linearProblemBuilder.withSensitivityResult(preOptimSensitivityResult);
        return linearProblemBuilder.build();
    }
}
