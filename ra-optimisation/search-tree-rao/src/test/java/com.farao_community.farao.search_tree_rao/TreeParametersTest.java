/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class TreeParametersTest {
    SearchTreeRaoParameters searchTreeRaoParameters;

    @Before
    public void setUp() {
        searchTreeRaoParameters = new SearchTreeRaoParameters();
        searchTreeRaoParameters.setMaximumSearchDepth(6);
        searchTreeRaoParameters.setLeavesInParallel(4);
        searchTreeRaoParameters.setRelativeNetworkActionMinimumImpactThreshold(0.1);
        searchTreeRaoParameters.setAbsoluteNetworkActionMinimumImpactThreshold(2);
        searchTreeRaoParameters.setMaxCurativeTopoPerTso(Map.of("Elia", 3, "Amprion", 0));
        searchTreeRaoParameters.setMaxCurativePstPerTso(Map.of("Tennet", 4));
        searchTreeRaoParameters.setMaxCurativeRaPerTso(Map.of("RTE", 5));
    }

    private void compareCommonParameters(TreeParameters treeParameters, SearchTreeRaoParameters searchTreeRaoParameters) {
        assertEquals(searchTreeRaoParameters.getMaximumSearchDepth(), treeParameters.getMaximumSearchDepth());
        assertEquals(searchTreeRaoParameters.getLeavesInParallel(), treeParameters.getLeavesInParallel());
        assertEquals(searchTreeRaoParameters.getRelativeNetworkActionMinimumImpactThreshold(), treeParameters.getRelativeNetworkActionMinimumImpactThreshold(), 1e-6);
        assertEquals(searchTreeRaoParameters.getAbsoluteNetworkActionMinimumImpactThreshold(), treeParameters.getAbsoluteNetworkActionMinimumImpactThreshold(), 1e-6);
    }

    private void compareCurativeParameters(TreeParameters treeParameters, SearchTreeRaoParameters searchTreeRaoParameters) {
        compareMaps(searchTreeRaoParameters.getMaxCurativeTopoPerTso(), treeParameters.getMaxTopoPerTso());
        compareMaps(searchTreeRaoParameters.getMaxCurativePstPerTso(), treeParameters.getMaxPstPerTso());
        compareMaps(searchTreeRaoParameters.getMaxCurativeRaPerTso(), treeParameters.getMaxRaPerTso());
    }

    private void compareMaps(Map expected, Map actual) {
        if (expected.isEmpty()) {
            assertTrue(actual.isEmpty());
        } else {
            assertSame(expected, actual);
        }
    }

    @Test
    public void testPreventive() {
        searchTreeRaoParameters.setPreventiveRaoStopCriterion(SearchTreeRaoParameters.PreventiveRaoStopCriterion.MIN_OBJECTIVE);
        TreeParameters treeParameters = TreeParameters.buildForPreventivePerimeter(searchTreeRaoParameters, true);
        assertEquals(TreeParameters.StopCriterion.MIN_OBJECTIVE, treeParameters.getStopCriterion());
        assertTrue(treeParameters.getShouldComputeInitialSensitivity());
        assertTrue(treeParameters.getMaxTopoPerTso().isEmpty());
        assertTrue(treeParameters.getMaxPstPerTso().isEmpty());
        assertTrue(treeParameters.getMaxRaPerTso().isEmpty());
        compareCommonParameters(treeParameters, searchTreeRaoParameters);

        searchTreeRaoParameters.setPreventiveRaoStopCriterion(SearchTreeRaoParameters.PreventiveRaoStopCriterion.SECURE);
        treeParameters = TreeParameters.buildForPreventivePerimeter(searchTreeRaoParameters, false);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(0, treeParameters.getTargetObjectiveValue(), 1e-6);
        assertFalse(treeParameters.getShouldComputeInitialSensitivity());
        assertTrue(treeParameters.getMaxTopoPerTso().isEmpty());
        assertTrue(treeParameters.getMaxPstPerTso().isEmpty());
        assertTrue(treeParameters.getMaxRaPerTso().isEmpty());
        compareCommonParameters(treeParameters, searchTreeRaoParameters);
    }

    @Test
    public void testCurative1() {
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.MIN_OBJECTIVE);
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, 100.0);
        assertEquals(TreeParameters.StopCriterion.MIN_OBJECTIVE, treeParameters.getStopCriterion());
        compareCommonParameters(treeParameters, searchTreeRaoParameters);
        compareCurativeParameters(treeParameters, searchTreeRaoParameters);
    }

    @Test
    public void testCurative2() {
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.SECURE);
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, 100.0);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(0, treeParameters.getTargetObjectiveValue(), 1e-6);
        compareCommonParameters(treeParameters, searchTreeRaoParameters);
        compareCurativeParameters(treeParameters, searchTreeRaoParameters);
    }

    @Test
    public void testCurative3() {
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE);
        searchTreeRaoParameters.setCurativeRaoMinObjImprovement(35);
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, 100.0);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(65, treeParameters.getTargetObjectiveValue(), 1e-6);
        compareCommonParameters(treeParameters, searchTreeRaoParameters);
        compareCurativeParameters(treeParameters, searchTreeRaoParameters);
    }

    @Test
    public void testCurative4() {
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE);
        searchTreeRaoParameters.setCurativeRaoMinObjImprovement(35);

        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, 100.0);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(0, treeParameters.getTargetObjectiveValue(), 1e-6);
        compareCommonParameters(treeParameters, searchTreeRaoParameters);
        compareCurativeParameters(treeParameters, searchTreeRaoParameters);

        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, 30.0);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(-5, treeParameters.getTargetObjectiveValue(), 1e-6);
        compareCommonParameters(treeParameters, searchTreeRaoParameters);
        compareCurativeParameters(treeParameters, searchTreeRaoParameters);

        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, -50.0);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(-85, treeParameters.getTargetObjectiveValue(), 1e-6);
        compareCommonParameters(treeParameters, searchTreeRaoParameters);
        compareCurativeParameters(treeParameters, searchTreeRaoParameters);
    }

    @Test
    public void testDefaultSearchTreeRaoParameters() {
        SearchTreeRaoParameters defaultParameters = new SearchTreeRaoParameters();
        TreeParameters treeParameters = TreeParameters.buildForPreventivePerimeter(null, true);
        compareCommonParameters(treeParameters, defaultParameters);
        assertTrue(treeParameters.getMaxTopoPerTso().isEmpty());
        assertTrue(treeParameters.getMaxPstPerTso().isEmpty());
        assertTrue(treeParameters.getMaxRaPerTso().isEmpty());
        treeParameters = TreeParameters.buildForCurativePerimeter(null, 0.);
        compareCommonParameters(treeParameters, defaultParameters);
        compareCurativeParameters(treeParameters, defaultParameters);
    }
}
