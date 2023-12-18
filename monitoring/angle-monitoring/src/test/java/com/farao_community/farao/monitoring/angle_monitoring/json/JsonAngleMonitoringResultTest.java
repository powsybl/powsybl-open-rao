/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.monitoring.angle_monitoring.json;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.*;
import com.powsybl.open_rao.data.crac_api.cnec.AngleCnec;

import com.powsybl.open_rao.data.crac_api.network_action.NetworkAction;
import com.powsybl.open_rao.monitoring.angle_monitoring.AngleMonitoringResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class JsonAngleMonitoringResultTest {
    private static final double ANGLE_TOLERANCE = 0.5;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    Crac crac;
    AngleCnec ac1;
    AngleCnec ac2;
    NetworkAction na1;
    NetworkAction na2;
    State preventiveState;
    Contingency co1;
    AngleMonitoringResultImporter angleMonitoringResultImporter;
    private Instant curativeInstant;

    public static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of("nok1", "Instant, CNEC ID and quantity must be defined in angle-cnec-quantities-in-degrees"),
            Arguments.of("nok2", "Instant, CNEC ID and quantity must be defined in angle-cnec-quantities-in-degrees"),
            Arguments.of("nok3", "Instant, CNEC ID and quantity must be defined in angle-cnec-quantities-in-degrees"),
            Arguments.of("nok4", "No contingency defined with instant curative"),
            Arguments.of("nok5", "Angle values for AngleCnec ac1, instant preventive and contingency null are defined more than once"),
            Arguments.of("nok6", "Angle values for AngleCnec ac2, instant curative and contingency co1 are defined more than once"),
            Arguments.of("nok7", "AngleCnec ac3 does not exist in the CRAC"),
            Arguments.of("nok8", "Unexpected field wrong in angle-cnec-quantities-in-degrees"),
            Arguments.of("nok9", "Unexpected field wrong in remedial-actions"),
            Arguments.of("nok10", "Unexpected field wrong in ANGLE_MONITORING_RESULT"),
            Arguments.of("nok11", "Type of document must be specified at the beginning as ANGLE_MONITORING_RESULT"),
            Arguments.of("nok12", "Type of document must be specified at the beginning as ANGLE_MONITORING_RESULT"),
            Arguments.of("nok13", "Status must be specified right after type of document."),
            Arguments.of("nok14", "Unhandled status : UNHANDLED_STATUS"),
            Arguments.of("nok15", "State with instant preventive and contingency null has previously been defined in remedial-actions"),
            Arguments.of("nok16", "State with instant curative and contingency co1 has previously been defined in remedial-actions"),
            Arguments.of("nok17", "No contingency defined with instant curative"),
            Arguments.of("nok18", "State with instant auto and contingency co1 does not exist in CRAC"),
            Arguments.of("nok19", "Instant 'unhandled_instant' has not been defined")
        );
    }

    @BeforeEach
    public void setUp() {
        crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        co1 = crac.newContingency().withId("co1").withNetworkElement("co1-ne").add();
        ac1 = addAngleCnec("ac1", "impNe1", "expNe1", PREVENTIVE_INSTANT_ID, null, 145., 150.);
        ac2 = addAngleCnec("ac2", "impNe2", "expNe2", CURATIVE_INSTANT_ID, co1.getId(), 140., 145.);
        preventiveState = crac.getPreventiveState();
        na1 = crac.newNetworkAction()
                .withId("na1")
                .newInjectionSetPoint().withNetworkElement("ne1").withSetpoint(50.).withUnit(Unit.MEGAWATT).add()
                .newOnAngleConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withAngleCnec(ac1.getId()).add()
                .add();
        na2 = crac.newNetworkAction()
                .withId("na2")
                .newInjectionSetPoint().withNetworkElement("ne2").withSetpoint(150.).withUnit(Unit.MEGAWATT).add()
                .newOnAngleConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withAngleCnec(ac2.getId()).add()
                .add();
        angleMonitoringResultImporter = new AngleMonitoringResultImporter();
    }

    private AngleCnec addAngleCnec(String id, String importingNetworkElement, String exportingNetworkElement, String instantId, String contingencyId, Double min, Double max) {
        if (Objects.isNull(contingencyId)) {
            return crac.newAngleCnec()
                    .withId(id)
                    .withInstant(instantId)
                    .withImportingNetworkElement(importingNetworkElement)
                    .withExportingNetworkElement(exportingNetworkElement)
                    .withMonitored()
                    .newThreshold().withUnit(Unit.DEGREE).withMin(min).withMax(max).add()
                    .add();
        } else {
            return crac.newAngleCnec()
                .withId(id)
                .withInstant(instantId)
                .withContingency(contingencyId)
                .withImportingNetworkElement(importingNetworkElement)
                .withExportingNetworkElement(exportingNetworkElement)
                .withMonitored()
                .newThreshold().withUnit(Unit.DEGREE).withMin(min).withMax(max).add()
                .add();
        }
    }

    @Test
    void testRoundTrip() throws IOException {
        AngleMonitoringResult angleMonitoringResult =
            new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/result-roundTrip.json"), crac);
        assertEquals("SECURE", angleMonitoringResult.getStatus().toString());
        assertEquals(Set.of("na1"), angleMonitoringResult.getAppliedCras(preventiveState).stream().map(NetworkAction::getId).collect(Collectors.toSet()));
        assertEquals(2, angleMonitoringResult.getAppliedCras().keySet().size());
        assertEquals(1, angleMonitoringResult.getAppliedCras().get(preventiveState).size());
        assertEquals(1, angleMonitoringResult.getAppliedCras().get(crac.getState(co1.getId(), curativeInstant)).size());
        assertEquals(2, angleMonitoringResult.getAngleCnecsWithAngle().size());
        Set<AngleMonitoringResult.AngleResult> expectedResult = Set.of(new AngleMonitoringResult.AngleResult(ac1, 2.3), new AngleMonitoringResult.AngleResult(ac2, 4.6));
        angleMonitoringResult.getAngleCnecsWithAngle().forEach(angleResult ->
                assertTrue(expectedResult.stream().anyMatch(exRes -> compareAngleResults(exRes, angleResult))));
        OutputStream os = new ByteArrayOutputStream();
        new AngleMonitoringResultExporter().export(angleMonitoringResult, os);
        String expected = new String(getClass().getResourceAsStream("/result-roundTrip.json").readAllBytes());
        assertEquals(expected.replaceAll("\r", ""), os.toString().replaceAll("\r", ""));
    }

    private boolean compareAngleResults(AngleMonitoringResult.AngleResult ar1, AngleMonitoringResult.AngleResult ar2) {
        return ar1.getAngleCnec().equals(ar2.getAngleCnec()) && ar1.getState().equals(ar2.getState())
                && Math.abs(ar1.getAngle() - ar2.getAngle()) < ANGLE_TOLERANCE;
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void importNokTest(String source, String message) {
        InputStream inputStream = getClass().getResourceAsStream("/result-" + source + ".json");
        FaraoException exception = assertThrows(FaraoException.class, () -> angleMonitoringResultImporter.importAngleMonitoringResult(inputStream, crac));
        assertEquals(message, exception.getMessage());
    }
}
