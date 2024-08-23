/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracioapi.CracImporters;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputerMultiTS;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.*;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.MultipleSensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jeremy Wang {@literal <jeremy.wang at rte-france.com>}
 */
public class NewObjectiveFunctionTest {

    List<Network> networks;
    List<Crac> cracs;
    RangeActionSetpointResult initialSetpoints;
    List<OptimizationPerimeter> optimizationPerimeters;
    List<Map<State, Set<PstRangeAction>>> rangeActionsPerStatePerTimestamp;
    MultipleSensitivityResult initialSensiResult;
    RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC_SCIP.json"));

    @BeforeEach
    public void setUp() {
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

    private void importNetworksAndCracs(List<String> cracsPaths, List<String> networksPaths) {
        cracs = new ArrayList<>();
        networks = new ArrayList<>();
        for (int i = 0; i < networksPaths.size(); i++) {
            networks.add(Network.read(networksPaths.get(i), getClass().getResourceAsStream("/" + networksPaths.get(i))));
            cracs.add(CracImporters.importCrac(cracsPaths.get(i), getClass().getResourceAsStream("/" + cracsPaths.get(i)), networks.get(i)));
        }
    }

    @Test
    public void testLinearProblem() {
        importNetworksAndCracs(
            List.of("multi-ts/crac/crac-cost-0.json"),
            List.of("multi-ts/network/12NodesProdFR.uct")
        );

        initialSetpoints = computeInitialSetpointsResults();
        optimizationPerimeters = computeOptimizationPerimeters();
        rangeActionsPerStatePerTimestamp = computeRangeActionsPerStatePerTimestamp();

        initialSensiResult = runInitialSensi();

        RangeActionsOptimizationParameters rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(new RaoParameters());
        rangeActionParameters.setPstModel(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);
        OpenRaoMPSolver orMpSolver = new OpenRaoMPSolver("solver", RangeActionsOptimizationParameters.Solver.SCIP);

        CoreProblemFiller coreProblemFiller0 = new CoreProblemFiller(
            optimizationPerimeters.get(0),
            initialSetpoints,
            new RangeActionActivationResultImpl(initialSetpoints),
            rangeActionParameters,
            Unit.MEGAWATT,
            false);

        DiscretePstTapFiller discretePstTapFiller0 = new DiscretePstTapFiller(
            networks.get(0),
            optimizationPerimeters.get(0),
            rangeActionsPerStatePerTimestamp.get(0),
            initialSetpoints);

        Set<FlowCnec> allCnecs = new HashSet<>();
        allCnecs.addAll(cracs.get(0).getFlowCnecs());
        MinCostHardFiller minCostHardFiller = new MinCostHardFiller(allCnecs, Unit.MEGAWATT, rangeActionsPerStatePerTimestamp.get(0));

        MultiTSFiller multiTSFiller = new MultiTSFiller(
            optimizationPerimeters,
            networks,
            rangeActionParameters,
            new RangeActionActivationResultImpl(initialSetpoints));

        LinearProblem linearProblemMerge = new LinearProblemBuilder()
            .withSolver(orMpSolver.getSolver())
            .withProblemFiller(coreProblemFiller0)
            .withProblemFiller(minCostHardFiller)
            .withProblemFiller(discretePstTapFiller0)
            .withProblemFiller(multiTSFiller)
            .build();

        linearProblemMerge.fill(initialSensiResult, initialSensiResult);
        linearProblemMerge.solve();

        PstRangeAction pstRa0 = cracs.get(0).getPstRangeActions().iterator().next();
        State state0 = optimizationPerimeters.get(0).getMainOptimizationState();
        double setpointMerge0 = linearProblemMerge.getRangeActionSetpointVariable(pstRa0, state0).solutionValue();

        System.out.println(setpointMerge0);
    }

}
