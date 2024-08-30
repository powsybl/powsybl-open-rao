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
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.IteratingLinearOptimizerMultiTS;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.MultiTSFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.CoreProblemFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.DiscretePstTapFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.MaxMinMarginFiller;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jeremy Wang {@literal <jeremy.wang at rte-france.com>}
 */
public class NewRAOTest {

    List<Network> networks;
    List<Crac> cracs;
    RangeActionSetpointResult initialSetpoints;
    List<OptimizationPerimeter> optimizationPerimeters;
    List<Map<State, Set<PstRangeAction>>> rangeActionsPerStatePerTimestamp;
    MultipleSensitivityResult initialSensiResult;
    RangeActionsOptimizationParameters.PstModel pstModel;
    RaoParameters raoParameters;

    @BeforeEach
    public void setUp() {
        networks = new ArrayList<>();
        networks.add(Network.read("multi-ts/network/12NodesProdFR.uct", getClass().getResourceAsStream("/multi-ts/network/12NodesProdFR.uct")));
//        networks.add(Network.read("multi-ts/network/12NodesProdFR_3PST.uct", getClass().getResourceAsStream("/multi-ts/network/12NodesProdFR_3PST.uct")));
        networks.add(Network.read("multi-ts/network/12NodesProdNL.uct", getClass().getResourceAsStream("/multi-ts/network/12NodesProdNL.uct")));

        cracs = new ArrayList<>();
        cracs.add(CracImporters.importCrac("multi-ts/crac/crac-network-action-0.json",
            getClass().getResourceAsStream("/multi-ts/crac/crac-network-action-0.json"),
            networks.get(0)));
        cracs.add(CracImporters.importCrac("multi-ts/crac/crac-network-action-1.json",
            getClass().getResourceAsStream("/multi-ts/crac/crac-network-action-1.json"),
            networks.get(1)));

        raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC_SCIP.json"));

        initialSetpoints = computeInitialSetpointsResults();
        optimizationPerimeters = computeOptimizationPerimeters();
        rangeActionsPerStatePerTimestamp = computeRangeActionsPerStatePerTimestamp();

        initialSensiResult = runInitialSensi();
        pstModel = RangeActionsOptimizationParameters.PstModel.CONTINUOUS;
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
    public void testLinearProblemMerge() {
        RangeActionsOptimizationParameters rangeActionParameters = raoParameters.getRangeActionsOptimizationParameters();
        rangeActionParameters.setPstModel(pstModel);
        OpenRaoMPSolver orMpSolver = new OpenRaoMPSolver("solver", RangeActionsOptimizationParameters.Solver.SCIP);

        CoreProblemFiller coreProblemFiller0 = new CoreProblemFiller(
            optimizationPerimeters.get(0),
            initialSetpoints,
            new RangeActionActivationResultImpl(initialSetpoints),
            rangeActionParameters,
            Unit.MEGAWATT,
            false, 0);
        CoreProblemFiller coreProblemFiller1 = new CoreProblemFiller(
            optimizationPerimeters.get(1),
            initialSetpoints,
            new RangeActionActivationResultImpl(initialSetpoints),
            rangeActionParameters,
            Unit.MEGAWATT,
            false, 1);

        DiscretePstTapFiller discretePstTapFiller0 = new DiscretePstTapFiller(
            networks.get(0),
            optimizationPerimeters.get(0),
            rangeActionsPerStatePerTimestamp.get(0),
            initialSetpoints);
        DiscretePstTapFiller discretePstTapFiller1 = new DiscretePstTapFiller(
            networks.get(1),
            optimizationPerimeters.get(1),
            rangeActionsPerStatePerTimestamp.get(1),
            initialSetpoints);

        Set<FlowCnec> allCnecs = new HashSet<>();
        allCnecs.addAll(cracs.get(0).getFlowCnecs());
        allCnecs.addAll(cracs.get(1).getFlowCnecs());
        MaxMinMarginFiller maxMinMarginFiller = new MaxMinMarginFiller(allCnecs, Unit.MEGAWATT);

        MultiTSFiller multiTSFiller = new MultiTSFiller(
            optimizationPerimeters,
            networks,
            rangeActionParameters,
            new RangeActionActivationResultImpl(initialSetpoints));

        LinearProblemBuilder linearProblemBuilder = new LinearProblemBuilder()
            .withSolver(orMpSolver.getSolver())
            .withProblemFiller(coreProblemFiller0)
            .withProblemFiller(coreProblemFiller1)
            .withProblemFiller(maxMinMarginFiller);

        if (pstModel == RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS) {
            linearProblemBuilder.withProblemFiller(discretePstTapFiller0)
                .withProblemFiller(discretePstTapFiller1);
        }
        LinearProblem linearProblemMerge = linearProblemBuilder
            .withProblemFiller(multiTSFiller)
            .build();

        linearProblemMerge.fill(initialSensiResult, initialSensiResult);
        linearProblemMerge.solve();
        // Pour avoir le setpoint après résolution du problème
        PstRangeAction pstRa0 = cracs.get(0).getPstRangeActions().iterator().next();
        PstRangeAction pstRa1 = cracs.get(1).getPstRangeActions().iterator().next();
        State state0 = optimizationPerimeters.get(0).getMainOptimizationState();
        State state1 = optimizationPerimeters.get(1).getMainOptimizationState();
        double setpointMerge0 = linearProblemMerge.getRangeActionSetpointVariable(pstRa0, state0).solutionValue();
        double setpointMerge1 = linearProblemMerge.getRangeActionSetpointVariable(pstRa1, state1).solutionValue();

        System.out.println(setpointMerge0);
        System.out.println(setpointMerge1);
    }

    @Test
    public void testIteratingLinearOptimization() {

        Set<FlowCnec> allCnecs = new HashSet<>();
        allCnecs.addAll(cracs.get(0).getFlowCnecs());
        allCnecs.addAll(cracs.get(1).getFlowCnecs());

        raoParameters.getRangeActionsOptimizationParameters().setPstModel(pstModel);

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
            .withOutageInstant(cracs.get(0).getOutageInstant()) //only useful l:92 SystematicSensitivityAdapter
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

        LinearOptimizationResult result = IteratingLinearOptimizerMultiTS.optimize(input, parameters, cracs.get(0).getOutageInstant());

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
    public void testLinearProblemsSeparated() {

        OpenRaoMPSolver orMpSolver0 = new OpenRaoMPSolver("solver0", RangeActionsOptimizationParameters.Solver.SCIP);
        OpenRaoMPSolver orMpSolver1 = new OpenRaoMPSolver("solver1", RangeActionsOptimizationParameters.Solver.SCIP);
        RangeActionsOptimizationParameters rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(new RaoParameters());

        MaxMinMarginFiller maxMinMarginFiller0 = new MaxMinMarginFiller(cracs.get(0).getFlowCnecs(), Unit.MEGAWATT);
        MaxMinMarginFiller maxMinMarginFiller1 = new MaxMinMarginFiller(cracs.get(1).getFlowCnecs(), Unit.MEGAWATT);

        CoreProblemFiller coreProblemFiller0 = new CoreProblemFiller(
            optimizationPerimeters.get(0),
            initialSetpoints,
            new RangeActionActivationResultImpl(initialSetpoints),
            rangeActionParameters,
            Unit.MEGAWATT,
            false,
            0);
        CoreProblemFiller coreProblemFiller1 = new CoreProblemFiller(
            optimizationPerimeters.get(1),
            initialSetpoints,
            new RangeActionActivationResultImpl(initialSetpoints),
            rangeActionParameters,
            Unit.MEGAWATT,
            false,
            1);

        LinearProblem linearProblem0 = new LinearProblemBuilder()
            .withSolver(orMpSolver0.getSolver())
            .withProblemFiller(coreProblemFiller0)
            .withProblemFiller(maxMinMarginFiller0)
            .build();

        linearProblem0.fill(initialSensiResult, initialSensiResult);
        linearProblem0.solve();

        LinearProblem linearProblem1 = new LinearProblemBuilder()
            .withSolver(orMpSolver1.getSolver())
            .withProblemFiller(coreProblemFiller1)
            .withProblemFiller(maxMinMarginFiller1)
            .build();

        linearProblem1.fill(initialSensiResult, initialSensiResult);
        linearProblem1.solve();

        PstRangeAction pstRa0 = cracs.get(0).getPstRangeActions().iterator().next();
        PstRangeAction pstRa1 = cracs.get(1).getPstRangeActions().iterator().next();
        State state0 = optimizationPerimeters.get(0).getMainOptimizationState();
        State state1 = optimizationPerimeters.get(1).getMainOptimizationState();
        double setpointMerge0 = linearProblem0.getRangeActionSetpointVariable(pstRa0, state0).solutionValue();
        double setpointMerge1 = linearProblem1.getRangeActionSetpointVariable(pstRa1, state1).solutionValue();

        System.out.println(setpointMerge0);
        System.out.println(setpointMerge1);
    }

    @Test
    void findBestTap() {

        Map<Integer, Double> marginsMap0 = computeMargins(0);
        Map<Integer, Double> marginsMap1 = computeMargins(1);

        double objFctValMax = -Double.MAX_VALUE;
        int bestTapTs0 = 0;
        int bestTapTs1 = 0;
        for (int tapTs0 = -16; tapTs0 <= 16; tapTs0++) {
            for (int tapTs1 = Math.max(-16, tapTs0 - 3); tapTs1 <= Math.min(16, tapTs0 + 3); tapTs1++) {
                double objFctVal = Math.min(marginsMap0.get(tapTs0), marginsMap1.get(tapTs1));
                if (objFctVal > objFctValMax) {
                    objFctValMax = objFctVal;
                    bestTapTs0 = tapTs0;
                    bestTapTs1 = tapTs1;
                }
            }
        }
        System.out.println(bestTapTs0);
        System.out.println(bestTapTs1);
    }

    private Map<Integer, Double> computeMargins(int i) {
        Network network = networks.get(i);
        Crac crac = cracs.get(i);

//        crac.getNetworkAction("close-fr3-be2-2 - TS" + i).apply(network);
//        crac.getNetworkAction("close-fr3-be2-3 - TS" + i).apply(network);
        Map<Integer, Double> marginsMap = new HashMap<>();

        for (int tap = -16; tap <= 16; tap++) {
            PstRangeAction pstRangeAction0 = crac.getPstRangeAction("pst_be - TS" + i);
            pstRangeAction0.apply(network, pstRangeAction0.convertTapToAngle(tap));
            LoadFlow.find("OpenLoadFlow").run(network, raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters());
            double p = network.getLine("BBE2AA1  FFR3AA1  1").getTerminal1().getP();
            double margin = crac.getFlowCnec("BBE2AA1  FFR3AA1  1 - preventive - TS" + i).computeMargin(p, Side.LEFT, Unit.MEGAWATT);
            marginsMap.put(tap, margin);
        }
        return marginsMap;
    }
}
