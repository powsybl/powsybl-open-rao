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
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.ProblemFiller;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.CoreProblemFiller;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.MaxLoopFlowFiller;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.MaxMinMarginFiller;
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
            LoopFlowComputation.checkDataConsistency(raoData);
            LoopFlowComputation.computeInitialLoopFlowsAndUpdateCnecLoopFlowConstraint(raoData);
        }

        return raoData;
    }

    public static IteratingLinearOptimizer createLinearOptimizerFromRaoParameters(RaoParameters raoParameters, SystematicSensitivityComputation systematicSensitivityComputation) {
        List<ProblemFiller> fillers = new ArrayList<>();
        fillers.add(new CoreProblemFiller(raoParameters.getPstSensitivityThreshold()));
        if (raoParameters.getObjectiveFunction().equals(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE)
            || raoParameters.getObjectiveFunction().equals(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)) {
            fillers.add(new MaxMinMarginFiller(raoParameters.getObjectiveFunction().getUnit(), raoParameters.getPstPenaltyCost()));
        }
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            fillers.add(new MaxLoopFlowFiller(raoParameters.isLoopFlowApproximation(), raoParameters.getLoopFlowConstraintAdjustmentCoefficient()));
            IteratingLinearOptimizerWithLoopFLowsParameters iteratingLinearOptimizerParameters =
                new IteratingLinearOptimizerWithLoopFLowsParameters(raoParameters.getObjectiveFunction().getUnit(),
                    raoParameters.getMaxIterations(), raoParameters.getFallbackOverCost(), raoParameters.isLoopFlowApproximation());
            return new IteratingLinearOptimizerWithLoopFlows(fillers, systematicSensitivityComputation,
                createCostEvaluatorFromRaoParameters(raoParameters), iteratingLinearOptimizerParameters);
        } else {
            IteratingLinearOptimizerParameters iteratingLinearOptimizerParameters = new IteratingLinearOptimizerParameters(
                raoParameters.getObjectiveFunction().getUnit(), raoParameters.getMaxIterations(), raoParameters.getFallbackOverCost());
            return new IteratingLinearOptimizer(fillers, systematicSensitivityComputation,
                createCostEvaluatorFromRaoParameters(raoParameters), iteratingLinearOptimizerParameters);
        }
    }

    public static CostEvaluator createCostEvaluatorFromRaoParameters(RaoParameters raoParameters) {
        if (raoParameters.getObjectiveFunction().equals(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE)) {
            return new MinMarginEvaluator(Unit.AMPERE);
        } else if (raoParameters.getObjectiveFunction().equals(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)) {
            return new MinMarginEvaluator(Unit.MEGAWATT);
        } else {
            throw new NotImplementedException("Not implemented objective function");
        }
    }
}
