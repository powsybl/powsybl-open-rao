/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgramBuilder;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.objective_function_evaluator.MinMarginObjectiveFunction;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.*;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.*;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityInterface;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

import com.powsybl.ucte.util.UcteAliasesCreation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.farao_community.farao.rao_api.RaoParameters.ObjectiveFunction.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaoUtil.class);

    private RaoUtil() { }

    public static RaoData initRaoData(RaoInput raoInput, RaoParameters raoParameters) {
        Network network = raoInput.getNetwork();
        Crac crac = raoInput.getCrac();
        String variantId = raoInput.getVariantId();

        network.getVariantManager().setWorkingVariant(variantId);
        UcteAliasesCreation.createAliases(network);
        RaoInputHelper.cleanCrac(crac, network);
        RaoInputHelper.synchronize(crac, network);

        if ((raoParameters.isRaoWithLoopFlowLimitation()
                || raoParameters.getObjectiveFunction().doesRequirePtdf())
                && (!raoInput.getReferenceProgram().isPresent())) {
            LOGGER.info("No ReferenceProgram provided. A ReferenceProgram will be generated using information in the network file.");
            raoInput.setReferenceProgram(ReferenceProgramBuilder.buildReferenceProgram(raoInput.getNetwork()));
        }

        RaoData raoData = new RaoData(network, crac, raoInput.getOptimizedState(), raoInput.getPerimeter(), raoInput.getReferenceProgram().orElse(null), raoInput.getGlskProvider().orElse(null));
        crac.getExtension(ResultVariantManager.class).setPreOptimVariantId(raoData.getInitialVariantId());

        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            LoopFlowUtil.checkDataConsistency(raoData);
        }

        return raoData;
    }

    public static SystematicSensitivityInterface createSystematicSensitivityInterface(RaoParameters raoParameters, RaoData raoData) {

        SystematicSensitivityInterface.SystematicSensitivityInterfaceBuilder builder = SystematicSensitivityInterface
            .builder()
            .withDefaultParameters(raoParameters.getDefaultSensitivityAnalysisParameters())
            .withFallbackParameters(raoParameters.getFallbackSensitivityAnalysisParameters())
            .withRangeActionSensitivities(raoData.getAvailableRangeActions(), raoData.getCnecs());

        if (raoParameters.isRaoWithLoopFlowLimitation() && !raoParameters.isLoopFlowApproximation()) {

            builder.withPtdfSensitivities(raoData.getGlskProvider(), raoData.getCrac().getCnecs(raoData.getCrac().getPreventiveState()));

            // For now, we compute the PTDF for all the preventive states at the LoopFLowComputation API does not allow
            // to compute the loopFlows only for a given set of cnecs
            // todo : compute sensitivities for Cnec with loopFlowExtensions only

            // We may also want to have a different interface for the first run and the successive runs if we do not wish to
            // compute the PTDFs at every iteration.
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
        }
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            fillers.add(createMaxLoopFlowFiller(raoParameters));
            return new IteratingLinearOptimizerWithLoopFlows(fillers, systematicSensitivityInterface,
                createObjectiveFunction(raoParameters), createIteratingLoopFlowsParameters(raoParameters));
        } else {
            return new IteratingLinearOptimizer(fillers, systematicSensitivityInterface, createObjectiveFunction(raoParameters), createIteratingParameters(raoParameters));
        }
    }

    private static MaxLoopFlowFiller createMaxLoopFlowFiller(RaoParameters raoParameters) {
        return new MaxLoopFlowFiller(raoParameters.isLoopFlowApproximation(),
            raoParameters.getLoopFlowConstraintAdjustmentCoefficient(), raoParameters.getLoopFlowViolationCost(), raoParameters.getDefaultSensitivityAnalysisParameters());
    }

    private static IteratingLinearOptimizerParameters createIteratingParameters(RaoParameters raoParameters) {
        return new IteratingLinearOptimizerParameters(raoParameters.getMaxIterations(), raoParameters.getFallbackOverCost());
    }

    private static IteratingLinearOptimizerWithLoopFLowsParameters createIteratingLoopFlowsParameters(RaoParameters raoParameters) {
        return new IteratingLinearOptimizerWithLoopFLowsParameters(raoParameters.getMaxIterations(),
            raoParameters.getFallbackOverCost(), raoParameters.isLoopFlowApproximation(), raoParameters.getLoopFlowViolationCost());
    }

    public static ObjectiveFunctionEvaluator createObjectiveFunction(RaoParameters raoParameters) {
        switch (raoParameters.getObjectiveFunction()) {
            case MAX_MIN_MARGIN_IN_AMPERE:
            case MAX_MIN_MARGIN_IN_MEGAWATT:
                return new MinMarginObjectiveFunction(raoParameters);
            default:
                throw new NotImplementedException("Not implemented objective function");
        }
    }
}
