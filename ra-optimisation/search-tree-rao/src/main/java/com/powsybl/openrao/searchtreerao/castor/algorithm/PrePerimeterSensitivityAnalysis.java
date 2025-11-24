/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
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
public class PrePerimeterSensitivityAnalysis extends AbstractMultiPerimeterSensitivityAnalysis {

    // built internally
    private SensitivityComputer sensitivityComputer;
    private ObjectiveFunction objectiveFunction;

    public PrePerimeterSensitivityAnalysis(Crac crac,
                                           Set<FlowCnec> flowCnecs,
                                           Set<RangeAction<?>> rangeActions,
                                           RaoParameters raoParameters,
                                           ToolProvider toolProvider) {
        super(crac, flowCnecs, rangeActions, raoParameters, toolProvider);
    }

    public PrePerimeterResult runInitialSensitivityAnalysis(final Network network, final ReportNode reportNode) {
        return runInitialSensitivityAnalysis(network, Set.of(), reportNode);
    }

    public PrePerimeterResult runInitialSensitivityAnalysis(final Network network, final Set<State> optimizedStates, final ReportNode reportNode) {
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = buildSensiBuilder(reportNode)
            .withOutageInstant(crac.getOutageInstant());
        if (raoParameters.getLoopFlowParameters().isPresent()) {
            sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecs));
        }
        if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            sensitivityComputerBuilder.withPtdfsResults(toolProvider.getAbsolutePtdfSumsComputation(), flowCnecs);
        }

        sensitivityComputer = sensitivityComputerBuilder.build();
        objectiveFunction = ObjectiveFunction.buildForInitialSensitivityComputation(flowCnecs, raoParameters, optimizedStates);

        return runAndGetResult(network, objectiveFunction, reportNode);
    }

    public PrePerimeterResult runBasedOnInitialResults(final Network network,
                                                       final FlowResult initialFlowResult,
                                                       final Set<String> operatorsNotSharingCras,
                                                       final AppliedRemedialActions appliedCurativeRemedialActions,
                                                       final ReportNode reportNode) {

        sensitivityComputer = buildSensitivityComputer(initialFlowResult, appliedCurativeRemedialActions, reportNode);
        objectiveFunction = ObjectiveFunction.build(flowCnecs, toolProvider.getLoopFlowCnecs(flowCnecs), initialFlowResult, initialFlowResult, operatorsNotSharingCras, raoParameters, Set.of(crac.getPreventiveState()));

        return runAndGetResult(network, objectiveFunction, reportNode);
    }

    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    private SensitivityComputer.SensitivityComputerBuilder buildSensiBuilder(final ReportNode reportNode) {
        return SensitivityComputer.create(reportNode)
                .withToolProvider(toolProvider)
                .withCnecs(flowCnecs)
                .withRangeActions(rangeActions);
    }

    private PrePerimeterResult runAndGetResult(final Network network, final ObjectiveFunction objectiveFunction, final ReportNode reportNode) {
        sensitivityComputer.compute(network);
        FlowResult flowResult = sensitivityComputer.getBranchResult(network);
        SensitivityResult sensitivityResult = sensitivityComputer.getSensitivityResult();
        RangeActionSetpointResult rangeActionSetpointResult = RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(network, rangeActions);
        ObjectiveFunctionResult objectiveFunctionResult = objectiveFunction.evaluate(flowResult, RemedialActionActivationResultImpl.empty(rangeActionSetpointResult), reportNode);
        return new PrePerimeterSensitivityResultImpl(
                flowResult,
                sensitivityResult,
                rangeActionSetpointResult,
                objectiveFunctionResult
        );
    }
}
