/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.rao_commons.RaoData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowViolationCostEvaluator implements CostEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoopFlowViolationCostEvaluator.class);

    private static final double MAX_LOOP_FLOW_VIOLATION_COST = 1000000.0;
    private double violationCost;

    LoopFlowViolationCostEvaluator(double violationCost) {
        if (Math.abs(violationCost) > 0) {
            this.violationCost = violationCost;
        } else {
            this.violationCost = MAX_LOOP_FLOW_VIOLATION_COST;
        }
    }

    @Override
    public double getCost(RaoData raoData) {
        double cost = raoData.getCnecs().stream()
            .filter(cnec -> !cnec.getState().getContingency().isPresent()) // preventive state
            .filter(cnec -> cnec.getExtension(CnecLoopFlowExtension.class) != null) // with loop-flow extension
            .mapToDouble(cnec -> getLoopFlowExcess(raoData, cnec) * violationCost)
            .sum();

        if (cost > 0) {
            LOGGER.info("Some loopflow constraints are not respected.");
        }

        return cost;
    }

    @Override
    public Unit getUnit() {
        return Unit.MEGAWATT;
    }

    private double getLoopFlowExcess(RaoData raoData, Cnec cnec) {
        CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId());
        return Math.max(0, Math.abs(cnecResult.getLoopflowInMW()) - Math.abs(cnecResult.getLoopflowThresholdInMW()));
    }
}
