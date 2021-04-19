/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThresholdImpl;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_commons.RaoData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowViolationCostEvaluator implements CostEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoopFlowViolationCostEvaluator.class);

    private double violationCost;
    private double loopFlowAcceptableAugmentation;

    LoopFlowViolationCostEvaluator(double violationCost, double loopFlowAcceptableAugmentation) {
        this.violationCost = violationCost;
        this.loopFlowAcceptableAugmentation = loopFlowAcceptableAugmentation;
    }

    @Override
    public double getCost(RaoData raoData) {
        String initialVariantId = raoData.getCrac().getExtension(ResultVariantManager.class).getInitialVariantId();

        double cost = raoData.getLoopflowCnecs()
            .stream()
            .mapToDouble(cnec -> getLoopFlowExcess(raoData, cnec, initialVariantId) * violationCost)
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

    private double getLoopFlowExcess(RaoData raoData, BranchCnec cnec, String initialVariantId) {
        CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId());
        return Math.max(0, Math.abs(cnecResult.getLoopflowInMW()) - getLoopFlowUpperBound(cnec, initialVariantId));
    }

    private double getLoopFlowUpperBound(BranchCnec cnec, String initialVariantId) {
        double loopFlowThreshold = cnec.getExtension(LoopFlowThresholdImpl.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT);
        double initialLoopFlow = cnec.getExtension(CnecResultExtension.class).getVariant(initialVariantId).getLoopflowInMW();
        return Math.max(0.0, Math.max(loopFlowThreshold, Math.abs(initialLoopFlow) + loopFlowAcceptableAugmentation));
    }
}
