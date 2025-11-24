/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.parameters;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class TreeParametersTest {
    RaoParameters raoParameters;
    OpenRaoSearchTreeParameters searchTreeParameters;

    private static final double DOUBLE_TOLERANCE = 1e-6;

    @BeforeEach
    public void setUp() {
        raoParameters = new RaoParameters(ReportNode.NO_OP);
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters(ReportNode.NO_OP));
        searchTreeParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class);
        searchTreeParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.DISABLED);
        searchTreeParameters.getTopoOptimizationParameters().setMaxPreventiveSearchTreeDepth(6);
        searchTreeParameters.getTopoOptimizationParameters().setMaxCurativeSearchTreeDepth(6);
        searchTreeParameters.getMultithreadingParameters().setAvailableCPUs(4);
    }

    @Test
    void testPreventive() {
        // test with min objective
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        TreeParameters treeParameters = TreeParameters.buildForPreventivePerimeter(raoParameters);

        assertEquals(TreeParameters.StopCriterion.MIN_OBJECTIVE, treeParameters.stopCriterion());
        assertEquals(4, treeParameters.leavesInParallel());
        assertEquals(6, treeParameters.maximumSearchDepth());
        assertFalse(treeParameters.raRangeShrinking());

        // test with secure, and different values of the parameters
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW);
        searchTreeParameters.getMultithreadingParameters().setAvailableCPUs(8);
        searchTreeParameters.getTopoOptimizationParameters().setMaxPreventiveSearchTreeDepth(15);
        searchTreeParameters.getTopoOptimizationParameters().setMaxCurativeSearchTreeDepth(15);
        searchTreeParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO);
        treeParameters = TreeParameters.buildForPreventivePerimeter(raoParameters);

        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.stopCriterion());
        assertEquals(0, treeParameters.targetObjectiveValue(), DOUBLE_TOLERANCE);
        assertEquals(8, treeParameters.leavesInParallel());
        assertEquals(15, treeParameters.maximumSearchDepth());
        assertTrue(treeParameters.raRangeShrinking());

        searchTreeParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED);
        treeParameters = TreeParameters.buildForPreventivePerimeter(raoParameters);
        assertTrue(treeParameters.raRangeShrinking());
    }

    @Test
    void testCurativeSecureStopCriterion() {
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW);
        searchTreeParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO);
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, 100.0);

        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.stopCriterion());
        assertEquals(0., treeParameters.targetObjectiveValue(), DOUBLE_TOLERANCE);
        assertEquals(1, treeParameters.leavesInParallel());
        assertEquals(6, treeParameters.maximumSearchDepth());
        assertTrue(treeParameters.raRangeShrinking());
    }

    @Test
    void testCurativePreventiveObjectiveStopCriterion() {
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(false);
        searchTreeParameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(35, ReportNode.NO_OP);
        searchTreeParameters.getTopoOptimizationParameters().setMaxPreventiveSearchTreeDepth(0);
        searchTreeParameters.getTopoOptimizationParameters().setMaxCurativeSearchTreeDepth(0);
        searchTreeParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED);
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, 100.0);

        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.stopCriterion());
        assertEquals(65, treeParameters.targetObjectiveValue(), DOUBLE_TOLERANCE);
        assertEquals(1, treeParameters.leavesInParallel());
        assertEquals(0, treeParameters.maximumSearchDepth());
        assertTrue(treeParameters.raRangeShrinking());
    }

    @Test
    void testCurativePreventiveObjectiveAndSecureStopCriterion() {
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(true);
        searchTreeParameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(35, ReportNode.NO_OP);

        // limited by secure
        TreeParameters treeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, 100.0);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.stopCriterion());
        assertEquals(0, treeParameters.targetObjectiveValue(), DOUBLE_TOLERANCE);
        assertEquals(1, treeParameters.leavesInParallel());
        assertEquals(6, treeParameters.maximumSearchDepth());

        // limited by preventive_objective + minObjImprovement
        treeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, 30.0);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.stopCriterion());
        assertEquals(-5, treeParameters.targetObjectiveValue(), DOUBLE_TOLERANCE);
        assertEquals(1, treeParameters.leavesInParallel());
        assertEquals(6, treeParameters.maximumSearchDepth());

        // limited by preventive_objective + minObjImprovement
        treeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, -50.0);
        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.stopCriterion());
        assertEquals(-85, treeParameters.targetObjectiveValue(), DOUBLE_TOLERANCE);
        assertEquals(1, treeParameters.leavesInParallel());
        assertEquals(6, treeParameters.maximumSearchDepth());
    }

    @Test
    void testSecondPreventive() {
        // test with min objective
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(false);
        searchTreeParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.DISABLED);
        TreeParameters treeParameters = TreeParameters.buildForSecondPreventivePerimeter(raoParameters);

        assertEquals(TreeParameters.StopCriterion.MIN_OBJECTIVE, treeParameters.stopCriterion());
        assertEquals(4, treeParameters.leavesInParallel());
        assertEquals(6, treeParameters.maximumSearchDepth());
        assertFalse(treeParameters.raRangeShrinking());

        // test with secure
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW);
        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(false);
        searchTreeParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED_IN_FIRST_PRAO_AND_CRAO);
        treeParameters = TreeParameters.buildForSecondPreventivePerimeter(raoParameters);

        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.stopCriterion());
        assertEquals(0, treeParameters.targetObjectiveValue(), DOUBLE_TOLERANCE);
        assertEquals(4, treeParameters.leavesInParallel());
        assertEquals(6, treeParameters.maximumSearchDepth());
        assertFalse(treeParameters.raRangeShrinking());

        // other combinations
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW);
        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(false);
        searchTreeParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.ENABLED);
        treeParameters = TreeParameters.buildForSecondPreventivePerimeter(raoParameters);

        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.stopCriterion());
        assertEquals(4, treeParameters.leavesInParallel());
        assertEquals(6, treeParameters.maximumSearchDepth());
        assertTrue(treeParameters.raRangeShrinking());

        // still another combination
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW);
        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(true);
        searchTreeParameters.getMultithreadingParameters().setAvailableCPUs(8);
        searchTreeParameters.getTopoOptimizationParameters().setMaxPreventiveSearchTreeDepth(15);
        searchTreeParameters.getTopoOptimizationParameters().setMaxCurativeSearchTreeDepth(15);
        treeParameters = TreeParameters.buildForSecondPreventivePerimeter(raoParameters);

        assertEquals(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE, treeParameters.stopCriterion());
        assertEquals(0, treeParameters.targetObjectiveValue(), DOUBLE_TOLERANCE);
        assertEquals(8, treeParameters.leavesInParallel());
        assertEquals(15, treeParameters.maximumSearchDepth());
    }
}
