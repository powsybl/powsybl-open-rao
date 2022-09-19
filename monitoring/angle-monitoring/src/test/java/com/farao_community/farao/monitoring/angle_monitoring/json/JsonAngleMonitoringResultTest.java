/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.angle_monitoring.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;

import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.monitoring.angle_monitoring.AngleMonitoringResult;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class JsonAngleMonitoringResultTest {
    private static final double ANGLE_TOLERANCE = 0.5;

    Crac crac;
    AngleCnec ac1;
    AngleCnec ac2;
    NetworkAction na1;
    NetworkAction na2;
    State preventiveState;
    Contingency co1;

    @Before
    public void setUp() {
        crac = CracFactory.findDefault().create("test-crac");
        co1 = crac.newContingency().withId("co1").withNetworkElement("co1-ne").add();
        ac1 = addAngleCnec("ac1", "impNe1", "expNe1", Instant.PREVENTIVE, null, 145., 150.);
        ac2 = addAngleCnec("ac2", "impNe2", "expNe2", Instant.CURATIVE, co1.getId(), 140., 145.);
        preventiveState = crac.getPreventiveState();
        na1 = crac.newNetworkAction()
                .withId("na1")
                .newInjectionSetPoint().withNetworkElement("ne1").withSetpoint(50.).add()
                .newOnAngleConstraintUsageRule().withInstant(Instant.PREVENTIVE).withAngleCnec(ac1.getId()).add()
                .add();
        na2 = crac.newNetworkAction()
                .withId("na2")
                .newInjectionSetPoint().withNetworkElement("ne2").withSetpoint(150.).add()
                .newOnAngleConstraintUsageRule().withInstant(Instant.CURATIVE).withAngleCnec(ac2.getId()).add()
                .add();
    }

    private AngleCnec addAngleCnec(String id, String importingNetworkElement, String exportingNetworkElement, Instant instant, String contingencyId, Double min, Double max) {
        if (Objects.isNull(contingencyId)) {
            return crac.newAngleCnec()
                    .withId(id)
                    .withInstant(instant)
                    .withImportingNetworkElement(importingNetworkElement)
                    .withExportingNetworkElement(exportingNetworkElement)
                    .withMonitored()
                    .newThreshold().withUnit(Unit.DEGREE).withMin(min).withMax(max).add()
                    .add();
        } else {
            return crac.newAngleCnec()
                .withId(id)
                .withInstant(instant)
                .withContingency(contingencyId)
                .withImportingNetworkElement(importingNetworkElement)
                .withExportingNetworkElement(exportingNetworkElement)
                .withMonitored()
                .newThreshold().withUnit(Unit.DEGREE).withMin(min).withMax(max).add()
                .add();
        }
    }

    @Test
    public void testRoundTrip() throws IOException {
        AngleMonitoringResult angleMonitoringResult =
            new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-roundTrip.json"), crac);
        System.out.println(angleMonitoringResult.getAngleCnecsWithAngle().iterator().next().getAngleCnec().getId());
        assertEquals("SECURE", angleMonitoringResult.getStatus().toString());
        assertEquals(Set.of("na1"), angleMonitoringResult.getAppliedCras(preventiveState).stream().map(NetworkAction::getId).collect(Collectors.toSet()));
        assertEquals(2, angleMonitoringResult.getAppliedCras().keySet().size());
        assertEquals(1, angleMonitoringResult.getAppliedCras().get(preventiveState).size());
        assertEquals(1, angleMonitoringResult.getAppliedCras().get(crac.getState(co1.getId(), Instant.CURATIVE)).size());
        assertEquals(2, angleMonitoringResult.getAngleCnecsWithAngle().size());
        Set<AngleMonitoringResult.AngleResult> expectedResult = Set.of(new AngleMonitoringResult.AngleResult(ac1, preventiveState, 2.0), new AngleMonitoringResult.AngleResult(ac2, crac.getState(co1.getId(), Instant.CURATIVE), 4.0));
        angleMonitoringResult.getAngleCnecsWithAngle().forEach(angleResult ->
                assertTrue(expectedResult.stream().anyMatch(exRes -> compareAngleResults(exRes, angleResult))));
        OutputStream os = new ByteArrayOutputStream();
        new AngleMonitoringResultExporter().export(angleMonitoringResult, os);
        String expected = new String(getClass().getResourceAsStream("/result-roundTrip.json").readAllBytes());
        assertEquals(expected, os.toString());
    }

    private boolean compareAngleResults(AngleMonitoringResult.AngleResult ar1, AngleMonitoringResult.AngleResult ar2) {
        return ar1.getAngleCnec().equals(ar2.getAngleCnec()) && ar1.getState().equals(ar2.getState())
                && (Math.abs(ar1.getAngle() - ar2.getAngle()) < ANGLE_TOLERANCE);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfCnecIdNull() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok1.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfQuantityNull() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok2.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfInstantNull() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok3.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfContingencyNull() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok4.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfCnecIdUsedTwiceInPreventive() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok5.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfCnecIdUsedTwiceInCurative() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok6.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfCnecNotInCrac() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok7.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfWrongField1() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok8.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfWrongField2() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok9.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfWrongField3() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok10.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfNoType() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok11.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfWrongType() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok12.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfNoStatus() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok13.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfWrongStatus() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok14.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfRaUsedTwiceInPreventive() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok15.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfRaUsedTwiceInCurative() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok16.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfNoContingencyDefinedInCurative() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok17.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfStateNotInCrac() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok18.json"), crac);
    }

    @Test (expected = FaraoException.class)
    public void testFailsIfInstantUnhandled() {
        new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-nok19.json"), crac);
    }
}
