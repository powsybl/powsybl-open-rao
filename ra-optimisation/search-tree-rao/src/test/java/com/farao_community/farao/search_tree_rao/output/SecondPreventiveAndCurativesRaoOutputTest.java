/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.output;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.results.OptimizationResult;
import com.farao_community.farao.rao_api.results.PerimeterResult;
import com.farao_community.farao.rao_api.results.PrePerimeterResult;
import org.junit.Before;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SecondPreventiveAndCurativesRaoOutputTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    State optimizedState;
    PrePerimeterResult initialResult;
    PerimeterResult post1PResult; // post 1st preventive result
    PerimeterResult post2PResult; // post 2nd preventive result
    PrePerimeterResult preCurativeResult;
    Map<State, OptimizationResult> postCurativeResults;
    PstRangeAction pstRangeAction = mock(PstRangeAction.class);
    RangeAction rangeAction;
    NetworkAction networkAction;
    FlowCnec cnec1;
    FlowCnec cnec2;
    State cnec1state;
    State cnec2state;
    State state1;
    State state2;

    @Before
    public void setUp() {
        optimizedState = mock(State.class);

        initialResult = mock(PrePerimeterResult.class);
        post1PResult = mock(PerimeterResult.class);
        post2PResult = mock(PerimeterResult.class);
        preCurativeResult = mock(PrePerimeterResult.class);

        OptimizationResult optimResult1 = mock(OptimizationResult.class);
        OptimizationResult optimResult2 = mock(OptimizationResult.class);
        postCurativeResults = Map.of(state1, optimResult1, state2, optimResult2);

        pstRangeAction = mock(PstRangeAction.class);
        rangeAction = mock(RangeAction.class);
        networkAction = mock(NetworkAction.class);

        cnec1 = mock(FlowCnec.class);
        cnec2 = mock(FlowCnec.class);
        cnec1state = mock(State.class);
        cnec2state = mock(State.class);
        when(cnec1.getState()).thenReturn(cnec1state);
        when(cnec2.getState()).thenReturn(cnec2state);

        when(initialResult.getFunctionalCost()).thenReturn(1000.);
        when(initialResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1));
        when(initialResult.getVirtualCost()).thenReturn(100.);
        when(initialResult.getVirtualCost("mnec")).thenReturn(20.);
        when(initialResult.getVirtualCost("lf")).thenReturn(80.);
        when(initialResult.getVirtualCostNames()).thenReturn(Set.of("mnec", "lf"));
        when(initialResult.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(cnec2));
        when(initialResult.getCostlyElements(eq("lf"), anyInt())).thenReturn(List.of(cnec1));
        when(initialResult.getOptimizedSetPoint(pstRangeAction)).thenReturn(6.7);
        when(initialResult.getOptimizedSetPoint(rangeAction)).thenReturn(5.6);
        when(initialResult.getOptimizedTap(pstRangeAction)).thenReturn(1);
        when(initialResult.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(initialResult.getOptimizedTaps()).thenReturn(Map.of(pstRangeAction, 1));
        when(initialResult.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 6.7, rangeAction, 5.6));
        when(initialResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(-1000.);
        when(initialResult.getMargin(cnec1, Unit.AMPERE)).thenReturn(-500.);
        when(initialResult.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(-2000.);
        when(initialResult.getRelativeMargin(cnec1, Unit.AMPERE)).thenReturn(-1000.);
        when(initialResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(-500.);
        when(initialResult.getMargin(cnec2, Unit.AMPERE)).thenReturn(-250.);
        when(initialResult.getRelativeMargin(cnec2, Unit.MEGAWATT)).thenReturn(-1500.);
        when(initialResult.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(-750.);

        when(post1PResult.getFunctionalCost()).thenReturn(-1000.);
        when(post1PResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        when(post1PResult.getVirtualCost()).thenReturn(-100.);
        when(post1PResult.getVirtualCost("mnec")).thenReturn(-20.);
        when(post1PResult.getVirtualCost("lf")).thenReturn(-80.);
        when(post1PResult.getVirtualCostNames()).thenReturn(Set.of("mnec", "lf"));
        when(post1PResult.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(cnec1));
        when(post1PResult.getCostlyElements(eq("lf"), anyInt())).thenReturn(List.of(cnec2));
        when(post1PResult.isActivated(networkAction)).thenReturn(true);
        when(post1PResult.getActivatedNetworkActions()).thenReturn(Set.of(networkAction));
        when(post1PResult.getOptimizedSetPoint(pstRangeAction)).thenReturn(8.9);
        when(post1PResult.getOptimizedSetPoint(rangeAction)).thenReturn(5.6);
        when(post1PResult.getOptimizedTap(pstRangeAction)).thenReturn(2);
        when(post1PResult.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(post1PResult.getOptimizedTaps()).thenReturn(Map.of(pstRangeAction, 2));
        when(post1PResult.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 8.9, rangeAction, 5.6));
        when(post1PResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(1000.);
        when(post1PResult.getMargin(cnec2, Unit.AMPERE)).thenReturn(500.);
        when(post1PResult.getRelativeMargin(cnec2, Unit.MEGAWATT)).thenReturn(2000.);
        when(post1PResult.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(1000.);
        when(post1PResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(500.);
        when(post1PResult.getMargin(cnec1, Unit.AMPERE)).thenReturn(250.);
        when(post1PResult.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(1500.);
        when(post1PResult.getRelativeMargin(cnec1, Unit.AMPERE)).thenReturn(750.);

        when(post2PResult.getFunctionalCost()).thenReturn(-1000.);
        when(post2PResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        when(post2PResult.getVirtualCost()).thenReturn(-100.);
        when(post2PResult.getVirtualCost("mnec")).thenReturn(-20.);
        when(post2PResult.getVirtualCost("lf")).thenReturn(-80.);
        when(post2PResult.getVirtualCostNames()).thenReturn(Set.of("mnec", "lf"));
        when(post2PResult.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(cnec1));
        when(post2PResult.getCostlyElements(eq("lf"), anyInt())).thenReturn(List.of(cnec2));
        when(post2PResult.isActivated(networkAction)).thenReturn(true);
        when(post2PResult.getActivatedNetworkActions()).thenReturn(Set.of(networkAction));
        when(post2PResult.getOptimizedSetPoint(pstRangeAction)).thenReturn(8.9);
        when(post2PResult.getOptimizedSetPoint(rangeAction)).thenReturn(5.6);
        when(post2PResult.getOptimizedTap(pstRangeAction)).thenReturn(2);
        when(post2PResult.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(post2PResult.getOptimizedTaps()).thenReturn(Map.of(pstRangeAction, 2));
        when(post2PResult.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 8.9, rangeAction, 5.6));
        when(post2PResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(1000.);
        when(post2PResult.getMargin(cnec2, Unit.AMPERE)).thenReturn(500.);
        when(post2PResult.getRelativeMargin(cnec2, Unit.MEGAWATT)).thenReturn(2000.);
        when(post2PResult.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(1000.);
        when(post2PResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(500.);
        when(post2PResult.getMargin(cnec1, Unit.AMPERE)).thenReturn(250.);
        when(post2PResult.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(1500.);
        when(post2PResult.getRelativeMargin(cnec1, Unit.AMPERE)).thenReturn(750.);

        when(optimResult1.getFunctionalCost()).thenReturn(-1000.);
        when(optimResult1.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        when(optimResult1.getVirtualCost()).thenReturn(-100.);
        when(optimResult1.getVirtualCost("mnec")).thenReturn(-20.);
        when(optimResult1.getVirtualCost("lf")).thenReturn(-80.);
        when(optimResult1.getVirtualCostNames()).thenReturn(Set.of("mnec", "lf"));
        when(optimResult1.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(cnec1));
        when(optimResult1.getCostlyElements(eq("lf"), anyInt())).thenReturn(List.of(cnec2));
        when(optimResult1.isActivated(networkAction)).thenReturn(true);
        when(optimResult1.getActivatedNetworkActions()).thenReturn(Set.of(networkAction));
        when(optimResult1.getOptimizedSetPoint(pstRangeAction)).thenReturn(8.9);
        when(optimResult1.getOptimizedSetPoint(rangeAction)).thenReturn(5.6);
        when(optimResult1.getOptimizedTap(pstRangeAction)).thenReturn(2);
        when(optimResult1.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(optimResult1.getOptimizedTaps()).thenReturn(Map.of(pstRangeAction, 2));
        when(optimResult1.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 8.9, rangeAction, 5.6));
        when(optimResult1.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(1000.);
        when(optimResult1.getMargin(cnec2, Unit.AMPERE)).thenReturn(500.);
        when(optimResult1.getRelativeMargin(cnec2, Unit.MEGAWATT)).thenReturn(2000.);
        when(optimResult1.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(1000.);
        when(optimResult1.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(500.);
        when(optimResult1.getMargin(cnec1, Unit.AMPERE)).thenReturn(250.);
        when(optimResult1.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(1500.);
        when(optimResult1.getRelativeMargin(cnec1, Unit.AMPERE)).thenReturn(750.);

        when(optimResult2.getFunctionalCost()).thenReturn(-1000.);
        when(optimResult2.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2));
        when(optimResult2.getVirtualCost()).thenReturn(-100.);
        when(optimResult2.getVirtualCost("mnec")).thenReturn(-20.);
        when(optimResult2.getVirtualCost("lf")).thenReturn(-80.);
        when(optimResult2.getVirtualCostNames()).thenReturn(Set.of("mnec", "lf"));
        when(optimResult2.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(cnec1));
        when(optimResult2.getCostlyElements(eq("lf"), anyInt())).thenReturn(List.of(cnec2));
        when(optimResult2.isActivated(networkAction)).thenReturn(true);
        when(optimResult2.getActivatedNetworkActions()).thenReturn(Set.of(networkAction));
        when(optimResult2.getOptimizedSetPoint(pstRangeAction)).thenReturn(8.9);
        when(optimResult2.getOptimizedSetPoint(rangeAction)).thenReturn(5.6);
        when(optimResult2.getOptimizedTap(pstRangeAction)).thenReturn(2);
        when(optimResult2.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(optimResult2.getOptimizedTaps()).thenReturn(Map.of(pstRangeAction, 2));
        when(optimResult2.getOptimizedSetPoints()).thenReturn(Map.of(pstRangeAction, 8.9, rangeAction, 5.6));
        when(optimResult2.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(1000.);
        when(optimResult2.getMargin(cnec2, Unit.AMPERE)).thenReturn(500.);
        when(optimResult2.getRelativeMargin(cnec2, Unit.MEGAWATT)).thenReturn(2000.);
        when(optimResult2.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(1000.);
        when(optimResult2.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(500.);
        when(optimResult2.getMargin(cnec1, Unit.AMPERE)).thenReturn(250.);
        when(optimResult2.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(1500.);
        when(optimResult2.getRelativeMargin(cnec1, Unit.AMPERE)).thenReturn(750.);


    }
}
