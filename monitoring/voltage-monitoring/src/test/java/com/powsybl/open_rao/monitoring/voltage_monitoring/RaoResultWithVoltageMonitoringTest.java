/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.monitoring.voltage_monitoring;

import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.Identifiable;
import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_io_json.JsonImport;
import com.powsybl.open_rao.data.rao_result_api.ComputationStatus;
import com.powsybl.open_rao.data.rao_result_api.RaoResult;
import com.powsybl.open_rao.data.rao_result_json.RaoResultImporter;
import com.powsybl.open_rao.monitoring.voltage_monitoring.json.VoltageMonitoringResultImporter;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class RaoResultWithVoltageMonitoringTest {
    private static final double DOUBLE_TOLERANCE = 0.1;

    @Test
    void testRaoResultWithVoltageMonitoring() {
        InputStream raoResultFile = getClass().getResourceAsStream("/rao-result-v1.4.json");
        InputStream cracFile = getClass().getResourceAsStream("/crac-for-rao-result-v1.4.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        Instant curativeInstant = crac.getInstant("curative");
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);
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

}
