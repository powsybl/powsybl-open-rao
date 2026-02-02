/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.impl.VoltageCnecValue;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class RaoResultWithVoltageMonitoringTest {

    @Test
    void testGetCnecResult() {
        // We have a preventive voltage CNEC. We should allow getCnecResult to return a CnecResult only if optimizationInstant is preventive.

        VoltageCnec voltageCnec = Mockito.mock(VoltageCnec.class);
        when(voltageCnec.getId()).thenReturn("voltage");
        Instant cnecInstant = Mockito.mock(Instant.class);
        State state = Mockito.mock(State.class);
        when(state.getInstant()).thenReturn(cnecInstant);
        when(voltageCnec.getState()).thenReturn(state);

        RaoResult raoResult = Mockito.mock(RaoResult.class);
        MonitoringResult voltageMonitoringResult = Mockito.mock(MonitoringResult.class);
        CnecResult cnecResult = new CnecResult(voltageCnec, Unit.KILOVOLT, new VoltageCnecValue(-20.0, 20.0), 10, Cnec.SecurityStatus.SECURE);
        when(voltageMonitoringResult.getCnecResults()).thenReturn(Set.of(cnecResult));

        RaoResultWithVoltageMonitoring raoResultWithVoltageMonitoring = new RaoResultWithVoltageMonitoring(raoResult, voltageMonitoringResult);

        assertEquals(cnecResult, raoResultWithVoltageMonitoring.getCnecResult(cnecInstant, voltageCnec).get());

        // if optimizationInstant == null,  throw an error
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> raoResultWithVoltageMonitoring.getCnecResult(null, voltageCnec).get());
        assertEquals("Unexpected optimization instant for voltage monitoring result (only optimization instant equal to voltage cnec' state's instant is accepted) : null", exception.getMessage());

        // if optimizationInstant != cnec's instant, throw an error
        Instant optimizationInstant = Mockito.mock(Instant.class);
        exception = assertThrows(OpenRaoException.class, () -> raoResultWithVoltageMonitoring.getCnecResult(optimizationInstant, voltageCnec).get());
        assertTrue(exception.getMessage().startsWith("Unexpected optimization instant for voltage monitoring result (only optimization instant equal to voltage cnec' state's instant is accepted) :"));

        // If we give a voltage cnec that was not monitored ex. an outage cnec or if monitoring didn't return a result for the cnec for some reason
        // => should return an empty optional (if optimizationInstant == CNEC instant)
        VoltageCnec voltageCnecNotMonitored = Mockito.mock(VoltageCnec.class);
        when(voltageCnecNotMonitored.getState()).thenReturn(state);
        when(voltageCnecNotMonitored.getId()).thenReturn("voltageNotMonitored");
        assertFalse(raoResultWithVoltageMonitoring.getCnecResult(cnecInstant, voltageCnecNotMonitored).isPresent());
    }
}
