/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import org.junit.Before;
import org.junit.Test;


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
    }

    @Test
    public void testPreventive() {
        // test with min objective
        searchTreeRaoParameters.setPreventiveRaoStopCriterion(SearchTreeRaoParameters.PreventiveRaoStopCriterion.MIN_OBJECTIVE);
        TreeParameters treeParameters = TreeParameters.buildForPreventivePerimeter(searchTreeRaoParameters);

        assertEquals(TreeParameters.StopCriterion.MIN_OBJECTIVE, treeParameters.getStopCriterion());
        assertEquals(4, treeParameters.getLeavesInParallel());
        assertEquals(6, treeParameters.getMaximumSearchDepth());

        // test with secure, and different values of the parameters
        searchTreeRaoParameters.setPreventiveRaoStopCriterion(SearchTreeRaoParameters.PreventiveRaoStopCriterion.SECURE);
        searchTreeRaoParameters.setPreventiveLeavesInParallel(8);
        searchTreeRaoParameters.setMaximumSearchDepth(15);
        treeParameters = TreeParameters.buildForPreventivePerimeter(searchTreeRaoParameters);

        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(0, treeParameters.getTargetObjectiveValue(), 1e-6);
        assertEquals(8, treeParameters.getLeavesInParallel());
        assertEquals(15, treeParameters.getMaximumSearchDepth());
    }

    @Test
    public void testCurativeMinObjective() {
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.MIN_OBJECTIVE);
        searchTreeRaoParameters.setCurativeRaoOptimizeOperatorsNotSharingCras(false);
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, 100.0);

        assertEquals(TreeParameters.StopCriterion.MIN_OBJECTIVE, treeParameters.getStopCriterion());
        assertEquals(2, treeParameters.getLeavesInParallel());
        assertEquals(6, treeParameters.getMaximumSearchDepth());
    }

    @Test
    public void testCurativeSecureStopCriterion() {
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.SECURE);
        searchTreeRaoParameters.setCurativeLeavesInParallel(16);
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, 100.0);

        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(0., treeParameters.getTargetObjectiveValue(), 1e-6);
        assertEquals(16, treeParameters.getLeavesInParallel());
        assertEquals(6, treeParameters.getMaximumSearchDepth());

    }

    @Test
    public void testCurativePreventiveObjectiveStopCriterion() {
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE);
        searchTreeRaoParameters.setCurativeRaoMinObjImprovement(35);
        searchTreeRaoParameters.setMaximumSearchDepth(0);
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, 100.0);

        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(65, treeParameters.getTargetObjectiveValue(), 1e-6);
        assertEquals(2, treeParameters.getLeavesInParallel());
        assertEquals(0, treeParameters.getMaximumSearchDepth());
    }

    @Test
    public void testCurativePreventiveObjectiveAndSecureStopCriterion() {
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE);
        searchTreeRaoParameters.setCurativeRaoMinObjImprovement(35);

        // limited by secure
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, 100.0);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(0, treeParameters.getTargetObjectiveValue(), 1e-6);
        assertEquals(2, treeParameters.getLeavesInParallel());
        assertEquals(6, treeParameters.getMaximumSearchDepth());

        // limited by preventive_objective + minObjImprovement
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, 30.0);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(-5, treeParameters.getTargetObjectiveValue(), 1e-6);
        assertEquals(2, treeParameters.getLeavesInParallel());
        assertEquals(6, treeParameters.getMaximumSearchDepth());

        // limited by preventive_objective + minObjImprovement
        treeParameters = TreeParameters.buildForCurativePerimeter(searchTreeRaoParameters, -50.0);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(-85, treeParameters.getTargetObjectiveValue(), 1e-6);
        assertEquals(2, treeParameters.getLeavesInParallel());
        assertEquals(6, treeParameters.getMaximumSearchDepth());
    }
}
