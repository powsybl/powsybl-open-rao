/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.rao_api.RaoResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SearchTreeRaoProviderTest {

    private static final double DOUBLE_TOLERANCE = 1e-3;

    private Crac crac;
    private String initialVariantId;
    private String postOptimPrevVariantId;
    private String postOptimCurVariantId;

    @Before
    public void setUp() {
        crac = CommonCracCreation.createWithPreventivePstRange();

        State curativeState = crac.getState("Contingency FR1 FR3", Instant.CURATIVE);

        crac.newNetworkAction().withId("open BE2-FR3")
                .withName("open BE2-FR3")
                .withOperator("FR")
                .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("BBE2AA1  FFR3AA1  1").add()
                .newOnStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency("Contingency FR1 FR3").withInstant(Instant.CURATIVE).add()
                .add();

        ResultVariantManager resultVariantManager = new ResultVariantManager();
        crac.addExtension(ResultVariantManager.class, resultVariantManager);
        initialVariantId = resultVariantManager.createNewUniqueVariantId("initial");
        postOptimPrevVariantId = resultVariantManager.createNewUniqueVariantId("postOptim-prev");
        postOptimCurVariantId = resultVariantManager.createNewUniqueVariantId("postOptim-cur");
        resultVariantManager.setInitialVariantId(initialVariantId);

        crac.getFlowCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(initialVariantId).setFlowInMW(600);
        crac.getFlowCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(postOptimPrevVariantId).setFlowInMW(300);

        crac.getFlowCnec("cnec1stateCurativeContingency1").getExtension(CnecResultExtension.class)
                .getVariant(postOptimPrevVariantId).setFlowInMW(400);
        crac.getFlowCnec("cnec1stateCurativeContingency1").getExtension(CnecResultExtension.class)
                .getVariant(postOptimCurVariantId).setFlowInMW(200);

        ((PstRangeResult) crac.getRangeAction("pst").getExtension(RangeActionResultExtension.class)
                .getVariant(initialVariantId)).setTap(crac.getPreventiveState().getId(), 0);
        ((PstRangeResult) crac.getRangeAction("pst").getExtension(RangeActionResultExtension.class)
                .getVariant(postOptimPrevVariantId)).setTap(crac.getPreventiveState().getId(), 5);
        ((PstRangeResult) crac.getRangeAction("pst").getExtension(RangeActionResultExtension.class)
                .getVariant(postOptimCurVariantId)).setTap(curativeState.getId(), -10);

        crac.getNetworkAction("open BE2-FR3").getExtension(NetworkActionResultExtension.class)
                .getVariant(postOptimCurVariantId).activate(curativeState.getId());
    }

    /* Creates simple state tree with :
     *  - preventive perimeter being {preventive state (optimized), curative state after FR1-FR2co}
     *  - a curative perimeer being {curative state after FR1-FR3co (optimized)}
     */
    private StateTree mockedStateTree(Crac crac) {
        State curativeState = crac.getState("Contingency FR1 FR3", Instant.CURATIVE);
        StateTree stateTree = Mockito.mock(StateTree.class);
        Mockito.when(stateTree.getOptimizedState(crac.getPreventiveState())).thenReturn(crac.getPreventiveState());
        Mockito.when(stateTree.getOptimizedState(crac.getState("Contingency FR1 FR2", Instant.CURATIVE))).thenReturn(crac.getPreventiveState());
        Mockito.when(stateTree.getOptimizedState(curativeState)).thenReturn(curativeState);
        Mockito.when(stateTree.getOptimizedStates()).thenReturn(Set.of(crac.getPreventiveState(), curativeState));
        Mockito.when(stateTree.getPerimeter(crac.getPreventiveState())).thenReturn(Set.of(crac.getPreventiveState(), crac.getState("Contingency FR1 FR2", Instant.CURATIVE)));
        Mockito.when(stateTree.getPerimeter(curativeState)).thenReturn(Set.of(curativeState));
        return stateTree;
    }

    @Test
    public void mergeRaoResults() {
        RaoResult preventiveRaoResult = new RaoResult(RaoResult.Status.DEFAULT);
        preventiveRaoResult.setPreOptimVariantId(initialVariantId);
        preventiveRaoResult.setPostOptimVariantId(postOptimPrevVariantId);

        RaoResult curativeRaoResult = new RaoResult(RaoResult.Status.DEFAULT);
        curativeRaoResult.setPreOptimVariantId(postOptimPrevVariantId);
        curativeRaoResult.setPostOptimVariantId(postOptimCurVariantId);

        StateTree stateTree = mockedStateTree(crac);

        State curativeState = crac.getState("Contingency FR1 FR3", Instant.CURATIVE);
        RaoResult mergedRaoResult = new SearchTreeRaoProvider(stateTree).mergeRaoResults(crac, preventiveRaoResult,
                Map.of(curativeState, curativeRaoResult));

        assertEquals(RaoResult.Status.DEFAULT, mergedRaoResult.getStatus());
        assertEquals(postOptimPrevVariantId, mergedRaoResult.getPostOptimVariantId());
        assertEquals(300, crac.getFlowCnec("cnec1basecase").getExtension(CnecResultExtension.class)
                .getVariant(postOptimPrevVariantId).getFlowInMW(), 0.1);
        assertEquals(200, crac.getFlowCnec("cnec1stateCurativeContingency1").getExtension(CnecResultExtension.class)
                .getVariant(postOptimPrevVariantId).getFlowInMW(), 0.1);
        assertEquals(Integer.valueOf(5), ((PstRangeResult) crac.getRangeAction("pst").getExtension(RangeActionResultExtension.class)
                .getVariant(postOptimPrevVariantId)).getTap(crac.getPreventiveState().getId()));
        assertEquals(Integer.valueOf(-10), ((PstRangeResult) crac.getRangeAction("pst").getExtension(RangeActionResultExtension.class)
                .getVariant(postOptimPrevVariantId)).getTap(curativeState.getId()));
        assertTrue(crac.getNetworkAction("open BE2-FR3").getExtension(NetworkActionResultExtension.class)
                .getVariant(postOptimPrevVariantId).isActivated(curativeState.getId()));
        assertEquals(2, crac.getExtension(ResultVariantManager.class).getVariants().size());
    }

    @Test
    public void mergeRaoResultsWithFailure() {
        RaoResult preventiveRaoResult = new RaoResult(RaoResult.Status.DEFAULT);
        preventiveRaoResult.setPreOptimVariantId(initialVariantId);
        preventiveRaoResult.setPostOptimVariantId(postOptimPrevVariantId);

        RaoResult curativeRaoResult = new RaoResult(RaoResult.Status.FAILURE);
        curativeRaoResult.setPreOptimVariantId(postOptimPrevVariantId);
        curativeRaoResult.setPostOptimVariantId(postOptimCurVariantId);

        StateTree stateTree = mockedStateTree(crac);

        State curativeState = crac.getState("Contingency FR1 FR3", Instant.CURATIVE);
        RaoResult mergedRaoResult = new SearchTreeRaoProvider(stateTree).mergeRaoResults(crac, preventiveRaoResult,
                Map.of(curativeState, curativeRaoResult));

        assertEquals(RaoResult.Status.FAILURE, mergedRaoResult.getStatus());
    }

    @Test
    public void mergeRaoResultsWithNoOptimizationInCurative() {

        RaoResult preventiveRaoResult = new RaoResult(RaoResult.Status.DEFAULT);
        preventiveRaoResult.setPreOptimVariantId(initialVariantId);
        preventiveRaoResult.setPostOptimVariantId(postOptimPrevVariantId);

        RaoResult curativeRaoResult = new RaoResult(RaoResult.Status.DEFAULT);
        curativeRaoResult.setPreOptimVariantId(postOptimPrevVariantId);
        curativeRaoResult.setPostOptimVariantId(postOptimPrevVariantId);

        StateTree stateTree = mockedStateTree(crac);

        State curativeState = crac.getState("Contingency FR1 FR3", Instant.CURATIVE);
        new SearchTreeRaoProvider(stateTree).mergeRaoResults(crac, preventiveRaoResult,
                Map.of(curativeState, curativeRaoResult));

        assertNotNull(crac.getExtension(CracResultExtension.class).getVariant(postOptimPrevVariantId));
    }

    @Test
    public void testMergeObjectiveFunctionCostWorstIsCurative() {
        RaoResult preventiveRaoResult = new RaoResult(RaoResult.Status.DEFAULT);
        preventiveRaoResult.setPreOptimVariantId(initialVariantId);
        preventiveRaoResult.setPostOptimVariantId(postOptimPrevVariantId);

        RaoResult curativeRaoResult1 = new RaoResult(RaoResult.Status.DEFAULT);
        curativeRaoResult1.setPreOptimVariantId(postOptimPrevVariantId);
        curativeRaoResult1.setPostOptimVariantId(postOptimCurVariantId);

        RaoResult curativeRaoResult2 = new RaoResult(RaoResult.Status.DEFAULT);
        curativeRaoResult2.setPreOptimVariantId(postOptimPrevVariantId);
        String postOptimCur2VariantId = crac.getExtension(ResultVariantManager.class).createNewUniqueVariantId("postOptim-cur-2");
        curativeRaoResult2.setPostOptimVariantId(postOptimCur2VariantId);

        StateTree stateTree = mockedStateTree(crac);

        State curativeState1 = crac.getState("Contingency FR1 FR3", Instant.CURATIVE);
        State curativeState2 = crac.getState("Contingency FR1 FR2", Instant.CURATIVE);

        CracResultExtension resultExtension = crac.getExtension(CracResultExtension.class);

        resultExtension.getVariant(postOptimPrevVariantId).setFunctionalCost(-1000);
        resultExtension.getVariant(postOptimPrevVariantId).setVirtualCost(10);

        resultExtension.getVariant(postOptimCurVariantId).setFunctionalCost(50);
        resultExtension.getVariant(postOptimCurVariantId).setVirtualCost(0);

        resultExtension.getVariant(postOptimCur2VariantId).setFunctionalCost(0);
        resultExtension.getVariant(postOptimCur2VariantId).setVirtualCost(100);

        RaoResult mergedRaoResult = new SearchTreeRaoProvider(stateTree).mergeRaoResults(crac, preventiveRaoResult,
                Map.of(curativeState1, curativeRaoResult1, curativeState2, curativeRaoResult2));
        assertEquals(0, resultExtension.getVariant(mergedRaoResult.getPostOptimVariantId()).getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(100, resultExtension.getVariant(mergedRaoResult.getPostOptimVariantId()).getVirtualCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testMergeObjectiveFunctionCostWorstIsPreventive() {
        RaoResult preventiveRaoResult = new RaoResult(RaoResult.Status.DEFAULT);
        preventiveRaoResult.setPreOptimVariantId(initialVariantId);
        preventiveRaoResult.setPostOptimVariantId(postOptimPrevVariantId);

        RaoResult curativeRaoResult1 = new RaoResult(RaoResult.Status.DEFAULT);
        curativeRaoResult1.setPreOptimVariantId(postOptimPrevVariantId);
        curativeRaoResult1.setPostOptimVariantId(postOptimCurVariantId);

        RaoResult curativeRaoResult2 = new RaoResult(RaoResult.Status.DEFAULT);
        curativeRaoResult2.setPreOptimVariantId(postOptimPrevVariantId);
        String postOptimCur2VariantId = crac.getExtension(ResultVariantManager.class).createNewUniqueVariantId("postOptim-cur-2");
        curativeRaoResult2.setPostOptimVariantId(postOptimCur2VariantId);

        StateTree stateTree = mockedStateTree(crac);

        State curativeState1 = crac.getState("Contingency FR1 FR3", Instant.CURATIVE);
        State curativeState2 = crac.getState("Contingency FR1 FR2", Instant.CURATIVE);

        CracResultExtension resultExtension = crac.getExtension(CracResultExtension.class);

        resultExtension.getVariant(postOptimPrevVariantId).setFunctionalCost(90);
        resultExtension.getVariant(postOptimPrevVariantId).setVirtualCost(10);

        resultExtension.getVariant(postOptimCurVariantId).setFunctionalCost(50);
        resultExtension.getVariant(postOptimCurVariantId).setVirtualCost(0);

        resultExtension.getVariant(postOptimCur2VariantId).setFunctionalCost(0);
        resultExtension.getVariant(postOptimCur2VariantId).setVirtualCost(100);

        RaoResult mergedRaoResult = new SearchTreeRaoProvider(stateTree).mergeRaoResults(crac, preventiveRaoResult,
                Map.of(curativeState1, curativeRaoResult1, curativeState2, curativeRaoResult2));
        assertEquals(90, resultExtension.getVariant(mergedRaoResult.getPostOptimVariantId()).getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(10, resultExtension.getVariant(mergedRaoResult.getPostOptimVariantId()).getVirtualCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testMergeObjectiveFunctionCostIgnorePerimetersWithPureMnecs() {
        Contingency contingency = crac.newContingency()
                .withId("pure_mnecs_cont")
                .withNetworkElement("BBE2AA1  FFR3AA1  1")
                .add();
        FlowCnec mnec = crac.newFlowCnec()
                .withId("pure_mnec")
                .withContingency(contingency.getId())
                .withInstant(Instant.CURATIVE)
                .withNetworkElement("BBE2AA1  FFR3AA1  1")
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
                .withMonitored()
                .add();

        RaoResult preventiveRaoResult = new RaoResult(RaoResult.Status.DEFAULT);
        preventiveRaoResult.setPreOptimVariantId(initialVariantId);
        preventiveRaoResult.setPostOptimVariantId(postOptimPrevVariantId);

        RaoResult curativeRaoResult = new RaoResult(RaoResult.Status.DEFAULT);
        curativeRaoResult.setPreOptimVariantId(postOptimPrevVariantId);
        curativeRaoResult.setPostOptimVariantId(postOptimCurVariantId);

        State curativeState = crac.getState(contingency, Instant.CURATIVE);
        State curativeState2 = crac.getState("Contingency FR1 FR3", Instant.CURATIVE);

        StateTree stateTree = mockedStateTree(crac);
        Mockito.when(stateTree.getOptimizedState(curativeState)).thenReturn(curativeState);

        CracResultExtension resultExtension = crac.getExtension(CracResultExtension.class);

        resultExtension.getVariant(postOptimPrevVariantId).setFunctionalCost(-1000);
        resultExtension.getVariant(postOptimPrevVariantId).setVirtualCost(10);

        resultExtension.getVariant(postOptimCurVariantId).setFunctionalCost(0);
        resultExtension.getVariant(postOptimCurVariantId).setVirtualCost(0);

        CnecResultExtension mockCnecResultExtension = Mockito.mock(CnecResultExtension.class);
        CnecResult cnecResult = new CnecResult();
        Mockito.when(mockCnecResultExtension.getVariant(Mockito.anyString())).thenReturn(cnecResult);
        mnec.addExtension(CnecResultExtension.class, mockCnecResultExtension); // just to avoid null pointer

        RaoResult mergedRaoResult = new SearchTreeRaoProvider(stateTree).mergeRaoResults(crac, preventiveRaoResult,
                Map.of(curativeState, curativeRaoResult, curativeState2, preventiveRaoResult));
        assertEquals(-1000, resultExtension.getVariant(mergedRaoResult.getPostOptimVariantId()).getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(10, resultExtension.getVariant(mergedRaoResult.getPostOptimVariantId()).getVirtualCost(), DOUBLE_TOLERANCE);
    }
}
