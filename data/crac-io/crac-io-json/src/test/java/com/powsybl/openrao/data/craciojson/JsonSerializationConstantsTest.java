/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craciojson;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.threshold.BranchThreshold;
import com.powsybl.openrao.data.cracapi.usagerule.*;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @ParameterizedTest
    @ValueSource(strings = {"v1.2", "1.3.1", "1.2b"})
    void versionNumberNokTest(String version) {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> getPrimaryVersionNumber(version));
        assertEquals("json CRAC version number must be of the form vX.Y", exception.getMessage());
    }

    private BranchThreshold mockBranchThreshold(Unit unit, TwoSides side, Double min) {
        BranchThreshold branchThreshold = mock(BranchThreshold.class);
        when(branchThreshold.getUnit()).thenReturn(unit);
        when(branchThreshold.getSide()).thenReturn(side);
        when(branchThreshold.min()).thenReturn(Optional.ofNullable(min));
        return branchThreshold;
    }

    @Test
    void testThresholdComparator() {
        ThresholdComparator comparator = new ThresholdComparator();
        BranchThreshold bt1 = mockBranchThreshold(Unit.AMPERE, TwoSides.ONE, -10.);
        BranchThreshold bt2 = mockBranchThreshold(Unit.AMPERE, TwoSides.ONE, null);

        assertTrue(comparator.compare(bt1, bt2) < 0);

        bt1 = mockBranchThreshold(Unit.AMPERE, TwoSides.ONE, null);
        bt2 = mockBranchThreshold(Unit.AMPERE, TwoSides.ONE, null);
        assertTrue(comparator.compare(bt1, bt2) > 0);

        bt1 = mockBranchThreshold(Unit.AMPERE, TwoSides.TWO, -10.);
        bt2 = mockBranchThreshold(Unit.AMPERE, TwoSides.ONE, null);
        assertTrue(comparator.compare(bt1, bt2) > 0);

        bt1 = mockBranchThreshold(Unit.AMPERE, TwoSides.TWO, -10.);
        bt2 = mockBranchThreshold(Unit.MEGAWATT, TwoSides.ONE, null);
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
            OnConstraint<FlowCnec> ur = mock(OnConstraint.class);
            FlowCnec flowCnec = mock(FlowCnec.class);
            when(flowCnec.getId()).thenReturn(flowCnecId);
            when(ur.getCnec()).thenReturn(flowCnec);
            usageRule = ur;
        } else if (angleCnecId != null) {
            OnConstraint<AngleCnec> ur = mock(OnConstraint.class);
            AngleCnec angleCnec = mock(AngleCnec.class);
            when(angleCnec.getId()).thenReturn(angleCnecId);
            when(ur.getCnec()).thenReturn(angleCnec);
            usageRule = ur;
        } else if (voltageCnecId != null) {
            OnConstraint<VoltageCnec> ur = mock(OnConstraint.class);
            VoltageCnec voltageCnec = mock(VoltageCnec.class);
            when(voltageCnec.getId()).thenReturn(voltageCnecId);
            when(ur.getCnec()).thenReturn(voltageCnec);
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
        Instant preventiveInstant = mock(Instant.class);
        Instant curativeInstant = mock(Instant.class);
        when(preventiveInstant.comesBefore(any())).thenReturn(true);
        UsageRuleComparator comparator = new UsageRuleComparator();

        UsageRule onInstant1 = mockUsageRule(preventiveInstant, UsageMethod.AVAILABLE, null, null, null, null, null);
        UsageRule onInstant2 = mockUsageRule(preventiveInstant, UsageMethod.FORCED, null, null, null, null, null);
        UsageRule onInstant3 = mockUsageRule(curativeInstant, UsageMethod.AVAILABLE, null, null, null, null, null);

        assertEquals(0, comparator.compare(onInstant1, onInstant1));
        assertEquals(0, comparator.compare(onInstant2, onInstant2));
        assertEquals(0, comparator.compare(onInstant3, onInstant3));
        assertTrue(comparator.compare(onInstant1, onInstant2) < 0);
        assertTrue(comparator.compare(onInstant2, onInstant3) < 0);
        assertTrue(comparator.compare(onInstant1, onInstant3) < 0);
        assertTrue(comparator.compare(onInstant2, onInstant1) > 0);
        assertTrue(comparator.compare(onInstant3, onInstant2) > 0);
        assertTrue(comparator.compare(onInstant3, onInstant1) > 0);
    }

    @Test
    void testUsageRuleComparatorMix() {
        Instant preventiveInstant = mock(Instant.class);
        Instant autoInstant = mock(Instant.class);
        Instant curativeInstant = mock(Instant.class);
        UsageRuleComparator comparator = new UsageRuleComparator();

        UsageRule oi1 = mockUsageRule(preventiveInstant, UsageMethod.AVAILABLE, null, null, null, null, null);

        UsageRule ocs1 = mockUsageRule(curativeInstant, UsageMethod.AVAILABLE, "state1", null, null, null, null);
        UsageRule ocs2 = mockUsageRule(curativeInstant, UsageMethod.AVAILABLE, "state2", null, null, null, null);
        assertTrue(comparator.compare(ocs1, ocs2) < 0);
        assertTrue(comparator.compare(ocs2, ocs1) > 0);

        UsageRule ofc1 = mockUsageRule(preventiveInstant, UsageMethod.AVAILABLE, null, "fc1", null, null, null);
        UsageRule ofc2 = mockUsageRule(preventiveInstant, UsageMethod.AVAILABLE, null, "fc2", null, null, null);
        assertTrue(comparator.compare(ofc1, ofc2) < 0);
        assertTrue(comparator.compare(ofc2, ofc1) > 0);

        UsageRule ofcc1 = mockUsageRule(preventiveInstant, UsageMethod.AVAILABLE, null, null, Country.FR, null, null);
        UsageRule ofcc2 = mockUsageRule(preventiveInstant, UsageMethod.AVAILABLE, null, null, Country.ES, null, null);
        assertTrue(comparator.compare(ofcc1, ofcc2) > 0);
        assertTrue(comparator.compare(ofcc2, ofcc1) < 0);

        UsageRule oac1 = mockUsageRule(autoInstant, UsageMethod.AVAILABLE, null, null, null, "BBB", null);
        UsageRule oac2 = mockUsageRule(autoInstant, UsageMethod.AVAILABLE, null, null, null, "AAA", null);
        assertTrue(comparator.compare(oac1, oac2) > 0);
        assertTrue(comparator.compare(oac2, oac1) < 0);

        UsageRule ovc1 = mockUsageRule(curativeInstant, UsageMethod.FORCED, null, null, null, null, "z");
        UsageRule ovc2 = mockUsageRule(curativeInstant, UsageMethod.FORCED, null, null, null, null, "x");
        assertTrue(comparator.compare(ovc1, ovc2) > 0);
        assertTrue(comparator.compare(ovc2, ovc1) < 0);

        assertTrue(comparator.compare(oi1, ocs1) > 0);
        assertTrue(comparator.compare(ocs1, ofc1) > 0);
        assertTrue(comparator.compare(ofc2, ofcc1) < 0);
        assertTrue(comparator.compare(oac1, ocs2) < 0);
        assertTrue(comparator.compare(oac1, ovc2) >= 0);
    }

    @Test
    void testSerializeInstantKind() {
        assertEquals("PREVENTIVE", seralizeInstantKind(InstantKind.PREVENTIVE));
        assertEquals("OUTAGE", seralizeInstantKind(InstantKind.OUTAGE));
        assertEquals("AUTO", seralizeInstantKind(InstantKind.AUTO));
        assertEquals("CURATIVE", seralizeInstantKind(InstantKind.CURATIVE));
    }

    @Test
    void testDeserializeInstantKind() {
        assertEquals(InstantKind.PREVENTIVE, deseralizeInstantKind("PREVENTIVE"));
        assertEquals(InstantKind.OUTAGE, deseralizeInstantKind("OUTAGE"));
        assertEquals(InstantKind.AUTO, deseralizeInstantKind("AUTO"));
        assertEquals(InstantKind.CURATIVE, deseralizeInstantKind("CURATIVE"));
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> deseralizeInstantKind("toto"));
        assertEquals("Unrecognized instant kind toto", exception.getMessage());
    }
}
