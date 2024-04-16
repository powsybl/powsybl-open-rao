/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.monitoring.anglemonitoring;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.craciojson.JsonImport;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.raoresultjson.RaoResultImporter;
import com.powsybl.openrao.monitoring.anglemonitoring.json.AngleMonitoringResultImporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class RaoResultWithAngleMonitoringTest {
    private Crac crac;
    private RaoResult raoResult;
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

    @BeforeEach
    public void setUp() {
        InputStream raoResultFile = getClass().getResourceAsStream("/rao-result-v1.4.json");
        InputStream cracFile = getClass().getResourceAsStream("/crac-for-rao-result-v1.4.json");
        crac = new JsonImport().importCrac(cracFile, getMockedNetwork());
        raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);
    }

    @Test
    void testRaoResultWithAngleMonitoring() {
        AngleMonitoringResult angleMonitoringResult = new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/angle-monitoring-result.json"), crac);
        RaoResult raoResultWithAngleMonitoring = new RaoResultWithAngleMonitoring(raoResult, angleMonitoringResult);
        Instant curativeInstant = crac.getInstant(InstantKind.CURATIVE);
        assertEquals(4.6, raoResultWithAngleMonitoring.getAngle(curativeInstant, crac.getAngleCnec("angleCnecId"), Unit.DEGREE), DOUBLE_TOLERANCE);
        assertEquals(85.4, raoResultWithAngleMonitoring.getMargin(curativeInstant, crac.getAngleCnec("angleCnecId"), Unit.DEGREE), DOUBLE_TOLERANCE);
        assertEquals(Set.of("pstSetpointRaId", "complexNetworkActionId"), raoResultWithAngleMonitoring.getActivatedNetworkActionsDuringState(crac.getState("contingency1Id", curativeInstant)).stream().map(Identifiable::getId).collect(Collectors.toSet()));
        assertTrue(raoResultWithAngleMonitoring.isActivatedDuringState(crac.getState("contingency1Id", curativeInstant), crac.getNetworkAction("complexNetworkActionId")));
        assertEquals(ComputationStatus.DEFAULT, raoResultWithAngleMonitoring.getComputationStatus());

        AngleMonitoringResult angleMonitoringResult2 = new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/angle-monitoring-result2.json"), crac);
        RaoResult raoResultWithAngleMonitoring2 = new RaoResultWithAngleMonitoring(raoResult, angleMonitoringResult2);
        assertEquals(ComputationStatus.FAILURE, raoResultWithAngleMonitoring2.getComputationStatus());
    }

    @Test
    void testRaoResultWithNullAngleMonitoring() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new RaoResultWithAngleMonitoring(raoResult, null));
        assertEquals("AngleMonitoringResult must not be null", exception.getMessage());
    }

    @Test
    void testIsSecureWhenRaoResultAndAngleMonitoringIsSecure() {
        RaoResult raoResult = Mockito.mock(RaoResult.class);
        AngleMonitoringResult angleMonitoringResult = Mockito.mock(AngleMonitoringResult.class);
        RaoResult raoResultWithVoltageMonitoring = new RaoResultWithAngleMonitoring(raoResult, angleMonitoringResult);
        Mockito.when(raoResult.isSecure()).thenReturn(true);
        Mockito.when(angleMonitoringResult.isSecure()).thenReturn(true);
        Mockito.when(raoResult.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE)).thenReturn(true);
        Mockito.when(raoResult.isSecure(Mockito.any(Instant.class), Mockito.eq(PhysicalParameter.FLOW), Mockito.eq(PhysicalParameter.ANGLE))).thenReturn(true);

        assertTrue(raoResultWithVoltageMonitoring.isSecure());
        assertTrue(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE));
        assertTrue(raoResultWithVoltageMonitoring.isSecure(Mockito.mock(Instant.class), PhysicalParameter.FLOW, PhysicalParameter.ANGLE));
    }

    @Test
    void testIsSecureWhenRaoResultAndAngleMonitoringUnsecureIfAngleNotChecked() {
        RaoResult raoResult = Mockito.mock(RaoResult.class);
        AngleMonitoringResult angleMonitoringResult = Mockito.mock(AngleMonitoringResult.class);
        RaoResult raoResultWithVoltageMonitoring = new RaoResultWithAngleMonitoring(raoResult, angleMonitoringResult);
        Mockito.when(raoResult.isSecure()).thenReturn(true);
        Mockito.when(angleMonitoringResult.isSecure()).thenReturn(false);
        Mockito.when(raoResult.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE)).thenReturn(true);
        Mockito.when(raoResult.isSecure(PhysicalParameter.FLOW, PhysicalParameter.VOLTAGE)).thenReturn(true);
        Mockito.when(raoResult.isSecure(Mockito.any(Instant.class), Mockito.eq(PhysicalParameter.FLOW), Mockito.eq(PhysicalParameter.ANGLE))).thenReturn(true);
        Mockito.when(raoResult.isSecure(Mockito.any(Instant.class), Mockito.eq(PhysicalParameter.FLOW), Mockito.eq(PhysicalParameter.VOLTAGE))).thenReturn(true);

        assertFalse(raoResultWithVoltageMonitoring.isSecure());
        assertFalse(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE));
        assertFalse(raoResultWithVoltageMonitoring.isSecure(Mockito.mock(Instant.class), PhysicalParameter.FLOW, PhysicalParameter.ANGLE));
        assertTrue(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.FLOW, PhysicalParameter.VOLTAGE));
        assertTrue(raoResultWithVoltageMonitoring.isSecure(Mockito.mock(Instant.class), PhysicalParameter.FLOW, PhysicalParameter.VOLTAGE));
    }

    @Test
    void testIsUnsecureWhenRaoResultIsUnsecureAndAngleMonitoringIsSecure() {
        RaoResult raoResult = Mockito.mock(RaoResult.class);
        AngleMonitoringResult angleMonitoringResult = Mockito.mock(AngleMonitoringResult.class);
        RaoResult raoResultWithVoltageMonitoring = new RaoResultWithAngleMonitoring(raoResult, angleMonitoringResult);
        Mockito.when(raoResult.isSecure()).thenReturn(false);
        Mockito.when(angleMonitoringResult.isSecure()).thenReturn(true);
        Mockito.when(raoResult.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE)).thenReturn(false);
        Mockito.when(raoResult.isSecure(Mockito.any(Instant.class), Mockito.eq(PhysicalParameter.FLOW), Mockito.eq(PhysicalParameter.ANGLE))).thenReturn(false);

        assertFalse(raoResultWithVoltageMonitoring.isSecure());
        assertFalse(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE));
        assertFalse(raoResultWithVoltageMonitoring.isSecure(Mockito.mock(Instant.class), PhysicalParameter.FLOW, PhysicalParameter.ANGLE));
    }
}
