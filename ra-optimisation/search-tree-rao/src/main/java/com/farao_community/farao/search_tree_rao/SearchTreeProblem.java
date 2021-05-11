/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.rao_api.parameters.*;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.RangeActionResult;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.*;
import com.powsybl.iidm.network.Network;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SearchTreeProblem {
    protected final BranchResult initialBranchResult;
    protected final BranchResult prePerimeterBranchResult;
    protected final RangeActionResult prePerimeterSetPoints;
    protected final Set<BranchCnec> cnecs;
    protected final Set<BranchCnec> loopFlowCnecs;
    protected final LinearOptimizerParameters linearOptimizerParameters;

    public SearchTreeProblem(BranchResult initialBranchResult,
                             BranchResult prePerimeterBranchResult,
                             RangeActionResult prePerimeterSetPoints,
                             Set<BranchCnec> cnecs,
                             Set<BranchCnec> loopFlowCnecs,
                             LinearOptimizerParameters linearOptimizerParameters) {
        this.initialBranchResult = initialBranchResult;
        this.prePerimeterBranchResult = prePerimeterBranchResult;
        this.prePerimeterSetPoints = prePerimeterSetPoints;
        this.cnecs = cnecs;
        this.loopFlowCnecs = loopFlowCnecs;
        this.linearOptimizerParameters = linearOptimizerParameters;
    }

    public LeafProblem getLeafProblem(Set<RangeAction> rangeActions) {
        return new LeafProblem(
                initialBranchResult,
                prePerimeterBranchResult,
                prePerimeterSetPoints,
                cnecs,
                loopFlowCnecs,
                linearOptimizerParameters,
                rangeActions
        );
    }

    protected ProblemFiller createCoreProblemFiller(Network network, Set<BranchCnec> cnecs, Set<RangeAction> rangeActions) {
        return new CoreProblemFiller(
                network,
                cnecs,
                rangeActions,
                prePerimeterSetPoints,
                linearOptimizerParameters.getPstSensitivityThreshold()
        );
    }

    protected ProblemFiller createMaxMinRelativeMarginFiller(Set<BranchCnec> cnecs, Set<RangeAction> rangeActions, BranchResult preOptimBranchResult) {
        return new MaxMinRelativeMarginFiller(
                cnecs.stream().filter(Cnec::isOptimized).collect(Collectors.toSet()),
                preOptimBranchResult,
                rangeActions,
                linearOptimizerParameters.getObjectiveFunction().getUnit(),
                linearOptimizerParameters.getMaxMinRelativeMarginParameters()
        );
    }

    protected ProblemFiller createMaxMinMarginFiller(Set<BranchCnec> cnecs, Set<RangeAction> rangeActions) {
        return new MaxMinMarginFiller(
                cnecs.stream().filter(Cnec::isOptimized).collect(Collectors.toSet()),
                rangeActions,
                linearOptimizerParameters.getUnit(),
                linearOptimizerParameters.getMaxMinMarginParameters()
        );
    }

    protected ProblemFiller createMnecFiller(Set<BranchCnec> cnecs) {
        return new MnecFiller(
                initialBranchResult,
                cnecs.stream().filter(Cnec::isMonitored).collect(Collectors.toSet()),
                linearOptimizerParameters.getUnit(),
                linearOptimizerParameters.getMnecParameters()
        );
    }

    protected ProblemFiller createLoopFlowFiller(Set<BranchCnec> loopFlowCnecs) {
        return new MaxLoopFlowFiller(
                loopFlowCnecs,
                initialBranchResult,
                linearOptimizerParameters.getLoopFlowParameters()
        );
    }

    protected ProblemFiller createUnoptimizedCnecFiller(Set<BranchCnec> cnecs) {
        return new UnoptimizedCnecFiller(
                cnecs,
                prePerimeterBranchResult,
                linearOptimizerParameters.getUnoptimizedCnecParameters()
        );
    }
}
