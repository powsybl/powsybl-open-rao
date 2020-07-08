/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_commons.*;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.*;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerWithLoopFLowsParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerWithLoopFlows;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoUtil {

    private RaoUtil() { }

    public static RaoData initRaoData(Network network, Crac crac, String variantId, RaoParameters raoParameters) {
        network.getVariantManager().setWorkingVariant(variantId);
        RaoInput.cleanCrac(crac, network);
        RaoInput.synchronize(crac, network);
        RaoData raoData = new RaoData(network, crac);
        crac.getExtension(ResultVariantManager.class).setPreOptimVariantId(raoData.getInitialVariantId());

        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            LoopFlowComputationService.checkDataConsistency(raoData);
            LoopFlowComputationService.computeInitialLoopFlowsAndUpdateCnecLoopFlowConstraint(raoData, raoParameters.getLoopFlowViolationCost());
        }

        return raoData;
    }

    public static IteratingLinearOptimizer createLinearOptimizer(RaoParameters raoParameters, SystematicSensitivityComputation systematicSensitivityComputation) {
        List<ProblemFiller> fillers = new ArrayList<>();
        fillers.add(new CoreProblemFiller(raoParameters.getPstSensitivityThreshold()));
        if (raoParameters.getObjectiveFunction().equals(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE)
            || raoParameters.getObjectiveFunction().equals(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)) {
            fillers.add(new MaxMinMarginFiller(raoParameters.getObjectiveFunction().getUnit(), raoParameters.getPstPenaltyCost()));
            fillers.add(new MnecFiller(raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost(), raoParameters.getMnecConstraintAdjustmentCoefficient()));
        }
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            fillers.add(createMaxLoopFlowFiller(raoParameters));
            return new IteratingLinearOptimizerWithLoopFlows(fillers, systematicSensitivityComputation,
                createCostEvaluator(raoParameters), createVirtualCostEvaluator(raoParameters), createIteratingLoopFlowsParameters(raoParameters));
        } else {
            return new IteratingLinearOptimizer(fillers, systematicSensitivityComputation, createCostEvaluator(raoParameters),
                    createVirtualCostEvaluator(raoParameters), createIteratingParameters(raoParameters));
        }
    }

    private static MaxLoopFlowFiller createMaxLoopFlowFiller(RaoParameters raoParameters) {
        return new MaxLoopFlowFiller(raoParameters.isLoopFlowApproximation(),
            raoParameters.getLoopFlowConstraintAdjustmentCoefficient(), raoParameters.getLoopFlowViolationCost());
    }

    private static IteratingLinearOptimizerParameters createIteratingParameters(RaoParameters raoParameters) {
        return new IteratingLinearOptimizerParameters(raoParameters.getMaxIterations(), raoParameters.getFallbackOverCost());
    }

    private static IteratingLinearOptimizerWithLoopFLowsParameters createIteratingLoopFlowsParameters(RaoParameters raoParameters) {
        return new IteratingLinearOptimizerWithLoopFLowsParameters(raoParameters.getMaxIterations(),
            raoParameters.getFallbackOverCost(), raoParameters.isLoopFlowApproximation(), raoParameters.getLoopFlowViolationCost());
    }

    public static CostEvaluator createCostEvaluator(RaoParameters raoParameters) {
        if (raoParameters.getObjectiveFunction().equals(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE)) {
            return new MinMarginEvaluator(Unit.AMPERE);
        } else if (raoParameters.getObjectiveFunction().equals(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)) {
            return new MinMarginEvaluator(Unit.MEGAWATT);
        } else {
            throw new NotImplementedException("Not implemented objective function");
        }
    }

    public static CostEvaluator createVirtualCostEvaluator(RaoParameters raoParameters) {
        if (raoParameters.getObjectiveFunction().equals(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE)) {
            return new MnecViolationCostEvaluator(Unit.AMPERE, raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost());
        } else if (raoParameters.getObjectiveFunction().equals(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)) {
            return new MnecViolationCostEvaluator(Unit.MEGAWATT, raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost());
        } else {
            throw new NotImplementedException("Not implemented virtual objective function");
        }
    }
}
