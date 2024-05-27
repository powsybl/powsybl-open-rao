package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracioapi.CracImporters;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.IteratingLinearOptimizer;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.BetweenTimeStepsFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.CoreProblemFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.DiscretePstTapFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.MaxMinMarginFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.FlowResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.MultipleSensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityInterface;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

public class NewRAOTest {

    List<Network> networks;
    List<Crac> cracs;
    List<RangeActionSetpointResult> initialSetpoints;
    List<OptimizationPerimeter> optimizationPerimeters;
    List<Map<State, Set<PstRangeAction>>> rangeActionsPerStatePerTimestamp;

    @BeforeEach
    public void setUp() {
        networks = new ArrayList<>();
        networks.add(Network.read("network/12NodesProdFR.uct", getClass().getResourceAsStream("/network/12NodesProdFR.uct")));
        networks.add(Network.read("network/12NodesProdNL.uct", getClass().getResourceAsStream("/network/12NodesProdNL.uct")));

        cracs = new ArrayList<>();
        cracs.add(CracImporters.importCrac("crac/crac-with-pst-range-action.json",
            getClass().getResourceAsStream("/crac/crac-with-pst-range-action.json"),
            networks.get(0)));
        cracs.add(CracImporters.importCrac("crac/crac-with-pst-range-action_2.json",
            getClass().getResourceAsStream("/crac/crac-with-pst-range-action_2.json"),
            networks.get(1)));

        initialSetpoints = computeInitialSetpointsResults();
        optimizationPerimeters = computeOptimizationPerimeters();
        rangeActionsPerStatePerTimestamp = computeRangeActionsPerStatePerTimestamp();


    }

