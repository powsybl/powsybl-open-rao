/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.parameters.extensions.LoopFlowParametersExtension;
import com.farao_community.farao.search_tree_rao.commons.SensitivityComputer;
import com.farao_community.farao.search_tree_rao.commons.ToolProvider;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.farao_community.farao.search_tree_rao.result.impl.PrePerimeterSensitivityResultImpl;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionActivationResultImpl;
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

    public PrePerimeterResult runInitialSensitivityAnalysis(Network network, Crac crac) {
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = buildSensiBuilder();
        if (raoParameters.hasExtension(LoopFlowParametersExtension.class)) {
            sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecs));
        }
        if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            sensitivityComputerBuilder.withPtdfsResults(toolProvider.getAbsolutePtdfSumsComputation(), flowCnecs);
        }

        sensitivityComputer = sensitivityComputerBuilder.build();
        objectiveFunction = ObjectiveFunction.create().buildForInitialSensitivityComputation(flowCnecs, raoParameters, crac, RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(network, rangeActions));

        return runAndGetResult(network, objectiveFunction, crac.getInstant(InstantKind.OUTAGE));
    }

    public PrePerimeterResult runBasedOnInitialResults(Network network,
                                                       Crac crac,
                                                       FlowResult initialFlowResult,
                                                       RangeActionSetpointResult initialRangeActionSetpointResult,
                                                       Set<String> operatorsNotSharingCras,
                                                       AppliedRemedialActions appliedCurativeRemedialActions) {

        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = buildSensiBuilder();
        if (raoParameters.hasExtension(LoopFlowParametersExtension.class)) {
            if (raoParameters.getExtension(LoopFlowParametersExtension.class).getApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecs));
            } else {
                sensitivityComputerBuilder.withCommercialFlowsResults(initialFlowResult);
            }
        }
        if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            sensitivityComputerBuilder.withPtdfsResults(initialFlowResult);
        }
        if (appliedCurativeRemedialActions != null) {
            // for 2nd preventive initial sensi
            sensitivityComputerBuilder.withAppliedRemedialActions(appliedCurativeRemedialActions);
        }
        sensitivityComputer = sensitivityComputerBuilder.build();

        objectiveFunction = ObjectiveFunction.create().build(flowCnecs, toolProvider.getLoopFlowCnecs(flowCnecs), initialFlowResult, initialFlowResult, initialRangeActionSetpointResult, crac, operatorsNotSharingCras, raoParameters);

        return runAndGetResult(network, objectiveFunction, crac.getInstant(InstantKind.OUTAGE));
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

    private PrePerimeterResult runAndGetResult(Network network, ObjectiveFunction objectiveFunction, Instant instantOutage) {
        sensitivityComputer.compute(network, instantOutage);
        FlowResult flowResult = sensitivityComputer.getBranchResult(network);
        SensitivityResult sensitivityResult = sensitivityComputer.getSensitivityResult();
        RangeActionSetpointResult rangeActionSetpointResult = RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(network, rangeActions);
        RangeActionActivationResult rangeActionActivationResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        ObjectiveFunctionResult objectiveFunctionResult = getResult(objectiveFunction, flowResult, rangeActionActivationResult, sensitivityResult);
        return new PrePerimeterSensitivityResultImpl(
                flowResult,
                sensitivityResult,
                rangeActionSetpointResult,
                objectiveFunctionResult
        );
    }

    private ObjectiveFunctionResult getResult(ObjectiveFunction objectiveFunction, FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult) {
        return objectiveFunction.evaluate(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityResult.getSensitivityStatus());
    }
}
