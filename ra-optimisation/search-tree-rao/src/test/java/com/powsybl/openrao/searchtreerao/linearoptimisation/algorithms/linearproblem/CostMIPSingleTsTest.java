/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracioapi.CracImporters;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputerMultiTS;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.IteratingLinearOptimizer;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.*;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * @author Jeremy Wang {@literal <jeremy.wang at rte-france.com>}
 */
public class CostMIPSingleTsTest {
    Network network;
    Crac crac;
    RangeActionSetpointResult initialSetpoints;
    OptimizationPerimeter optimizationPerimeter;
    MultipleSensitivityResult initialSensiResult;
    RangeActionsOptimizationParameters.PstModel pstModel = RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS;
    RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_MIN_COST.json"));

    @BeforeEach
    public void setUp() {
        raoParameters.getRangeActionsOptimizationParameters().setPstModel(pstModel);
    }

    private RangeActionSetpointResult computeInitialSetpointsResults() {
        Map<RangeAction<?>, Double> setpoints = new HashMap<>();
        for (RangeAction<?> rangeAction : crac.getRangeActions()) {
            setpoints.put(rangeAction, rangeAction.getCurrentSetpoint(network));
        }
        return new RangeActionSetpointResultImpl(setpoints);
    }

    private OptimizationPerimeter computeOptimizationPerimeter() {
        return new PreventiveOptimizationPerimeter(
            crac.getPreventiveState(),
            crac.getFlowCnecs(),
            new HashSet<>(),
            crac.getNetworkActions(),
            crac.getRangeActions()
        );
    }

    private MultipleSensitivityResult runInitialSensi() {
        ToolProvider toolProvider = ToolProvider.create().withNetwork(network).withRaoParameters(raoParameters).build();

        SensitivityComputerMultiTS sensitivityComputer = SensitivityComputerMultiTS.create()
            .withCnecs(List.of(crac.getFlowCnecs()))
            .withRangeActions(crac.getRangeActions())
            .withOutageInstant(crac.getOutageInstant())
            .withToolProvider(toolProvider)
            .build();
        sensitivityComputer.compute(List.of(network));
        return sensitivityComputer.getSensitivityResults();
    }

    @Test
    public void testSimpleCase() {
        network = Network.read("multi-ts/network/12NodesProdFR.uct", getClass().getResourceAsStream("/multi-ts/network/12NodesProdFR.uct"));
        crac = CracImporters.importCrac("multi-ts/crac/crac-cost-0.json",
            getClass().getResourceAsStream("/multi-ts/crac/crac-cost-0.json"),
            network);
        initialSetpoints = computeInitialSetpointsResults();
        optimizationPerimeter = computeOptimizationPerimeter();
        initialSensiResult = runInitialSensi();

        LinearOptimizationResult result = runIteratingLinearOptimization();
        System.out.println(result.getStatus());

        PstRangeAction pstRa0Ts0 = crac.getPstRangeAction("pst_be - TS0");
        State state0 = optimizationPerimeter.getMainOptimizationState();
        double pstOptimizedSetPoint0Ts0 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0Ts0, state0);
        System.out.println(pstOptimizedSetPoint0Ts0);
    }

    @Test
    public void testTwoPst() {
        network = Network.read("multi-ts/network/12NodesProdFR_3PST.uct", getClass().getResourceAsStream("/multi-ts/network/12NodesProdFR_3PST.uct"));
        crac = CracImporters.importCrac("multi-ts/crac/crac-cost-2pst.json",
            getClass().getResourceAsStream("/multi-ts/crac/crac-cost-2pst.json"),
            network);
        initialSetpoints = computeInitialSetpointsResults();
        optimizationPerimeter = computeOptimizationPerimeter();
        initialSensiResult = runInitialSensi();

        LinearOptimizationResult result = runIteratingLinearOptimization();
        System.out.println(result.getStatus());

        PstRangeAction pstRa0Ts0 = crac.getPstRangeAction("pst_be_0 - TS0");
        PstRangeAction pstRa1Ts0 = crac.getPstRangeAction("pst_be_1 - TS0");
        State state0 = optimizationPerimeter.getMainOptimizationState();
        double pstOptimizedSetPoint0Ts0 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0Ts0, state0);
        double pstOptimizedSetPoint1Ts0 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa1Ts0, state0);
        System.out.println("---- PST 0 ----");
        System.out.println(pstOptimizedSetPoint0Ts0);
        System.out.println("---- PST 1 ----");
        System.out.println(pstOptimizedSetPoint1Ts0);
    }

    public LinearOptimizationResult runIteratingLinearOptimization() {
        ObjectiveFunction objectiveFunction = ObjectiveFunction.create().build(
            crac.getFlowCnecs(),
            Collections.emptySet(),
            initialSensiResult,
            initialSensiResult,
            initialSetpoints,
            null,
            Collections.emptySet(),
            raoParameters);

        ToolProvider toolProvider = ToolProvider.create().withNetwork(network).withRaoParameters(raoParameters).build();

        IteratingLinearOptimizerInput input = IteratingLinearOptimizerInput.create()
            .withNetwork(network)
            .withOptimizationPerimeter(optimizationPerimeter)
            .withInitialFlowResult(initialSensiResult)
            .withPrePerimeterFlowResult(initialSensiResult)
            .withPrePerimeterSetpoints(initialSetpoints)
            .withPreOptimizationFlowResult(initialSensiResult)
            .withPreOptimizationSensitivityResult(initialSensiResult)
            .withPreOptimizationAppliedRemedialActions(new AppliedRemedialActions())
            .withRaActivationFromParentLeaf(new RangeActionActivationResultImpl(initialSetpoints))
            .withObjectiveFunction(objectiveFunction)
            .withToolProvider(toolProvider)
            .withOutageInstant(crac.getOutageInstant())
            .build();

        IteratingLinearOptimizerParameters parameters = IteratingLinearOptimizerParameters.create()
            .withObjectiveFunction(raoParameters.getObjectiveFunctionParameters().getType())
            .withRangeActionParameters(raoParameters.getRangeActionsOptimizationParameters())
            .withMnecParameters(raoParameters.getExtension(MnecParametersExtension.class))
            .withMaxMinRelativeMarginParameters(raoParameters.getExtension(RelativeMarginsParametersExtension.class))
            .withLoopFlowParameters(raoParameters.getExtension(LoopFlowParametersExtension.class))
            .withUnoptimizedCnecParameters(null)
            .withRaLimitationParameters(new RangeActionLimitationParameters())
            .withSolverParameters(raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver())
            .withMaxNumberOfIterations(raoParameters.getRangeActionsOptimizationParameters().getMaxMipIterations())
            .withRaRangeShrinking(!raoParameters.getRangeActionsOptimizationParameters().getRaRangeShrinking().equals(RangeActionsOptimizationParameters.RaRangeShrinking.DISABLED))
            .build();

        return IteratingLinearOptimizer.optimize(input, parameters, crac.getOutageInstant());
    }
}
