/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craciojson;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.threshold.BranchThreshold;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerCondition;
import com.powsybl.openrao.data.cracapi.triggercondition.UsageMethod;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

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

    private TriggerCondition mockTriggerCondition(Instant instant, UsageMethod usageMethod, String contingencyId, String flowCnecId, String angleCnecId, String voltageCnecId, Country country) {
        TriggerCondition triggerCondition = Mockito.mock(TriggerCondition.class);
        Mockito.when(triggerCondition.getInstant()).thenReturn(instant);
        Mockito.when(triggerCondition.getUsageMethod()).thenReturn(usageMethod);

        if (contingencyId != null) {
            Contingency contingency = Mockito.mock(Contingency.class);
            Mockito.when(contingency.getId()).thenReturn(contingencyId);
            Mockito.when(triggerCondition.getContingency()).thenReturn(Optional.of(contingency));
        } else {
            Mockito.when(triggerCondition.getContingency()).thenReturn(Optional.empty());
        }

        Mockito.when(triggerCondition.getCountry()).thenReturn(Optional.ofNullable(country));

        if (flowCnecId != null) {
            FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
            Mockito.when(flowCnec.getId()).thenReturn(flowCnecId);
            Mockito.when(triggerCondition.getCnec()).thenReturn(Optional.of(flowCnec));
        } else if (angleCnecId != null) {
            AngleCnec angleCnec = Mockito.mock(AngleCnec.class);
            Mockito.when(angleCnec.getId()).thenReturn(angleCnecId);
            Mockito.when(triggerCondition.getCnec()).thenReturn(Optional.of(angleCnec));
        } else if (voltageCnecId != null) {
            VoltageCnec voltageCnec = Mockito.mock(VoltageCnec.class);
            Mockito.when(voltageCnec.getId()).thenReturn(voltageCnecId);
            Mockito.when(triggerCondition.getCnec()).thenReturn(Optional.of(voltageCnec));
        } else {
            Mockito.when(triggerCondition.getCnec()).thenReturn(Optional.empty());
        }

        return triggerCondition;
    }

    @Test
    void testCompareTriggerConditions() {
        Instant preventiveInstant = mock(Instant.class);
        Instant curativeInstant = mock(Instant.class);
        when(preventiveInstant.comesBefore(any())).thenReturn(true);

        TriggerCondition availablePrev = mockTriggerCondition(preventiveInstant, UsageMethod.AVAILABLE, null, null, null, null, null);
        TriggerCondition forcedPrev = mockTriggerCondition(preventiveInstant, UsageMethod.FORCED, null, null, null, null, null);
        TriggerCondition availableCur = mockTriggerCondition(curativeInstant, UsageMethod.AVAILABLE, null, null, null, null, null);
        TriggerCondition forcedCur = mockTriggerCondition(curativeInstant, UsageMethod.FORCED, null, null, null, null, null);

        TriggerCondition availableCo1 = mockTriggerCondition(curativeInstant, UsageMethod.AVAILABLE, "co1", null, null, null, null);
        TriggerCondition forcedCo1 = mockTriggerCondition(curativeInstant, UsageMethod.FORCED, "co1", null, null, null, null);
        TriggerCondition availableCo2 = mockTriggerCondition(curativeInstant, UsageMethod.AVAILABLE, "co1", null, null, null, null);
        TriggerCondition forcedCo2 = mockTriggerCondition(curativeInstant, UsageMethod.FORCED, "co2", null, null, null, null);

        TriggerCondition availableFrPrev = mockTriggerCondition(preventiveInstant, UsageMethod.AVAILABLE, null, null, null, null, Country.FR);
        TriggerCondition forcedFrPrev = mockTriggerCondition(preventiveInstant, UsageMethod.FORCED, null, null, null, null, Country.FR);
        TriggerCondition availableFrCur = mockTriggerCondition(curativeInstant, UsageMethod.AVAILABLE, null, null, null, null, Country.FR);
        TriggerCondition forcedFrCur = mockTriggerCondition(curativeInstant, UsageMethod.FORCED, null, null, null, null, Country.FR);
        TriggerCondition availableBePrev = mockTriggerCondition(preventiveInstant, UsageMethod.AVAILABLE, null, null, null, null, Country.BE);
        TriggerCondition forcedBePrev = mockTriggerCondition(preventiveInstant, UsageMethod.FORCED, null, null, null, null, Country.BE);
        TriggerCondition availableBeCur = mockTriggerCondition(curativeInstant, UsageMethod.AVAILABLE, null, null, null, null, Country.BE);
        TriggerCondition forcedBeCur = mockTriggerCondition(curativeInstant, UsageMethod.FORCED, null, null, null, null, Country.BE);

        TriggerCondition availableCo1Fr = mockTriggerCondition(curativeInstant, UsageMethod.AVAILABLE, "co1", null, null, null, Country.FR);
        TriggerCondition forcedCo1Fr = mockTriggerCondition(curativeInstant, UsageMethod.FORCED, "co1", null, null, null, Country.FR);
        TriggerCondition availableCo2Fr = mockTriggerCondition(curativeInstant, UsageMethod.AVAILABLE, "co1", null, null, null, Country.FR);
        TriggerCondition forcedCo2Fr = mockTriggerCondition(curativeInstant, UsageMethod.FORCED, "co2", null, null, null, Country.FR);
        TriggerCondition availableCo1Be = mockTriggerCondition(curativeInstant, UsageMethod.AVAILABLE, "co1", null, null, null, Country.BE);
        TriggerCondition forcedCo1Be = mockTriggerCondition(curativeInstant, UsageMethod.FORCED, "co1", null, null, null, Country.BE);
        TriggerCondition availableCo2Be = mockTriggerCondition(curativeInstant, UsageMethod.AVAILABLE, "co1", null, null, null, Country.BE);
        TriggerCondition forcedCo2Be = mockTriggerCondition(curativeInstant, UsageMethod.FORCED, "co2", null, null, null, Country.BE);

        TriggerCondition availablePrevFlow = mockTriggerCondition(preventiveInstant, UsageMethod.AVAILABLE, null, "flowCnec", null, null, null);
        TriggerCondition forcedPrevFlow = mockTriggerCondition(preventiveInstant, UsageMethod.FORCED, null, "flowCnec", null, null, null);
        TriggerCondition availableCurFlow = mockTriggerCondition(curativeInstant, UsageMethod.AVAILABLE, null, "flowCnec", null, null, null);
        TriggerCondition forcedCurFlow = mockTriggerCondition(curativeInstant, UsageMethod.FORCED, null, "flowCnec", null, null, null);
        TriggerCondition availablePrevAngle = mockTriggerCondition(preventiveInstant, UsageMethod.AVAILABLE, null, null, "angleCnec", null, null);
        TriggerCondition forcedPrevAngle = mockTriggerCondition(preventiveInstant, UsageMethod.FORCED, null, null, "angleCnec", null, null);
        TriggerCondition availableCurAngle = mockTriggerCondition(curativeInstant, UsageMethod.AVAILABLE, null, null, "angleCnec", null, null);
        TriggerCondition forcedCurAngle = mockTriggerCondition(curativeInstant, UsageMethod.FORCED, null, null, "angleCnec", null, null);
        TriggerCondition availablePrevVoltage = mockTriggerCondition(preventiveInstant, UsageMethod.AVAILABLE, null, null, null, "voltageCnec", null);
        TriggerCondition forcedPrevVoltage = mockTriggerCondition(preventiveInstant, UsageMethod.FORCED, null, null, null, "voltageCnec", null);
        TriggerCondition availableCurVoltage = mockTriggerCondition(curativeInstant, UsageMethod.AVAILABLE, null, null, null, "voltageCnec", null);
        TriggerCondition forcedCurVoltage = mockTriggerCondition(curativeInstant, UsageMethod.FORCED, null, null, null, "voltageCnec", null);

        TriggerConditionComparator comparator = new TriggerConditionComparator();

        assertEquals(0, comparator.compare(availablePrev, availablePrev));
        assertEquals(-1, comparator.compare(availablePrev, availableCur));
        assertEquals(-1, comparator.compare(availableCur, availableCo1));

        // TODO: complete with more exhaustive cases
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
