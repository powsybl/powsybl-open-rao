/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoLoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.*;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This class aims at performing the sensitivity analysis before the optimization of a perimeter. At these specific
 * instants we actually want to compute all the results on the network. They will be useful either for the optimization
 * or to fill results in the final output.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PostPerimeterSensitivityAnalysis {

    // actual input
    private final Set<FlowCnec> flowCnecs;
    private final Set<RangeAction<?>> rangeActions;
    private final RaoParameters raoParameters;
    private final ToolProvider toolProvider;

    // built internally
    private SensitivityComputer sensitivityComputer;
    private ObjectiveFunction objectiveFunction;

    public PostPerimeterSensitivityAnalysis(Set<FlowCnec> flowCnecs,
                                            Set<RangeAction<?>> rangeActions,
                                            RaoParameters raoParameters,
                                            ToolProvider toolProvider) {
        this.flowCnecs = flowCnecs;
        this.rangeActions = rangeActions;
        this.raoParameters = raoParameters;
        this.toolProvider = toolProvider;
    }

    public PostPerimeterSensitivityAnalysis(Crac crac,
                                            Set<State> states,
                                            RaoParameters raoParameters,
                                            ToolProvider toolProvider) {
        Set<RangeAction<?>> rangeActions = new HashSet<>();
        Set<FlowCnec> flowCnecs = new HashSet<>();
        for (State state : states) {
            rangeActions.addAll(crac.getPotentiallyAvailableRangeActions(state));
            flowCnecs.addAll(crac.getFlowCnecs(state));
        }
        this.flowCnecs = flowCnecs;
        this.rangeActions = rangeActions;
        this.raoParameters = raoParameters;
        this.toolProvider = toolProvider;
    }

    public Future<PostPerimeterResult> runBasedOnInitialPreviousAndOptimizationResults(Network network,
                                                                           Crac crac,
                                                                           FlowResult initialFlowResult,
                                                                           Future<FlowResult> previousResultsFuture,
                                                                           Set<String> operatorsNotSharingCras,
                                                                           OptimizationResult optimizationResult,
                                                                           AppliedRemedialActions appliedCurativeRemedialActions) {

        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = SensitivityComputer.create()
            .withToolProvider(toolProvider)
            .withCnecs(flowCnecs)
            .withRangeActions(rangeActions)
            .withOutageInstant(crac.getOutageInstant());

        Optional<SearchTreeRaoLoopFlowParameters> optionalLoopFlowParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoopFlowParameters();
        if (optionalLoopFlowParameters.isPresent()) {
            if (optionalLoopFlowParameters.get().getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecs));
            } else {
                sensitivityComputerBuilder.withCommercialFlowsResults(initialFlowResult);
            }
        }
        Optional<SearchTreeRaoRelativeMarginsParameters> optionalRelativeMarginParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getRelativeMarginsParameters();
        if (optionalRelativeMarginParameters.isPresent()) {
            if (optionalRelativeMarginParameters.get().getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                sensitivityComputerBuilder.withPtdfsResults(toolProvider.getAbsolutePtdfSumsComputation(), flowCnecs);
            } else {
                sensitivityComputerBuilder.withPtdfsResults(initialFlowResult);
            }
        }
        if (appliedCurativeRemedialActions != null) {
            // for 2nd preventive initial sensi
            sensitivityComputerBuilder.withAppliedRemedialActions(appliedCurativeRemedialActions);
        }
        SensitivityComputer sensitivityComputer = sensitivityComputerBuilder.build();
        sensitivityComputer.compute(network);

        FlowResult flowResult = sensitivityComputer.getBranchResult(network);
        SensitivityResult sensitivityResult = sensitivityComputer.getSensitivityResult();

        return Executors.newSingleThreadExecutor().submit(() -> {
            ObjectiveFunction objectiveFunction = ObjectiveFunction.build(flowCnecs,
                toolProvider.getLoopFlowCnecs(flowCnecs),
                initialFlowResult,
                previousResultsFuture.get(),
                operatorsNotSharingCras,
                raoParameters,
                Set.of()); //TODO: To complete later if we want to use costly objective function not needed otherwise

            ObjectiveFunctionResult objectiveFunctionResult = objectiveFunction.evaluate(
                flowResult,
                new RemedialActionActivationResultImpl(optimizationResult, optimizationResult)
            );

            return new PostPerimeterResult(optimizationResult, new PrePerimeterSensitivityResultImpl(
                flowResult,
                sensitivityResult,
                RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(network, rangeActions),
                objectiveFunctionResult
            ));
        });
    }
}
