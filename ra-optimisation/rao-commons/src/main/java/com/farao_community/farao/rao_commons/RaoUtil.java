/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgramBuilder;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.*;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerWithLoopFLowsParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerWithLoopFlows;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityInterface;
import com.farao_community.farao.util.EICode;
import com.farao_community.farao.util.SensitivityComputationService;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFactorsProvider;
import com.powsybl.sensitivity.SensitivityValue;
import com.powsybl.sensitivity.factors.BranchFlowPerLinearGlsk;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import com.powsybl.ucte.util.UcteAliasesCreation;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.farao_community.farao.rao_api.RaoParameters.ObjectiveFunction.*;
import static com.farao_community.farao.rao_api.RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaoUtil.class);

    private RaoUtil() {
    }

    public static RaoData initRaoData(RaoInput raoInput, RaoParameters raoParameters) {
        Network network = raoInput.getNetwork();
        Crac crac = raoInput.getCrac();
        String variantId = raoInput.getVariantId();

        network.getVariantManager().setWorkingVariant(variantId);
        UcteAliasesCreation.createAliases(network);
        RaoInputHelper.cleanCrac(crac, network);
        RaoInputHelper.synchronize(crac, network);

        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            if (!raoInput.getGlskProvider().isPresent()) {
                throw new FaraoException("Relative margin objective function requires a GLSK provider.");
            }
            if (Objects.isNull(raoParameters.getExtension(RaoPtdfParameters.class))
                    || Objects.isNull(raoParameters.getExtension(RaoPtdfParameters.class).getBoundaries())
                    || raoParameters.getExtension(RaoPtdfParameters.class).getBoundaries().isEmpty()) {
                throw new FaraoException("Relative margin objective function requires a list of pairs of country boundaries.");
            }
        }

        if ((raoParameters.isRaoWithLoopFlowLimitation()
                || raoParameters.getObjectiveFunction().doesRequirePtdf())
                && (!raoInput.getReferenceProgram().isPresent())) {
            LOGGER.info("No ReferenceProgram provided. A ReferenceProgram will be generated using information in the network file.");
            raoInput.setReferenceProgram(ReferenceProgramBuilder.buildReferenceProgram(raoInput.getNetwork()));
        }

        RaoData raoData = new RaoData(network, crac, raoInput.getOptimizedState(), raoInput.getPerimeter(), raoInput.getReferenceProgram().orElse(null), raoInput.getGlskProvider().orElse(null));
        crac.getExtension(ResultVariantManager.class).setPreOptimVariantId(raoData.getInitialVariantId());

        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            LoopFlowComputationService.checkDataConsistency(raoData);
            LoopFlowComputationService.computeInitialLoopFlowsAndUpdateCnecLoopFlowConstraint(raoData, raoParameters.getLoopFlowViolationCost());
        }

        // TO DO : move to ReferenceSensitivityComputation
        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            List<Pair<Country, Country>> boundaries = raoParameters.getExtension(RaoPtdfParameters.class).getBoundaries();
            computeAndSaveAbsolutePtdfSums(raoInput, raoData, boundaries);
        }

        return raoData;
    }

    public static IteratingLinearOptimizer createLinearOptimizer(RaoParameters raoParameters, SystematicSensitivityInterface systematicSensitivityInterface) {
        List<ProblemFiller> fillers = new ArrayList<>();
        fillers.add(new CoreProblemFiller(raoParameters.getPstSensitivityThreshold()));
        if (raoParameters.getObjectiveFunction().equals(MAX_MIN_MARGIN_IN_AMPERE)
                || raoParameters.getObjectiveFunction().equals(MAX_MIN_MARGIN_IN_MEGAWATT)
                || raoParameters.getObjectiveFunction().equals(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE)
                || raoParameters.getObjectiveFunction().equals(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT)) {
            fillers.add(new MaxMinMarginFiller(raoParameters.getObjectiveFunction().getUnit(), raoParameters.getPstPenaltyCost(), false));
            fillers.add(new MnecFiller(raoParameters.getObjectiveFunction().getUnit(), raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost(), raoParameters.getMnecConstraintAdjustmentCoefficient()));
        }
        boolean optimizeRelativeMargins = raoParameters.getObjectiveFunction().equals(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE) || raoParameters.getObjectiveFunction().equals(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            // TO DO : add relative margins to IteratingLinearOptimizerWithLoopFlows
            // or merge IteratingLinearOptimizerWithLoopFlows with IteratingLinearOptimizer
            fillers.add(createMaxLoopFlowFiller(raoParameters));
            return new IteratingLinearOptimizerWithLoopFlows(fillers, systematicSensitivityInterface,
                    createObjectiveFunction(raoParameters), createIteratingLoopFlowsParameters(raoParameters));
        } else {
            return new IteratingLinearOptimizer(fillers, systematicSensitivityInterface, createObjectiveFunction(raoParameters), createIteratingParameters(raoParameters), optimizeRelativeMargins);
        }
    }

    private static void computeAndSaveAbsolutePtdfSums(RaoInput raoInput, RaoData raoData, List<Pair<Country, Country>> boundaries) {
        Map<String, Double> ptdfSums = new HashMap<>();
        Map<Cnec, Map<Country, Double>> ptdfMap = computePtdfOnCurrentNetwork(raoInput.getCrac(), raoInput.getNetwork(), raoInput.getGlskProvider().orElse(null));
        raoInput.getCrac().getCnecs().forEach(cnec -> {
            double ptdfSum = 0;
            for (Pair<Country, Country> countryPair : boundaries) {
                if (ptdfMap.get(cnec).containsKey(countryPair.getLeft()) && ptdfMap.get(cnec).containsKey(countryPair.getRight())) {
                    ptdfSum += Math.abs(ptdfMap.get(cnec).get(countryPair.getLeft()).doubleValue() - ptdfMap.get(cnec).get(countryPair.getRight()).doubleValue());
                }
            }
            ptdfSums.put(cnec.getId(), ptdfSum);
        });
        raoData.getCracResult(raoData.getInitialVariantId()).setAbsPtdfSums(ptdfSums);
    }

    private static Map<Cnec, Map<Country, Double>> computePtdfOnCurrentNetwork(Crac crac, Network network, GlskProvider glskProvider) {
        Map<Cnec, Map<Country, Double>> ptdfs = new HashMap<>();
        SensitivityFactorsProvider factorsProvider = net -> generateSensitivityFactorsProvider(net, crac.getCnecs(), glskProvider);
        SensitivityComputationResults sensiResults = SensitivityComputationService.runSensitivity(network, network.getVariantManager().getWorkingVariantId(), factorsProvider);
        sensiResults.getSensitivityValues().forEach(sensitivityValue -> addSensitivityValue(sensitivityValue, crac, ptdfs));
        return ptdfs;
    }

    private static List<SensitivityFactor> generateSensitivityFactorsProvider(Network network, Set<Cnec> cnecs, GlskProvider glskProvider) {
        List<SensitivityFactor> factors = new ArrayList<>();
        Map<String, LinearGlsk> mapCountryLinearGlsk = glskProvider.getAllGlsk(network);
        cnecs.forEach(cnec -> mapCountryLinearGlsk.values().stream()
                .map(linearGlsk -> new BranchFlowPerLinearGlsk(new BranchFlow(cnec.getId(), cnec.getName(), cnec.getNetworkElement().getId()), linearGlsk))
                .forEach(factors::add));
        return factors;
    }

    private static void addSensitivityValue(SensitivityValue sensitivityValue, Crac crac, Map<Cnec, Map<Country, Double>> ptdfs) {
        String cnecId = sensitivityValue.getFactor().getFunction().getId();
        Cnec cnec = crac.getCnec(cnecId);
        String glskId = sensitivityValue.getFactor().getVariable().getId();
        double ptdfValue = sensitivityValue.getValue();
        if (!ptdfs.containsKey(cnec)) {
            ptdfs.put(cnec, new HashMap<>());
        }
        ptdfs.get(cnec).put(glskIdToCountry(glskId), ptdfValue);
    }

    private static Country glskIdToCountry(String glskId) {
        if (glskId.length() < EICode.LENGTH) {
            throw new IllegalArgumentException(String.format("GlskId [%s] should starts with an EI Code", glskId));
        }
        EICode eiCode = new EICode(glskId.substring(0, EICode.LENGTH));
        return eiCode.getCountry();
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

    public static ObjectiveFunctionEvaluator createObjectiveFunction(RaoParameters raoParameters) {
        switch (raoParameters.getObjectiveFunction()) {
            case MAX_MIN_MARGIN_IN_AMPERE:
                return new MinMarginObjectiveFunction(Unit.AMPERE, raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost());
            case MAX_MIN_MARGIN_IN_MEGAWATT:
                return new MinMarginObjectiveFunction(Unit.MEGAWATT, raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost());
            case MAX_MIN_RELATIVE_MARGIN_IN_AMPERE:
                return new MinRelativeMarginObjectiveFunction(Unit.AMPERE, raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost());
            case MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT:
                return new MinRelativeMarginObjectiveFunction(Unit.MEGAWATT, raoParameters.getMnecAcceptableMarginDiminution(), raoParameters.getMnecViolationCost());
            default:
                throw new NotImplementedException("Not implemented objective function");
        }
    }
}
