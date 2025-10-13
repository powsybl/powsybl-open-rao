/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class CastorContingencyScenariosTest {

    @Test
    void testIsStopCriterionChecked() {
        TreeParameters treeParameters = Mockito.mock(TreeParameters.class);
        ObjectiveFunctionResult objectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);

        // if virtual cost positive return false
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(100.);
        assertFalse(CastorContingencyScenarios.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if purely virtual with null virtual cost, return true
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-Double.MAX_VALUE);
        assertTrue(CastorContingencyScenarios.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if not purely virtual and stop criterion is MIN_OBJECTIVE return false
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-10.);
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.MIN_OBJECTIVE);
        assertFalse(CastorContingencyScenarios.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if not purely virtual and stop criterion is AT_TARGET_OBJECTIVE_VALUE and cost is higher than target return false
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-10.);
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(-20.);
        assertFalse(CastorContingencyScenarios.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if not purely virtual and stop criterion is AT_TARGET_OBJECTIVE_VALUE and cost is lower than target return true
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-10.);
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(0.);
        assertFalse(CastorContingencyScenarios.isStopCriterionChecked(objectiveFunctionResult, treeParameters));
    }
}
