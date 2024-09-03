/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.monitoring.voltagemonitoring;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.monitoring.voltagemonitoring.json.VoltageMonitoringResultImporter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class RaoResultWithVoltageMonitoringTest {
    private static final double DOUBLE_TOLERANCE = 0.1;

    private static Network getMockedNetwork() {
        Network network = Mockito.mock(Network.class);
        com.powsybl.iidm.network.Identifiable ne = Mockito.mock(com.powsybl.iidm.network.Identifiable.class);
        Mockito.when(ne.getType()).thenReturn(IdentifiableType.SHUNT_COMPENSATOR);
        Mockito.when(network.getIdentifiable("injection")).thenReturn(ne);
        for (String lineId : List.of("ne1Id", "ne2Id", "ne3Id")) {
            Branch l = Mockito.mock(Line.class);
            Mockito.when(l.getId()).thenReturn(lineId);
            Mockito.when(network.getIdentifiable(lineId)).thenReturn(l);
        }
        return network;
    }

    @Test
    void testRaoResultWithVoltageMonitoring() throws IOException {
        InputStream raoResultFile = getClass().getResourceAsStream("/rao-result-v1.4.json");
        InputStream cracFile = getClass().getResourceAsStream("/crac-for-rao-result-v1.4.json");

        Crac crac = Crac.read("crac-for-rao-result-v1.4.json", cracFile, getMockedNetwork());
        Instant curativeInstant = crac.getInstant("curative");
        RaoResult raoResult = RaoResult.read(raoResultFile, crac);
        VoltageMonitoringResult voltageMonitoringResult = new VoltageMonitoringResultImporter().importVoltageMonitoringResult(getClass().getResourceAsStream("/voltage-monitoring-result.json"), crac);
        RaoResult raoResultWithVoltageMonitoring = new RaoResultWithVoltageMonitoring(raoResult, voltageMonitoringResult);

        assertEquals(144.38, raoResultWithVoltageMonitoring.getVoltage(curativeInstant, crac.getVoltageCnec("voltageCnecId"), Unit.KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(-236.61, raoResultWithVoltageMonitoring.getMargin(curativeInstant, crac.getVoltageCnec("voltageCnecId"), Unit.KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(Set.of("pstSetpointRaId", "complexNetworkActionId"), raoResultWithVoltageMonitoring.getActivatedNetworkActionsDuringState(crac.getState("contingency1Id", curativeInstant)).stream().map(Identifiable::getId).collect(Collectors.toSet()));
        assertTrue(raoResultWithVoltageMonitoring.isActivatedDuringState(crac.getState("contingency1Id", curativeInstant), crac.getNetworkAction("complexNetworkActionId")));
        assertEquals(ComputationStatus.FAILURE, raoResultWithVoltageMonitoring.getComputationStatus());

        VoltageMonitoringResult voltageMonitoringResult2 = new VoltageMonitoringResultImporter().importVoltageMonitoringResult(getClass().getResourceAsStream("/voltage-monitoring-result2.json"), crac);
        RaoResult raoResultWithVoltageMonitoring2 = new RaoResultWithVoltageMonitoring(raoResult, voltageMonitoringResult2);

        assertEquals(398., raoResultWithVoltageMonitoring2.getVoltage(curativeInstant, crac.getVoltageCnec("voltageCnecId"), Unit.KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(1., raoResultWithVoltageMonitoring2.getMargin(curativeInstant, crac.getVoltageCnec("voltageCnecId"), Unit.KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(ComputationStatus.DEFAULT, raoResultWithVoltageMonitoring2.getComputationStatus());
    }

    @Test
    void testIsSecureWhenRaoResultAndVoltageMonitoringIsSecure() {
        RaoResult raoResult = Mockito.mock(RaoResult.class);
        VoltageMonitoringResult voltageMonitoringResult = Mockito.mock(VoltageMonitoringResult.class);
        RaoResult raoResultWithVoltageMonitoring = new RaoResultWithVoltageMonitoring(raoResult, voltageMonitoringResult);
        Mockito.when(raoResult.isSecure()).thenReturn(true);
        Mockito.when(voltageMonitoringResult.isSecure()).thenReturn(true);
        Mockito.when(raoResult.isSecure(PhysicalParameter.FLOW)).thenReturn(true);
        Mockito.when(raoResult.isSecure(Mockito.any(Instant.class), Mockito.eq(PhysicalParameter.FLOW))).thenReturn(true);

        assertTrue(raoResultWithVoltageMonitoring.isSecure());
        assertTrue(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.VOLTAGE));
        assertTrue(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.FLOW, PhysicalParameter.VOLTAGE));
        assertTrue(raoResultWithVoltageMonitoring.isSecure(Mockito.mock(Instant.class), PhysicalParameter.VOLTAGE));
        assertTrue(raoResultWithVoltageMonitoring.isSecure(Mockito.mock(Instant.class), PhysicalParameter.FLOW, PhysicalParameter.VOLTAGE));
    }

    @Test
    void testIsSecureWhenRaoResultAndVoltageMonitoringUnsecureIfVoltageNotChecked() {
        RaoResult raoResult = Mockito.mock(RaoResult.class);
        VoltageMonitoringResult voltageMonitoringResult = Mockito.mock(VoltageMonitoringResult.class);
        RaoResult raoResultWithVoltageMonitoring = new RaoResultWithVoltageMonitoring(raoResult, voltageMonitoringResult);
        Mockito.when(raoResult.isSecure()).thenReturn(true);
        Mockito.when(voltageMonitoringResult.isSecure()).thenReturn(false);
        Mockito.when(raoResult.isSecure(PhysicalParameter.FLOW)).thenReturn(true);
        Mockito.when(raoResult.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE)).thenReturn(true);
        Mockito.when(raoResult.isSecure(Mockito.any(Instant.class), Mockito.eq(PhysicalParameter.FLOW))).thenReturn(true);
        Mockito.when(raoResult.isSecure(Mockito.any(Instant.class), Mockito.eq(PhysicalParameter.FLOW), Mockito.eq(PhysicalParameter.ANGLE))).thenReturn(true);

        assertFalse(raoResultWithVoltageMonitoring.isSecure());
        assertFalse(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.VOLTAGE));
        assertFalse(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.FLOW, PhysicalParameter.VOLTAGE));
        assertFalse(raoResultWithVoltageMonitoring.isSecure(Mockito.mock(Instant.class), PhysicalParameter.VOLTAGE));
        assertFalse(raoResultWithVoltageMonitoring.isSecure(Mockito.mock(Instant.class), PhysicalParameter.FLOW, PhysicalParameter.VOLTAGE));
        assertTrue(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE));
        assertTrue(raoResultWithVoltageMonitoring.isSecure(Mockito.mock(Instant.class), PhysicalParameter.FLOW, PhysicalParameter.ANGLE));
    }

    @Test
    void testIsUnsecureWhenRaoResultIsUnsecureAndVoltageMonitoringIsSecure() {
        RaoResult raoResult = Mockito.mock(RaoResult.class);
        VoltageMonitoringResult voltageMonitoringResult = Mockito.mock(VoltageMonitoringResult.class);
        RaoResult raoResultWithVoltageMonitoring = new RaoResultWithVoltageMonitoring(raoResult, voltageMonitoringResult);
        Mockito.when(raoResult.isSecure()).thenReturn(false);
        Mockito.when(voltageMonitoringResult.isSecure()).thenReturn(true);
        Mockito.when(raoResult.isSecure(PhysicalParameter.FLOW, PhysicalParameter.VOLTAGE)).thenReturn(false);
        Mockito.when(raoResult.isSecure(Mockito.any(Instant.class), Mockito.eq(PhysicalParameter.FLOW), Mockito.eq(PhysicalParameter.VOLTAGE))).thenReturn(false);

        assertFalse(raoResultWithVoltageMonitoring.isSecure());
        assertTrue(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.VOLTAGE));
        assertFalse(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.FLOW, PhysicalParameter.VOLTAGE));
        assertFalse(raoResultWithVoltageMonitoring.isSecure(Mockito.mock(Instant.class), PhysicalParameter.FLOW, PhysicalParameter.VOLTAGE));
    }

}
