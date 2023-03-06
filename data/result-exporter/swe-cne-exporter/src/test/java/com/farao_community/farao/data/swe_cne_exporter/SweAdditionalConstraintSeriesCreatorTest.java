/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.AngleCnecCreationContext;
import com.farao_community.farao.data.swe_cne_exporter.xsd.AdditionalConstraintSeries;
import com.farao_community.farao.monitoring.angle_monitoring.AngleMonitoringResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class SweAdditionalConstraintSeriesCreatorTest {

    private SweCneHelper sweCneHelper;
    private Crac crac;
    private AngleMonitoringResult angleMonitoringResult;
    private CimCracCreationContext cracCreationContext;

    @Before
    public void setup() {
        this.crac = Mockito.mock(Crac.class);
        this.angleMonitoringResult = Mockito.mock(AngleMonitoringResult.class);
        this.cracCreationContext = Mockito.mock(CimCracCreationContext.class);
        this.sweCneHelper = Mockito.mock(SweCneHelper.class);

        Mockito.when(sweCneHelper.getCrac()).thenReturn(crac);
        Mockito.when(sweCneHelper.getAngleMonitoringResult()).thenReturn(angleMonitoringResult);
    }

    private SweAdditionalConstraintSeriesCreator setUpAngleCnecs(Contingency contingency) {
        Mockito.when(contingency.getId()).thenReturn("contingency");
        AngleCnecCreationContext acc1 = createAdcs("AngleCnecId1", "contingency");
        AngleCnecCreationContext acc2 = createAdcs("AngleCnecId2", "contingency");
        AngleCnec angleCnec1 = Mockito.mock(AngleCnec.class);
        AngleCnec angleCnec2 = Mockito.mock(AngleCnec.class);
        Mockito.when(crac.getAngleCnec(acc1.getCreatedCnecId())).thenReturn(angleCnec1);
        Mockito.when(crac.getAngleCnec(acc2.getCreatedCnecId())).thenReturn(angleCnec2);
        Mockito.when(angleCnec1.getName()).thenReturn("AngleCnecName1");
        Mockito.when(angleCnec2.getName()).thenReturn("AngleCnecName2");
        Mockito.when(angleMonitoringResult.getAngle(angleCnec1, Unit.DEGREE)).thenReturn(1.37);
        Mockito.when(angleMonitoringResult.getAngle(angleCnec2, Unit.DEGREE)).thenReturn(-21.34);
        Mockito.when(cracCreationContext.getAngleCnecCreationContexts()).thenReturn(Set.of(acc1, acc2));
        State curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(Instant.CURATIVE);
        Mockito.when(angleCnec1.getState()).thenReturn(curativeState);
        Mockito.when(angleCnec2.getState()).thenReturn(curativeState);
        return new SweAdditionalConstraintSeriesCreator(sweCneHelper, cracCreationContext);
    }

    @Test
    public void generatePreventiveAdditionalConstraintSeriesTest() {
        AngleCnecCreationContext accPrev = createAdcs("AngleCnecIdPrev", null);
        AngleCnec angleCnecPrev = Mockito.mock(AngleCnec.class);
        Mockito.when(crac.getAngleCnec(accPrev.getCreatedCnecId())).thenReturn(angleCnecPrev);
        Mockito.when(angleCnecPrev.getName()).thenReturn("AngleCnecNamePrev");
        State prevState = Mockito.mock(State.class);
        Mockito.when(prevState.getInstant()).thenReturn(Instant.PREVENTIVE);
        Mockito.when(angleCnecPrev.getState()).thenReturn(prevState);
        Mockito.when(cracCreationContext.getAngleCnecCreationContexts()).thenReturn(Set.of(accPrev));
        SweAdditionalConstraintSeriesCreator additionalConstraintSeriesCreator = new SweAdditionalConstraintSeriesCreator(sweCneHelper, cracCreationContext);
        List<AdditionalConstraintSeries> basecaseAngleSeries = additionalConstraintSeriesCreator.generateAdditionalConstraintSeries(null);
        assertEquals(0, basecaseAngleSeries.size());
    }

    @Test
    public void generateWrongPostContingencyAdditionalConstraintSeriesTest() {
        AngleCnecCreationContext accOutage = createAdcs("AngleCnecIdOutage", "contingency");
        AngleCnecCreationContext accAuto = createAdcs("AngleCnecIdAuto", "contingency");
        AngleCnec angleCnecOutage = Mockito.mock(AngleCnec.class);
        AngleCnec angleCnecAuto = Mockito.mock(AngleCnec.class);
        Mockito.when(crac.getAngleCnec(accOutage.getCreatedCnecId())).thenReturn(angleCnecOutage);
        Mockito.when(crac.getAngleCnec(accAuto.getCreatedCnecId())).thenReturn(angleCnecAuto);
        Mockito.when(angleCnecOutage.getName()).thenReturn("AngleCnecNameOutage");
        State outageState = Mockito.mock(State.class);
        Mockito.when(outageState.getInstant()).thenReturn(Instant.OUTAGE);
        Mockito.when(angleCnecOutage.getState()).thenReturn(outageState);
        Mockito.when(angleCnecAuto.getName()).thenReturn("AngleCnecNameAuto");
        State autoState = Mockito.mock(State.class);
        Mockito.when(autoState.getInstant()).thenReturn(Instant.AUTO);
        Mockito.when(angleCnecAuto.getState()).thenReturn(autoState);
        Mockito.when(cracCreationContext.getAngleCnecCreationContexts()).thenReturn(Set.of(accOutage, accAuto));
        SweAdditionalConstraintSeriesCreator additionalConstraintSeriesCreator = new SweAdditionalConstraintSeriesCreator(sweCneHelper, cracCreationContext);
        Contingency contingency = Mockito.mock(Contingency.class);
        Mockito.when(contingency.getId()).thenReturn("contingency");
        List<AdditionalConstraintSeries> otherAngleSeries = additionalConstraintSeriesCreator.generateAdditionalConstraintSeries(contingency);
        assertEquals(0, otherAngleSeries.size());
    }

    @Test
    public void generateContingencyAdditionalConstraintSeriesTest() {
        Contingency contingency = Mockito.mock(Contingency.class);
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
    public void generateContingencyAdditionalConstraintSeriesWithDivergentAngleMonitoringTest() {
        Contingency contingency = Mockito.mock(Contingency.class);
        Mockito.when(angleMonitoringResult.isDivergent()).thenReturn(true);
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
        AngleCnecCreationContext acc = Mockito.mock(AngleCnecCreationContext.class);
        Mockito.when(acc.getCreatedCnecId()).thenReturn(nativeId);
        Mockito.when(acc.getContingencyId()).thenReturn(contingencyId);
        Mockito.when(acc.isImported()).thenReturn(true);
        return acc;
    }
}
