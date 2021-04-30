/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.BranchResult;

import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RelativeMinMarginEvaluator extends AbstractMinMarginEvaluator {

    public RelativeMinMarginEvaluator(Set<BranchCnec> cnecs, Unit unit, Set<String> countriesNotToOptimize, BranchResult prePerimeterBranchResult) {
        super(cnecs, unit, countriesNotToOptimize, prePerimeterBranchResult);
    }

    @Override
    double getMargin(BranchResult branchResult, BranchCnec branchCnec, Unit unit) {
        double newMargin = branchResult.getRelativeMargin(branchCnec, unit);
        if (countriesNotToOptimize.contains(branchCnec.getOperator())) {
            double prePerimeterMargin = prePerimeterBranchResult.getRelativeMargin(branchCnec, unit);
            if (newMargin > prePerimeterMargin - .0001 * Math.abs(prePerimeterMargin)) {
                return Double.MAX_VALUE;
            }
        }
        return newMargin;
    }

    @Override
    public String getName() {
        return "relative-min-margin-cost";
    }
}
