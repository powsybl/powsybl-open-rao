/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.swe_cne_exporter;

import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.*;
import com.powsybl.open_rao.data.crac_api.cnec.AngleCnec;
import com.powsybl.open_rao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.powsybl.open_rao.data.crac_creation.creator.cim.crac_creator.cnec.AngleCnecCreationContext;
import com.powsybl.open_rao.data.rao_result_api.ComputationStatus;
import com.powsybl.open_rao.data.rao_result_api.RaoResult;
import com.powsybl.open_rao.data.swe_cne_exporter.xsd.AdditionalConstraintSeries;
import com.powsybl.open_rao.monitoring.angle_monitoring.RaoResultWithAngleMonitoring;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class SweAdditionalConstraintSeriesCreatorTest {

    private SweCneHelper sweCneHelper;
    private Crac crac;
    private RaoResult raoResult;
    private CimCracCreationContext cracCreationContext;
    private Instant preventiveInstant;
    private Instant outageInstant;
    private Instant autoInstant;
    private Instant curativeInstant;

    @BeforeEach
    public void setup() {
        this.crac = Mockito.mock(Crac.class);
        this.raoResult = Mockito.mock(RaoResultWithAngleMonitoring.class);
        Mockito.when(raoResult.getComputationStatus()).thenReturn(ComputationStatus.DEFAULT);
        preventiveInstant = Mockito.mock(Instant.class);
        outageInstant = Mockito.mock(Instant.class);
        autoInstant = Mockito.mock(Instant.class);
        curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(crac.getInstant(InstantKind.PREVENTIVE)).thenReturn(preventiveInstant);
        Mockito.when(crac.getInstant(InstantKind.OUTAGE)).thenReturn(outageInstant);
        Mockito.when(crac.getInstant(InstantKind.AUTO)).thenReturn(autoInstant);
        Mockito.when(crac.getInstant(InstantKind.CURATIVE)).thenReturn(curativeInstant);
        Mockito.when(curativeInstant.isCurative()).thenReturn(true);
        this.cracCreationContext = Mockito.mock(CimCracCreationContext.class);
        this.sweCneHelper = Mockito.mock(SweCneHelper.class);

        Mockito.when(sweCneHelper.getCrac()).thenReturn(crac);
        Mockito.when(sweCneHelper.getRaoResult()).thenReturn(raoResult);
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
        Mockito.when(raoResult.getAngle(curativeInstant, angleCnec1, Unit.DEGREE)).thenReturn(1.37);
        Mockito.when(raoResult.getAngle(curativeInstant, angleCnec2, Unit.DEGREE)).thenReturn(-21.34);
        Mockito.when(cracCreationContext.getAngleCnecCreationContexts()).thenReturn(Set.of(acc1, acc2));
        State curativeState = Mockito.mock(State.class);
        Mockito.when(curativeState.getInstant()).thenReturn(curativeInstant);
        Mockito.when(angleCnec1.getState()).thenReturn(curativeState);
        Mockito.when(angleCnec2.getState()).thenReturn(curativeState);
        return new SweAdditionalConstraintSeriesCreator(sweCneHelper, cracCreationContext);
    }

    @Test
    void generatePreventiveAdditionalConstraintSeriesTest() {
        AngleCnecCreationContext accPrev = createAdcs("AngleCnecIdPrev", null);
        AngleCnec angleCnecPrev = Mockito.mock(AngleCnec.class);
        Mockito.when(crac.getAngleCnec(accPrev.getCreatedCnecId())).thenReturn(angleCnecPrev);
        Mockito.when(angleCnecPrev.getName()).thenReturn("AngleCnecNamePrev");
        State prevState = Mockito.mock(State.class);
        Mockito.when(prevState.getInstant()).thenReturn(preventiveInstant);
        Mockito.when(angleCnecPrev.getState()).thenReturn(prevState);
        Mockito.when(cracCreationContext.getAngleCnecCreationContexts()).thenReturn(Set.of(accPrev));
        SweAdditionalConstraintSeriesCreator additionalConstraintSeriesCreator = new SweAdditionalConstraintSeriesCreator(sweCneHelper, cracCreationContext);
        List<AdditionalConstraintSeries> basecaseAngleSeries = additionalConstraintSeriesCreator.generateAdditionalConstraintSeries(null);
        assertEquals(0, basecaseAngleSeries.size());
    }

    @Test
    void generateWrongPostContingencyAdditionalConstraintSeriesTest() {
        AngleCnecCreationContext accOutage = createAdcs("AngleCnecIdOutage", "contingency");
        AngleCnecCreationContext accAuto = createAdcs("AngleCnecIdAuto", "contingency");
        AngleCnec angleCnecOutage = Mockito.mock(AngleCnec.class);
        AngleCnec angleCnecAuto = Mockito.mock(AngleCnec.class);
        Mockito.when(crac.getAngleCnec(accOutage.getCreatedCnecId())).thenReturn(angleCnecOutage);
        Mockito.when(crac.getAngleCnec(accAuto.getCreatedCnecId())).thenReturn(angleCnecAuto);
        Mockito.when(angleCnecOutage.getName()).thenReturn("AngleCnecNameOutage");
        State outageState = Mockito.mock(State.class);
        Mockito.when(outageState.getInstant()).thenReturn(outageInstant);
        Mockito.when(angleCnecOutage.getState()).thenReturn(outageState);
        Mockito.when(angleCnecAuto.getName()).thenReturn("AngleCnecNameAuto");
        State autoState = Mockito.mock(State.class);
        Mockito.when(autoState.getInstant()).thenReturn(autoInstant);
        Mockito.when(angleCnecAuto.getState()).thenReturn(autoState);
        Mockito.when(cracCreationContext.getAngleCnecCreationContexts()).thenReturn(Set.of(accOutage, accAuto));
        SweAdditionalConstraintSeriesCreator additionalConstraintSeriesCreator = new SweAdditionalConstraintSeriesCreator(sweCneHelper, cracCreationContext);
        Contingency contingency = Mockito.mock(Contingency.class);
        Mockito.when(contingency.getId()).thenReturn("contingency");
        List<AdditionalConstraintSeries> otherAngleSeries = additionalConstraintSeriesCreator.generateAdditionalConstraintSeries(contingency);
        assertEquals(0, otherAngleSeries.size());
    }

    @Test
    void generateContingencyAdditionalConstraintSeriesTest() {
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
    void generateContingencyAdditionalConstraintSeriesWithDivergentAngleMonitoringTest() {
        Contingency contingency = Mockito.mock(Contingency.class);
        Mockito.when(raoResult.getComputationStatus()).thenReturn(ComputationStatus.FAILURE);
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
