/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Atena Amnache {@literal <atena.amnache at rte-france.com>}
 */
class TimeCoupledCastorContingencyScenariosTest {
    private static final OffsetDateTime TIMESTAMP = OffsetDateTime.of(2026, 1, 9, 0, 0, 0, 0, ZoneOffset.UTC);

    @Test
    /* same as original one. */
    void testIsStopCriterionChecked() {
        TreeParameters treeParameters = Mockito.mock(TreeParameters.class);
        ObjectiveFunctionResult objectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);

        // if virtual cost positive return false
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(100.);
        assertFalse(TimeCoupledCastorContingencyScenarios.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if purely virtual with null virtual cost, return true
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-Double.MAX_VALUE);
        assertTrue(TimeCoupledCastorContingencyScenarios.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if not purely virtual and stop criterion is MIN_OBJECTIVE return false
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-10.);
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.MIN_OBJECTIVE);
        assertFalse(TimeCoupledCastorContingencyScenarios.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if not purely virtual and stop criterion is AT_TARGET_OBJECTIVE_VALUE and cost is higher than target return false
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-10.);
        when(objectiveFunctionResult.getCost()).thenReturn(-10.0);
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(-20.);
        assertFalse(TimeCoupledCastorContingencyScenarios.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-10.);
        when(objectiveFunctionResult.getCost()).thenReturn(-10.0);
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(0.);
        assertTrue(TimeCoupledCastorContingencyScenarios.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if not purely virtual and stop criterion is AT_TARGET_OBJECTIVE_VALUE and cost is lower than target return true
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-10.);
        when(treeParameters.stopCriterion()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> TimeCoupledCastorContingencyScenarios.isStopCriterionChecked(objectiveFunctionResult, treeParameters));
    }

    @Test
    void testMergeRaUsageLimits() {
        Instant curativeInstant1 = Mockito.mock(Instant.class);
        Instant curativeInstant2 = Mockito.mock(Instant.class);
        RaUsageLimits raUsageLimits1 = new RaUsageLimits();
        RaUsageLimits raUsageLimits2 = new RaUsageLimits();
        Crac crac1 = Mockito.mock(Crac.class);
        Crac crac2 = Mockito.mock(Crac.class);
        when(crac1.getRaUsageLimitsPerInstant()).thenReturn(Map.of(curativeInstant1, raUsageLimits1));
        when(crac2.getRaUsageLimitsPerInstant()).thenReturn(Map.of(curativeInstant2, raUsageLimits2));
        Map<Instant, RaUsageLimits> mergedRaUsageLimits = TimeCoupledCastorContingencyScenarios.mergeRaUsageLimits(new TemporalDataImpl<>(Map.of(TIMESTAMP, crac1, TIMESTAMP.plusHours(1), crac2)));
        assertEquals(2, mergedRaUsageLimits.size());
        assertEquals(raUsageLimits1, mergedRaUsageLimits.get(curativeInstant1));
        assertEquals(raUsageLimits2, mergedRaUsageLimits.get(curativeInstant2));
    }

    @Test
    void testGetSameNetworkActionsOfTimestamp() {
        // search tree actvated 2 network actions
        NetworkAction sharedNetworkActionOfTimestamp1 = Mockito.mock(NetworkAction.class);
        NetworkAction sharedNetworkActionOfTimestamp2 = Mockito.mock(NetworkAction.class);
        NetworkAction timestamp1OnlyNetworkAction = Mockito.mock(NetworkAction.class);
        when(sharedNetworkActionOfTimestamp1.getId()).thenReturn("sharedNa");
        when(timestamp1OnlyNetworkAction.getId()).thenReturn("notSharedNa");
        // the crac of the 2nd tiemstamp has the sharedNa but not the other Na
        Crac crac = Mockito.mock(Crac.class);
        when(crac.getNetworkAction("sharedNa")).thenReturn(sharedNetworkActionOfTimestamp2);
        when(crac.getNetworkAction("notSharedNa")).thenReturn(null);
        // for each of the 2 network actions, the method looks the id in the timestamp 2 crac, finds sharedNa
        // doesnt find notSharedNa and filters it out.
        Set<NetworkAction> networkActions = createContingencyScenarios(crac).getSameNetworkActionsOfTimestamp(
                Set.of(sharedNetworkActionOfTimestamp1, timestamp1OnlyNetworkAction), TIMESTAMP
        );
        assertEquals(Set.of(sharedNetworkActionOfTimestamp2), networkActions);
    }

    private static TimeCoupledCastorContingencyScenarios createContingencyScenarios(Crac crac) {
        TemporalData<Crac> cracs = new TemporalDataImpl<>(Map.of(TIMESTAMP, crac));
        return new TimeCoupledCastorContingencyScenarios(cracs, null, null, null, null, null);
    }
}
