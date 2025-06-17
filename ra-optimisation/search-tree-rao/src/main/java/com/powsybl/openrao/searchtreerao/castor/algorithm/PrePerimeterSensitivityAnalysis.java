/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.PrePerimeterSensitivityResultImpl;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RemedialActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
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
        return runInitialSensitivityAnalysis(network, crac, Set.of());
    }

    public PrePerimeterResult runInitialSensitivityAnalysis(Network network, Crac crac, Set<State> optimizedStates) {
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = buildSensiBuilder()
            .withOutageInstant(crac.getOutageInstant());
        if (raoParameters.hasExtension(LoopFlowParametersExtension.class)) {
            sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecs));
        }
        if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            sensitivityComputerBuilder.withPtdfsResults(toolProvider.getAbsolutePtdfSumsComputation(), flowCnecs);
        }

        sensitivityComputer = sensitivityComputerBuilder.build();
        objectiveFunction = ObjectiveFunction.buildForInitialSensitivityComputation(flowCnecs, raoParameters, optimizedStates);

        return runAndGetResult(network, objectiveFunction);
    }

    public PrePerimeterResult runBasedOnInitialResults(Network network,
                                                       Crac crac,
                                                       FlowResult initialFlowResult,
                                                       Set<String> operatorsNotSharingCras,
                                                       AppliedRemedialActions appliedCurativeRemedialActions) {

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

        objectiveFunction = ObjectiveFunction.build(flowCnecs, toolProvider.getLoopFlowCnecs(flowCnecs), initialFlowResult, initialFlowResult, operatorsNotSharingCras, raoParameters, Set.of(crac.getPreventiveState()));

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
        FlowResult flowResult = sensitivityComputer.getBranchResult(network);
        SensitivityResult sensitivityResult = sensitivityComputer.getSensitivityResult();
        RangeActionSetpointResult rangeActionSetpointResult = RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(network, rangeActions);
        ObjectiveFunctionResult objectiveFunctionResult = objectiveFunction.evaluate(flowResult, RemedialActionActivationResultImpl.empty(rangeActionSetpointResult));
        return new PrePerimeterSensitivityResultImpl(
                flowResult,
                sensitivityResult,
                rangeActionSetpointResult,
                objectiveFunctionResult
        );
    }
}
