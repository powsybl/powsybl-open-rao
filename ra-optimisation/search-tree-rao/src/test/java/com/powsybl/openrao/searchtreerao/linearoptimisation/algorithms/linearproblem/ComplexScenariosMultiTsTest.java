/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
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

import static java.util.Objects.nonNull;

/**
 * @author Jeremy Wang {@literal <jeremy.wang at rte-france.com>}
 */
public class ComplexScenariosMultiTsTest {
    List<Network> networks;
    List<Crac> cracs;
    RangeActionSetpointResult initialSetpoints;
    List<OptimizationPerimeter> optimizationPerimeters;
    MultipleSensitivityResult initialSensiResult;
    RangeActionsOptimizationParameters.PstModel pstModel = RangeActionsOptimizationParameters.PstModel.CONTINUOUS;
    RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC_SCIP.json"));

    @BeforeEach
    public void setUp() {
        raoParameters.getRangeActionsOptimizationParameters().setPstModel(pstModel);
    }

    private void importNetworksAndCracs(List<String> cracsPaths, List<String> networksPaths) {
        cracs = new ArrayList<>();
        networks = new ArrayList<>();
        for (int i = 0; i < networksPaths.size(); i++) {
            networks.add(Network.read(networksPaths.get(i), getClass().getResourceAsStream("/" + networksPaths.get(i))));
            cracs.add(CracImporters.importCrac(cracsPaths.get(i), getClass().getResourceAsStream("/" + cracsPaths.get(i)), networks.get(i)));
        }
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

    //This test highlights a problem about optimizing multiple time steps at once:
    //The RAO does not try to optimize a TS if a worse TS exists
    //Here pst2 from TS1 could have a setpoint of 6.22, but it keeps it to 0.0 (initial setpoint) because TS0 is worse
    @Test
    public void testTwoTimestepsThreePst() {
        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-3pst-ts0.json",
            "multi-ts/crac/crac-3pst-ts1.json"
        );
        List<String> networksPaths = Collections.nCopies(2, "multi-ts/network/12NodesProdFR_3PST.uct");

        importNetworksAndCracs(cracsPaths, networksPaths);
        initialSetpoints = computeInitialSetpointsResults();
        optimizationPerimeters = computeOptimizationPerimeters();
        initialSensiResult = runInitialSensi();

        LinearOptimizationResult result = runIteratingLinearOptimization();
        System.out.println(result.getStatus());

        PstRangeAction pstRa0Ts0 = cracs.get(0).getPstRangeAction("pst_be_0 - TS0");
        PstRangeAction pstRa1Ts0 = cracs.get(0).getPstRangeAction("pst_be_1 - TS0");
        PstRangeAction pstRa1Ts1 = cracs.get(1).getPstRangeAction("pst_be_1 - TS1");
        PstRangeAction pstRa2Ts1 = cracs.get(1).getPstRangeAction("pst_be_2 - TS1");

        State state0 = optimizationPerimeters.get(0).getMainOptimizationState();
        State state1 = optimizationPerimeters.get(1).getMainOptimizationState();
        double pstOptimizedSetPoint0Ts0 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0Ts0, state0);
        double pstOptimizedSetPoint1Ts0 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa1Ts0, state0);
        double pstOptimizedSetPoint1Ts1 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa1Ts1, state1);
        double pstOptimizedSetPoint2Ts1 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa2Ts1, state1);

        System.out.println("---- TS0 ----");
        System.out.println(pstOptimizedSetPoint0Ts0);
        System.out.println(pstOptimizedSetPoint1Ts0);
        System.out.println("---- TS1 ----");
        System.out.println(pstOptimizedSetPoint1Ts1);
        System.out.println(pstOptimizedSetPoint2Ts1);
    }

    @Test
    public void testThreeTimestepsTwoPst() {
        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-case0_0.json",
            "multi-ts/crac/crac-2pst-ts1.json",
            "multi-ts/crac/crac-2pst-ts2.json"
        );
        List<String> networksPaths = List.of(
            "multi-ts/network/12NodesProdFR_3PST.uct",
            "multi-ts/network/12NodesProdFR_3PST.uct",
            "multi-ts/network/12NodesProdNL_3PST.uct"
        );

        importNetworksAndCracs(cracsPaths, networksPaths);
        initialSetpoints = computeInitialSetpointsResults();
        optimizationPerimeters = computeOptimizationPerimeters();
        initialSensiResult = runInitialSensi();
