/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.results.OptimizationResult;
import com.farao_community.farao.rao_api.results.PrePerimeterResult;
import com.farao_community.farao.rao_commons.result.RangeActionResultImpl;
import com.powsybl.iidm.network.Network;

import java.util.HashSet;
import java.util.Set;

/**
 * This class aims at performing the sensitivity analysis before the optimization of a perimeter. At these specific
 * instants we actually want to compute all the results on the network. They will be useful either for the optimization
 * or to fill results in the final output.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PrePerimeterSensitivityAnalysis {
    private final Set<FlowCnec> flowCnecs;
    private final Set<RangeAction> rangeActions;
    private final ToolProvider toolProvider;
    private final RaoParameters raoParameters;

    private SensitivityComputer sensitivityComputer;

    public PrePerimeterSensitivityAnalysis(Crac crac, ToolProvider toolProvider, RaoParameters raoParameters) {
        this.toolProvider = toolProvider;
        flowCnecs = crac.getFlowCnecs();
        rangeActions = new HashSet<>();
        crac.getStates().forEach(state -> rangeActions.addAll(crac.getRangeActions(state, UsageMethod.AVAILABLE)));
        this.raoParameters = raoParameters;
    }

    public PrePerimeterResult run(Network network) {
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = getBuilder();
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecs));
        }
        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            sensitivityComputerBuilder.withPtdfsResults(toolProvider.getAbsolutePtdfSumsComputation(), flowCnecs);
        }
        sensitivityComputer = sensitivityComputerBuilder.build();
        return runAndGetResult(network);
    }

    public PrePerimeterResult runBasedOn(Network network, OptimizationResult optimizationResult) {
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = getBuilder();
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            if (raoParameters.getLoopFlowApproximationLevel().shouldUpdatePtdfWithTopologicalChange()) {
                sensitivityComputerBuilder.withCommercialFlowsResults(toolProvider.getLoopFlowComputation(), toolProvider.getLoopFlowCnecs(flowCnecs));
            } else {
                sensitivityComputerBuilder.withCommercialFlowsResults(optimizationResult);
            }
        }
        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            sensitivityComputerBuilder.withPtdfsResults(optimizationResult);
        }
        sensitivityComputer = sensitivityComputerBuilder.build();
        return runAndGetResult(network);
    }

    private SensitivityComputer.SensitivityComputerBuilder getBuilder() {
        return SensitivityComputer.create()
                .withToolProvider(toolProvider)
                .withCnecs(flowCnecs)
                .withRangeActions(rangeActions);
    }

    private PrePerimeterResult runAndGetResult(Network network) {
        sensitivityComputer.compute(network);
        return new PrePerimeterSensitivityOutput(
                sensitivityComputer.getBranchResult(),
                sensitivityComputer.getSensitivityResult(),
                new RangeActionResultImpl(network, rangeActions)
        );
    }
}
