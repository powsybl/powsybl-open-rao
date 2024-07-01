/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.*;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.iidm.network.Network;

import java.util.Objects;
import java.util.Set;

/**
 * This class aims at performing the sensitivity analysis before the optimization of a perimeter. At these specific
 * instants we actually want to compute all the results on the network. They will be useful either for the optimization
 * or to fill results in the final output.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SensitivityAnalysisRunner {

    // actual input
    private final Set<FlowCnec> flowCnecs;
    private final Set<RangeAction<?>> rangeActions;
    private final RaoParameters raoParameters;
    private final ToolProvider toolProvider;

    // built internally
    private SensitivityComputer sensitivityComputer;
    private ObjectiveFunction objectiveFunction;

    public SensitivityAnalysisRunner(Set<FlowCnec> flowCnecs,
                                     Set<RangeAction<?>> rangeActions,
                                     RaoParameters raoParameters,
                                     ToolProvider toolProvider) {
        this.flowCnecs = flowCnecs;
        this.rangeActions = rangeActions;
        this.raoParameters = raoParameters;
        this.toolProvider = toolProvider;
    }

    public PerimeterResultWithCnecs runInitialSensitivityAnalysis(Network network, Crac crac) {
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = buildSensiBuilder()
            .withOutageInstant(crac.getOutageInstant());
        if (raoParameters.hasExtension(LoopFlowParametersExtension.class)) {
            sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecs));
        }
        if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            sensitivityComputerBuilder.withPtdfsResults(toolProvider.getAbsolutePtdfSumsComputation(), flowCnecs);
        }

        sensitivityComputer = sensitivityComputerBuilder.build();
        objectiveFunction = ObjectiveFunction.create().buildForInitialSensitivityComputation(flowCnecs, raoParameters);

        return runAndGetResult(network, objectiveFunction, null, null, null);
    }

    public PerimeterResultWithCnecs runBasedOnInitialResults(Network network,
                                                             Crac crac,
                                                             FlowResult initialFlowResult,
                                                             Set<String> operatorsNotSharingCras,
                                                             AppliedRemedialActions appliedCurativeRemedialActions,
                                                             PerimeterResultWithCnecs previousResults,
                                                             NetworkActionsResult networkActionsResult,
                                                             RangeActionResult rangeActionResult) {

        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = buildSensiBuilder()
            .withOutageInstant(crac.getOutageInstant());
        if (raoParameters.hasExtension(LoopFlowParametersExtension.class)) {
            if (raoParameters.getExtension(LoopFlowParametersExtension.class).getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecs));
            } else {
                sensitivityComputerBuilder.withCommercialFlowsResults(initialFlowResult);
            }
        }
        if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            if (raoParameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                sensitivityComputerBuilder.withPtdfsResults(toolProvider.getAbsolutePtdfSumsComputation(), flowCnecs);
            } else {
                sensitivityComputerBuilder.withPtdfsResults(initialFlowResult);
            }
        }
        if (appliedCurativeRemedialActions != null) {
            // for 2nd preventive initial sensi
            sensitivityComputerBuilder.withAppliedRemedialActions(appliedCurativeRemedialActions);
        }
        sensitivityComputer = sensitivityComputerBuilder.build();

        objectiveFunction = ObjectiveFunction.create().build(flowCnecs, toolProvider.getLoopFlowCnecs(flowCnecs), initialFlowResult, initialFlowResult, operatorsNotSharingCras, raoParameters);

        return runAndGetResult(network, objectiveFunction, previousResults, networkActionsResult, rangeActionResult);
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

    private PerimeterResultWithCnecs runAndGetResult(Network network, ObjectiveFunction objectiveFunction, PerimeterResultWithCnecs previousResults, NetworkActionsResult networkActionsResult, RangeActionResult rangeActionResult) {
        sensitivityComputer.compute(network);
        FlowResult flowResult = sensitivityComputer.getBranchResult(network);
        SensitivityResult sensitivityResult = sensitivityComputer.getSensitivityResult();
        ObjectiveFunctionResult objectiveFunctionResult = getResult(objectiveFunction, flowResult, sensitivityResult);
        if (Objects.nonNull(previousResults)) {
            return new PerimeterResultWithCnecs(previousResults,
                new OptimizationResultImpl(flowResult, sensitivityResult, networkActionsResult, rangeActionResult, objectiveFunctionResult));
        }
        RangeActionResult rangeActionResultFromNetwork = RangeActionResultImpl.buildWithSetpointsFromNetwork(network, rangeActions);
        return new PerimeterResultWithCnecs(null,
                new OptimizationResultImpl(flowResult, sensitivityResult, null, rangeActionResultFromNetwork, objectiveFunctionResult));
    }

    private ObjectiveFunctionResult getResult(ObjectiveFunction objectiveFunction, FlowResult flowResult, SensitivityResult sensitivityResult) {
        return objectiveFunction.evaluate(flowResult, sensitivityResult);
    }
}
