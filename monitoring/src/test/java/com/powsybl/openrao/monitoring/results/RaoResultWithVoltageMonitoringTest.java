/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.voltage.VoltageCnecMonitoringResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class RaoResultWithVoltageMonitoringTest {

    @Test
    void testGetCnecResult() {
        // We have a preventive voltage CNEC. We should allow getCnecResult to return a CnecMonitoringResult only if optimizationInstant is preventive.

        VoltageCnec voltageCnec = Mockito.mock(VoltageCnec.class);
        when(voltageCnec.getId()).thenReturn("voltage");
        Instant cnecInstant = Mockito.mock(Instant.class);
        when(cnecInstant.getId()).thenReturn("preventive");
        State state = Mockito.mock(State.class);
        when(state.getInstant()).thenReturn(cnecInstant);
        when(voltageCnec.getState()).thenReturn(state);

        RaoResult raoResult = Mockito.mock(RaoResult.class);
        MonitoringResult voltageMonitoringResult = Mockito.mock(MonitoringResult.class);
        VoltageCnecMonitoringResult cnecResult = new VoltageCnecMonitoringResult(voltageCnec, Unit.KILOVOLT, -20., 20.);
        when(voltageMonitoringResult.getCnecResult("voltage")).thenReturn(Optional.of(cnecResult));

        RaoResultWithVoltageMonitoring raoResultWithVoltageMonitoring = new RaoResultWithVoltageMonitoring(raoResult, voltageMonitoringResult);

        assertEquals(cnecResult, raoResultWithVoltageMonitoring.getCnecResult(cnecInstant, voltageCnec).get());

        // if optimizationInstant == null,  throw an error
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> raoResultWithVoltageMonitoring.getCnecResult(null, voltageCnec).get());
        assertEquals(
            "Unexpected optimization instant for voltage monitoring result: initial. Only optimization instant equal to voltage cnec's instant is accepted: preventive",
            exception.getMessage()
        );

        // if optimizationInstant != cnec's instant, throw an error
        Instant optimizationInstant = Mockito.mock(Instant.class);
        when(optimizationInstant.getId()).thenReturn("curative");
        exception = assertThrows(OpenRaoException.class, () -> raoResultWithVoltageMonitoring.getCnecResult(optimizationInstant, voltageCnec).get());
        assertEquals(
            "Unexpected optimization instant for voltage monitoring result: curative. Only optimization instant equal to voltage cnec's instant is accepted: preventive",
            exception.getMessage()
        );

        // If we give a voltage cnec that was not monitored ex. an outage cnec or if monitoring didn't return a result for the cnec for some reason
        // => should return an optional.empty (if optimizationInstant == CNEC instant)
        VoltageCnec voltageCnecNotMonitored = Mockito.mock(VoltageCnec.class);
        when(voltageCnecNotMonitored.getState()).thenReturn(state);
        when(voltageCnecNotMonitored.getId()).thenReturn("voltageNotMonitored");
        assertFalse(raoResultWithVoltageMonitoring.getCnecResult(cnecInstant, voltageCnecNotMonitored).isPresent());
    }

    @Test
    void testIsSecure() {
        RaoResult raoResult = Mockito.mock(RaoResult.class);
        MonitoringResult voltageMonitoringResult = Mockito.mock(MonitoringResult.class);
        RaoResultWithVoltageMonitoring raoResultWithVoltageMonitoring = new RaoResultWithVoltageMonitoring(raoResult, voltageMonitoringResult);

        // all secure
        when(voltageMonitoringResult.getStatus()).thenReturn(Cnec.SecurityStatus.SECURE);
        when(raoResult.isSecure(PhysicalParameter.FLOW)).thenReturn(true);
        when(raoResult.isSecure(PhysicalParameter.ANGLE)).thenReturn(true);
        when(raoResult.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE)).thenReturn(true);
        assertTrue(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.VOLTAGE, PhysicalParameter.ANGLE));
        assertTrue(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.VOLTAGE, PhysicalParameter.FLOW));
        assertTrue(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE, PhysicalParameter.VOLTAGE));

        // unsecure flow
        when(raoResult.isSecure(PhysicalParameter.FLOW)).thenReturn(false);
        when(raoResult.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE)).thenReturn(false);
        assertTrue(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.VOLTAGE, PhysicalParameter.ANGLE));
        assertFalse(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.VOLTAGE, PhysicalParameter.FLOW));
        assertFalse(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE, PhysicalParameter.VOLTAGE));

        // unsecure angle
        when(voltageMonitoringResult.getStatus()).thenReturn(Cnec.SecurityStatus.HIGH_CONSTRAINT);
        when(raoResult.isSecure(PhysicalParameter.FLOW)).thenReturn(true);
        when(raoResult.isSecure(PhysicalParameter.ANGLE)).thenReturn(true);
        when(raoResult.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE)).thenReturn(true);
        assertFalse(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.VOLTAGE, PhysicalParameter.ANGLE));
        assertFalse(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.VOLTAGE, PhysicalParameter.FLOW));
        assertFalse(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE, PhysicalParameter.VOLTAGE));
    }
}
