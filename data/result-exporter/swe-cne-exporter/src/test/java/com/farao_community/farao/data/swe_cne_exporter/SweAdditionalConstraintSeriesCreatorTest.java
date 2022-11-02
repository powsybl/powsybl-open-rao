/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.data.crac_api.*;
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

    @Test
    public void generatePreventiveAdditionalConstraintSeriesTest() {
        AngleCnecCreationContext acc3 = createAdcs("AngleCnecId3", null);
        AngleCnecCreationContext acc4 = createAdcs("AngleCnecId4", null);
        AngleCnec angleCnec3 = Mockito.mock(AngleCnec.class);
        AngleCnec angleCnec4 = Mockito.mock(AngleCnec.class);
        Mockito.when(crac.getAngleCnec(acc3.getNativeId())).thenReturn(angleCnec3);
        Mockito.when(crac.getAngleCnec(acc4.getNativeId())).thenReturn(angleCnec4);
        Mockito.when(angleCnec3.getName()).thenReturn("AngleCnecName3");
        Mockito.when(angleCnec4.getName()).thenReturn("AngleCnecName4");
        Mockito.when(cracCreationContext.getAngleCnecCreationContexts()).thenReturn(Set.of(acc3, acc4));
        SweAdditionalConstraintSeriesCreator additionalConstraintSeriesCreator = new SweAdditionalConstraintSeriesCreator(sweCneHelper, cracCreationContext);
        List<AdditionalConstraintSeries> basecaseAngleSeries = additionalConstraintSeriesCreator.generateAdditionalConstraintSeries(null);
        assertEquals(2, basecaseAngleSeries.size());
    }

    @Test
    public void generateContingencyAdditionalConstraintSeriesTest() {
        Contingency contingency = Mockito.mock(Contingency.class);
        Mockito.when(contingency.getId()).thenReturn("contingency");
        AngleCnecCreationContext acc1 = createAdcs("AngleCnecId1", "contingency");
        AngleCnecCreationContext acc2 = createAdcs("AngleCnecId2", "contingency");
        AngleCnec angleCnec1 = Mockito.mock(AngleCnec.class);
        AngleCnec angleCnec2 = Mockito.mock(AngleCnec.class);
        Mockito.when(crac.getAngleCnec(acc1.getNativeId())).thenReturn(angleCnec1);
        Mockito.when(crac.getAngleCnec(acc2.getNativeId())).thenReturn(angleCnec2);
        Mockito.when(angleCnec1.getName()).thenReturn("AngleCnecName1");
        Mockito.when(angleCnec2.getName()).thenReturn("AngleCnecName2");
        Mockito.when(cracCreationContext.getAngleCnecCreationContexts()).thenReturn(Set.of(acc1, acc2));
        SweAdditionalConstraintSeriesCreator additionalConstraintSeriesCreator = new SweAdditionalConstraintSeriesCreator(sweCneHelper, cracCreationContext);
        List<AdditionalConstraintSeries> contingencyAngleSeries = additionalConstraintSeriesCreator.generateAdditionalConstraintSeries(contingency);
        assertEquals(2, contingencyAngleSeries.size());
    }

    private AngleCnecCreationContext createAdcs(String nativeId, String contingencyId) {
        AngleCnecCreationContext acc = Mockito.mock(AngleCnecCreationContext.class);
        Mockito.when(acc.getNativeId()).thenReturn(nativeId);
        Mockito.when(acc.getContingencyId()).thenReturn(contingencyId);
        Mockito.when(acc.isImported()).thenReturn(true);
        return acc;
    }
}
