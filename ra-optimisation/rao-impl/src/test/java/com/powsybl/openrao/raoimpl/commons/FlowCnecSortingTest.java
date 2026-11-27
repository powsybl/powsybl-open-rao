/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.commons;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.raoimpl.commons.marginevaluator.MarginEvaluator;
import com.powsybl.openrao.raoimpl.result.api.FlowResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.powsybl.openrao.commons.Unit.MEGAWATT;
import static org.ejml.UtilEjml.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class FlowCnecSortingTest {
    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private FlowCnec cnec3;
    private FlowCnec pureMnec;
    private FlowResult flowResult;
    private MarginEvaluator marginEvaluator;

    @BeforeEach
    public void setUp() {
        State state = Mockito.mock(State.class);
        when(state.getContingency()).thenReturn(Optional.empty());
        cnec1 = Mockito.mock(FlowCnec.class); // Only optimized
        when(cnec1.isMonitored()).thenReturn(false);
        when(cnec1.isOptimized()).thenReturn(true);
        when(cnec1.getState()).thenReturn(state);
        cnec2 = Mockito.mock(FlowCnec.class); // Only optimized
        when(cnec2.isMonitored()).thenReturn(false);
        when(cnec2.isOptimized()).thenReturn(true);
        when(cnec2.getState()).thenReturn(state);
        cnec3 = Mockito.mock(FlowCnec.class); // Optimized and monitored
        when(cnec3.isMonitored()).thenReturn(true);
        when(cnec3.isOptimized()).thenReturn(true);
        when(cnec3.getState()).thenReturn(state);
        pureMnec = Mockito.mock(FlowCnec.class); // Only monitored
        when(pureMnec.isMonitored()).thenReturn(true);
        when(pureMnec.isOptimized()).thenReturn(false);
        when(pureMnec.getState()).thenReturn(state);

        marginEvaluator = Mockito.mock(MarginEvaluator.class);
        flowResult = Mockito.mock(FlowResult.class);
        when(marginEvaluator.getMargin(flowResult, cnec1, MEGAWATT)).thenReturn(-150.);
        when(marginEvaluator.getMargin(flowResult, cnec2, MEGAWATT)).thenReturn(200.);
        when(marginEvaluator.getMargin(flowResult, cnec3, MEGAWATT)).thenReturn(-250.);
        when(marginEvaluator.getMargin(flowResult, pureMnec, MEGAWATT)).thenReturn(50.);
    }

    @Test
    void getMostLimitingElements() {
        List<FlowCnec> costlyElements = FlowCnecSorting.sortByMargin(Set.of(cnec1, cnec2, cnec3, pureMnec), MEGAWATT, marginEvaluator, flowResult);
        assertEquals(3, costlyElements.size());
        assertSame(cnec3, costlyElements.getFirst());
        assertSame(cnec1, costlyElements.get(1));
        assertSame(cnec2, costlyElements.get(2));
    }

    @Test
    void testWithPureMnecs() {
        State state = Mockito.mock(State.class);
        when(state.getContingency()).thenReturn(Optional.empty());
        FlowCnec mnec1 = Mockito.mock(FlowCnec.class);
        when(mnec1.isMonitored()).thenReturn(true);
        when(mnec1.isOptimized()).thenReturn(false);
        when(mnec1.getState()).thenReturn(state);
        FlowCnec mnec2 = Mockito.mock(FlowCnec.class);
        when(mnec2.isMonitored()).thenReturn(true);
        when(mnec2.isOptimized()).thenReturn(false);
        when(mnec2.getState()).thenReturn(state);
        mockCnecThresholds(mnec1, 1000);
        mockCnecThresholds(mnec2, 2000);

        marginEvaluator = Mockito.mock(MarginEvaluator.class);
        flowResult = Mockito.mock(FlowResult.class);
        when(marginEvaluator.getMargin(flowResult, mnec1, MEGAWATT)).thenReturn(-150.);
        when(marginEvaluator.getMargin(flowResult, mnec2, MEGAWATT)).thenReturn(200.);

        assertTrue(FlowCnecSorting.sortByMargin(Set.of(mnec1, mnec2), MEGAWATT, marginEvaluator, flowResult).isEmpty());
    }

    private void mockCnecThresholds(FlowCnec cnec, double threshold) {
        when(cnec.getRawUpperBound(any(), any())).thenReturn(Optional.of(threshold));
        when(cnec.getRawLowerBound(any(), any())).thenReturn(Optional.of(-threshold));
    }

    @Test
    void sortByNegativeMarginKeepsOnlyNegativeOptimizedCnecs() {
        List<FlowCnec> sorted = FlowCnecSorting.sortByNegativeMargin(new LinkedHashSet<>(List.of(cnec1, cnec2, cnec3, pureMnec)), MEGAWATT, marginEvaluator, flowResult);
        assertEquals(List.of(cnec3, cnec1), sorted);
    }

    @Test
    void tiedMarginsKeepTheInputOrder() {
        when(marginEvaluator.getMargin(flowResult, cnec1, MEGAWATT)).thenReturn(-100.);
        when(marginEvaluator.getMargin(flowResult, cnec2, MEGAWATT)).thenReturn(-100.);
        when(marginEvaluator.getMargin(flowResult, cnec3, MEGAWATT)).thenReturn(-100.);
        Set<FlowCnec> inputOrder = new LinkedHashSet<>(List.of(cnec2, cnec3, cnec1));
        assertEquals(List.of(cnec2, cnec3, cnec1), FlowCnecSorting.sortByMargin(inputOrder, MEGAWATT, marginEvaluator, flowResult));
        assertEquals(List.of(cnec2, cnec3, cnec1), FlowCnecSorting.sortByNegativeMargin(inputOrder, MEGAWATT, marginEvaluator, flowResult));
    }

    @Test
    void eachMarginIsComputedOnce() {
        FlowCnecSorting.sortByMargin(new LinkedHashSet<>(List.of(cnec1, cnec2, cnec3)), MEGAWATT, marginEvaluator, flowResult);
        verify(marginEvaluator, times(1)).getMargin(flowResult, cnec1, MEGAWATT);
        verify(marginEvaluator, times(1)).getMargin(flowResult, cnec2, MEGAWATT);
        verify(marginEvaluator, times(1)).getMargin(flowResult, cnec3, MEGAWATT);
    }
}