//        LinearOptimizationResult result0 = testProblemAlone(0);
//        LinearOptimizationResult result1 = testProblemAlone(1);
//        LinearOptimizationResult result2 = testProblemAlone(2);
        LinearOptimizationResult result = runIteratingLinearOptimization();
        System.out.println(result.getStatus());

        PstRangeAction pstRa0Ts0 = cracs.get(0).getPstRangeActions().stream().toList().get(0);
        PstRangeAction pstRa1Ts1 = cracs.get(1).getPstRangeActions().stream().toList().get(0);
        PstRangeAction pstRa0Ts2 = cracs.get(2).getPstRangeActions().stream().toList().get(0);

        State state0 = optimizationPerimeters.get(0).getMainOptimizationState();
        State state1 = optimizationPerimeters.get(1).getMainOptimizationState();
        State state2 = optimizationPerimeters.get(2).getMainOptimizationState();
        double pstOptimizedSetPoint0Ts0 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0Ts0, state0);
        double pstOptimizedSetPoint1Ts1 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa1Ts1, state1);
        double pstOptimizedSetPoint0Ts2 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0Ts2, state2);

        System.out.println("---- TS0 ----");
        System.out.println(pstOptimizedSetPoint0Ts0);
        System.out.println("---- TS1 ----");
        System.out.println(pstOptimizedSetPoint1Ts1);
        System.out.println("---- TS2 ----");
        System.out.println(pstOptimizedSetPoint0Ts2);

