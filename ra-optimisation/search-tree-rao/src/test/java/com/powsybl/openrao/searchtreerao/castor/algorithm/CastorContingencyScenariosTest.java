/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.RaoBusinessWarns;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class CastorContingencyScenariosTest {

    @Test
    void testIsStopCriterionChecked() {
        TreeParameters treeParameters = mock(TreeParameters.class);
        ObjectiveFunctionResult objectiveFunctionResult = mock(ObjectiveFunctionResult.class);

        // if virtual cost positive return false
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(100.);
        assertFalse(CastorSearchTreeHelper.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if purely virtual with null virtual cost, return true
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-Double.MAX_VALUE);
        assertTrue(CastorSearchTreeHelper.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if not purely virtual and stop criterion is MIN_OBJECTIVE return false
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-10.);
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.MIN_OBJECTIVE);
        assertFalse(CastorSearchTreeHelper.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if not purely virtual and stop criterion is AT_TARGET_OBJECTIVE_VALUE and cost is higher than target return false
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-10.);
        when(objectiveFunctionResult.getCost()).thenReturn(-10.);
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(-20.);
        assertFalse(CastorSearchTreeHelper.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if not purely virtual and stop criterion is AT_TARGET_OBJECTIVE_VALUE and cost is lower than target return true
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-10.);
        when(objectiveFunctionResult.getCost()).thenReturn(-10.);
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(0.);
        assertTrue(CastorSearchTreeHelper.isStopCriterionChecked(objectiveFunctionResult, treeParameters));
    }

    /**
     * Holds what the runDcSearch tests below need: a CastorContingencyScenarios configured with a distinct working
     * (DC) ToolProvider, and a real-but-empty curative state/perimeter (no RAs, no CNECs) so the real
     * CurativeOptimizationPerimeter/ObjectiveFunction construction inside runDcSearch has nothing to evaluate and
     * needs no further mocking. Only CastorSearchTreeHelper (the DC-search/AC-recompute seam) is mocked.
     */
    private record DcSearchFixture(CastorContingencyScenarios castorContingencyScenarios,
                                   Perimeter curativePerimeter,
                                   Network network,
                                   PrePerimeterResult prePerimeterSensitivityOutput,
                                   State curativeState,
                                   ObjectiveFunctionResult noRaObjectiveFunctionResult,
                                   RangeActionActivationResult noRaRangeActionsResult) {
    }

    private DcSearchFixture buildDcSearchFixture() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CracFactory.findDefault().create("test-crac")
            .newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("curative", InstantKind.CURATIVE);
        crac.newContingency().withId("contingency1").withContingencyElement("contingency1-ne", ContingencyElementType.LINE).add();
        // a CNEC is needed to make the curative state actually exist on the CRAC (no RA is needed: runDcSearch's
        // search/recompute calls are mocked away, so the perimeter's remedial-action lists can stay empty)
        crac.newFlowCnec().withId("c1").withInstant("curative").withContingency("contingency1")
            .withNetworkElement("BBE1AA1  BBE2AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(2000.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();
        State curativeState = crac.getState("contingency1", crac.getInstant("curative"));

        RaoParameters raoParameters = new RaoParameters(ReportNode.NO_OP);
        ToolProvider toolProvider = mock(ToolProvider.class);
        when(toolProvider.getRaoParameters()).thenReturn(raoParameters);
        ToolProvider workingToolProvider = mock(ToolProvider.class);
        when(workingToolProvider.getRaoParameters()).thenReturn(new RaoParameters(ReportNode.NO_OP));
        StateTree stateTree = mock(StateTree.class);
        when(stateTree.getOperatorsNotSharingCras()).thenReturn(Set.of());
        TreeParameters curativeTreeParameters = new TreeParameters(TreeParameters.StopCriterion.MIN_OBJECTIVE, 0.0, 2, 1, false);
        PrePerimeterResult initialSensitivityOutput = mock(PrePerimeterResult.class);
        when(initialSensitivityOutput.getSensitivityStatus(any())).thenReturn(ComputationStatus.DEFAULT);

        CastorContingencyScenarios castorContingencyScenarios = new CastorContingencyScenarios(
            crac, raoParameters, toolProvider, stateTree, curativeTreeParameters, initialSensitivityOutput);
        castorContingencyScenarios.setWorkingToolProvider(workingToolProvider);

        PrePerimeterResult prePerimeterSensitivityOutput = mock(PrePerimeterResult.class);
        when(prePerimeterSensitivityOutput.getSensitivityStatus(any())).thenReturn(ComputationStatus.DEFAULT);
        ObjectiveFunctionResult noRaObjectiveFunctionResult = mock(ObjectiveFunctionResult.class);
        when(noRaObjectiveFunctionResult.getCost()).thenReturn(0.);
        RangeActionActivationResult noRaRangeActionsResult = mock(RangeActionActivationResult.class);

        return new DcSearchFixture(castorContingencyScenarios, new Perimeter(curativeState, Set.of()), network,
            prePerimeterSensitivityOutput, curativeState, noRaObjectiveFunctionResult, noRaRangeActionsResult);
    }

    private OptimizationResult callRunDcSearch(DcSearchFixture fixture) {
        return fixture.castorContingencyScenarios().runDcSearch(
            fixture.curativePerimeter(), fixture.network(), fixture.prePerimeterSensitivityOutput(),
            Map.of(), Map.of(), ReportNode.NO_OP, mock(ObjectiveFunction.class), fixture.curativeState(),
            Set.of(), Set.of(), null, null, fixture.noRaObjectiveFunctionResult(), fixture.noRaRangeActionsResult());
    }

    @Test
    void runDcSearchReturnsAcRecomputedResultWhenSecureOnFirstTry() {
        DcSearchFixture fixture = buildDcSearchFixture();
        OptimizationResult decidedResult = mock(OptimizationResult.class);
        when(decidedResult.getActivatedNetworkActions()).thenReturn(Set.of(mock(NetworkAction.class)));
        OptimizationResult acRecomputedResult = mock(OptimizationResult.class);

        Logger logger = (Logger) LoggerFactory.getLogger(RaoBusinessWarns.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        OptimizationResult result;
        try (MockedStatic<CastorSearchTreeHelper> castorSearchTreeHelper = mockStatic(CastorSearchTreeHelper.class)) {
            castorSearchTreeHelper.when(() -> CastorSearchTreeHelper.searchNetworkActionCombination(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(decidedResult);
            castorSearchTreeHelper.when(() -> CastorSearchTreeHelper.reoptimizeNetworkActionCombination(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(acRecomputedResult);
            castorSearchTreeHelper.when(() -> CastorSearchTreeHelper.isSecureAndBetterThan(any(), anyDouble()))
                .thenReturn(true);

            result = callRunDcSearch(fixture);
        } finally {
            listAppender.stop();
        }

        assertSame(acRecomputedResult, result);
        List<String> warnLogs = listAppender.list.stream().map(Object::toString).toList();
        assertFalse(warnLogs.stream().anyMatch(l -> l.contains("but the AC recompute shows a margin violation; falling back to a full AC search")));
        assertFalse(warnLogs.stream().anyMatch(l -> l.contains("could not find any network action combination that is secure in AC")));
    }

    @Test
    void runDcSearchFallsBackToAcSearchWhenAcVerificationFails() {
        DcSearchFixture fixture = buildDcSearchFixture();
        OptimizationResult dcAndAcResult = mock(OptimizationResult.class);
        when(dcAndAcResult.getActivatedNetworkActions()).thenReturn(Set.of(mock(NetworkAction.class)));
        when(dcAndAcResult.getCost()).thenReturn(10.);
        SearchTreeParameters searchTreeParameters = mock(SearchTreeParameters.class);

        Logger logger = (Logger) LoggerFactory.getLogger(RaoBusinessWarns.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        OptimizationResult result;
        try (MockedStatic<CastorSearchTreeHelper> castorSearchTreeHelper = mockStatic(CastorSearchTreeHelper.class)) {
            castorSearchTreeHelper.when(() -> CastorSearchTreeHelper.buildSearchTreeParameters(any(), any(), any(), any(), any(), any()))
                .thenReturn(searchTreeParameters);
            castorSearchTreeHelper.when(() -> CastorSearchTreeHelper.searchNetworkActionCombination(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(dcAndAcResult);
            castorSearchTreeHelper.when(() -> CastorSearchTreeHelper.reoptimizeNetworkActionCombination(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(dcAndAcResult);
            castorSearchTreeHelper.when(() -> CastorSearchTreeHelper.isSecureAndBetterThan(any(), anyDouble()))
                .thenReturn(false);

            result = callRunDcSearch(fixture);
        } finally {
            listAppender.stop();
        }

        assertSame(dcAndAcResult, result);
        List<String> warnLogs = listAppender.list.stream().map(Object::toString).toList();
        assertTrue(warnLogs.stream().anyMatch(l -> l.contains("but the AC recompute shows a margin violation; falling back to a full AC search")));
        assertTrue(warnLogs.stream().anyMatch(l -> l.contains("could not find any network action combination that is secure in AC")));
    }
}
