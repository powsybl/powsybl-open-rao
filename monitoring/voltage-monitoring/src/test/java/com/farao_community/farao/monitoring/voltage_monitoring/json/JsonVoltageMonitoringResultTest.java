/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.voltage_monitoring.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult.Status.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class JsonVoltageMonitoringResultTest {
    private static final double VOLTAGE_TOLERANCE = 0.5;

    Crac crac;
    VoltageCnec vc1;
    VoltageCnec vc2;
    State preventiveState;
    Contingency co1;
    VoltageMonitoringResultImporter voltageMonitoringResultImporter;

    @BeforeEach
    public void setUp() {
        crac = CracFactory.findDefault().create("test-crac");
        crac.addInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.addInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.addInstant("auto", InstantKind.AUTO, "outage");
        crac.addInstant("curative", InstantKind.CURATIVE, "auto");
        co1 = crac.newContingency().withId("co1").withNetworkElement("co1-ne").add();
        vc1 = addVoltageCnec("VL45", "VL45", 145., 150., "preventive", null);
        vc2 = addVoltageCnec("VL46", "VL46", 140., 145., "curative", co1.getId());
        preventiveState = crac.getPreventiveState();
        crac.newNetworkAction()
            .withId("na1")
            .newInjectionSetPoint().withNetworkElement("ne1").withSetpoint(50.).withUnit(Unit.MEGAWATT).add()
            .newOnVoltageConstraintUsageRule().withInstantId("preventive").withVoltageCnec(vc1.getId()).add()
            .add();
        crac.newNetworkAction()
            .withId("na2")
            .newInjectionSetPoint().withNetworkElement("ne2").withSetpoint(150.).withUnit(Unit.MEGAWATT).add()
            .newOnVoltageConstraintUsageRule().withInstantId("curative").withVoltageCnec(vc2.getId()).add()
            .add();
        voltageMonitoringResultImporter = new VoltageMonitoringResultImporter();
    }

    private VoltageCnec addVoltageCnec(String id, String networkElement, Double min, Double max, String instantId, String contingencyId) {
        return crac.newVoltageCnec()
            .withId(id)
            .withInstantId(instantId)
            .withNetworkElement(networkElement)
            .withMonitored()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(min).withMax(max).add()
            .withContingency(contingencyId)
            .add();
    }

    @Test
    void testRoundTrip() throws IOException {
        VoltageMonitoringResult voltageMonitoringResult =
            new VoltageMonitoringResultImporter().importVoltageMonitoringResult(getClass().getResourceAsStream("/result.json"), crac);

        assertEquals(UNKNOWN, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(vc1, vc2), voltageMonitoringResult.getConstrainedElements());
        assertEquals(144.4, voltageMonitoringResult.getMinVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(148.4, voltageMonitoringResult.getMaxVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(143.1, voltageMonitoringResult.getMinVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(147.7, voltageMonitoringResult.getMaxVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(List.of(
                "Some voltage CNECs are not secure:",
                "Network element VL45 at state preventive has a voltage of 144 - 148 kV.",
                "Network element VL46 at state co1 - curative has a voltage of 143 - 148 kV."),
            voltageMonitoringResult.printConstraints());

        OutputStream os = new ByteArrayOutputStream();
        new VoltageMonitoringResultExporter().export(voltageMonitoringResult, os);
        String expected = new String(Objects.requireNonNull(getClass().getResourceAsStream("/result.json")).readAllBytes());
        assertEquals(expected.replaceAll("\r", ""), os.toString().replaceAll("\r", ""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"nok1", "nok2", "nok3", "nok4", "nok5", "nok6", "nok7", "nok8", "nok9"})
    void importNokTest(String source) {
        InputStream inputStream = getClass().getResourceAsStream("/result-" + source + ".json");
        FaraoException exception = assertThrows(FaraoException.class, () -> voltageMonitoringResultImporter.importVoltageMonitoringResult(inputStream, crac));
        assertEquals("Status must be specified right after type of document.", exception.getMessage());
    }
}
