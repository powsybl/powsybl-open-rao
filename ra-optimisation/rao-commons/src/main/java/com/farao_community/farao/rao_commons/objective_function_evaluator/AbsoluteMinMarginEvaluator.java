/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.BranchResult;

import java.util.*;

/**
 * It enables to evaluate the absolute or relative minimal margin as a cost
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class AbsoluteMinMarginEvaluator extends AbstractMinMarginEvaluator {

    public AbsoluteMinMarginEvaluator(Set<BranchCnec> cnecs, Unit unit) {
        super(cnecs, unit);
    }

    @Override
    double getMargin(BranchResult branchResult, BranchCnec branchCnec, Unit unit) {
        return branchResult.getMargin(branchCnec, unit);
    }

    @Override
    public String getName() {
        return "absolute-min-margin-cost";
    }


    /*protected List<BranchCnec> getMostLimitingElements(FlowResult flowResult, int numberOfElements) {
        if (sortedElements.isEmpty()) {
            sortedElements = cnecs.stream()
                    .sorted(Comparator.comparing(branchCnec -> getCnecMarginWithoutCnecNotToOptimize(flowResult, branchCnec, unit)))
                    .collect(Collectors.toList());
        }

        return sortedElements.subList(0, Math.min(sortedElements.size(), numberOfElements));
    }

    private BranchCnec getMostLimitingElement(FlowResult flowResult) {
        return getMostLimitingElements(flowResult, 1).get(0);
    }

    public double computeCost(FlowResult flowResult) {
        return flowResult.getMargin(getMostLimitingElement(flowResult), unit);
    }

    private double getCnecMarginWithoutCnecNotToOptimize(FlowResult flowResult, BranchCnec cnec, Unit unit) {
        if (operatorsNotToOptimize.contains(cnec.getOperator())) {
            // do not consider this kind of cnecs if they have a better margin than before optimization
            double prePerimeterMarginInAbsoluteMW = prePerimeterMarginsInAbsoluteMW.get(cnec);
            double newMarginInAbsoluteMW = flowResult.getMargin(cnec, MEGAWATT);
            if (newMarginInAbsoluteMW > prePerimeterMarginInAbsoluteMW - .0001 * Math.abs(prePerimeterMarginInAbsoluteMW)) {
                return Double.MAX_VALUE;
            }
        }
        return relativePositiveMargins
                ? flowResult.getRelativeMargin(cnec, unit)
                : flowResult.getMargin(cnec, unit);
    }*/
}
