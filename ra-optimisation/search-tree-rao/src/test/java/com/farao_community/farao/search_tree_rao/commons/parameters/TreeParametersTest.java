/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.rao_api.parameters.ObjectiveFunctionParameters;
import com.farao_community.farao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class TreeParametersTest {
    RaoParameters raoParameters;
    private static final double DOUBLE_TOLERANCE = 1e-6;

    @BeforeEach
    public void setUp() {
        raoParameters = new RaoParameters();
        raoParameters.getTopoOptimizationParameters().setMaxSearchTreeDepth(6);
        raoParameters.getMultithreadingParameters().setPreventiveLeavesInParallel(4);
        raoParameters.getMultithreadingParameters().setCurativeLeavesInParallel(2);
        raoParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(RangeActionsOptimizationParameters.RaRangeShrinking.DISABLED);
    }

    @Test
    void testPreventive() {
        // test with min objective
        raoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.MIN_OBJECTIVE);
        TreeParameters treeParameters = TreeParameters.buildForPreventivePerimeter(raoParameters);

        assertEquals(TreeParameters.StopCriterion.MIN_OBJECTIVE, treeParameters.getStopCriterion());
        assertEquals(4, treeParameters.getLeavesInParallel());
        assertEquals(6, treeParameters.getMaximumSearchDepth());
        assertFalse(treeParameters.getRaRangeShrinking());

        // test with secure, and different values of the parameters
        raoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
        raoParameters.getMultithreadingParameters().setPreventiveLeavesInParallel(8);
        raoParameters.getTopoOptimizationParameters().setMaxSearchTreeDepth(15);
        raoParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(RangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO);
        treeParameters = TreeParameters.buildForPreventivePerimeter(raoParameters);

        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(0, treeParameters.getTargetObjectiveValue(), DOUBLE_TOLERANCE);
        assertEquals(8, treeParameters.getLeavesInParallel());
        assertEquals(15, treeParameters.getMaximumSearchDepth());
        assertTrue(treeParameters.getRaRangeShrinking());

        raoParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(RangeActionsOptimizationParameters.RaRangeShrinking.ENABLED);
        treeParameters = TreeParameters.buildForPreventivePerimeter(raoParameters);
        assertTrue(treeParameters.getRaRangeShrinking());
    }

    @Test
    void testCurativeMinObjective() {
        raoParameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.MIN_OBJECTIVE);
        raoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCurativeCnecsForTsosWithoutCras(false);
        raoParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(RangeActionsOptimizationParameters.RaRangeShrinking.DISABLED);
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, 100.0);

        assertEquals(TreeParameters.StopCriterion.MIN_OBJECTIVE, treeParameters.getStopCriterion());
        assertEquals(2, treeParameters.getLeavesInParallel());
        assertEquals(6, treeParameters.getMaximumSearchDepth());
        assertFalse(treeParameters.getRaRangeShrinking());
    }

    @Test
    void testCurativeSecureStopCriterion() {
        raoParameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.SECURE);
        raoParameters.getMultithreadingParameters().setCurativeLeavesInParallel(16);
        raoParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(RangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO);
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, 100.0);

        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(0., treeParameters.getTargetObjectiveValue(), DOUBLE_TOLERANCE);
        assertEquals(16, treeParameters.getLeavesInParallel());
        assertEquals(6, treeParameters.getMaximumSearchDepth());
        assertTrue(treeParameters.getRaRangeShrinking());
    }

    @Test
    void testCurativePreventiveObjectiveStopCriterion() {
        raoParameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.PREVENTIVE_OBJECTIVE);
        raoParameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(35);
        raoParameters.getTopoOptimizationParameters().setMaxSearchTreeDepth(0);
        raoParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(RangeActionsOptimizationParameters.RaRangeShrinking.ENABLED);
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, 100.0);

        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(65, treeParameters.getTargetObjectiveValue(), DOUBLE_TOLERANCE);
        assertEquals(2, treeParameters.getLeavesInParallel());
        assertEquals(0, treeParameters.getMaximumSearchDepth());
        assertTrue(treeParameters.getRaRangeShrinking());
    }

    @Test
    void testCurativePreventiveObjectiveAndSecureStopCriterion() {
        raoParameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE);
        raoParameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(35);

        // limited by secure
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, 100.0);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(0, treeParameters.getTargetObjectiveValue(), DOUBLE_TOLERANCE);
        assertEquals(2, treeParameters.getLeavesInParallel());
        assertEquals(6, treeParameters.getMaximumSearchDepth());

        // limited by preventive_objective + minObjImprovement
        treeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, 30.0);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(-5, treeParameters.getTargetObjectiveValue(), DOUBLE_TOLERANCE);
        assertEquals(2, treeParameters.getLeavesInParallel());
        assertEquals(6, treeParameters.getMaximumSearchDepth());

        // limited by preventive_objective + minObjImprovement
        treeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, -50.0);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(-85, treeParameters.getTargetObjectiveValue(), DOUBLE_TOLERANCE);
        assertEquals(2, treeParameters.getLeavesInParallel());
        assertEquals(6, treeParameters.getMaximumSearchDepth());
    }

    @Test
    void testSecondPreventive() {
        // test with min objective
        raoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.MIN_OBJECTIVE);
        raoParameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.MIN_OBJECTIVE);
        raoParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(RangeActionsOptimizationParameters.RaRangeShrinking.DISABLED);
        TreeParameters treeParameters = TreeParameters.buildForSecondPreventivePerimeter(raoParameters);

        assertEquals(TreeParameters.StopCriterion.MIN_OBJECTIVE, treeParameters.getStopCriterion());
        assertEquals(4, treeParameters.getLeavesInParallel());
        assertEquals(6, treeParameters.getMaximumSearchDepth());
        assertFalse(treeParameters.getRaRangeShrinking());

        // test with secure
        raoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
        raoParameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.SECURE);
        raoParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(RangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO);
        treeParameters = TreeParameters.buildForSecondPreventivePerimeter(raoParameters);

        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(0, treeParameters.getTargetObjectiveValue(), DOUBLE_TOLERANCE);
        assertEquals(4, treeParameters.getLeavesInParallel());
        assertEquals(6, treeParameters.getMaximumSearchDepth());
        assertFalse(treeParameters.getRaRangeShrinking());

        // other combinations
        raoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
        raoParameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.MIN_OBJECTIVE);
        raoParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(RangeActionsOptimizationParameters.RaRangeShrinking.ENABLED);
        treeParameters = TreeParameters.buildForSecondPreventivePerimeter(raoParameters);

        assertEquals(TreeParameters.StopCriterion.MIN_OBJECTIVE, treeParameters.getStopCriterion());
        assertEquals(4, treeParameters.getLeavesInParallel());
        assertEquals(6, treeParameters.getMaximumSearchDepth());
        assertTrue(treeParameters.getRaRangeShrinking());

        // still another combination
        raoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
        raoParameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE);
        raoParameters.getMultithreadingParameters().setPreventiveLeavesInParallel(8);
        raoParameters.getTopoOptimizationParameters().setMaxSearchTreeDepth(15);
        treeParameters = TreeParameters.buildForSecondPreventivePerimeter(raoParameters);

        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.getStopCriterion());
        assertEquals(0, treeParameters.getTargetObjectiveValue(), DOUBLE_TOLERANCE);
        assertEquals(8, treeParameters.getLeavesInParallel());
        assertEquals(15, treeParameters.getMaximumSearchDepth());
    }
}
