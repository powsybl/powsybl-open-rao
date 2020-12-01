/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_util.CracCleaner;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgramBuilder;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.*;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerWithLoopFLowsParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerWithLoopFlows;
import com.farao_community.farao.rao_commons.objective_function_evaluator.MinMarginObjectiveFunction;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.powsybl.iidm.network.Network;
import com.powsybl.ucte.util.UcteAliasesCreation;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.farao_community.farao.rao_api.RaoParameters.ObjectiveFunction.*;
import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaoUtil.class);

    private RaoUtil() {
    }

    public static void initData(RaoInput raoInput, RaoParameters raoParameters) {
        checkParameters(raoParameters, raoInput);
        initNetwork(raoInput.getNetwork(), raoInput.getNetworkVariantId());
        initCrac(raoInput.getCrac(), raoInput.getNetwork());
    }

    public static void initNetwork(Network network, String networkVariantId) {
        network.getVariantManager().setWorkingVariant(networkVariantId);
        UcteAliasesCreation.createAliases(network);
    }

    public static void initCrac(Crac crac, Network network) {
        CracCleaner.cleanCrac(crac, network);
        RaoInputHelper.synchronize(crac, network);
    }

    public static void checkParameters(RaoParameters raoParameters, RaoInput raoInput) {
        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            if (raoInput.getGlskProvider() == null) {
                throw new FaraoException("Relative margin objective function requires a GLSK provider.");
            }
            if (raoParameters.getPtdfBoundaries().isEmpty()) {
                throw new FaraoException("Relative margin objective function requires a list of pairs of country boundaries to compute PTDF sums upon.");
            }
        }

        if ((raoParameters.isRaoWithLoopFlowLimitation()
            || raoParameters.getObjectiveFunction().doesRequirePtdf())
            && (raoInput.getReferenceProgram() == null)) {
            LOGGER.info("No ReferenceProgram provided. A ReferenceProgram will be generated using information in the network file.");
            raoInput.setReferenceProgram(ReferenceProgramBuilder.buildReferenceProgram(raoInput.getNetwork(), raoParameters.getDefaultSensitivityAnalysisParameters().getLoadFlowParameters()));
        }

        if (raoParameters.isRaoWithLoopFlowLimitation() && (Objects.isNull(raoInput.getReferenceProgram()) || Objects.isNull(raoInput.getGlskProvider()))) {
            String msg = format(
                "Loopflow computation cannot be performed CRAC %s because it lacks a ReferenceProgram or a GlskProvider",
                raoInput.getCrac().getId());
            LOGGER.error(msg);
            throw new FaraoException(msg);
        }
    }

    public static SystematicSensitivityInterface createSystematicSensitivityInterface(RaoParameters raoParameters, RaoData raoData, boolean withPtdfSensitivitiesForLoopFlows) {

        SystematicSensitivityInterface.SystematicSensitivityInterfaceBuilder builder = SystematicSensitivityInterface
            .builder()
            .withDefaultParameters(raoParameters.getDefaultSensitivityAnalysisParameters())
            .withFallbackParameters(raoParameters.getFallbackSensitivityAnalysisParameters())
            .withRangeActionSensitivities(raoData.getAvailableRangeActions(), raoData.getCnecs());

        if (raoParameters.isRaoWithLoopFlowLimitation() && withPtdfSensitivitiesForLoopFlows) {
            builder.withPtdfSensitivities(raoData.getGlskProvider(), raoData.getLoopflowCnecs());
        }

        return builder.build();
    }

    public static IteratingLinearOptimizer createLinearOptimizer(RaoParameters raoParameters, SystematicSensitivityInterface systematicSensitivityInterface) {
        List<ProblemFiller> fillers = new ArrayList<>();
        fillers.add(new CoreProblemFiller(raoParameters.getPstSensitivityThreshold()));
        if (raoParameters.getObjectiveFunction().equals(MAX_MIN_MARGIN_IN_AMPERE)
                || raoParameters.getObjectiveFunction().equals(MAX_MIN_MARGIN_IN_MEGAWATT)) {
            fillers.add(new MaxMinMarginFiller(raoParameters.getObjectiveFunction().getUnit(), raoParameters.getPstPenaltyCost()));
            fillers.add(new MnecFiller(raoParameters.getObjectiveFunction().getUnit(), raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost(), raoParameters.getMnecConstraintAdjustmentCoefficient()));
        } else if (raoParameters.getObjectiveFunction().equals(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE)
                || raoParameters.getObjectiveFunction().equals(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT)) {
            fillers.add(new MaxMinRelativeMarginFiller(raoParameters.getObjectiveFunction().getUnit(), raoParameters.getPstPenaltyCost(), raoParameters.getNegativeMarginObjectiveCoefficient(), raoParameters.getPtdfSumLowerBound()));
            fillers.add(new MnecFiller(raoParameters.getObjectiveFunction().getUnit(), raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost(), raoParameters.getMnecConstraintAdjustmentCoefficient()));
        }
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            // TO DO : add relative margins to IteratingLinearOptimizerWithLoopFlows
            // or merge IteratingLinearOptimizerWithLoopFlows with IteratingLinearOptimizer
            fillers.add(createMaxLoopFlowFiller(raoParameters));
            return new IteratingLinearOptimizerWithLoopFlows(fillers, systematicSensitivityInterface,
                    createObjectiveFunction(raoParameters), createIteratingLoopFlowsParameters(raoParameters));
        } else {
            return new IteratingLinearOptimizer(fillers, systematicSensitivityInterface, createObjectiveFunction(raoParameters), createIteratingParameters(raoParameters));
        }
    }

    private static MaxLoopFlowFiller createMaxLoopFlowFiller(RaoParameters raoParameters) {
        return new MaxLoopFlowFiller(raoParameters.getLoopFlowConstraintAdjustmentCoefficient(), raoParameters.getLoopFlowViolationCost(), raoParameters.getLoopFlowApproximationLevel());
    }

    private static IteratingLinearOptimizerParameters createIteratingParameters(RaoParameters raoParameters) {
        return new IteratingLinearOptimizerParameters(raoParameters.getMaxIterations(), raoParameters.getFallbackOverCost());
    }

    private static IteratingLinearOptimizerWithLoopFLowsParameters createIteratingLoopFlowsParameters(RaoParameters raoParameters) {
        return new IteratingLinearOptimizerWithLoopFLowsParameters(raoParameters.getMaxIterations(),
                raoParameters.getFallbackOverCost(), raoParameters.getLoopFlowApproximationLevel(), raoParameters.getLoopFlowViolationCost());
    }

    public static ObjectiveFunctionEvaluator createObjectiveFunction(RaoParameters raoParameters) {
        switch (raoParameters.getObjectiveFunction()) {
            case MAX_MIN_MARGIN_IN_AMPERE:
            case MAX_MIN_MARGIN_IN_MEGAWATT:
            case MAX_MIN_RELATIVE_MARGIN_IN_AMPERE:
            case MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT:
                return new MinMarginObjectiveFunction(raoParameters);
            default:
                throw new NotImplementedException("Not implemented objective function");
        }
    }
}
