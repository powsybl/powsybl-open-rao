/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.AngleCnecCreationContext;
import com.farao_community.farao.data.swe_cne_exporter.xsd.AdditionalConstraintSeries;
import com.farao_community.farao.monitoring.angle_monitoring.AngleMonitoringResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class SweAdditionalConstraintSeriesCreatorTest {

    private SweCneHelper sweCneHelper;
    private Crac crac;
    private AngleMonitoringResult angleMonitoringResult;
    private CimCracCreationContext cracCreationContext;
    private Instant preventiveInstant;
    private Instant outageInstant;
    private Instant autoInstant;
    private Instant curativeInstant;

    @BeforeEach
    public void setup() {
        this.crac = mock(Crac.class);
        preventiveInstant = mock(Instant.class);
        outageInstant = mock(Instant.class);
        autoInstant = mock(Instant.class);
        curativeInstant = mock(Instant.class);
        when(curativeInstant.isCurative()).thenReturn(true);
        this.angleMonitoringResult = mock(AngleMonitoringResult.class);
        this.cracCreationContext = mock(CimCracCreationContext.class);
        this.sweCneHelper = mock(SweCneHelper.class);

        when(sweCneHelper.getCrac()).thenReturn(crac);
        when(sweCneHelper.getAngleMonitoringResult()).thenReturn(angleMonitoringResult);
    }

    private SweAdditionalConstraintSeriesCreator setUpAngleCnecs(Contingency contingency) {
        when(contingency.getId()).thenReturn("contingency");
        AngleCnecCreationContext acc1 = createAdcs("AngleCnecId1", "contingency");
        AngleCnecCreationContext acc2 = createAdcs("AngleCnecId2", "contingency");
        AngleCnec angleCnec1 = mock(AngleCnec.class);
        AngleCnec angleCnec2 = mock(AngleCnec.class);
        when(crac.getAngleCnec(acc1.getCreatedCnecId())).thenReturn(angleCnec1);
        when(crac.getAngleCnec(acc2.getCreatedCnecId())).thenReturn(angleCnec2);
        when(angleCnec1.getName()).thenReturn("AngleCnecName1");
        when(angleCnec2.getName()).thenReturn("AngleCnecName2");
        when(angleMonitoringResult.getAngle(angleCnec1, Unit.DEGREE)).thenReturn(1.37);
        when(angleMonitoringResult.getAngle(angleCnec2, Unit.DEGREE)).thenReturn(-21.34);
        when(cracCreationContext.getAngleCnecCreationContexts()).thenReturn(Set.of(acc1, acc2));
        State curativeState = mock(State.class);
        when(curativeState.getInstant()).thenReturn(curativeInstant);
        when(angleCnec1.getState()).thenReturn(curativeState);
        when(angleCnec2.getState()).thenReturn(curativeState);
        return new SweAdditionalConstraintSeriesCreator(sweCneHelper, cracCreationContext);
    }

    @Test
    void generatePreventiveAdditionalConstraintSeriesTest() {
        AngleCnecCreationContext accPrev = createAdcs("AngleCnecIdPrev", null);
        AngleCnec angleCnecPrev = mock(AngleCnec.class);
        when(crac.getAngleCnec(accPrev.getCreatedCnecId())).thenReturn(angleCnecPrev);
        when(angleCnecPrev.getName()).thenReturn("AngleCnecNamePrev");
        State prevState = mock(State.class);
        when(prevState.getInstant()).thenReturn(preventiveInstant);
        when(angleCnecPrev.getState()).thenReturn(prevState);
        when(cracCreationContext.getAngleCnecCreationContexts()).thenReturn(Set.of(accPrev));
        SweAdditionalConstraintSeriesCreator additionalConstraintSeriesCreator = new SweAdditionalConstraintSeriesCreator(sweCneHelper, cracCreationContext);
        List<AdditionalConstraintSeries> basecaseAngleSeries = additionalConstraintSeriesCreator.generateAdditionalConstraintSeries(null);
        assertEquals(0, basecaseAngleSeries.size());
    }

    @Test
    void generateWrongPostContingencyAdditionalConstraintSeriesTest() {
        AngleCnecCreationContext accOutage = createAdcs("AngleCnecIdOutage", "contingency");
        AngleCnecCreationContext accAuto = createAdcs("AngleCnecIdAuto", "contingency");
        AngleCnec angleCnecOutage = mock(AngleCnec.class);
        AngleCnec angleCnecAuto = mock(AngleCnec.class);
        when(crac.getAngleCnec(accOutage.getCreatedCnecId())).thenReturn(angleCnecOutage);
        when(crac.getAngleCnec(accAuto.getCreatedCnecId())).thenReturn(angleCnecAuto);
        when(angleCnecOutage.getName()).thenReturn("AngleCnecNameOutage");
        State outageState = mock(State.class);
        when(outageState.getInstant()).thenReturn(outageInstant);
        when(angleCnecOutage.getState()).thenReturn(outageState);
        when(angleCnecAuto.getName()).thenReturn("AngleCnecNameAuto");
        State autoState = mock(State.class);
        when(autoState.getInstant()).thenReturn(autoInstant);
        when(angleCnecAuto.getState()).thenReturn(autoState);
        when(cracCreationContext.getAngleCnecCreationContexts()).thenReturn(Set.of(accOutage, accAuto));
        SweAdditionalConstraintSeriesCreator additionalConstraintSeriesCreator = new SweAdditionalConstraintSeriesCreator(sweCneHelper, cracCreationContext);
        Contingency contingency = mock(Contingency.class);
        when(contingency.getId()).thenReturn("contingency");
        List<AdditionalConstraintSeries> otherAngleSeries = additionalConstraintSeriesCreator.generateAdditionalConstraintSeries(contingency);
        assertEquals(0, otherAngleSeries.size());
    }

    @Test
    void generateContingencyAdditionalConstraintSeriesTest() {
        Contingency contingency = mock(Contingency.class);
        SweAdditionalConstraintSeriesCreator additionalConstraintSeriesCreator = setUpAngleCnecs(contingency);
        List<AdditionalConstraintSeries> contingencyAngleSeries = additionalConstraintSeriesCreator.generateAdditionalConstraintSeries(contingency);
        assertEquals(2, contingencyAngleSeries.size());
        assertEquals("AngleCnecId1", contingencyAngleSeries.get(0).getMRID());
        assertEquals("AngleCnecName1", contingencyAngleSeries.get(0).getName());
        assertEquals("B87", contingencyAngleSeries.get(0).getBusinessType());
        assertEquals(1.4, contingencyAngleSeries.get(0).getQuantityQuantity().doubleValue(), 1.0E-6);
        assertEquals("AngleCnecId2", contingencyAngleSeries.get(1).getMRID());
        assertEquals("AngleCnecName2", contingencyAngleSeries.get(1).getName());
        assertEquals("B87", contingencyAngleSeries.get(1).getBusinessType());
        assertEquals(-21.3, contingencyAngleSeries.get(1).getQuantityQuantity().doubleValue(), 1.0E-6);
    }

    @Test
    void generateContingencyAdditionalConstraintSeriesWithDivergentAngleMonitoringTest() {
        Contingency contingency = mock(Contingency.class);
        when(angleMonitoringResult.isDivergent()).thenReturn(true);
        setUpAngleCnecs(contingency);
        SweAdditionalConstraintSeriesCreator additionalConstraintSeriesCreator = new SweAdditionalConstraintSeriesCreator(sweCneHelper, cracCreationContext);
        List<AdditionalConstraintSeries> contingencyAngleSeries = additionalConstraintSeriesCreator.generateAdditionalConstraintSeries(contingency);
        assertEquals(2, contingencyAngleSeries.size());
        assertEquals("AngleCnecId1", contingencyAngleSeries.get(0).getMRID());
        assertEquals("AngleCnecName1", contingencyAngleSeries.get(0).getName());
        assertEquals("B87", contingencyAngleSeries.get(0).getBusinessType());
        assertNull(contingencyAngleSeries.get(0).getQuantityQuantity());
        assertEquals("AngleCnecId2", contingencyAngleSeries.get(1).getMRID());
        assertEquals("AngleCnecName2", contingencyAngleSeries.get(1).getName());
        assertEquals("B87", contingencyAngleSeries.get(1).getBusinessType());
        assertNull(contingencyAngleSeries.get(1).getQuantityQuantity());
    }

    private AngleCnecCreationContext createAdcs(String nativeId, String contingencyId) {
        AngleCnecCreationContext acc = mock(AngleCnecCreationContext.class);
        when(acc.getCreatedCnecId()).thenReturn(nativeId);
        when(acc.getContingencyId()).thenReturn(contingencyId);
        when(acc.isImported()).thenReturn(true);
        return acc;
    }
}
