/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityResult;
import com.farao_community.farao.util.EICode;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class aims at performing the initial sensitivity analysis of a RAO, the one
 * which defines the pre-optimisation variant. It is common to both the Search Tree
 * and the Linear RAO.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InitialSensitivityAnalysis {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialSensitivityAnalysis.class);
    private RaoData raoData;
    private RaoParameters raoParameters;
    private SystematicSensitivityInterface systematicSensitivityInterface;

    public InitialSensitivityAnalysis(RaoData raoData, RaoParameters raoParameters) {
        this.raoData = raoData;
        this.raoParameters = raoParameters;
        this.systematicSensitivityInterface = getSystematicSensitivityInterface();
    }

    public void run() {
        LOGGER.info("Initial systematic analysis [start]");

        runSensitivityComputation();

        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            LOGGER.info("Initial systematic analysis [...] - fill reference loop-flow values");
            fillReferenceLoopFlow();
        }

        fillReferenceFlowsAndObjectiveFunction();

        LOGGER.info("Initial systematic analysis [end] - with initial min margin of {} MW", -raoData.getCracResult().getFunctionalCost());
    }

    private void runSensitivityComputation() {
        SystematicSensitivityResult sensitivityResult = systematicSensitivityInterface.run(raoData.getNetwork(), raoParameters.getObjectiveFunction().getUnit());
        raoData.setSystematicSensitivityResult(sensitivityResult);
        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            computeAndSaveAbsolutePtdfSums(raoData, raoParameters.getExtension(RaoPtdfParameters.class).getBoundaries(), sensitivityResult);
        }
    }

    private void fillReferenceFlowsAndObjectiveFunction() {
        ObjectiveFunctionEvaluator objectiveFunction = RaoUtil.createObjectiveFunction(raoParameters);
        raoData.getRaoDataManager().fillCnecResultWithFlows();
        raoData.getRaoDataManager().fillCracResultWithCosts(
            objectiveFunction.getFunctionalCost(raoData), objectiveFunction.getVirtualCost(raoData));
    }

    private void fillReferenceLoopFlow() {
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(raoData.getCrac(), raoData.getGlskProvider(), raoData.getReferenceProgram());
        LoopFlowResult lfResults = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(raoData.getSystematicSensitivityResult(), raoData.getNetwork());
        raoData.getRaoDataManager().fillCnecLoopFlowExtensionsWithInitialResults(lfResults, raoData.getNetwork());
        raoData.getRaoDataManager().fillCnecResultsWithLoopFlows(lfResults);
    }

    private SystematicSensitivityInterface getSystematicSensitivityInterface() {

        SystematicSensitivityInterface.SystematicSensitivityInterfaceBuilder builder = SystematicSensitivityInterface.builder()
            .withDefaultParameters(raoParameters.getDefaultSensitivityComputationParameters())
            .withFallbackParameters(raoParameters.getFallbackSensitivityComputationParameters())
            .withRangeActionSensitivities(raoData.getAvailableRangeActions(), raoData.getCnecs());

        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            builder.withPtdfSensitivities(raoData.getGlskProvider(), raoData.getCrac().getCnecs());
        } else if (raoParameters.isRaoWithLoopFlowLimitation()) {
            builder.withPtdfSensitivities(raoData.getGlskProvider(), raoData.getCrac().getCnecs(raoData.getCrac().getPreventiveState()));
        }

        return builder.build();
    }

    private static void computeAndSaveAbsolutePtdfSums(RaoData raoData, List<Pair<Country, Country>> boundaries, SystematicSensitivityResult sensitivityResult) {
        Map<String, Double> ptdfSums = new HashMap<>();
        Map<String, Map<Country, Double>> ptdfMap = computePtdf(raoData.getCnecs(), raoData.getNetwork(), raoData.getGlskProvider(), sensitivityResult);
        raoData.getCrac().getCnecs().forEach(cnec -> {
            double ptdfSum = 0;
            for (Pair<Country, Country> countryPair : boundaries) {
                if (ptdfMap.get(cnec.getId()).containsKey(countryPair.getLeft()) && ptdfMap.get(cnec.getId()).containsKey(countryPair.getRight())) {
                    ptdfSum += Math.abs(ptdfMap.get(cnec.getId()).get(countryPair.getLeft()).doubleValue() - ptdfMap.get(cnec.getId()).get(countryPair.getRight()).doubleValue());
                }
            }
            ptdfSums.put(cnec.getId(), ptdfSum);
        });
        raoData.getCracResult(raoData.getInitialVariantId()).setAbsPtdfSums(ptdfSums);
    }

    private static Map<String, Map<Country, Double>> computePtdf(Set<Cnec> cnecs, Network network, GlskProvider glskProvider, SystematicSensitivityResult ptdfsAndRefFlows) {
        Map<String, Map<Country, Double>> ptdfs = new HashMap<>();
        Map<String, LinearGlsk> mapCountryLinearGlsk = glskProvider.getAllGlsk(network);
        Map<String, Double> ptdfSums = new HashMap<>();
        for (Cnec cnec : cnecs) {
            for (LinearGlsk linearGlsk: mapCountryLinearGlsk.values()) {
                double ptdfValue = ptdfsAndRefFlows.getSensitivityOnFlow(linearGlsk, cnec);
                Country country = glskIdToCountry(linearGlsk.getId());
                if (!ptdfs.containsKey(cnec.getId())) {
                    ptdfs.put(cnec.getId(), new HashMap<>());
                }
                ptdfs.get(cnec.getId()).put(country, ptdfValue);
            }
        }
        return ptdfs;
    }

    private static Country glskIdToCountry(String glskId) {
        if (glskId.length() < EICode.LENGTH) {
            throw new IllegalArgumentException(String.format("GlskId [%s] should starts with an EI Code", glskId));
        }
        EICode eiCode = new EICode(glskId.substring(0, EICode.LENGTH));
        return eiCode.getCountry();
    }

}
