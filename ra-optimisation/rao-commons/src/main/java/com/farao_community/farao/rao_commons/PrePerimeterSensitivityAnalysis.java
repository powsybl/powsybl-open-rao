/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.*;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.SensitivityStatus;
import com.farao_community.farao.rao_commons.adapter.BranchResultAdapter;
import com.farao_community.farao.rao_commons.adapter.BranchResultAdapterWithFixedPtdfsAndCommercialFlows;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class aims at performing the initial sensitivity analysis of a RAO, the one
 * which defines the pre-optimisation variant. It is common to both the Search Tree
 * and the Linear RAO.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PrePerimeterSensitivityAnalysis {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrePerimeterSensitivityAnalysis.class);
    private SystematicSensitivityInterface systematicSensitivityInterface;
    private RaoParameters raoParameters;
    private RaoInput raoInput;
    private Network network;
    private Set<BranchCnec> cnecs;
    private Set<RangeAction> rangeActions;
    private Set<BranchCnec> loopflowCnecs;
    private Set<String> countriesNotToOptimize;
    private BranchResult initialBranchResult;

    public PrePerimeterSensitivityAnalysis(RaoInput raoInput, Set<State> optimizedStates, Set<State> perimeter, RaoParameters raoParameters, Set<String> countriesNotToOptimize, BranchResult initialBranchResult) {
        // it is actually quite strange to ask for a RaoData here, but it is required in
        // order to use the fillResultsWithXXX() methods of the CracResultManager.
        this.raoInput = raoInput;
        this.raoParameters = raoParameters;
        this.countriesNotToOptimize = countriesNotToOptimize;
        this.initialBranchResult = initialBranchResult;
        this.cnecs = RaoUtil.computePerimeterCnecs(raoInput.getCrac(), perimeter);
        rangeActions = new HashSet<>();
        optimizedStates.forEach(optimizedState -> rangeActions.addAll(raoInput.getCrac().getRangeActions(raoInput.getNetwork(), optimizedState, UsageMethod.AVAILABLE)));
        this.network = raoInput.getNetwork();
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            LoopFlowUtil.computeLoopflowCnecs(cnecs, raoInput.getNetwork(), raoParameters);
        }
        this.systematicSensitivityInterface = createSystematicSensitivityInterface();
    }

    public PrePerimeterSensitivityAnalysisOutput run() {
        LOGGER.info("Initial systematic analysis [start]");

        // run sensitivity analysis
        SystematicSensitivityResult sensitivityResult = systematicSensitivityInterface.run(network);

        Map<BranchCnec, Double> commercialFlows = null;
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            LOGGER.info("Initial systematic analysis [...] - compute reference loop-flow values");
            commercialFlows = LoopFlowUtil.computeCommercialFlows(network, loopflowCnecs, raoInput.getGlskProvider(), raoInput.getReferenceProgram(), sensitivityResult);
        }
        SensitivityStatus sensitivityStatus = systematicSensitivityInterface.isFallback() ? SensitivityStatus.FALLBACK : SensitivityStatus.DEFAULT;
        SensitivityAndLoopflowResults sensitivityAndLoopflowResults = new SensitivityAndLoopflowResults(sensitivityResult, sensitivityStatus, commercialFlows);

        Map<BranchCnec, Double> ptdfSums = new HashMap<>();
        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            LOGGER.info("Initial systematic analysis [...] - fill zone-to-zone PTDFs");
            ptdfSums = AbsolutePtdfSumsComputation.computeAbsolutePtdfSums(cnecs, raoInput.getGlskProvider(), raoParameters.getRelativeMarginPtdfBoundaries(), sensitivityResult);
        }

        Map<RangeAction, Double> rangeActionSetPoints = new HashMap<>();
        Map<PstRangeAction, Integer> pstTaps = new HashMap<>();
        for (RangeAction rangeAction : rangeActions) {
            rangeActionSetPoints.put(rangeAction, rangeAction.getCurrentValue(network));
            if (rangeAction instanceof PstRangeAction) {
                PstRangeAction pstRangeAction = (PstRangeAction) rangeAction;
                pstTaps.put(pstRangeAction, pstRangeAction.getCurrentTapPosition(network, RangeDefinition.CENTERED_ON_ZERO));
            }
        }

        Map<BranchCnec, Double> cnecFlowsInMW = new HashMap<>();
        Map<BranchCnec, Double> cnecFlowsInA = new HashMap<>();
        Map<BranchCnec, Double> prePerimeterMarginsInAbsoluteMW = new HashMap<>();
        cnecs.forEach(cnec -> {
            cnecFlowsInMW.put(cnec, sensitivityAndLoopflowResults.getSystematicSensitivityResult().getReferenceFlow(cnec));
            cnecFlowsInA.put(cnec, sensitivityAndLoopflowResults.getSystematicSensitivityResult().getReferenceIntensity(cnec));
            prePerimeterMarginsInAbsoluteMW.put(cnec, cnec.computeMargin(sensitivityAndLoopflowResults.getSystematicSensitivityResult().getReferenceFlow(cnec), Side.LEFT, Unit.MEGAWATT));
        });

        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            Map<BranchCnec, Double> loopflowsInMW = new HashMap<>();
            Map<BranchCnec, Double> finalCommercialFlows = commercialFlows;
            loopflowCnecs.forEach(cnec -> loopflowsInMW.put(cnec, cnecFlowsInMW.get(cnec) - finalCommercialFlows.get(cnec)));
        }

        BranchResultAdapter branchResultAdapter = new BranchResultAdapterWithFixedPtdfsAndCommercialFlows(ptdfSums, commercialFlows);
        BranchResult branchResult = branchResultAdapter.getResult(sensitivityResult);
        if (Objects.isNull(initialBranchResult)) {
            initialBranchResult = branchResult;
        }
        ObjectiveFunction objectiveFunction = RaoUtil.createObjectiveFunction(raoParameters, cnecs, loopflowCnecs, countriesNotToOptimize, initialBranchResult, branchResult);

        double functionalCost = objectiveFunction.getFunctionalCost(branchResult, sensitivityAndLoopflowResults.getSensitivityStatus());
        LOGGER.info("Initial systematic analysis [end] - with initial min margin of {} MW", -functionalCost);

        PrePerimeterSensitivityAnalysisOutput prePerimeterSensitivityAnalysisOutput = new PrePerimeterSensitivityAnalysisOutput();
        prePerimeterSensitivityAnalysisOutput.setBranchResult(branchResult);
        prePerimeterSensitivityAnalysisOutput.setFunctionalCost(functionalCost);
        prePerimeterSensitivityAnalysisOutput.setVirtualCost(objectiveFunction.getVirtualCost(branchResult, sensitivityAndLoopflowResults.getSensitivityStatus()));
        prePerimeterSensitivityAnalysisOutput.setRangeActionSetPoints(rangeActionSetPoints);
        prePerimeterSensitivityAnalysisOutput.setPstTaps(pstTaps);
        prePerimeterSensitivityAnalysisOutput.setSensitivityAndLoopflowResults(sensitivityAndLoopflowResults);

        return prePerimeterSensitivityAnalysisOutput;
    }

    private SystematicSensitivityInterface createSystematicSensitivityInterface() {
        Set<Unit> flowUnits = new HashSet<>();
        flowUnits.add(Unit.MEGAWATT);
        if (!raoParameters.getDefaultSensitivityAnalysisParameters().getLoadFlowParameters().isDc()) {
            flowUnits.add(Unit.AMPERE);
        }

        SystematicSensitivityInterface.SystematicSensitivityInterfaceBuilder builder = SystematicSensitivityInterface.builder()
            .withDefaultParameters(raoParameters.getDefaultSensitivityAnalysisParameters())
            .withFallbackParameters(raoParameters.getFallbackSensitivityAnalysisParameters())
            .withRangeActionSensitivities(rangeActions, cnecs, flowUnits);

        if (raoParameters.getObjectiveFunction().doesRequirePtdf() && raoParameters.isRaoWithLoopFlowLimitation()) {
            Set<String> eic = getEicForObjectiveFunction();
            eic.addAll(getEicForLoopFlows());
            builder.withPtdfSensitivities(getGlskForEic(eic), cnecs, Collections.singleton(Unit.MEGAWATT));
        } else if (raoParameters.isRaoWithLoopFlowLimitation()) {
            loopflowCnecs = LoopFlowUtil.computeLoopflowCnecs(cnecs, raoInput.getNetwork(), raoParameters);
            builder.withPtdfSensitivities(getGlskForEic(getEicForLoopFlows()), loopflowCnecs, Collections.singleton(Unit.MEGAWATT));
        } else if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            builder.withPtdfSensitivities(getGlskForEic(getEicForObjectiveFunction()), cnecs, Collections.singleton(Unit.MEGAWATT));
        }

        return builder.build();
    }

    private Set<String> getEicForObjectiveFunction() {
        return raoParameters.getRelativeMarginPtdfBoundaries().stream().
            flatMap(boundary -> boundary.getEiCodes().stream()).
            map(EICode::getAreaCode).
            collect(Collectors.toSet());
    }

    private Set<String> getEicForLoopFlows() {
        return raoInput.getReferenceProgram().getListOfAreas().stream().
            map(EICode::getAreaCode).
            collect(Collectors.toSet());
    }

    private ZonalData<LinearGlsk> getGlskForEic(Set<String> listEicCode) {
        Map<String, LinearGlsk> glskBoundaries = new HashMap<>();

        for (String eiCode : listEicCode) {
            LinearGlsk linearGlsk = raoInput.getGlskProvider().getData(eiCode);
            if (Objects.isNull(linearGlsk)) {
                LOGGER.warn("No GLSK found for CountryEICode {}", eiCode);
            } else {
                glskBoundaries.put(eiCode, linearGlsk);
            }
        }

        return new ZonalDataImpl<>(glskBoundaries);
    }
}
