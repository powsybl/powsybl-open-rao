/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.monitoring.angle_monitoring;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_io_json.JsonImport;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.monitoring.angle_monitoring.json.AngleMonitoringResultImporter;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class RaoResultWithAngleMonitoringTest {
    private static final double DOUBLE_TOLERANCE = 0.1;

    @Test
    void testRaoResultWithAngleMonitoring() {
        InputStream raoResultFile = getClass().getResourceAsStream("/rao-result-v1.4.json");
        InputStream cracFile = getClass().getResourceAsStream("/crac-for-rao-result-v1.4.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);
        AngleMonitoringResult angleMonitoringResult = new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/angle-m-result.json"), crac);
        RaoResult raoResultWithAngleMonitoring = new RaoResultWithAngleMonitoring(raoResult, angleMonitoringResult);

        assertEquals(4.6, raoResultWithAngleMonitoring.getAngle(Instant.CURATIVE, crac.getAngleCnec("angleCnecId"), Unit.DEGREE), DOUBLE_TOLERANCE);
        assertEquals(85.4, raoResultWithAngleMonitoring.getMargin(Instant.CURATIVE, crac.getAngleCnec("angleCnecId"), Unit.DEGREE), DOUBLE_TOLERANCE);
        assertEquals(Set.of("pstSetpointRaId", "complexNetworkActionId"), raoResultWithAngleMonitoring.getActivatedNetworkActionsDuringState(crac.getState("contingency1Id", Instant.CURATIVE)).stream().map(Identifiable::getId).collect(Collectors.toSet()));
        assertTrue(raoResultWithAngleMonitoring.isActivatedDuringState(crac.getState("contingency1Id", Instant.CURATIVE), crac.getNetworkAction("complexNetworkActionId")));
        assertEquals(ComputationStatus.DEFAULT, raoResultWithAngleMonitoring.getComputationStatus());
    }

    @Test
    void testRaoResultWithAngleMonitoring2() {
        InputStream raoResultFile = getClass().getResourceAsStream("/rao-result-v1.4.json");
        InputStream cracFile = getClass().getResourceAsStream("/crac-for-rao-result-v1.4.json");
        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus());
        AngleMonitoringResult angleMonitoringResult = new AngleMonitoringResultImporter().importAngleMonitoringResult(getClass().getResourceAsStream("/angle-m-result2.json"), crac);
        RaoResult raoResultWithAngleMonitoring = new RaoResultWithAngleMonitoring(raoResult, angleMonitoringResult);
        assertEquals(ComputationStatus.FAILURE, raoResultWithAngleMonitoring.getComputationStatus());
    }
}
