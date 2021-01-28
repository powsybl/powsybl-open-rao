/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.commons.ZonalDataImpl;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgramArea;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.farao_community.farao.util.EICode;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private SystematicSensitivityInterface systematicSensitivityInterface;
    private RaoParameters raoParameters;

    public InitialSensitivityAnalysis(RaoData raoData) {
        // it is actually quite strange to ask for a RaoData here, but it is required in
        // order to use the fillResultsWithXXX() methods of the CracResultManager.

        this.raoData = raoData;
        this.raoParameters = raoData.getRaoParameters();
        this.systematicSensitivityInterface = getSystematicSensitivityInterface();
    }

    public void run() {
        LOGGER.info("Initial systematic analysis [start]");

        // run sensitivity analysis
        SystematicSensitivityResult sensitivityResult = runSensitivityComputation();

        // tag initial variant
        raoData.getCrac().getExtension(ResultVariantManager.class).setInitialVariantId(raoData.getWorkingVariantId());
        raoData.getCrac().getExtension(ResultVariantManager.class).setPrePerimeterVariantId(raoData.getWorkingVariantId());

        // fill results of initial variant
        LOGGER.info("Initial systematic analysis [...] - fill initial range actions values");
        raoData.getCracResultManager().fillRangeActionResultsWithNetworkValues();

        LOGGER.info("Initial systematic analysis [...] - fill reference flow values");
        raoData.getCracResultManager().fillCnecResultWithFlows();

        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            LOGGER.info("Initial systematic analysis [...] - fill reference loop-flow values");
            fillReferenceLoopFlow();
        }
        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            LOGGER.info("Initial systematic analysis [...] - fill zone-to-zone PTDFs");
            fillAbsolutePtdfSums(sensitivityResult);
        }

        fillObjectiveFunction();

        LOGGER.info("Initial systematic analysis [end] - with initial min margin of {} MW", -raoData.getCracResult().getFunctionalCost());
    }

    private SystematicSensitivityResult runSensitivityComputation() {
        SystematicSensitivityResult sensitivityResult = systematicSensitivityInterface.run(raoData.getNetwork());
        raoData.setSystematicSensitivityResult(sensitivityResult);
        return sensitivityResult;
    }

    private void fillObjectiveFunction() {
        ObjectiveFunctionEvaluator objectiveFunction = RaoUtil.createObjectiveFunction(raoParameters);
        raoData.getCracResultManager().fillCracResultWithCosts(
            objectiveFunction.getFunctionalCost(raoData), objectiveFunction.getVirtualCost(raoData));
    }

    private void fillReferenceLoopFlow() {
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(raoData.getGlskProvider(), raoData.getReferenceProgram());
        LoopFlowResult lfResults = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(raoData.getSystematicSensitivityResult(), raoData.getLoopflowCnecs());
        raoData.getCracResultManager().fillCnecResultsWithLoopFlows(lfResults);
    }

    private void fillAbsolutePtdfSums(SystematicSensitivityResult sensitivityResult) {
        Map<BranchCnec, Double> ptdfSums = AbsolutePtdfSumsComputation.computeAbsolutePtdfSums(raoData.getCnecs(), raoData.getGlskProvider(), raoParameters.getPtdfBoundaries(), sensitivityResult);
        ptdfSums.forEach((key, value) -> key.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId()).setAbsolutePtdfSum(value));
    }

    private SystematicSensitivityInterface getSystematicSensitivityInterface() {

        Set<Unit> flowUnits = new HashSet<>();
        flowUnits.add(Unit.MEGAWATT);
        if (!raoParameters.getDefaultSensitivityAnalysisParameters().getLoadFlowParameters().isDc()) {
            flowUnits.add(Unit.AMPERE);
        }

        SystematicSensitivityInterface.SystematicSensitivityInterfaceBuilder builder = SystematicSensitivityInterface.builder()
            .withDefaultParameters(raoParameters.getDefaultSensitivityAnalysisParameters())
            .withFallbackParameters(raoParameters.getFallbackSensitivityAnalysisParameters())
            .withRangeActionSensitivities(raoData.getAvailableRangeActions(), raoData.getCnecs(), flowUnits);

        if (raoParameters.getObjectiveFunction().doesRequirePtdf() && raoParameters.isRaoWithLoopFlowLimitation()) {
            Set<String> eic = getEicForObjectiveFunction();
            eic.addAll(getEicForLoopFlows());
            builder.withPtdfSensitivities(getGlskForEic(eic), raoData.getCnecs(), Collections.singleton(Unit.MEGAWATT));
        } else if (raoParameters.isRaoWithLoopFlowLimitation()) {
            builder.withPtdfSensitivities(getGlskForEic(getEicForLoopFlows()), raoData.getLoopflowCnecs(), Collections.singleton(Unit.MEGAWATT));
        } else if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            builder.withPtdfSensitivities(getGlskForEic(getEicForObjectiveFunction()), raoData.getCnecs(), Collections.singleton(Unit.MEGAWATT));
        }

        return builder.build();
    }

    private Set<String> getEicForObjectiveFunction() {
        return raoParameters.getPtdfBoundaries().stream().
            flatMap(pair -> Stream.of(pair.getLeft(), pair.getRight())).
            map(country -> new EICode(country).getCode()).
            collect(Collectors.toSet());
    }

    private Set<String> getEicForLoopFlows() {
        return raoData.getReferenceProgram().getListOfAreas().stream().
            map(ReferenceProgramArea::getAreaCode).
            collect(Collectors.toSet());
    }

    private ZonalData<LinearGlsk> getGlskForEic(Set<String> listEicCode) {
        Map<String, LinearGlsk> glskBoundaries = new HashMap<>();

        raoData.getGlskProvider().getDataPerZone().forEach((k, v) -> {
            if (listEicCode.contains(k)) {
                glskBoundaries.put(k, v);
            }
        });

        return new ZonalDataImpl<>(glskBoundaries);
    }
}
