package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;


import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracioapi.CracImporters;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.IteratingLinearOptimizer;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.BetweenTimeStepsFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.CoreProblemFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.DiscretePstTapFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.MaxMinMarginFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.MultipleSensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityInterface;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.when;

public class NewRAOTest {

    @Test
    public void testNewRAO(){
        Network network1 = Network.read("network/12NodesProdFR.uct", getClass().getResourceAsStream("/network/12NodesProdFR.uct"));;
        Network network2 = Network.read("network/12NodesProdNL.uct", getClass().getResourceAsStream("/network/12NodesProdNL.uct"));;

        Crac crac1 = CracImporters.importCrac("crac/crac-with-pst-range-action.json", getClass().getResourceAsStream("/crac/crac-with-pst-range-action.json"), network1);;
        Crac crac2 = CracImporters.importCrac("crac/crac-with-pst-range-action_2.json", getClass().getResourceAsStream("/crac/crac-with-pst-range-action_2.json"), network2);;

        RangeActionsOptimizationParameters.PstModel pstModel = RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS;

        // prepare data
        State state1 = crac1.getPreventiveState();
        State state2 = crac2.getPreventiveState();

        PstRangeAction pstRa1 = crac1.getPstRangeAction("pst_be - TS1");
        PstRangeAction pstRa2 = crac2.getPstRangeAction("pst_be - TS2");

        FlowCnec cnec1 = crac1.getFlowCnec("BBE2AA1  FFR3AA1  1 - preventive - TS1");
        FlowCnec cnec2 = crac2.getFlowCnec("BBE2AA1  FFR3AA1  1 - preventive - TS2");

        Map<Integer, Double> tapToAngle = pstRa1.getTapToAngleConversionMap(); // both PSTs have the same map
        double initialAlpha = tapToAngle.get(0);
        RangeActionSetpointResult initialRangeActionSetpointResult1 = new RangeActionSetpointResultImpl(Map.of(pstRa1, initialAlpha));
        RangeActionSetpointResult initialRangeActionSetpointResult2 = new RangeActionSetpointResultImpl(Map.of(pstRa2, initialAlpha));

        OptimizationPerimeter optimizationPerimeter1 = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter1.getFlowCnecs()).thenReturn(Set.of(cnec1));
        Map<State, Set<RangeAction<?>>> rangeActions1 = new HashMap<>();
        rangeActions1.put(state1, Set.of(pstRa1));
        Mockito.when(optimizationPerimeter1.getRangeActionsPerState()).thenReturn(rangeActions1);
        OptimizationPerimeter optimizationPerimeter2 = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter2.getFlowCnecs()).thenReturn(Set.of(cnec2));
        Map<State, Set<RangeAction<?>>> rangeActions2 = new HashMap<>();
        rangeActions2.put(state1, Set.of(pstRa2));
        Mockito.when(optimizationPerimeter2.getRangeActionsPerState()).thenReturn(rangeActions2);

        RangeActionsOptimizationParameters rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(new RaoParameters());
        rangeActionParameters.setPstModel(pstModel);
        OpenRaoMPSolver orMpSolver = new OpenRaoMPSolver("solver", RangeActionsOptimizationParameters.Solver.SCIP);

        CoreProblemFiller coreProblemFiller1 = new CoreProblemFiller(
                optimizationPerimeter1,
                initialRangeActionSetpointResult1,
                new RangeActionActivationResultImpl(initialRangeActionSetpointResult1),
                rangeActionParameters,
                Unit.MEGAWATT,
                false);
        CoreProblemFiller coreProblemFiller2 = new CoreProblemFiller(
                optimizationPerimeter2,
                initialRangeActionSetpointResult2,
                new RangeActionActivationResultImpl(initialRangeActionSetpointResult2),
                rangeActionParameters,
                Unit.MEGAWATT,
                false);
        Map<State, Set<PstRangeAction>> rangeActionsPst1 = new HashMap<>();
        rangeActionsPst1.put(state1, Set.of(pstRa1));
        DiscretePstTapFiller discretePstTapFiller1 = new DiscretePstTapFiller(
                network1,
                state1,
                rangeActionsPst1,
                initialRangeActionSetpointResult1);

        Map<State, Set<PstRangeAction>> rangeActionsPst2 = new HashMap<>();
        rangeActionsPst2.put(state1, Set.of(pstRa2));
        DiscretePstTapFiller discretePstTapFiller2 = new DiscretePstTapFiller(
                network2,
                state1,
                rangeActionsPst2,
                initialRangeActionSetpointResult2);


        MaxMinMarginFiller maxMinMarginFiller = new MaxMinMarginFiller(Set.of(cnec1, cnec2),Unit.MEGAWATT);
        BetweenTimeStepsFiller betweenTimeStepsFiller = new BetweenTimeStepsFiller(
                List.of(crac1,crac2),
                List.of(network1,network2),
                state1,
                rangeActionParameters);
        LinearProblem linearProblemMerge = new LinearProblemBuilder()
                .withSolver(orMpSolver)
                .withProblemFiller(coreProblemFiller1)
                .withProblemFiller(coreProblemFiller2)
                .withProblemFiller(maxMinMarginFiller)
                .withProblemFiller(discretePstTapFiller1)
                .withProblemFiller(discretePstTapFiller2)
                .withProblemFiller(betweenTimeStepsFiller)
                .build();

        SystematicSensitivityInterface systematicSensitivityInterface1 = SystematicSensitivityInterface.builder()
                .withSensitivityProviderName("OpenLoadFlow")
                .withParameters(new SensitivityAnalysisParameters())
                .withRangeActionSensitivities(Set.of(pstRa1),Set.of(cnec1), Set.of(Unit.MEGAWATT))
                .withOutageInstant(crac1.getOutageInstant())
                .build();
        SystematicSensitivityResult systematicSensitivityResult1 = systematicSensitivityInterface1.run(network1);
        SystematicSensitivityInterface systematicSensitivityInterface2 = SystematicSensitivityInterface.builder()
                .withSensitivityProviderName("OpenLoadFlow")
                .withParameters(new SensitivityAnalysisParameters())
                .withRangeActionSensitivities(Set.of(pstRa2),Set.of(cnec2), Set.of(Unit.MEGAWATT))
                .withOutageInstant(crac2.getOutageInstant())
                .build();
        SystematicSensitivityResult systematicSensitivityResult2 = systematicSensitivityInterface2.run(network2);

        MultipleSensitivityResult multipleSensitivityResult = new MultipleSensitivityResult();
        multipleSensitivityResult.addResult(systematicSensitivityResult1, Set.of(cnec1));
        multipleSensitivityResult.addResult(systematicSensitivityResult2, Set.of(cnec2));

        linearProblemMerge.fill(multipleSensitivityResult, multipleSensitivityResult);
        linearProblemMerge.solve();
        System.out.println(orMpSolver.getMpSolver().exportModelAsLpFormat());

        // Pour avoir le setpoint après résolution du problème
        double setpointMerge1 = linearProblemMerge.getRangeActionSetpointVariable(pstRa1, state1).solutionValue();
        double setpointMerge2 = linearProblemMerge.getRangeActionSetpointVariable(pstRa2, state1).solutionValue();

        System.out.println(setpointMerge1);
        System.out.println(setpointMerge2);

        IteratingLinearOptimizerInput input;
        IteratingLinearOptimizerParameters parameters;
        Instant preventiveInstant = Mockito.mock(Instant.class);

