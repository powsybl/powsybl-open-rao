/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.SearchTree;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CastorSearchTreeHelperTest {

    @Test
    void buildSearchTreeParametersSkipsLoadFlowAndSensitivityParametersWithoutHvdcInAcEmulation() {
        RaoParameters raoParameters = new RaoParameters(ReportNode.NO_OP);
        TreeParameters treeParameters = Mockito.mock(TreeParameters.class);
        Crac crac = Mockito.mock(Crac.class);
        Mockito.when(crac.getHvdcRangeActions()).thenReturn(Set.of());
        Network network = Mockito.mock(Network.class);
        Set<String> operatorsNotSharingCras = Set.of("operator1");

        SearchTreeParameters searchTreeParameters = CastorSearchTreeHelper.buildSearchTreeParameters(raoParameters, treeParameters, crac, network,
            operatorsNotSharingCras, ReportNode.NO_OP);

        assertSame(treeParameters, searchTreeParameters.getTreeParameters());
        assertTrue(searchTreeParameters.getLoadFlowAndSensitivityParameters().isEmpty());
    }

    @Test
    void buildSearchTreeParametersUsesExtensionLoadFlowAndSensitivityParametersWithHvdcInAcEmulation() {
        RaoParameters raoParameters = new RaoParameters(ReportNode.NO_OP);
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters(ReportNode.NO_OP));
        LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters();
        TreeParameters treeParameters = Mockito.mock(TreeParameters.class);
        Network network = Mockito.mock(Network.class);
        HvdcRangeAction hvdcRangeActionInAcEmulation = Mockito.mock(HvdcRangeAction.class);
        Mockito.when(hvdcRangeActionInAcEmulation.isAngleDroopActivePowerControlEnabled(network)).thenReturn(true);
        Crac crac = Mockito.mock(Crac.class);
        Mockito.when(crac.getHvdcRangeActions()).thenReturn(Set.of(hvdcRangeActionInAcEmulation));
        Set<String> operatorsNotSharingCras = Set.of("operator1");

        SearchTreeParameters searchTreeParameters = CastorSearchTreeHelper.buildSearchTreeParameters(raoParameters, treeParameters, crac, network,
            operatorsNotSharingCras, ReportNode.NO_OP);

        assertSame(loadFlowAndSensitivityParameters, searchTreeParameters.getLoadFlowAndSensitivityParameters().orElseThrow());
    }

    @Test
    void searchNetworkActionCombinationBuildsSearchTreeInputAndRunsASearchTree() {
        Network network = Mockito.mock(Network.class);
        OptimizationPerimeter optPerimeter = Mockito.mock(OptimizationPerimeter.class);
        ObjectiveFunction objectiveFunction = Mockito.mock(ObjectiveFunction.class);
        PrePerimeterResult initialResult = Mockito.mock(PrePerimeterResult.class);
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        AppliedRemedialActions appliedRemedialActions = Mockito.mock(AppliedRemedialActions.class);
        ToolProvider toolProvider = Mockito.mock(ToolProvider.class);
        Instant outageInstant = Mockito.mock(Instant.class);
        SearchTreeParameters searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        ReportNode reportNode = ReportNode.NO_OP;
        OptimizationResult decidedResult = Mockito.mock(OptimizationResult.class);

        try (MockedConstruction<SearchTree> mockedSearchTree = Mockito.mockConstruction(SearchTree.class,
            (mock, context) -> {
                SearchTreeInput searchTreeInput = (SearchTreeInput) context.arguments().get(0);
                assertSame(network, searchTreeInput.getNetwork());
                assertSame(optPerimeter, searchTreeInput.getOptimizationPerimeter());
                assertSame(objectiveFunction, searchTreeInput.getObjectiveFunction());
                assertSame(initialResult, searchTreeInput.getInitialFlowResult());
                assertSame(prePerimeterResult, searchTreeInput.getPrePerimeterResult());
                assertSame(appliedRemedialActions, searchTreeInput.getPreOptimizationAppliedRemedialActions());
                assertSame(toolProvider, searchTreeInput.getToolProvider());
                assertSame(outageInstant, searchTreeInput.getOutageInstant());
                assertSame(searchTreeParameters, context.arguments().get(1));
                assertEquals(true, context.arguments().get(2));
                assertSame(reportNode, context.arguments().get(3));
                Mockito.when(mock.run()).thenReturn(CompletableFuture.completedFuture(decidedResult));
            })) {

            OptimizationResult result = CastorSearchTreeHelper.searchNetworkActionCombination(network, optPerimeter, objectiveFunction, initialResult, prePerimeterResult,
                appliedRemedialActions, toolProvider, outageInstant, searchTreeParameters, true, reportNode);

            assertEquals(1, mockedSearchTree.constructed().size());
            assertSame(decidedResult, result);
        }
    }

    @Test
    void reoptimizeNetworkActionCombinationBuildsSearchTreeInputAndDelegatesToOptimizeDecidedNetworkActionCombination() {
        Network network = Mockito.mock(Network.class);
        OptimizationPerimeter acOptPerimeter = Mockito.mock(OptimizationPerimeter.class);
        ObjectiveFunction acObjectiveFunction = Mockito.mock(ObjectiveFunction.class);
        PrePerimeterResult acInitialResult = Mockito.mock(PrePerimeterResult.class);
        PrePerimeterResult acPrePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        AppliedRemedialActions appliedRemedialActions = Mockito.mock(AppliedRemedialActions.class);
        ToolProvider acToolProvider = Mockito.mock(ToolProvider.class);
        Instant outageInstant = Mockito.mock(Instant.class);
        SearchTreeParameters searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        OptimizationResult decidedResult = Mockito.mock(OptimizationResult.class);
        ReportNode reportNode = ReportNode.NO_OP;
        OptimizationResult acResult = Mockito.mock(OptimizationResult.class);

        try (MockedConstruction<SearchTree> mockedSearchTree = Mockito.mockConstruction(SearchTree.class,
            (mock, context) -> {
                SearchTreeInput acSearchTreeInput = (SearchTreeInput) context.arguments().get(0);
                // the recompute must run on the AC-flavored perimeter/objective function/results given by the
                // caller, not on whichever (possibly DC-built) ones were used to decide the combination
                assertSame(network, acSearchTreeInput.getNetwork());
                assertSame(acOptPerimeter, acSearchTreeInput.getOptimizationPerimeter());
                assertSame(acObjectiveFunction, acSearchTreeInput.getObjectiveFunction());
                assertSame(acInitialResult, acSearchTreeInput.getInitialFlowResult());
                assertSame(acPrePerimeterResult, acSearchTreeInput.getPrePerimeterResult());
                assertSame(appliedRemedialActions, acSearchTreeInput.getPreOptimizationAppliedRemedialActions());
                assertSame(acToolProvider, acSearchTreeInput.getToolProvider());
                assertSame(outageInstant, acSearchTreeInput.getOutageInstant());
                assertSame(searchTreeParameters, context.arguments().get(1));
                assertEquals(false, context.arguments().get(2));
                assertSame(reportNode, context.arguments().get(3));
                Mockito.when(mock.optimizeDecidedNetworkActionCombination(decidedResult, reportNode)).thenReturn(acResult);
            })) {

            OptimizationResult result = CastorSearchTreeHelper.reoptimizeNetworkActionCombination(network, acOptPerimeter, acObjectiveFunction, acInitialResult, acPrePerimeterResult,
                appliedRemedialActions, acToolProvider, outageInstant, searchTreeParameters, decidedResult, false, reportNode);

            assertEquals(1, mockedSearchTree.constructed().size());
            assertSame(acResult, result);
        }
    }
}
