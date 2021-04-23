/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.*;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerParameters;
import com.farao_community.farao.rao_commons.objective_function_evaluator.MinMarginEvaluator;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
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
public class InitialSensitivityAnalysis {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialSensitivityAnalysis.class);
    private SystematicSensitivityInterface systematicSensitivityInterface;
    private RaoParameters raoParameters;
    private LinearOptimizerParameters linearOptimizerParameters;
    private RaoInput raoInput;
    private Set<BranchCnec> cnecs;
    private Set<RangeAction> rangeActions;
    private Set<BranchCnec> loopflowCnecs;
    private State optimizedState;
    private Set<State> perimeter;

    public InitialSensitivityAnalysis(RaoInput raoInput, State optimizedState, Set<State> perimeter, RaoParameters raoParameters) {
        // it is actually quite strange to ask for a RaoData here, but it is required in
        // order to use the fillResultsWithXXX() methods of the CracResultManager.
        this.raoInput = raoInput;
        this.optimizedState = optimizedState;
        this.perimeter = perimeter;
        this.raoParameters = raoParameters;
        this.cnecs = RaoUtil.computePerimeterCnecs(raoInput.getCrac(), perimeter);
        this.rangeActions = raoInput.getCrac().getRangeActions(raoInput.getNetwork(), optimizedState, UsageMethod.AVAILABLE);
        if(raoParameters.isRaoWithLoopFlowLimitation()) {
            LoopFlowUtil.computeLoopflowCnecs(cnecs, raoInput.getNetwork(), raoParameters);
        }
        this.systematicSensitivityInterface = createSystematicSensitivityInterface();
    }

    public SystematicSensitivityResult run() {
        LOGGER.info("Initial systematic analysis [start]");

        // run sensitivity analysis
        SystematicSensitivityResult sensitivityResult = runSensitivityComputation();

        // tag initial variant
        //raoData.getCrac().getExtension(ResultVariantManager.class).setInitialVariantId(raoData.getWorkingVariantId());
        //raoData.getCrac().getExtension(ResultVariantManager.class).setPrePerimeterVariantId(raoData.getWorkingVariantId());

        // fill results of initial variant
        LOGGER.info("Initial systematic analysis [...] - fill initial range actions values");
        //raoData.getCracResultManager().fillRangeActionResultsWithNetworkValues();

        LOGGER.info("Initial systematic analysis [...] - fill reference flow values");
        //raoData.getCracResultManager().fillCnecResultWithFlows();

        Crac crac = raoInput.getCrac();
        Map<BranchCnec, Double> commercialFlows = null;

        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            LOGGER.info("Initial systematic analysis [...] - compute reference loop-flow values");

            commercialFlows = LoopFlowUtil.computeCommercialFlows(raoInput.getNetwork(), loopflowCnecs, raoInput.getGlskProvider(), raoInput.getReferenceProgram(), sensitivityResult);
        }
        SensitivityAndLoopflowResults sensitivityAndLoopflowResults = new SensitivityAndLoopflowResults(sensitivityResult, commercialFlows);

        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            LOGGER.info("Initial systematic analysis [...] - fill zone-to-zone PTDFs");
            Map<BranchCnec, Double> ptdfSums = AbsolutePtdfSumsComputation.computeAbsolutePtdfSums(cnecs, raoInput.getGlskProvider(), raoParameters.getRelativeMarginPtdfBoundaries(), sensitivityResult);
        }

        //CnecResults initialCnecResults = buildInitialCnecResults(sensitivityResult);
        Map<BranchCnec, Double> initialAbsolutePtdfSums = new HashMap<>();

        double functionalCost = new MinMarginEvaluator(cnecs, null, initialAbsolutePtdfSums, linearOptimizerParameters).computeCost(sensitivityAndLoopflowResults);
        //fillObjectiveFunction();
        LOGGER.info("Initial systematic analysis [end] - with initial min margin of {} MW", -functionalCost);

        //TODO : build PerimeterOutput

        return sensitivityResult;
    }

    private SystematicSensitivityResult runSensitivityComputation() {
        SystematicSensitivityResult sensitivityResult = systematicSensitivityInterface.run(raoInput.getNetwork());
        //raoData.setSystematicSensitivityResult(sensitivityResult);
        return sensitivityResult;
    }

    /*private void fillObjectiveFunction() {
        ObjectiveFunctionEvaluator objectiveFunction = RaoUtil.createObjectiveFunction(raoData, linearOptimizerParameters, raoParameters.getFallbackOverCost());
        raoData.getCracResultManager().fillCracResultWithCosts(
            objectiveFunction.computeFunctionalCost(raoData.getSensitivityAndLoopflowResults()), objectiveFunction.computeVirtualCost(raoData.getSensitivityAndLoopflowResults()));
    }*/

    /*private void fillReferenceLoopFlow() {
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(raoData.getGlskProvider(), raoData.getReferenceProgram());
        LoopFlowResult lfResults = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(raoData.getNetwork(), raoData.getSystematicSensitivityResult(), raoData.getLoopflowCnecs());
        raoData.getCracResultManager().fillCnecResultsWithLoopFlows(lfResults);
    }*/

    /*private void fillAbsolutePtdfSums(SystematicSensitivityResult sensitivityResult) {
        Map<BranchCnec, Double> ptdfSums = AbsolutePtdfSumsComputation.computeAbsolutePtdfSums(raoData.getCnecs(), raoData.getGlskProvider(), raoParameters.getRelativeMarginPtdfBoundaries(), sensitivityResult);
        ptdfSums.forEach((key, value) -> key.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId()).setAbsolutePtdfSum(value));
    }*/

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
