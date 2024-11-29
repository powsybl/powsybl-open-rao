/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.PrePerimeterSensitivityResultImpl;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.iidm.network.Network;

import java.util.Objects;
import java.util.Set;

import static java.lang.String.format;

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
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = buildSensiBuilder()
            .withOutageInstant(crac.getOutageInstant());
        if (raoParameters.getLoopFlowParameters().isPresent()) {
            sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecs));
        }
        if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            sensitivityComputerBuilder.withPtdfsResults(toolProvider.getAbsolutePtdfSumsComputation(), flowCnecs);
        }

        sensitivityComputer = sensitivityComputerBuilder.build();
        objectiveFunction = ObjectiveFunction.create().buildForInitialSensitivityComputation(flowCnecs, raoParameters);

        return runAndGetResult(network, objectiveFunction);
    }

    public PrePerimeterResult runBasedOnInitialResults(Network network,
                                                       Crac crac,
                                                       FlowResult initialFlowResult,
                                                       Set<String> operatorsNotSharingCras,
                                                       AppliedRemedialActions appliedCurativeRemedialActions) {

        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = buildSensiBuilder()
            .withOutageInstant(crac.getOutageInstant());
        OpenRaoSearchTreeParameters searchTreeParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class);
        if (!Objects.isNull(searchTreeParameters)) {
            searchTreeParameters.getLoopFlowParameters().ifPresent(loopFlowParameters -> {
                if (loopFlowParameters.getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                    sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecs));
                } else {
                    sensitivityComputerBuilder.withCommercialFlowsResults(initialFlowResult);
                }
            });
        }
        if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            if (Objects.isNull(searchTreeParameters)) {
                throw new OpenRaoException(format("Objective function %s requires an extension with relative margins parameters", raoParameters.getObjectiveFunctionParameters().getType()));
            }
            if (searchTreeParameters.getRelativeMarginsParameters().orElseThrow().getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
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
        ObjectiveFunctionResult objectiveFunctionResult = getResult(objectiveFunction, flowResult);
        return new PrePerimeterSensitivityResultImpl(
                flowResult,
                sensitivityResult,
                rangeActionSetpointResult,
                objectiveFunctionResult
        );
    }

    private ObjectiveFunctionResult getResult(ObjectiveFunction objectiveFunction, FlowResult flowResult) {
        return objectiveFunction.evaluate(flowResult);
    }
}
