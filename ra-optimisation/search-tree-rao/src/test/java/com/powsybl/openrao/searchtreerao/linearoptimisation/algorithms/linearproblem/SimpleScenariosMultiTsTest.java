/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
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
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.IteratingLinearOptimizerMultiTS;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerMultiTSInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.MultipleSensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jeremy Wang {@literal <jeremy.wang at rte-france.com>}
 */
public class SimpleScenariosMultiTsTest {
    static final double DOUBLE_TOLERANCE = 1e-4;
    static final double SET_POINT_MAX_TAP = 6.2276423729910535;
    List<Network> networks;
    List<Crac> cracs;
    RangeActionSetpointResult initialSetpoints;
    List<OptimizationPerimeter> optimizationPerimeters;
    MultipleSensitivityResult initialSensiResult;
    RangeActionsOptimizationParameters.PstModel pstModel = RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS;
    RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC_SCIP.json"));

    @BeforeEach
    public void setUp() {
        raoParameters.getRangeActionsOptimizationParameters().setPstModel(pstModel);

    }

    private RangeActionSetpointResult computeInitialSetpointsResults() {
        Map<RangeAction<?>, Double> setpoints = new HashMap<>();
        for (int i = 0; i < cracs.size(); i++) {
            for (RangeAction<?> rangeAction : cracs.get(i).getRangeActions()) {
                setpoints.put(rangeAction, rangeAction.getCurrentSetpoint(networks.get(i)));
            }
        }
        return new RangeActionSetpointResultImpl(setpoints);
    }

    private List<OptimizationPerimeter> computeOptimizationPerimeters() {
        List<OptimizationPerimeter> perimeters = new ArrayList<>();
        for (Crac crac : cracs) {
            perimeters.add(new PreventiveOptimizationPerimeter(
                crac.getPreventiveState(),
                crac.getFlowCnecs(),
                new HashSet<>(),
                crac.getNetworkActions(),
                crac.getRangeActions()));
        }
        return perimeters;
    }

    private MultipleSensitivityResult runInitialSensi() {
        List<Set<FlowCnec>> cnecsList = new ArrayList<>();
        cracs.forEach(crac -> cnecsList.add(crac.getFlowCnecs()));

        Set<RangeAction<?>> rangeActionsSet = new HashSet<>();
        cracs.forEach(crac -> rangeActionsSet.addAll(crac.getRangeActions()));

        ToolProvider toolProvider = ToolProvider.create().withNetwork(networks.get(0)).withRaoParameters(raoParameters).build(); //the attributes in the class are only used for loopflow things

        SensitivityComputerMultiTS sensitivityComputerMultiTS = SensitivityComputerMultiTS.create()
            .withCnecs(cnecsList)
            .withRangeActions(rangeActionsSet)
            .withOutageInstant(cracs.get(0).getOutageInstant())
            .withToolProvider(toolProvider)
            .build();
        sensitivityComputerMultiTS.compute(networks);
        return sensitivityComputerMultiTS.getSensitivityResults();
    }

    @Test
    public void testCase0() {
        testEasyCasesSameNetwork(0);
    }

    @Test
    public void testCase1() {
        testEasyCasesSameNetwork(1);
    }

    @Test
    public void testCase2() {
        testEasyCasesSameNetwork(2);
    }

    @Test
    public void testCase3() {
        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-case3_0.json",
            "multi-ts/crac/crac-case3_1.json"
        );
        List<String> networksPaths = List.of(
            "multi-ts/network/12NodesProdFR.uct",
            "multi-ts/network/12NodesProdNL.uct"
        );

        cracs = new ArrayList<>();
        networks = new ArrayList<>();

        for (int i = 0; i < networksPaths.size(); i++) {
            networks.add(Network.read(networksPaths.get(i), getClass().getResourceAsStream("/" + networksPaths.get(i))));
            cracs.add(CracImporters.importCrac(cracsPaths.get(i), getClass().getResourceAsStream("/" + cracsPaths.get(i)), networks.get(i)));
        }

        initialSetpoints = computeInitialSetpointsResults();
        optimizationPerimeters = computeOptimizationPerimeters();
        initialSensiResult = runInitialSensi();
//        LinearOptimizationResult result0 = testProblemAlone(0);
//        LinearOptimizationResult result1 = testProblemAlone(1);
        LinearOptimizationResult result = runIteratingLinearOptimization();
        System.out.println(result.getStatus());

        PstRangeAction pstRaTs0 = cracs.get(0).getPstRangeActions().stream().toList().get(0);
        PstRangeAction pstRaTs1 = cracs.get(1).getPstRangeActions().stream().toList().get(0);

