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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MultiTSScenariosTest {
    List<Network> networks;
    List<Crac> cracs;
    RangeActionSetpointResult initialSetpoints;
    List<OptimizationPerimeter> optimizationPerimeters;
    MultipleSensitivityResult initialSensiResult;
    RangeActionsOptimizationParameters.PstModel pstModel;

    @BeforeEach
    public void setUp() {
        pstModel = RangeActionsOptimizationParameters.PstModel.CONTINUOUS;
        int caseNumber = 1;

        String path0 = "crac/crac-case" + caseNumber + "_0.json";
        String path1 = "crac/crac-case" + caseNumber + "_1.json";

        networks = new ArrayList<>();
        networks.add(Network.read("network/12NodesProdFR.uct", getClass().getResourceAsStream("/network/12NodesProdFR.uct")));

        //for test cases number from 0 to 2
        networks.add(Network.read("network/12NodesProdFR.uct", getClass().getResourceAsStream("/network/12NodesProdFR.uct")));
        //for test cases number from 3 onwards
//        networks.add(Network.read("network/12NodesProdNL.uct", getClass().getResourceAsStream("/network/12NodesProdNL.uct")));

        cracs = new ArrayList<>();
//        cracs.add(CracImporters.importCrac("crac/crac-with-pst-range-action.json",
//            getClass().getResourceAsStream("/crac/crac-with-pst-range-action.json"),
//            networks.get(0)));
//        cracs.add(CracImporters.importCrac("crac/crac-with-pst-range-action_2.json",
//            getClass().getResourceAsStream("/crac/crac-with-pst-range-action_2.json"),
//            networks.get(1)));

        cracs.add(CracImporters.importCrac(path0,
            getClass().getResourceAsStream("/" + path0),
            networks.get(0)));
        cracs.add(CracImporters.importCrac(path1,
            getClass().getResourceAsStream("/" + path1),
            networks.get(1)));

        initialSetpoints = computeInitialSetpointsResults();
        optimizationPerimeters = computeOptimizationPerimeters();
        initialSensiResult = runInitialSensi();
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
        List<Set<FlowCnec>> cnecsList = List.of(cracs.get(0).getFlowCnecs(), cracs.get(1).getFlowCnecs());
        Set<RangeAction<?>> rangeActionsSet = Stream.of(cracs.get(0).getRangeActions(), cracs.get(1).getRangeActions())
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
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
    public void testIteratingLinearOptimization() {
        Instant preventiveInstant = Mockito.mock(Instant.class);
        Instant outageInstant = Mockito.mock(Instant.class);

        Set<FlowCnec> allCnecs = new HashSet<>();
        allCnecs.addAll(cracs.get(0).getFlowCnecs());
        allCnecs.addAll(cracs.get(1).getFlowCnecs());

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

        System.out.println(result.getStatus());

        PstRangeAction pstRa0 = cracs.get(0).getPstRangeActions().iterator().next();
        PstRangeAction pstRa1 = cracs.get(1).getPstRangeActions().iterator().next();

        State state0 = optimizationPerimeters.get(0).getMainOptimizationState();
        State state1 = optimizationPerimeters.get(1).getMainOptimizationState();
        double pstOptimizedSetPoint0 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0, state0);
        double pstOptimizedSetPoint1 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa1, state1);

        System.out.println(pstOptimizedSetPoint0);
        System.out.println(pstOptimizedSetPoint1);

    }

    @Test
    public void testLinearFirstProblemsAlone() {
        Instant outageInstant = Mockito.mock(Instant.class);

        Set<FlowCnec> allCnecs = new HashSet<>();
        allCnecs.addAll(cracs.get(0).getFlowCnecs());

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
            .withNetwork(networks.get(0))
            .withOptimizationPerimeter(optimizationPerimeters.get(0))
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

        LinearOptimizationResult result0 = IteratingLinearOptimizer.optimize(input0, parameters, outageInstant);

        PstRangeAction pstRa0 = cracs.get(0).getPstRangeActions().iterator().next();

        State state0 = optimizationPerimeters.get(0).getMainOptimizationState();
        double pstOptimizedSetPoint0 = result0.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0, state0);

        System.out.println("First TS:");
        System.out.println(pstOptimizedSetPoint0);
    }

    @Test
    public void testLinearSecondProblemAlone() {
        Instant outageInstant = Mockito.mock(Instant.class);

        Set<FlowCnec> allCnecs = new HashSet<>();
        allCnecs.addAll(cracs.get(1).getFlowCnecs());

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

        ToolProvider toolProvider = ToolProvider.create().withNetwork(networks.get(1)).withRaoParameters(raoParameters).build(); //the attributes in the class are only used for loopflow things

        IteratingLinearOptimizerInput input1 = IteratingLinearOptimizerInput.create()
            .withNetwork(networks.get(1))
            .withOptimizationPerimeter(optimizationPerimeters.get(1))
            .withInitialFlowResult(initialSensiResult)
            .withPrePerimeterFlowResult(initialSensiResult)
            .withPrePerimeterSetpoints(initialSetpoints)
            .withPreOptimizationFlowResult(initialSensiResult)
            .withPreOptimizationSensitivityResult(initialSensiResult)
            .withPreOptimizationAppliedRemedialActions(new AppliedRemedialActions())
            .withRaActivationFromParentLeaf(new RangeActionActivationResultImpl(initialSetpoints))
            .withObjectiveFunction(objectiveFunction)
            .withToolProvider(toolProvider)
            .withOutageInstant(cracs.get(1).getOutageInstant())
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

        LinearOptimizationResult result1 = IteratingLinearOptimizer.optimize(input1, parameters, outageInstant);

        PstRangeAction pstRa1 = cracs.get(1).getPstRangeActions().iterator().next();

        State state1 = optimizationPerimeters.get(1).getMainOptimizationState();
        double pstOptimizedSetPoint1 = result1.getRangeActionActivationResult().getOptimizedSetpoint(pstRa1, state1);

        System.out.println("Second TS:");
        System.out.println(pstOptimizedSetPoint1);
    }

}



