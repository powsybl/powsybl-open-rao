/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerCondition;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.ejml.UtilEjml.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class TriggerConditionImplTest {

    private final Instant preventiveInstant = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private final Instant curativeInstant = new InstantImpl("curative", InstantKind.CURATIVE, preventiveInstant);
    private final Contingency contingency = new Contingency("contingency", "contingency", List.of());
    private final FlowCnec flowCnec = new FlowCnecImpl("flowCnec", "flowCnec", null, null, null, null, true, false, null, 0d, null, null, null, null);
    private final String country = "country";

    @Test
    void testInitTriggerCondition() {
        TriggerCondition triggerCondition1 = new TriggerConditionImpl(preventiveInstant, null, null, null, UsageMethod.AVAILABLE);
        assertEquals(preventiveInstant, triggerCondition1.getInstant());
        assertTrue(triggerCondition1.getContingency().isEmpty());
        assertTrue(triggerCondition1.getCnec().isEmpty());
        assertTrue(triggerCondition1.getCountry().isEmpty());
        assertEquals(UsageMethod.AVAILABLE, triggerCondition1.getUsageMethod());

        TriggerCondition triggerCondition2 = new TriggerConditionImpl(curativeInstant, contingency, flowCnec, null, UsageMethod.FORCED);
        assertEquals(curativeInstant, triggerCondition2.getInstant());
        assertTrue(triggerCondition2.getContingency().isPresent());
        assertEquals(contingency, triggerCondition2.getContingency().get());
        assertTrue(triggerCondition2.getCnec().isPresent());
        assertEquals(flowCnec, triggerCondition2.getCnec().get());
        assertTrue(triggerCondition2.getCountry().isEmpty());
        assertEquals(UsageMethod.FORCED, triggerCondition2.getUsageMethod());

        TriggerCondition triggerCondition3 = new TriggerConditionImpl(curativeInstant, contingency, null, country, UsageMethod.UNAVAILABLE);
        assertEquals(curativeInstant, triggerCondition3.getInstant());
        assertTrue(triggerCondition3.getContingency().isPresent());
        assertEquals(contingency, triggerCondition3.getContingency().get());
        assertTrue(triggerCondition3.getCnec().isEmpty());
        assertTrue(triggerCondition3.getCountry().isPresent());
        assertEquals(country, triggerCondition3.getCountry().get());
        assertEquals(UsageMethod.UNAVAILABLE, triggerCondition3.getUsageMethod());
    }

    @Test
    void testEquals() {
        TriggerCondition preventiveAvailableTriggerCondition = new TriggerConditionImpl(preventiveInstant, null, null, null, UsageMethod.AVAILABLE);
        TriggerCondition preventiveForcedTriggerCondition = new TriggerConditionImpl(preventiveInstant, null, null, null, UsageMethod.FORCED);
        TriggerCondition curativeTriggerCondition = new TriggerConditionImpl(curativeInstant, contingency, null, null, UsageMethod.AVAILABLE);
        TriggerCondition curativeTriggerConditionWithCnec = new TriggerConditionImpl(curativeInstant, contingency, flowCnec, null, UsageMethod.AVAILABLE);
        TriggerCondition curativeTriggerConditionWithCountry = new TriggerConditionImpl(curativeInstant, contingency, null, country, UsageMethod.UNAVAILABLE);

        assertEquals(preventiveAvailableTriggerCondition, preventiveAvailableTriggerCondition);
        assertNotEquals(preventiveAvailableTriggerCondition, preventiveForcedTriggerCondition);
        assertNotEquals(preventiveAvailableTriggerCondition, curativeTriggerCondition);
        assertNotEquals(preventiveAvailableTriggerCondition, curativeTriggerConditionWithCnec);
        assertNotEquals(preventiveAvailableTriggerCondition, curativeTriggerConditionWithCountry);
        assertNotEquals(null, preventiveAvailableTriggerCondition);
        assertNotEquals("Hello world!", preventiveAvailableTriggerCondition);
    }
}
