/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoimpl.commons.ToolProvider;
import com.powsybl.openrao.raoimpl.commons.parameters.TreeParameters;
import com.powsybl.openrao.raoimpl.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.raoimpl.result.api.OptimizationResult;
import com.powsybl.openrao.raoimpl.result.api.PrePerimeterResult;
import com.powsybl.openrao.raoimpl.result.impl.PostPerimeterResult;
import com.powsybl.openrao.raoimpl.result.impl.SkippedOptimizationResultImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class CastorContingencyScenariosTest {

    @Test
    void testIsStopCriterionChecked() {
        TreeParameters treeParameters = Mockito.mock(TreeParameters.class);
        ObjectiveFunctionResult objectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);

        // if virtual cost positive return false
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(100.);
        assertFalse(CastorContingencyScenarios.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if purely virtual with null virtual cost, return true
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-Double.MAX_VALUE);
        assertTrue(CastorContingencyScenarios.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if not purely virtual and stop criterion is MIN_OBJECTIVE return false
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-10.);
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.MIN_OBJECTIVE);
        assertFalse(CastorContingencyScenarios.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if not purely virtual and stop criterion is AT_TARGET_OBJECTIVE_VALUE and cost is higher than target return false
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-10.);
        when(objectiveFunctionResult.getCost()).thenReturn(-10.);
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(-20.);
        assertFalse(CastorContingencyScenarios.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if not purely virtual and stop criterion is AT_TARGET_OBJECTIVE_VALUE and cost is lower than target return true
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-10.);
        when(objectiveFunctionResult.getCost()).thenReturn(-10.);
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(0.);
        assertTrue(CastorContingencyScenarios.isStopCriterionChecked(objectiveFunctionResult, treeParameters));
    }

    @Test
    void testHasFailedInitialSensitivity() {
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);
        PrePerimeterResult initialSensitivityOutput = Mockito.mock(PrePerimeterResult.class);
        when(initialSensitivityOutput.getSensitivityStatus(state1)).thenReturn(ComputationStatus.DEFAULT);
        when(initialSensitivityOutput.getSensitivityStatus(state2)).thenReturn(ComputationStatus.PARTIAL_FAILURE);
        assertFalse(CastorContingencyScenarios.hasFailedInitialSensitivity(initialSensitivityOutput, Set.of(state1, state2)));

        // a diverged state in the initial sensitivity computation makes the scenario failed
        when(initialSensitivityOutput.getSensitivityStatus(state2)).thenReturn(ComputationStatus.FAILURE);
        assertTrue(CastorContingencyScenarios.hasFailedInitialSensitivity(initialSensitivityOutput, Set.of(state1, state2)));
    }

    /**
     * If a contingency diverged during the initial sensitivity computation, no remedial action is simulated
     * nor applied on its case: all its states are reported as skipped with the sensitivity failure overcost (issue #1893).
     */
    @Test
    void testScenarioSkippedWhenInitialSensitivityFailed() throws IOException {
        Network network = Network.read("12Nodes_2_pst.uct", getClass().getResourceAsStream("/network/12Nodes_2_pst.uct"));
        Crac crac = Crac.read("small-crac-2pst-1600.json", getClass().getResourceAsStream("/crac/small-crac-2pst-1600.json"), network);
        RaoParameters raoParameters = new RaoParameters(ReportNode.NO_OP);
        StateTree stateTree = new StateTree(crac, ReportNode.NO_OP);
        assertFalse(stateTree.getContingencyScenarios().isEmpty());

        // the contingency diverged during the initial sensitivity computation, on all its states
        PrePerimeterResult initialSensitivityOutput = Mockito.mock(PrePerimeterResult.class);
        when(initialSensitivityOutput.getSensitivityStatus(Mockito.any(State.class))).thenReturn(ComputationStatus.FAILURE);

        CastorContingencyScenarios castorContingencyScenarios = new CastorContingencyScenarios(
            crac,
            raoParameters,
            Mockito.mock(ToolProvider.class),
            stateTree,
            TreeParameters.buildForCurativePerimeter(raoParameters, 0.),
            initialSensitivityOutput
        );

        Map<State, PostPerimeterResult> results = castorContingencyScenarios.optimizeContingencyScenarios(
            network,
            Mockito.mock(PrePerimeterResult.class),
            false,
            ReportNode.NO_OP
        );

        // all states of the scenario are skipped, with the sensitivity failure overcost, and no remedial action applied
        stateTree.getContingencyScenarios().forEach(scenario -> assertScenarioSkipped(results, scenario));

        // same when only the automaton states are re-simulated (second preventive): the diverged case is not simulated
        Map<State, PostPerimeterResult> secondRunResults = castorContingencyScenarios.optimizeContingencyScenarios(
            network,
            Mockito.mock(PrePerimeterResult.class),
            true,
            ReportNode.NO_OP
        );
        stateTree.getContingencyScenarios().forEach(scenario -> assertScenarioSkipped(secondRunResults, scenario));
    }

    private static void assertScenarioSkipped(Map<State, PostPerimeterResult> results, ContingencyScenario scenario) {
        Set<State> scenarioStates = new HashSet<>();
        scenario.getCurativePerimeters().forEach(perimeter -> scenarioStates.addAll(perimeter.getAllStates()));
        scenario.getAutomatonState().ifPresent(scenarioStates::add);
        scenarioStates.forEach(state -> {
            PostPerimeterResult result = results.get(state);
            assertNotNull(result);
            OptimizationResult optimizationResult = result.optimizationResult();
            assertTrue(optimizationResult instanceof SkippedOptimizationResultImpl);
            assertEquals(ComputationStatus.FAILURE, optimizationResult.getSensitivityStatus());
            assertEquals(10000., optimizationResult.getVirtualCost("sensitivity-failure-cost"), 1e-6);
            assertTrue(optimizationResult.getActivatedNetworkActions().isEmpty());
            assertTrue(optimizationResult.getActivatedRangeActions(state).isEmpty());
        });
    }
}
