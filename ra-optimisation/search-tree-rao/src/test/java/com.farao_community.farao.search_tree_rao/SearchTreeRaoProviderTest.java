/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.ActionType;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.Topology;
import com.farao_community.farao.data.crac_impl.usage_rule.OnState;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.rao_api.RaoResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SearchTreeRaoProviderTest {

    private SimpleCrac crac;
    private String initialVariantId;
    private String postOptimPrevVariantId;
    private String postOptimCurVariantId;

    @Before
    public void setUp() {
        crac = CommonCracCreation.createWithPstRange();

        State curativeState = crac.getState("Contingency FR1 FR3", "curative");

        crac.addNetworkAction(new Topology("open BE2-FR3", "open BE2-FR3", "FR",
            List.of(new OnState(UsageMethod.AVAILABLE, curativeState)),
            crac.addNetworkElement("BBE2AA1  FFR3AA1  1"), ActionType.OPEN));

        ResultVariantManager resultVariantManager = new ResultVariantManager();
        crac.addExtension(ResultVariantManager.class, resultVariantManager);
        initialVariantId = resultVariantManager.createNewUniqueVariantId("initial");
        postOptimPrevVariantId = resultVariantManager.createNewUniqueVariantId("postOptim-prev");
        postOptimCurVariantId = resultVariantManager.createNewUniqueVariantId("postOptim-cur");
        resultVariantManager.setInitialVariantId(initialVariantId);

        crac.getCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(initialVariantId).setFlowInMW(600);
        crac.getCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(postOptimPrevVariantId).setFlowInMW(300);

        crac.getCnec("cnec1stateCurativeContingency1").getExtension(CnecResultExtension.class)
            .getVariant(postOptimPrevVariantId).setFlowInMW(400);
        crac.getCnec("cnec1stateCurativeContingency1").getExtension(CnecResultExtension.class)
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
        State curativeState = crac.getState("Contingency FR1 FR3", "curative");
        StateTree stateTree = Mockito.mock(StateTree.class);
        Mockito.when(stateTree.getOptimizedState(crac.getPreventiveState())).thenReturn(crac.getPreventiveState());
        Mockito.when(stateTree.getOptimizedState(crac.getState("Contingency FR1 FR2", "curative"))).thenReturn(crac.getPreventiveState());
        Mockito.when(stateTree.getOptimizedState(curativeState)).thenReturn(curativeState);
        Mockito.when(stateTree.getOptimizedStates()).thenReturn(Set.of(crac.getPreventiveState(), curativeState));
        Mockito.when(stateTree.getPerimeter(crac.getPreventiveState())).thenReturn(Set.of(crac.getPreventiveState(), crac.getState("Contingency FR1 FR2", "curative")));
        Mockito.when(stateTree.getPerimeter(curativeState)).thenReturn(Set.of(curativeState));
        return stateTree;
    }

    @Test
    public void mergeRaoResults() {

        RaoResult preventiveRaoResult = new RaoResult(RaoResult.Status.SUCCESS);
        preventiveRaoResult.setPreOptimVariantId(initialVariantId);
        preventiveRaoResult.setPostOptimVariantId(postOptimPrevVariantId);

        RaoResult curativeRaoResult = new RaoResult(RaoResult.Status.SUCCESS);
        curativeRaoResult.setPreOptimVariantId(postOptimPrevVariantId);
        curativeRaoResult.setPostOptimVariantId(postOptimCurVariantId);

        StateTree stateTree = mockedStateTree(crac);

        State curativeState = crac.getState("Contingency FR1 FR3", "curative");
        RaoResult mergedRaoResult = new SearchTreeRaoProvider(stateTree).mergeRaoResults(crac, preventiveRaoResult,
            Map.of(curativeState, curativeRaoResult));

        assertEquals(RaoResult.Status.SUCCESS, mergedRaoResult.getStatus());
        assertEquals(postOptimPrevVariantId, mergedRaoResult.getPostOptimVariantId());
        assertEquals(300, crac.getCnec("cnec1basecase").getExtension(CnecResultExtension.class)
            .getVariant(postOptimPrevVariantId).getFlowInMW(), 0.1);
        assertEquals(200, crac.getCnec("cnec1stateCurativeContingency1").getExtension(CnecResultExtension.class)
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

        RaoResult preventiveRaoResult = new RaoResult(RaoResult.Status.SUCCESS);
        preventiveRaoResult.setPreOptimVariantId(initialVariantId);
        preventiveRaoResult.setPostOptimVariantId(postOptimPrevVariantId);

        RaoResult curativeRaoResult = new RaoResult(RaoResult.Status.FAILURE);
        curativeRaoResult.setPreOptimVariantId(postOptimPrevVariantId);
        curativeRaoResult.setPostOptimVariantId(postOptimCurVariantId);

        StateTree stateTree = mockedStateTree(crac);

        State curativeState = crac.getState("Contingency FR1 FR3", "curative");
        RaoResult mergedRaoResult = new SearchTreeRaoProvider(stateTree).mergeRaoResults(crac, preventiveRaoResult,
            Map.of(curativeState, curativeRaoResult));

        assertEquals(RaoResult.Status.FAILURE, mergedRaoResult.getStatus());
    }
}