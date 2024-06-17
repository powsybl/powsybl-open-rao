package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracioapi.CracImporters;
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

public class MultiTSScenariosTest {
    List<Network> networks;
    List<Crac> cracs;
    RangeActionSetpointResult initialSetpoints;
    List<OptimizationPerimeter> optimizationPerimeters;
    MultipleSensitivityResult initialSensiResult;
    RangeActionsOptimizationParameters.PstModel pstModel = RangeActionsOptimizationParameters.PstModel.CONTINUOUS;

    @BeforeEach
    public void setUp() {
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

        RaoParameters raoParameters = RaoParameters.load();
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
    public void testTwoTimestepsThreePst() {
        networks = new ArrayList<>();
        networks.add(Network.read("multi-ts/network/12NodesProdFR_3PST.uct",
            getClass().getResourceAsStream("/multi-ts/network/12NodesProdFR_3PST.uct")));
        networks.add(Network.read("multi-ts/network/12NodesProdFR_3PST.uct",
            getClass().getResourceAsStream("/multi-ts/network/12NodesProdFR_3PST.uct")));

        cracs = new ArrayList<>();
        cracs.add(CracImporters.importCrac("multi-ts/crac/crac-3pst-ts0.json",
            getClass().getResourceAsStream("/multi-ts/crac/crac-3pst-ts0.json"),
            networks.get(0)));
        cracs.add(CracImporters.importCrac("multi-ts/crac/crac-3pst-ts1.json",
            getClass().getResourceAsStream("/multi-ts/crac/crac-3pst-ts1.json"),
            networks.get(1)));

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
            "multi-ts/network/12NodesProdFR_2PST.uct",
            "multi-ts/network/12NodesProdFR_2PST.uct",
            "multi-ts/network/12NodesProdNL_2PST.uct"
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
        LinearOptimizationResult result0 = testProblemAlone(0);
        LinearOptimizationResult result1 = testProblemAlone(1);
        LinearOptimizationResult result2 = testProblemAlone(2);
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

        System.out.println("---- Problems alone----");
        double pstOptimizedSetPointAlonets0 = result0.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0Ts0, state0);
        System.out.println(pstOptimizedSetPointAlonets0);

        double pstOptimizedSetPointAlonets1 = result1.getRangeActionActivationResult().getOptimizedSetpoint(pstRa1Ts1, state1);
        System.out.println(pstOptimizedSetPointAlonets1);

        double pstOptimizedSetPointAlonets2 = result2.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0Ts2, state2);
        System.out.println(pstOptimizedSetPointAlonets2);
    }

    public LinearOptimizationResult runIteratingLinearOptimization() {

        Instant preventiveInstant = Mockito.mock(Instant.class);
        Instant outageInstant = Mockito.mock(Instant.class);

        Set<FlowCnec> allCnecs = new HashSet<>();
        cracs.forEach(crac -> allCnecs.addAll(crac.getFlowCnecs()));

        RaoParameters raoParameters = RaoParameters.load();
        raoParameters.getRangeActionsOptimizationParameters().setPstModel(pstModel);

        ObjectiveFunction objectiveFunction = ObjectiveFunction.create().build(
            allCnecs,
            Collections.emptySet(), // loopflows
            initialSensiResult,
            initialSensiResult,
            initialSetpoints,
            null, //crac(s), not useful (CNECs secured by PST)
            Collections.emptySet(), // operators not sharing CRAs
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
            .withSolverParameters(RangeActionsOptimizationParameters.LinearOptimizationSolver.load(PlatformConfig.defaultConfig()))
            .withMaxNumberOfIterations(3)
            .withRaRangeShrinking(false)
            .build();

        LinearOptimizationResult result = IteratingLinearOptimizerMultiTS.optimize(input, parameters, outageInstant);
        return result;

    }

    public void testEasyCasesSameNetwork(int caseNumber) {
        String pathCrac0 = "multi-ts/crac/crac-case" + caseNumber + "_0.json";
        String pathCrac1 = "multi-ts/crac/crac-case" + caseNumber + "_1.json";

        Map<String, String> pathsCracsAndNetworks = Map.of(
            pathCrac0, "multi-ts/network/12NodesProdFR.uct",
            pathCrac1, "multi-ts/network/12NodesProdFR.uct"
        );

        cracs = new ArrayList<>();
        networks = new ArrayList<>();
        pathsCracsAndNetworks.forEach((cracPath, networkPath) -> {
                networks.add(Network.read(networkPath, getClass().getResourceAsStream("/" + networkPath)));
                cracs.add(CracImporters.importCrac(cracPath, getClass().getResourceAsStream("/" + cracPath), networks.iterator().next()));
            }
        );

        initialSetpoints = computeInitialSetpointsResults();
        optimizationPerimeters = computeOptimizationPerimeters();
        initialSensiResult = runInitialSensi();

        LinearOptimizationResult resultTs0 = testProblemAlone(0);
        LinearOptimizationResult resultTs1 = testProblemAlone(1);

        LinearOptimizationResult result = runIteratingLinearOptimization();
        System.out.println(result.getStatus());

        PstRangeAction pstRa0 = cracs.get(0).getPstRangeActions().iterator().next();
        PstRangeAction pstRa1 = cracs.get(1).getPstRangeActions().iterator().next();

        State state0 = optimizationPerimeters.get(0).getMainOptimizationState();
        State state1 = optimizationPerimeters.get(1).getMainOptimizationState();
        double pstOptimizedSetPoint0 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0, state0);
        double pstOptimizedSetPoint1 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa1, state1);

        double pstOptimizedSetPointAlone0 = resultTs0.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0, state0);
        double pstOptimizedSetPointAlone1 = resultTs1.getRangeActionActivationResult().getOptimizedSetpoint(pstRa1, state1);

        System.out.println("---- First problem alone -----");
        System.out.println(pstOptimizedSetPointAlone0);
        System.out.println("---- Second problem alone -----");
        System.out.println(pstOptimizedSetPointAlone1);
        System.out.println("---- Merged problem -----");
        System.out.println(pstOptimizedSetPoint0);
        System.out.println(pstOptimizedSetPoint1);
    }

    public LinearOptimizationResult testProblemAlone(int timeStepIndex) {
        Instant outageInstant = Mockito.mock(Instant.class);

        Set<FlowCnec> allCnecs = new HashSet<>();
        allCnecs.addAll(cracs.get(timeStepIndex).getFlowCnecs());

        RaoParameters raoParameters = RaoParameters.load();
        raoParameters.getRangeActionsOptimizationParameters().setPstModel(pstModel);

        ObjectiveFunction objectiveFunction = ObjectiveFunction.create().build(
            allCnecs,
            Collections.emptySet(), // loopflows
            initialSensiResult,
            initialSensiResult,
            initialSetpoints,
            null, //crac(s), not useful (CNECs secured by PST)
            Collections.emptySet(), // operators not sharing CRAs
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
            .withSolverParameters(RangeActionsOptimizationParameters.LinearOptimizationSolver.load(PlatformConfig.defaultConfig()))
            .withMaxNumberOfIterations(3)
            .withRaRangeShrinking(false)
            .build();

        LinearOptimizationResult result = IteratingLinearOptimizer.optimize(input0, parameters, outageInstant);
        return result;
    }
}