//
//        input = Mockito.mock(IteratingLinearOptimizerInput.class);
//        when(input.getObjectiveFunction()).thenReturn(objectiveFunction);
//        when(input.getPreOptimizationSensitivityResult()).thenReturn(multipleSensitivityResult);
//
//        // which perimeter??
//        when(input.getOptimizationPerimeter()).thenReturn(optimizationPerimeter1);
//
//        parameters = Mockito.mock(IteratingLinearOptimizerParameters.class);
//        RangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters = Mockito.mock(RangeActionsOptimizationParameters.LinearOptimizationSolver.class);
//        when(solverParameters.getSolver()).thenReturn(RangeActionsOptimizationParameters.Solver.CBC);
//        when(parameters.getSolverParameters()).thenReturn(solverParameters);
//        when(parameters.getMaxNumberOfIterations()).thenReturn(5);
//        when(parameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT);
//        when(parameters.getRaRangeShrinking()).thenReturn(false);
//
//        LinearOptimizationResult result = IteratingLinearOptimizer.optimize(input, parameters, preventiveInstant);



//
//        // Résultat des deux LP séparés
//        OpenRaoMPSolver orMpSolver1 = new OpenRaoMPSolver("solver1", RangeActionsOptimizationParameters.Solver.SCIP);
//        OpenRaoMPSolver orMpSolver2 = new OpenRaoMPSolver("solver2", RangeActionsOptimizationParameters.Solver.SCIP);
//        MaxMinMarginFiller maxMinMarginFiller1 = new MaxMinMarginFiller(Set.of(cnec1),Unit.MEGAWATT);
//        MaxMinMarginFiller maxMinMarginFiller2 = new MaxMinMarginFiller(Set.of(cnec2),Unit.MEGAWATT);
//
//        LinearProblem linearProblem1 = new LinearProblemBuilder()
//                .withSolver(orMpSolver1)
//                .withProblemFiller(coreProblemFiller1)
//                .withProblemFiller(maxMinMarginFiller1)
//                .build();
//
//        linearProblem1.fill(multipleSensitivityResult, multipleSensitivityResult);
//        linearProblem1.solve();
//
//        LinearProblem linearProblem2 = new LinearProblemBuilder()
//                .withSolver(orMpSolver2)
//                .withProblemFiller(coreProblemFiller2)
//                .withProblemFiller(maxMinMarginFiller2)
//                .build();
//
//        linearProblem2.fill(multipleSensitivityResult, multipleSensitivityResult);
//        linearProblem2.solve();
//
//        double setpoint1 = linearProblem1.getRangeActionSetpointVariable(pstRa1, state1).solutionValue();
//        double setpoint2 = linearProblem2.getRangeActionSetpointVariable(pstRa2, state1).solutionValue();
//        System.out.println(setpoint1);
//        System.out.println(setpoint2);
//
//        System.out.println(orMpSolver2.getMpSolver().exportModelAsLpFormat());

    }
}
