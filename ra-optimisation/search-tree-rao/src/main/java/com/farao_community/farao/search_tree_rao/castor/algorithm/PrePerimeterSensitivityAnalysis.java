/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.ObjectiveFunctionResult;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import com.farao_community.farao.search_tree_rao.result.impl.PrePerimeterSensitivityResultImpl;
import com.farao_community.farao.search_tree_rao.commons.SensitivityComputer;
import com.farao_community.farao.search_tree_rao.commons.ToolProvider;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunctionSmartBuilder;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionSetpointResultImpl;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.powsybl.iidm.network.Network;

import java.util.Set;

/**
 * This class aims at performing the sensitivity analysis before the optimization of a perimeter. At these specific
 * instants we actually want to compute all the results on the network. They will be useful either for the optimization
 * or to fill results in the final output.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PrePerimeterSensitivityAnalysis {

    // actual input
    private final Set<FlowCnec> flowCnecs;
    private final Set<RangeAction<?>> rangeActions;
    private final RaoParameters raoParameters;
    private final ToolProvider toolProvider;

    // built internally
    private SensitivityComputer sensitivityComputer;
    private ObjectiveFunction objectiveFunction;

    public PrePerimeterSensitivityAnalysis(Set<FlowCnec> flowCnecs,
                                           Set<RangeAction<?>> rangeActions,
                                           RaoParameters raoParameters,
                                           ToolProvider toolProvider) {
        this.flowCnecs = flowCnecs;
        this.rangeActions = rangeActions;
        this.raoParameters = raoParameters;
        this.toolProvider = toolProvider;
    }

    public PrePerimeterResult runInitialSensitivityAnalysis(Network network) {
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = buildSensiBuilder();
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecs));
        }
        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            sensitivityComputerBuilder.withPtdfsResults(toolProvider.getAbsolutePtdfSumsComputation(), flowCnecs);
        }

        sensitivityComputer = sensitivityComputerBuilder.build();
        objectiveFunction = ObjectiveFunctionSmartBuilder.buildForInitialSensitivityComputation(flowCnecs, raoParameters);

        return runAndGetResult(network, objectiveFunction);
    }

    public PrePerimeterResult runBasedOnInitialResults(Network network,
                                                       FlowResult initialFlowResult,
                                                       Set<String> operatorsNotSharingCras,
                                                       AppliedRemedialActions appliedCurativeRemedialActions) {

        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = buildSensiBuilder();
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            if (raoParameters.getLoopFlowApproximationLevel().shouldUpdatePtdfWithTopologicalChange()) {
                sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecs));
            } else {
                sensitivityComputerBuilder.withCommercialFlowsResults(initialFlowResult);
            }
        }
        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            sensitivityComputerBuilder.withPtdfsResults(initialFlowResult);
        }
        if (appliedCurativeRemedialActions != null) {
            // for 2nd preventive initial sensi
            sensitivityComputerBuilder.withAppliedRemedialActions(appliedCurativeRemedialActions);
        }
        sensitivityComputer = sensitivityComputerBuilder.build();

        objectiveFunction = ObjectiveFunctionSmartBuilder.build(flowCnecs, toolProvider.getLoopFlowCnecs(flowCnecs), initialFlowResult, initialFlowResult, operatorsNotSharingCras, raoParameters);

        return runAndGetResult(network, objectiveFunction);
    }

    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    private SensitivityComputer.SensitivityComputerBuilder buildSensiBuilder() {
        return SensitivityComputer.create()
                .withToolProvider(toolProvider)
                .withCnecs(flowCnecs)
                .withRangeActions(rangeActions);
    }

    private PrePerimeterResult runAndGetResult(Network network, ObjectiveFunction objectiveFunction) {
        sensitivityComputer.compute(network);
        FlowResult flowResult = sensitivityComputer.getBranchResult();
        SensitivityResult sensitivityResult = sensitivityComputer.getSensitivityResult();
        ObjectiveFunctionResult objectiveFunctionResult = getResult(objectiveFunction, flowResult, sensitivityResult);
        return new PrePerimeterSensitivityResultImpl(
                flowResult,
                sensitivityResult,
                new RangeActionSetpointResultImpl(network, rangeActions),
                objectiveFunctionResult
        );
    }

    private ObjectiveFunctionResult getResult(ObjectiveFunction objectiveFunction, FlowResult flowResult, SensitivityResult sensitivityResult) {
        return objectiveFunction.evaluate(flowResult, sensitivityResult.getSensitivityStatus());
    }
}
