/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.voltage_monitoring.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

import static com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult.Status.HIGH_AND_LOW_VOLTAGE_CONSTRAINTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class JsonVoltageMonitoringResultTest {
    private static final double VOLTAGE_TOLERANCE = 0.5;

    Crac crac;
    VoltageCnec vc1;
    VoltageCnec vc2;

    @BeforeEach
    public void setUp() {
        crac = CracFactory.findDefault().create("test-crac");
        vc1 = addVoltageCnec("VL45", "VL45", 145., 150.);
        vc2 = addVoltageCnec("VL46", "VL46", 140., 145.);
    }

    private VoltageCnec addVoltageCnec(String id, String networkElement, Double min, Double max) {
        return crac.newVoltageCnec()
            .withId(id)
            .withInstant(Instant.PREVENTIVE)
            .withNetworkElement(networkElement)
            .withMonitored()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(min).withMax(max).add()
            .add();
    }

    @Test
    public void testRoundTrip() throws IOException {
        VoltageMonitoringResult voltageMonitoringResult =
            new VoltageMonitoringResultImporter().importVoltageMonitoringResult(getClass().getResourceAsStream("/result.json"), crac);

        assertEquals(HIGH_AND_LOW_VOLTAGE_CONSTRAINTS, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(vc1, vc2), voltageMonitoringResult.getConstrainedElements());
        assertEquals(144.4, voltageMonitoringResult.getMinVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(148.4, voltageMonitoringResult.getMaxVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(143.1, voltageMonitoringResult.getMinVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(147.7, voltageMonitoringResult.getMaxVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(List.of(
            "Network element VL45 at state preventive has a voltage of 144 - 148 kV.",
            "Network element VL46 at state preventive has a voltage of 143 - 148 kV."),
            voltageMonitoringResult.printConstraints());

        OutputStream os = new ByteArrayOutputStream();
        new VoltageMonitoringResultExporter().export(voltageMonitoringResult, os);
        String expected = new String(getClass().getResourceAsStream("/result.json").readAllBytes());
        assertEquals(expected, os.toString());
    }

    @Test
    public void testFailsIfCnecIdNull() {
        assertThrows(FaraoException.class, () -> new VoltageMonitoringResultImporter().importVoltageMonitoringResult(getClass().getResourceAsStream("/result-nok1.json"), crac));
    }

    @Test
    public void testFailsIfMinNull() {
        assertThrows(FaraoException.class, () -> new VoltageMonitoringResultImporter().importVoltageMonitoringResult(getClass().getResourceAsStream("/result-nok2.json"), crac));
    }

    @Test
    public void testFailsIfMaxNull() {
        assertThrows(FaraoException.class, () -> new VoltageMonitoringResultImporter().importVoltageMonitoringResult(getClass().getResourceAsStream("/result-nok3.json"), crac));
    }

    @Test
    public void testFailsIfCnecIdUsedTwice() {
        assertThrows(FaraoException.class, () -> new VoltageMonitoringResultImporter().importVoltageMonitoringResult(getClass().getResourceAsStream("/result-nok4.json"), crac));
    }

    @Test
    public void testFailsIfCnecNotInCrac() {
        assertThrows(FaraoException.class, () -> new VoltageMonitoringResultImporter().importVoltageMonitoringResult(getClass().getResourceAsStream("/result-nok5.json"), crac));
    }

    @Test
    public void testFailsIfWrongField1() {
        assertThrows(FaraoException.class, () -> new VoltageMonitoringResultImporter().importVoltageMonitoringResult(getClass().getResourceAsStream("/result-nok6.json"), crac));
    }

    @Test
    public void testFailsIfWrongField2() {
        assertThrows(FaraoException.class, () -> new VoltageMonitoringResultImporter().importVoltageMonitoringResult(getClass().getResourceAsStream("/result-nok7.json"), crac));
    }

    @Test
    public void testFailsIfNoType() {
        assertThrows(FaraoException.class, () -> new VoltageMonitoringResultImporter().importVoltageMonitoringResult(getClass().getResourceAsStream("/result-nok8.json"), crac));
    }

    @Test
    public void testFailsIfWrongType() {
        assertThrows(FaraoException.class, () -> new VoltageMonitoringResultImporter().importVoltageMonitoringResult(getClass().getResourceAsStream("/result-nok9.json"), crac));
    }
}