//        System.out.println("---- Problems alone----");
//        double pstOptimizedSetPointAlonets0 = result0.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0Ts0, state0);
//        System.out.println(pstOptimizedSetPointAlonets0);
//
//        double pstOptimizedSetPointAlonets1 = result1.getRangeActionActivationResult().getOptimizedSetpoint(pstRa1Ts1, state1);
//        System.out.println(pstOptimizedSetPointAlonets1);
//
//        double pstOptimizedSetPointAlonets2 = result2.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0Ts2, state2);
//        System.out.println(pstOptimizedSetPointAlonets2);
    }

    @Test
    public void testFourTimestepsOnePst() {
        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-case0_0.json",
            "multi-ts/crac/crac-no-ra-1.json",
            "multi-ts/crac/crac-no-ra-2.json",
            "multi-ts/crac/crac-pst-3.json"
        );
        List<String> networksPaths = Collections.nCopies(4, "multi-ts/network/12NodesProdFR.uct");
        importNetworksAndCracs(cracsPaths, networksPaths);

        initialSetpoints = computeInitialSetpointsResults();
        optimizationPerimeters = computeOptimizationPerimeters();
        initialSensiResult = runInitialSensi();

        LinearOptimizationResult result = runIteratingLinearOptimization();
        System.out.println(result.getStatus());

        PstRangeAction pstRa0Ts0 = cracs.get(0).getPstRangeActions().stream().toList().get(0);

        State state0 = optimizationPerimeters.get(0).getMainOptimizationState();
        double pstOptimizedSetPoint0Ts0 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0Ts0, state0);

        System.out.println("---- TS0 ----");
        System.out.println(pstOptimizedSetPoint0Ts0);
    }

    @Test
    public void sensiTwoTimestepsThreePst() {
        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-3pst-ts0.json",
            "multi-ts/crac/crac-3pst-ts1.json"
        );
        List<String> networksPaths = Collections.nCopies(2, "multi-ts/network/12NodesProdFR_3PST.uct");

        importNetworksAndCracs(cracsPaths, networksPaths);
        List<Double> setPointsTs0 = List.of(6.227642059326172, 6.227642059326172, 0.0);
        List<Double> setPointsTs1 = List.of(6.227642059326172, 6.227642059326172, 6.227642059326172);
        getMarginFromSetPointManyPst(setPointsTs0, 0);
        getMarginFromSetPointManyPst(setPointsTs1, 1);
    }

    private void getMarginFromSetPointManyPst(List<Double> setPoints, int timeStepIndex) {
        for (int pstIndex = 0; pstIndex < setPoints.size(); pstIndex++) {
            PstRangeAction pstRangeAction = cracs.get(timeStepIndex).getPstRangeAction("pst_be_" + pstIndex + " - TS" + timeStepIndex);
            if (nonNull(pstRangeAction)) {
                pstRangeAction.apply(networks.get(timeStepIndex), setPoints.get(pstIndex));
            }
        }

        LoadFlow.find("OpenLoadFlow").run(networks.get(timeStepIndex), raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters());
        double p = networks.get(timeStepIndex).getLine("BBE2AA1  FFR3AA1  1").getTerminal1().getP();
        double margin = cracs.get(timeStepIndex).getFlowCnec("BBE2AA1  FFR3AA1  1 - preventive - TS" + timeStepIndex).computeMargin(p, Side.LEFT, Unit.MEGAWATT);
        System.out.println(margin);
    }

    @Test
    public void sensiThreeTimestepsTwoPst() {
        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-case0_0.json",
            "multi-ts/crac/crac-2pst-ts1.json",
            "multi-ts/crac/crac-2pst-ts2.json"
        );
        List<String> networksPaths = List.of(
            "multi-ts/network/12NodesProdFR_3PST.uct",
            "multi-ts/network/12NodesProdFR_3PST.uct",
            "multi-ts/network/12NodesProdNL_3PST.uct"
        );

        importNetworksAndCracs(cracsPaths, networksPaths);

        Map<Integer, Double> setPoints0 = Map.of(
            0, 3.505407871356285
        );
        Map<Integer, Double> setPoints1 = Map.of(
            0, 3.505407871356285,
            1, -6.2276423729910535
        );
        Map<Integer, Double> setPoints2 = Map.of(
            1, -6.2276423729910535,
            2, 2.337343603803646
        );

        getMarginFromSetPointOnePst(setPoints0, 0);
        getMarginFromSetPointOnePst(setPoints1, 1);
        getMarginFromSetPointOnePst(setPoints2, 2);
    }

    private void getMarginFromSetPointOnePst(Map<Integer, Double> setPoints, int timeStepIndex) {
        setPoints.forEach((rangeActionIndex, setPointValue) -> {
            PstRangeAction pstRangeAction = cracs.get(rangeActionIndex).getPstRangeAction("pst_be - TS" + rangeActionIndex);
            if (nonNull(pstRangeAction)) {
                pstRangeAction.apply(networks.get(timeStepIndex), setPointValue);
            }
        });

        LoadFlow.find("OpenLoadFlow").run(networks.get(timeStepIndex), raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters());
        double p = networks.get(timeStepIndex).getLine("BBE2AA1  FFR3AA1  1").getTerminal1().getP();
        double margin = cracs.get(timeStepIndex).getFlowCnec("BBE2AA1  FFR3AA1  1 - preventive - TS" + timeStepIndex).computeMargin(p, Side.LEFT, Unit.MEGAWATT);
        System.out.println(margin);
    }

    @Test
    public void findBestTapsThreeTimestepsTwoPst() {
        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-case0_0.json",
            "multi-ts/crac/crac-2pst-ts1.json",
            "multi-ts/crac/crac-2pst-ts2.json"
        );
        List<String> networksPaths = List.of(
            "multi-ts/network/12NodesProdFR_3PST.uct",
            "multi-ts/network/12NodesProdFR_3PST.uct",
            "multi-ts/network/12NodesProdNL_3PST.uct"
        );
        importNetworksAndCracs(cracsPaths, networksPaths);

        List<Integer> bestTapsList = new ArrayList<>();
        double objFctValMax = -Double.MAX_VALUE;
        for (int tapTs0 = -16; tapTs0 <= 16; tapTs0++) {
            for (int tapTs1 = -16; tapTs1 <= 16; tapTs1++) {
                for (int tapTs2 = Math.max(-16, tapTs0 - 3); tapTs2 <= Math.min(16, tapTs0 + 3); tapTs2++) {

                    double minMargin = Math.min(
                        Math.min(getMarginFromTap(List.of(tapTs0, 0), 0),
                            getMarginFromTap(List.of(tapTs0, tapTs1), 1)),
                        getMarginFromTap(List.of(tapTs2, tapTs1), 2)
                    );
                    if (minMargin > objFctValMax) {
                        objFctValMax = minMargin;
                        bestTapsList = List.of(tapTs0, tapTs1, tapTs2);
                    }
                }
            }
        }
        System.out.println(bestTapsList);
        System.out.println(objFctValMax);
    }

    private double getMarginFromTap(List<Integer> taps, int timeStepIndex) {
//        for (int pstIndex = 0; pstIndex < taps.size() ; pstIndex++) {
//            PstRangeAction pstRangeAction = cracs.get(timeStepIndex).getPstRangeAction("pst_be - TS" + timeStepIndex);
//            if (nonNull(pstRangeAction)) {
//                pstRangeAction.apply(networks.get(timeStepIndex), pstRangeAction.convertTapToAngle(taps.get(pstIndex)));
//            }
//        }

        if (timeStepIndex == 0) {
            PstRangeAction pstRangeAction = cracs.get(0).getPstRangeAction("pst_be - TS0");
            pstRangeAction.apply(networks.get(timeStepIndex), pstRangeAction.convertTapToAngle(taps.get(0)));

        } else if (timeStepIndex == 1) {
            PstRangeAction pstRangeAction0 = cracs.get(0).getPstRangeAction("pst_be - TS0");
            pstRangeAction0.apply(networks.get(timeStepIndex), pstRangeAction0.convertTapToAngle(taps.get(0)));
            PstRangeAction pstRangeAction1 = cracs.get(1).getPstRangeAction("pst_be - TS1");
            pstRangeAction1.apply(networks.get(timeStepIndex), pstRangeAction1.convertTapToAngle(taps.get(1)));
        } else if (timeStepIndex == 2) {
            PstRangeAction pstRangeAction1 = cracs.get(1).getPstRangeAction("pst_be - TS1");
            pstRangeAction1.apply(networks.get(timeStepIndex), pstRangeAction1.convertTapToAngle(taps.get(1)));
            PstRangeAction pstRangeAction2 = cracs.get(2).getPstRangeAction("pst_be - TS2");
            pstRangeAction2.apply(networks.get(timeStepIndex), pstRangeAction2.convertTapToAngle(taps.get(0)));
        }

        LoadFlow.find("OpenLoadFlow").run(networks.get(timeStepIndex), raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters());
        double p = networks.get(timeStepIndex).getLine("BBE2AA1  FFR3AA1  1").getTerminal1().getP();
        return cracs.get(timeStepIndex).getFlowCnec("BBE2AA1  FFR3AA1  1 - preventive - TS" + timeStepIndex).computeMargin(p, Side.LEFT, Unit.MEGAWATT);
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