        State state0 = optimizationPerimeters.get(0).getMainOptimizationState();
        State state1 = optimizationPerimeters.get(1).getMainOptimizationState();
        double pstOptimizedSetPointTs0 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRaTs0, state0);
        double pstOptimizedSetPointTs1 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRaTs1, state1);

        System.out.println("---- TS0 ----");
        System.out.println(pstOptimizedSetPointTs0);
        System.out.println("---- TS1 ----");
        System.out.println(pstOptimizedSetPointTs1);
    }

    public LinearOptimizationResult runIteratingLinearOptimization() {

        Instant outageInstant = Mockito.mock(Instant.class);

        Set<FlowCnec> allCnecs = new HashSet<>();
        cracs.forEach(crac -> allCnecs.addAll(crac.getFlowCnecs()));

        ObjectiveFunction objectiveFunction = ObjectiveFunction.create().build(
            allCnecs,
            Collections.emptySet(),
            initialSensiResult,
            initialSensiResult,
            initialSetpoints,
            null,
            Collections.emptySet(),
            raoParameters);

        ToolProvider toolProvider = ToolProvider.create().withNetwork(networks.get(0)).withRaoParameters(raoParameters).build(); //the attributes in the class are only used for loopflow things

        IteratingLinearOptimizerMultiTSInput input = IteratingLinearOptimizerMultiTSInput.create()
            .withNetworks(networks)
            .withOptimizationPerimeters(optimizationPerimeters)
            .withInitialFlowResult(initialSensiResult)
            .withPrePerimeterFlowResult(initialSensiResult)
            .withPrePerimeterSetpoints(initialSetpoints)
            .withPreOptimizationFlowResult(initialSensiResult)
            .withPreOptimizationSensitivityResult(initialSensiResult)
            .withPreOptimizationAppliedRemedialActions(new AppliedRemedialActions())
            .withRaActivationFromParentLeaf(new RangeActionActivationResultImpl(initialSetpoints))
            .withObjectiveFunction(objectiveFunction)
            .withToolProvider(toolProvider)
            .withOutageInstant(cracs.get(0).getOutageInstant())
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

        return IteratingLinearOptimizerMultiTS.optimize(input, parameters, outageInstant);

    }

    public void testEasyCasesSameNetwork(int caseNumber) {
        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-case" + caseNumber + "_0.json",
            "multi-ts/crac/crac-case" + caseNumber + "_1.json"
        );
        List<String> networksPaths = List.of(
            "multi-ts/network/12NodesProdFR.uct",
            "multi-ts/network/12NodesProdFR.uct"
        );

        cracs = new ArrayList<>();
        networks = new ArrayList<>();

        for (int i = 0; i < networksPaths.size(); i++) {
            networks.add(Network.read(networksPaths.get(i), getClass().getResourceAsStream("/" + networksPaths.get(i))));
            cracs.add(CracImporters.importCrac(cracsPaths.get(i), getClass().getResourceAsStream("/" + cracsPaths.get(i)), networks.get(i)));
        }

        initialSetpoints = computeInitialSetpointsResults();
        optimizationPerimeters = computeOptimizationPerimeters();
        initialSensiResult = runInitialSensi();

//        LinearOptimizationResult resultTs0 = testProblemAlone(0);
//        LinearOptimizationResult resultTs1 = testProblemAlone(1);

        LinearOptimizationResult result = runIteratingLinearOptimization();
        System.out.println(result.getStatus());

        PstRangeAction pstRa0 = cracs.get(0).getPstRangeActions().iterator().next();
        PstRangeAction pstRa1 = cracs.get(1).getPstRangeActions().iterator().next();

        State state0 = optimizationPerimeters.get(0).getMainOptimizationState();
        State state1 = optimizationPerimeters.get(1).getMainOptimizationState();
        double pstOptimizedSetPoint0 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0, state0);
        double pstOptimizedSetPoint1 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa1, state1);

//        double pstOptimizedSetPointAlone0 = resultTs0.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0, state0);
//        double pstOptimizedSetPointAlone1 = resultTs1.getRangeActionActivationResult().getOptimizedSetpoint(pstRa1, state1);

//        System.out.println("---- First problem alone -----");
//        System.out.println(pstOptimizedSetPointAlone0);
//        System.out.println("---- Second problem alone -----");
//        System.out.println(pstOptimizedSetPointAlone1);
        System.out.println("---- Merged problem -----");
        System.out.println(pstOptimizedSetPoint0);
        System.out.println(pstOptimizedSetPoint1);

        assertEquals(pstOptimizedSetPoint0, SET_POINT_MAX_TAP, DOUBLE_TOLERANCE);
        assertEquals(pstOptimizedSetPoint1, SET_POINT_MAX_TAP, DOUBLE_TOLERANCE);
    }

    public LinearOptimizationResult testProblemAlone(int timeStepIndex) {
        Instant outageInstant = Mockito.mock(Instant.class);

        Set<FlowCnec> allCnecs = new HashSet<>(cracs.get(timeStepIndex).getFlowCnecs());

        ObjectiveFunction objectiveFunction = ObjectiveFunction.create().build(
            allCnecs,
            Collections.emptySet(),
            initialSensiResult,
            initialSensiResult,
            initialSetpoints,
            null,
            Collections.emptySet(),
            raoParameters);

        ToolProvider toolProvider = ToolProvider.create().withNetwork(networks.get(0)).withRaoParameters(raoParameters).build(); //the attributes in the class are only used for loopflow things

        IteratingLinearOptimizerInput input0 = IteratingLinearOptimizerInput.create()
            .withNetwork(networks.get(timeStepIndex))
            .withOptimizationPerimeter(optimizationPerimeters.get(timeStepIndex))
            .withInitialFlowResult(initialSensiResult)
            .withPrePerimeterFlowResult(initialSensiResult)
            .withPrePerimeterSetpoints(initialSetpoints)
            .withPreOptimizationFlowResult(initialSensiResult)
            .withPreOptimizationSensitivityResult(initialSensiResult)
            .withPreOptimizationAppliedRemedialActions(new AppliedRemedialActions())
            .withRaActivationFromParentLeaf(new RangeActionActivationResultImpl(initialSetpoints))
            .withObjectiveFunction(objectiveFunction)
            .withToolProvider(toolProvider)
            .withOutageInstant(cracs.get(timeStepIndex).getOutageInstant())
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

        return IteratingLinearOptimizer.optimize(input0, parameters, outageInstant);
    }
}
