/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.usage_rule.*;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class JsonSerializationConstantsTest {

    @Test
    void versionNumberOkTest() {
        assertEquals(1, getPrimaryVersionNumber("1.2"));
        assertEquals(2, getSubVersionNumber("1.2"));
        assertEquals(2, getPrimaryVersionNumber("2.51"));
        assertEquals(51, getSubVersionNumber("2.51"));
    }

    @Test
    void versionNumberNok1Test() {
        assertThrows(FaraoException.class, () -> getPrimaryVersionNumber("v1.2"));
    }

    @Test
    void versionNumberNok2Test() {
        assertThrows(FaraoException.class, () -> getPrimaryVersionNumber("1.3.1"));
    }

    @Test
    void versionNumberNok3Test() {
        assertThrows(FaraoException.class, () -> getPrimaryVersionNumber("1.2b"));
    }

    private BranchThreshold mockBranchThreshold(Unit unit, Side side, Double min) {
        BranchThreshold branchThreshold = mock(BranchThreshold.class);
        when(branchThreshold.getUnit()).thenReturn(unit);
        when(branchThreshold.getSide()).thenReturn(side);
        when(branchThreshold.min()).thenReturn(Optional.ofNullable(min));
        return branchThreshold;
    }

    @Test
    void testThresholdComparator() {
        ThresholdComparator comparator = new ThresholdComparator();
        BranchThreshold bt1 = mockBranchThreshold(Unit.AMPERE, Side.LEFT, -10.);
        BranchThreshold bt2 = mockBranchThreshold(Unit.AMPERE, Side.LEFT, null);

        assertTrue(comparator.compare(bt1, bt2) < 0);

        bt1 = mockBranchThreshold(Unit.AMPERE, Side.LEFT, null);
        bt2 = mockBranchThreshold(Unit.AMPERE, Side.LEFT, null);
        assertTrue(comparator.compare(bt1, bt2) > 0);

        bt1 = mockBranchThreshold(Unit.AMPERE, Side.RIGHT, -10.);
        bt2 = mockBranchThreshold(Unit.AMPERE, Side.LEFT, null);
        assertTrue(comparator.compare(bt1, bt2) > 0);

        bt1 = mockBranchThreshold(Unit.AMPERE, Side.RIGHT, -10.);
        bt2 = mockBranchThreshold(Unit.MEGAWATT, Side.LEFT, null);
        assertTrue(comparator.compare(bt1, bt2) < 0);
    }

    private UsageRule mockUsageRule(Instant instant, UsageMethod usageMethod, String stateId, String flowCnecId, Country country, String angleCnecId, String voltageCnecId) {
        UsageRule usageRule = null;
        if (stateId != null) {
            OnContingencyState ur = mock(OnContingencyState.class);
            State state = mock(State.class);
            when(state.getId()).thenReturn(stateId);
            when(ur.getState()).thenReturn(state);
            usageRule = ur;
        } else if (flowCnecId != null) {
            OnFlowConstraint ur = mock(OnFlowConstraint.class);
            FlowCnec flowCnec = mock(FlowCnec.class);
            when(flowCnec.getId()).thenReturn(flowCnecId);
            when(ur.getFlowCnec()).thenReturn(flowCnec);
            usageRule = ur;
        } else if (angleCnecId != null) {
            OnAngleConstraint ur = mock(OnAngleConstraint.class);
            AngleCnec angleCnec = mock(AngleCnec.class);
            when(angleCnec.getId()).thenReturn(angleCnecId);
            when(ur.getAngleCnec()).thenReturn(angleCnec);
            usageRule = ur;
        } else if (voltageCnecId != null) {
            OnVoltageConstraint ur = mock(OnVoltageConstraint.class);
            VoltageCnec voltageCnec = mock(VoltageCnec.class);
            when(voltageCnec.getId()).thenReturn(voltageCnecId);
            when(ur.getVoltageCnec()).thenReturn(voltageCnec);
            usageRule = ur;
        } else if (country != null) {
            OnFlowConstraintInCountry ur = mock(OnFlowConstraintInCountry.class);
            when(ur.getCountry()).thenReturn(country);
            usageRule = ur;
        } else {
            usageRule = mock(OnInstant.class);
        }
        when(usageRule.getInstant()).thenReturn(instant);
        when(usageRule.getUsageMethod()).thenReturn(usageMethod);
        return usageRule;
    }

    @Test
    void testUsageRuleComparatorOnInstant() {
        UsageRuleComparator comparator = new UsageRuleComparator();

        UsageRule onInstant1 = mockUsageRule(Instant.PREVENTIVE, UsageMethod.AVAILABLE, null, null, null, null, null);
        UsageRule onInstant2 = mockUsageRule(Instant.PREVENTIVE, UsageMethod.FORCED, null, null, null, null, null);
        UsageRule onInstant3 = mockUsageRule(Instant.CURATIVE, UsageMethod.AVAILABLE, null, null, null, null, null);

        assertEquals(comparator.compare(onInstant1, onInstant1), 0);
        assertEquals(comparator.compare(onInstant2, onInstant2), 0);
        assertEquals(comparator.compare(onInstant3, onInstant3), 0);
        assertTrue(comparator.compare(onInstant1, onInstant2) < 0);
        assertTrue(comparator.compare(onInstant2, onInstant3) < 0);
        assertTrue(comparator.compare(onInstant1, onInstant3) < 0);
        assertTrue(comparator.compare(onInstant2, onInstant1) > 0);
        assertTrue(comparator.compare(onInstant3, onInstant2) > 0);
        assertTrue(comparator.compare(onInstant3, onInstant1) > 0);
    }

    @Test
    void testUsageRuleComparatorMix() {
        UsageRuleComparator comparator = new UsageRuleComparator();

        UsageRule oi1 = mockUsageRule(Instant.PREVENTIVE, UsageMethod.AVAILABLE, null, null, null, null, null);

        UsageRule ocs1 = mockUsageRule(Instant.CURATIVE, UsageMethod.AVAILABLE, "state1", null, null, null, null);
        UsageRule ocs2 = mockUsageRule(Instant.CURATIVE, UsageMethod.AVAILABLE, "state2", null, null, null, null);
        assertTrue(comparator.compare(ocs1, ocs2) < 0);
        assertTrue(comparator.compare(ocs2, ocs1) > 0);

        UsageRule ofc1 = mockUsageRule(Instant.PREVENTIVE, UsageMethod.AVAILABLE, null, "fc1", null, null, null);
        UsageRule ofc2 = mockUsageRule(Instant.PREVENTIVE, UsageMethod.AVAILABLE, null, "fc2", null, null, null);
        assertTrue(comparator.compare(ofc1, ofc2) < 0);
        assertTrue(comparator.compare(ofc2, ofc1) > 0);

        UsageRule ofcc1 = mockUsageRule(Instant.PREVENTIVE, UsageMethod.AVAILABLE, null, null, Country.FR, null, null);
        UsageRule ofcc2 = mockUsageRule(Instant.PREVENTIVE, UsageMethod.AVAILABLE, null, null, Country.ES, null, null);
        assertTrue(comparator.compare(ofcc1, ofcc2) > 0);
        assertTrue(comparator.compare(ofcc2, ofcc1) < 0);

        UsageRule oac1 = mockUsageRule(Instant.AUTO, UsageMethod.AVAILABLE, null, null, null, "BBB", null);
        UsageRule oac2 = mockUsageRule(Instant.AUTO, UsageMethod.AVAILABLE, null, null, null, "AAA", null);
        assertTrue(comparator.compare(oac1, oac2) > 0);
        assertTrue(comparator.compare(oac2, oac1) < 0);

        UsageRule ovc1 = mockUsageRule(Instant.CURATIVE, UsageMethod.FORCED, null, null, null, null, "z");
        UsageRule ovc2 = mockUsageRule(Instant.CURATIVE, UsageMethod.FORCED, null, null, null, null, "x");
        assertTrue(comparator.compare(ovc1, ovc2) > 0);
        assertTrue(comparator.compare(ovc2, ovc1) < 0);

        assertTrue(comparator.compare(oi1, ocs1) > 0);
        assertTrue(comparator.compare(ocs1, ofc1) < 0);
        assertTrue(comparator.compare(ofc2, ofcc1) < 0);
        assertTrue(comparator.compare(oac1, ocs2) < 0);
        assertTrue(comparator.compare(oac1, ovc2) < 0);
    }
}