    private List<RangeActionSetpointResult> computeInitialSetpointsResults() {
        List<RangeActionSetpointResult> results = new ArrayList<>();
        for (int i = 0; i < cracs.size(); i++) {
            results.add(RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(networks.get(i), cracs.get(i).getRangeActions()));
        }
        return results;
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

    private List<Map<State, Set<PstRangeAction>>> computeRangeActionsPerStatePerTimestamp() {
        List<Map<State, Set<PstRangeAction>>> rangeActionsPerStatePerTimestamp = new ArrayList<>();
        for (Crac crac : cracs) {
            Map<State, Set<PstRangeAction>> rangeActionsPerState = new HashMap<>();
            crac.getStates().forEach(state -> rangeActionsPerState.put(state,
                crac.getPotentiallyAvailableRangeActions(state).stream()
                    .filter(ra -> ra instanceof PstRangeAction)
                    .map(ra -> (PstRangeAction) ra)
                    .collect(Collectors.toSet())));
            rangeActionsPerStatePerTimestamp.add(rangeActionsPerState);
        }
        return rangeActionsPerStatePerTimestamp;

    }

    @Test
    public void testNewRAO() {
        RangeActionsOptimizationParameters rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(new RaoParameters());
        rangeActionParameters.setPstModel(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);
        OpenRaoMPSolver orMpSolver = new OpenRaoMPSolver("solver", RangeActionsOptimizationParameters.Solver.SCIP);

        CoreProblemFiller coreProblemFiller0 = new CoreProblemFiller(
            optimizationPerimeters.get(0),
            initialSetpoints.get(0),
            new RangeActionActivationResultImpl(initialSetpoints.get(0)),
            rangeActionParameters,
            Unit.MEGAWATT,
            false);
        CoreProblemFiller coreProblemFiller1 = new CoreProblemFiller(
            optimizationPerimeters.get(1),
            initialSetpoints.get(1),
            new RangeActionActivationResultImpl(initialSetpoints.get(1)),
            rangeActionParameters,
            Unit.MEGAWATT,
            false);

        DiscretePstTapFiller discretePstTapFiller0 = new DiscretePstTapFiller(
            networks.get(0),
            optimizationPerimeters.get(0).getMainOptimizationState(),
            rangeActionsPerStatePerTimestamp.get(0),
            initialSetpoints.get(0));
        DiscretePstTapFiller discretePstTapFiller1 = new DiscretePstTapFiller(
            networks.get(1),
            optimizationPerimeters.get(1).getMainOptimizationState(),
            rangeActionsPerStatePerTimestamp.get(1),
            initialSetpoints.get(1));

        Set<FlowCnec> allCnecs = new HashSet<>();
        allCnecs.addAll(cracs.get(0).getFlowCnecs());
        allCnecs.addAll(cracs.get(1).getFlowCnecs());
        MaxMinMarginFiller maxMinMarginFiller = new MaxMinMarginFiller(allCnecs, Unit.MEGAWATT);

        BetweenTimeStepsFiller betweenTimeStepsFiller = new BetweenTimeStepsFiller(
            cracs,
            networks,
            optimizationPerimeters.get(0).getMainOptimizationState(),
            rangeActionParameters);

        LinearProblem linearProblemMerge = new LinearProblemBuilder()
            .withSolver(orMpSolver)
            .withProblemFiller(coreProblemFiller0)
            .withProblemFiller(coreProblemFiller1)
            .withProblemFiller(maxMinMarginFiller)
            .withProblemFiller(discretePstTapFiller0)
            .withProblemFiller(discretePstTapFiller1)
            .withProblemFiller(betweenTimeStepsFiller)
            .build();

        SystematicSensitivityInterface systematicSensitivityInterface0 = SystematicSensitivityInterface.builder()
            .withSensitivityProviderName("OpenLoadFlow")
            .withParameters(new SensitivityAnalysisParameters())
            .withRangeActionSensitivities(cracs.get(0).getRangeActions(), cracs.get(0).getFlowCnecs(), Set.of(Unit.MEGAWATT))
            .withOutageInstant(cracs.get(0).getOutageInstant())
            .build();
        SystematicSensitivityResult systematicSensitivityResult0 = systematicSensitivityInterface0.run(networks.get(0));

        SystematicSensitivityInterface systematicSensitivityInterface1 = SystematicSensitivityInterface.builder()
            .withSensitivityProviderName("OpenLoadFlow")
            .withParameters(new SensitivityAnalysisParameters())
            .withRangeActionSensitivities(cracs.get(1).getRangeActions(), cracs.get(1).getFlowCnecs(), Set.of(Unit.MEGAWATT))
            .withOutageInstant(cracs.get(1).getOutageInstant())
            .build();
        SystematicSensitivityResult systematicSensitivityResult1 = systematicSensitivityInterface1.run(networks.get(1));

        MultipleSensitivityResult multipleSensitivityResult = new MultipleSensitivityResult();
        multipleSensitivityResult.addResult(systematicSensitivityResult0, cracs.get(0).getFlowCnecs());
        multipleSensitivityResult.addResult(systematicSensitivityResult1, cracs.get(1).getFlowCnecs());

        linearProblemMerge.fill(multipleSensitivityResult, multipleSensitivityResult);
        linearProblemMerge.solve();
//        System.out.println(orMpSolver.getMpSolver().exportModelAsLpFormat());

        // Pour avoir le setpoint après résolution du problème
        PstRangeAction pstRa0 = cracs.get(0).getPstRangeActions().iterator().next();
        PstRangeAction pstRa1 = cracs.get(1).getPstRangeActions().iterator().next();
        State state0 = optimizationPerimeters.get(0).getMainOptimizationState();
        State state1 = optimizationPerimeters.get(1).getMainOptimizationState();
        double setpointMerge0 = linearProblemMerge.getRangeActionSetpointVariable(pstRa0, state0).solutionValue();
        double setpointMerge1 = linearProblemMerge.getRangeActionSetpointVariable(pstRa1, state1).solutionValue();

        System.out.println(setpointMerge0);
        System.out.println(setpointMerge1);

        // --------------------------

        Instant preventiveInstant = Mockito.mock(Instant.class);
        Instant outageInstant = Mockito.mock(Instant.class);

//        FlowResultImpl initialFlowResult = new FlowResultImpl();

        ObjectiveFunction objectiveFunction = ObjectiveFunction.create().build(
            allCnecs,
            Collections.emptySet(),
            multipleSensitivityResult,
            multipleSensitivityResult,
            initialSetpoints.get(0), //??
            cracs.get(0), //??
            Collections.emptySet(),
            RaoParameters.load()
            );
//        ObjectiveFunction.create().build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialResult, initialResult, initialResult, raoInput.getCrac(), Collections.emptySet(), raoParameters)


        IteratingLinearOptimizerInput input = IteratingLinearOptimizerInput.create()
            .withNetwork(network1)
            .withOptimizationPerimeter(optimizationPerimeter1)
            .withInitialFlowResult(multipleSensitivityResult)
            .withPrePerimeterFlowResult(multipleSensitivityResult)
            .withPrePerimeterSetpoints(initialRangeActionSetpointResult1)
            .withPreOptimizationFlowResult(multipleSensitivityResult) //??
            .withPreOptimizationSensitivityResult(multipleSensitivityResult) //???
//            .withPreOptimizationAppliedRemedialActions(appliedRemedialActionsInSecondaryStates)
            .withRaActivationFromParentLeaf(new RangeActionActivationResultImpl(initialRangeActionSetpointResult1))
            .withObjectiveFunction(objectiveFunction)
//            .withToolProvider(searchTreeInput.getToolProvider())
//            .withOutageInstant(searchTreeInput.getOutageInstant())
            .build();


        IteratingLinearOptimizerParameters parameters = IteratingLinearOptimizerParameters.create()
            .withObjectiveFunction(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT)
            .withRangeActionParameters(rangeActionParameters)
//            .withMnecParameters(parameters.getMnecParameters())
            .withMaxMinRelativeMarginParameters(parameters.getMaxMinRelativeMarginParameters())
//            .withLoopFlowParameters(parameters.getLoopFlowParameters())
//            .withUnoptimizedCnecParameters(parameters.getUnoptimizedCnecParameters())
//            .withRaLimitationParameters(getRaLimitationParameters(searchTreeInput.getOptimizationPerimeter(), parameters))
            .withSolverParameters(RangeActionsOptimizationParameters.LinearOptimizationSolver.load(PlatformConfig.defaultConfig()))
            .withMaxNumberOfIterations(3)
//            .withRaRangeShrinking(parameters.getTreeParameters().raRangeShrinking())
            .build();

        LinearOptimizationResult result = IteratingLinearOptimizer.optimize(input, parameters, preventiveInstant);

        System.out.println(result.getStatus());
        System.out.println(result.getOptimizedSetpoint(pstRa1, state1));


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
//        System.out.println(orMpSolver2.getMpSolver().exportModelAsLpFormat());/**/

    }
}
