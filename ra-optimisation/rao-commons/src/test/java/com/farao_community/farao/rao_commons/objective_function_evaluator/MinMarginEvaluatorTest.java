/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.SensitivityStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MinMarginEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private BranchCnec cnec1;
    private BranchCnec cnec2;
    private BranchCnec cnec3;
    private BranchResult branchResult;
    private MinMarginEvaluator minMarginEvaluator;

    @Before
    public void setUp() {
        cnec1 = Mockito.mock(BranchCnec.class); // Only optimized
        when(cnec1.isMonitored()).thenReturn(false);
        when(cnec1.isOptimized()).thenReturn(true);
        cnec2 = Mockito.mock(BranchCnec.class); // Only optimized
        when(cnec2.isMonitored()).thenReturn(false);
        when(cnec2.isOptimized()).thenReturn(true);
        cnec3 = Mockito.mock(BranchCnec.class); // Optimized and monitored
        when(cnec3.isMonitored()).thenReturn(true);
        when(cnec3.isOptimized()).thenReturn(true);
        BranchCnec pureMnec = Mockito.mock(BranchCnec.class); // Only monitored
        when(pureMnec.isMonitored()).thenReturn(true);
        when(pureMnec.isOptimized()).thenReturn(false);

        MarginEvaluator marginEvaluator = Mockito.mock(MarginEvaluator.class);
        branchResult = Mockito.mock(BranchResult.class);
        when(marginEvaluator.getMargin(branchResult, cnec1, MEGAWATT)).thenReturn(-150.);
        when(marginEvaluator.getMargin(branchResult, cnec2, MEGAWATT)).thenReturn(200.);
        when(marginEvaluator.getMargin(branchResult, cnec3, MEGAWATT)).thenReturn(-250.);
        when(marginEvaluator.getMargin(branchResult, pureMnec, MEGAWATT)).thenReturn(50.);

        minMarginEvaluator = new MinMarginEvaluator(Set.of(cnec1, cnec2, cnec3, pureMnec), MEGAWATT, marginEvaluator);
    }

    @Test
    public void getName() {
        assertEquals("min-margin-evaluator", minMarginEvaluator.getName());
    }

    @Test
    public void getUnit() {
        assertEquals(MEGAWATT, minMarginEvaluator.getUnit());
    }

    @Test
    public void getMostLimitingElements() {
        List<BranchCnec> costlyElements = minMarginEvaluator.getCostlyElements(branchResult, 5);
        assertEquals(3, costlyElements.size());
        assertSame(cnec3, costlyElements.get(0));
        assertSame(cnec1, costlyElements.get(1));
        assertSame(cnec2, costlyElements.get(2));
    }

    @Test
    public void getMostLimitingElementsWithLimitedElements() {
        List<BranchCnec> costlyElements = minMarginEvaluator.getCostlyElements(branchResult, 2);
        assertEquals(2, costlyElements.size());
        assertSame(cnec3, costlyElements.get(0));
        assertSame(cnec1, costlyElements.get(1));
    }

    @Test
    public void getMostLimitingElement() {
        assertSame(cnec3, minMarginEvaluator.getMostLimitingElement(branchResult));
    }

    @Test
    public void computeCost() {
        assertEquals(250., minMarginEvaluator.computeCost(branchResult, Mockito.mock(SensitivityStatus.class)), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWithPureMnecs() {
        BranchCnec mnec1 = Mockito.mock(BranchCnec.class);
        when(mnec1.isMonitored()).thenReturn(true);
        when(mnec1.isOptimized()).thenReturn(false);
        BranchCnec mnec2 = Mockito.mock(BranchCnec.class);
        when(mnec2.isMonitored()).thenReturn(true);
        when(mnec2.isOptimized()).thenReturn(false);

        MarginEvaluator marginEvaluator = Mockito.mock(MarginEvaluator.class);
        branchResult = Mockito.mock(BranchResult.class);
        when(marginEvaluator.getMargin(branchResult, mnec1, MEGAWATT)).thenReturn(-150.);
        when(marginEvaluator.getMargin(branchResult, mnec2, MEGAWATT)).thenReturn(200.);

        minMarginEvaluator = new MinMarginEvaluator(Set.of(mnec1, mnec2), MEGAWATT, marginEvaluator);
        assertTrue(minMarginEvaluator.getCostlyElements(branchResult, 10).isEmpty());
        assertNull(minMarginEvaluator.getMostLimitingElement(branchResult));
        assertEquals(0, minMarginEvaluator.computeCost(branchResult, Mockito.mock(SensitivityStatus.class)), DOUBLE_TOLERANCE);
    }
}
