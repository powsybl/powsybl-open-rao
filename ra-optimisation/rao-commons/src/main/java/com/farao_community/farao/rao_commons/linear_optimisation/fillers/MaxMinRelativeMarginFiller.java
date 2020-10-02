/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Map;
import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MaxMinRelativeMarginFiller extends MaxMinMarginFiller {
    /**
     * MaxMinRelativeMarginFiller constructor
     * @param unit unit used in optimization problem (MW / A)
     * @param pstPenaltyCost penalty cost applied upon using PSTs
     */
    public MaxMinRelativeMarginFiller(Unit unit, double pstPenaltyCost) {
        super(unit, pstPenaltyCost);
    }

    @Override
    public void fill(RaoData raoData, LinearProblem linearProblem) {
        super.fill(raoData, linearProblem);
        updateMinMarginDefinition(raoData, linearProblem);
    }

    /**
     * This function changes the minimum margin definition by dividing all margins by the sum of PTDFs,
     * in order to have a minimum relative margin
     */
    private void updateMinMarginDefinition(RaoData raoData, LinearProblem linearProblem) {
        Map<String, Double> ptdfSums = raoData.getCrac().getExtension(CracResultExtension.class).getVariant(raoData.getInitialVariantId()).getPtdfSums();
        raoData.getCnecs().stream().filter(Cnec::isOptimized).forEach(cnec -> {
            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);
            Double ptdfSum = ptdfSums.get(cnec.getId());

            MPConstraint minimumMarginNegative = linearProblem.getMinimumMarginConstraint(cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD);
            if (!Objects.isNull(minimumMarginNegative)) {
                minimumMarginNegative.setCoefficient(flowVariable, minimumMarginNegative.getCoefficient(flowVariable) / ptdfSum);
            }

            MPConstraint minimumMarginPositive = linearProblem.getMinimumMarginConstraint(cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
            if (!Objects.isNull(minimumMarginPositive)) {
                minimumMarginPositive.setCoefficient(flowVariable, minimumMarginPositive.getCoefficient(flowVariable) / ptdfSum);
            }
        });
    }
}
