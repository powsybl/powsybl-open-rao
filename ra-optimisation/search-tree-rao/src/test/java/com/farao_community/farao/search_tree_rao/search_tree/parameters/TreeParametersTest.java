/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.search_tree.parameters;

import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

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
        searchTreeRaoParameters.setPreventiveLeavesInParallel(4);
        searchTreeRaoParameters.setCurativeLeavesInParallel(2);
        searchTreeRaoParameters.setRelativeNetworkActionMinimumImpactThreshold(0.1);
        searchTreeRaoParameters.setAbsoluteNetworkActionMinimumImpactThreshold(2);
        searchTreeRaoParameters.setMaxCurativeTopoPerTso(Map.of("Elia", 3, "Amprion", 0));
        searchTreeRaoParameters.setMaxCurativePstPerTso(Map.of("Tennet", 4));
        searchTreeRaoParameters.setMaxCurativeRaPerTso(Map.of("RTE", 5));
    }

    private void compareCommonParameters(TreeParameters treeParameters, SearchTreeRaoParameters searchTreeRaoParameters) {
        assertEquals(searchTreeRaoParameters.getMaximumSearchDepth(), treeParameters.getMaximumSearchDepth());
        assertEquals(searchTreeRaoParameters.getRelativeNetworkActionMinimumImpactThreshold(), treeParameters.getRelativeNetworkActionMinimumImpactThreshold(), 1e-6);
        assertEquals(searchTreeRaoParameters.getAbsoluteNetworkActionMinimumImpactThreshold(), treeParameters.getAbsoluteNetworkActionMinimumImpactThreshold(), 1e-6);
    }

    private void compareCurativeParameters(TreeParameters treeParameters, SearchTreeRaoParameters searchTreeRaoParameters) {
        compareMaps(searchTreeRaoParameters.getMaxCurativeTopoPerTso(), treeParameters.getMaxTopoPerTso());
        compareMaps(searchTreeRaoParameters.getMaxCurativePstPerTso(), treeParameters.getMaxPstPerTso());
        compareMaps(searchTreeRaoParameters.getMaxCurativeRaPerTso(), treeParameters.getMaxRaPerTso());
        assertEquals(searchTreeRaoParameters.getCurativeLeavesInParallel(), treeParameters.getLeavesInParallel());
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
        TreeParameters treeParameters = TreeParameters.buildForPreventivePerimeter(searchTreeRaoParameters);
        assertEquals(TreeParameters.StopCriterion.MIN_OBJECTIVE, treeParameters.getStopCriterion());
        assertTrue(treeParameters.getMaxTopoPerTso().isEmpty());
        assertTrue(treeParameters.getMaxPstPerTso().isEmpty());
        assertTrue(treeParameters.getMaxRaPerTso().isEmpty());
        assertEquals(4, treeParameters.getLeavesInParallel());
        compareCommonParameters(treeParameters, searchTreeRaoParameters);

        searchTreeRaoParameters.setPreventiveRaoStopCriterion(SearchTreeRaoParameters.PreventiveRaoStopCriterion.SECURE);
        searchTreeRaoParameters.setPreventiveLeavesInParallel(8);
        treeParameters = TreeParameters.buildForPreventivePerimeter(searchTreeRaoParameters);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(0, treeParameters.getTargetObjectiveValue(), 1e-6);
        assertTrue(treeParameters.getMaxTopoPerTso().isEmpty());
        assertTrue(treeParameters.getMaxPstPerTso().isEmpty());
        assertTrue(treeParameters.getMaxRaPerTso().isEmpty());
        assertEquals(8, treeParameters.getLeavesInParallel());
        compareCommonParameters(treeParameters, searchTreeRaoParameters);
    }

    @Test
    public void testCurative1() {
        Set<String> operators = Set.of("NL", "AT");
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.MIN_OBJECTIVE);
        searchTreeRaoParameters.setCurativeRaoOptimizeOperatorsNotSharingCras(false);
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, 100.0);
        assertEquals(TreeParameters.StopCriterion.MIN_OBJECTIVE, treeParameters.getStopCriterion());
        //assertSame(operators, treeParameters.getOperatorsNotToOptimize());
        compareCommonParameters(treeParameters, searchTreeRaoParameters);
        compareCurativeParameters(treeParameters, searchTreeRaoParameters);
    }

    @Test
    public void testCurative2() {
        Set<String> operators = Set.of("NL", "AT");
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.SECURE);
        searchTreeRaoParameters.setCurativeLeavesInParallel(16);
        searchTreeRaoParameters.setCurativeRaoOptimizeOperatorsNotSharingCras(true);
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, 100.0);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(0, treeParameters.getTargetObjectiveValue(), 1e-6);
        //assertNull(treeParameters.getOperatorsNotToOptimize());
        compareCommonParameters(treeParameters, searchTreeRaoParameters);
        compareCurativeParameters(treeParameters, searchTreeRaoParameters);
    }

    @Test
    public void testCurative3() {
        Set<String> operators = Set.of("NL", "AT");
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE);
        searchTreeRaoParameters.setCurativeRaoMinObjImprovement(35);
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, 100.0);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(65, treeParameters.getTargetObjectiveValue(), 1e-6);
        //assertNull(treeParameters.getOperatorsNotToOptimize());
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
        TreeParameters treeParameters = TreeParameters.buildForPreventivePerimeter(null);
        compareCommonParameters(treeParameters, defaultParameters);
        assertTrue(treeParameters.getMaxTopoPerTso().isEmpty());
        assertTrue(treeParameters.getMaxPstPerTso().isEmpty());
        assertTrue(treeParameters.getMaxRaPerTso().isEmpty());
        assertEquals(1, treeParameters.getLeavesInParallel());
        treeParameters = TreeParameters.buildForCurativePerimeter(null, 0.);
        compareCommonParameters(treeParameters, defaultParameters);
        compareCurativeParameters(treeParameters, defaultParameters);
    }
}
