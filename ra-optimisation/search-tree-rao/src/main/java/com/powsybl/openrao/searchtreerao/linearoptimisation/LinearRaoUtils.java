/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.util.Map;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class LinearRaoUtils {
    private LinearRaoUtils() {
    }

    public static PrePerimeterResult runInitialSensitivityAnalysis(Network network,
                                                                   Crac crac,
                                                                   ToolProvider toolProvider,
                                                                   RaoParameters raoParameters,
                                                                   ReportNode reportNode) {
        return new PrePerimeterSensitivityAnalysis(
            crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true
        ).runInitialSensitivityAnalysis(network, reportNode);
    }

    public static IteratingLinearOptimizerInput buildOptimizerInput(RaoInput raoInput,
                                                                    OptimizationPerimeter optimizationPerimeter,
                                                                    PrePerimeterResult initialFlowResult,
                                                                    ToolProvider toolProvider,
                                                                    ObjectiveFunction globalObjectiveFunction) {
        PrePerimeterResult prePerimeterResult = initialFlowResult; // FIXME: use remedial actions from previous state
        PrePerimeterResult preOptimizationResult = initialFlowResult; // FIXME: use remedial actions from previous state and current state
        return IteratingLinearOptimizerInput.create()
            .withNetwork(raoInput.getNetwork())
            .withOptimizationPerimeter(optimizationPerimeter)
            .withInitialFlowResult(initialFlowResult)
            .withPrePerimeterFlowResult(prePerimeterResult)
            .withPrePerimeterSetpoints(prePerimeterResult)
            .withPreOptimizationFlowResult(preOptimizationResult)
            .withPreOptimizationSensitivityResult(preOptimizationResult)
            .withPreOptimizationAppliedRemedialActions(new AppliedRemedialActions())
            .withRaActivationFromParentLeaf(new RangeActionActivationResultImpl(preOptimizationResult))
            .withAppliedNetworkActionsInPrimaryState(new NetworkActionsResultImpl(Map.of()))
            .withObjectiveFunction(globalObjectiveFunction)
            .withToolProvider(toolProvider)
            .withOutageInstant(raoInput.getCrac().getOutageInstant())
            .build();
    }
}
