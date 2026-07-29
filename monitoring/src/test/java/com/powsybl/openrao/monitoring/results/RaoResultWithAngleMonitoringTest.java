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
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.impl.AngleCnecValue;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class RaoResultWithAngleMonitoringTest {

    @Test
    void testGetCnecResult() {
        // We have a preventive angle CNEC. We should allow getCnecResult to return a CnecResult only if optimizationInstant is preventive.
        AngleCnec angleCnec = Mockito.mock(AngleCnec.class);
        when(angleCnec.getId()).thenReturn("angle");
        Instant cnecInstant = Mockito.mock(Instant.class);
        when(cnecInstant.getId()).thenReturn("preventive");
        State state = Mockito.mock(State.class);
        when(state.getInstant()).thenReturn(cnecInstant);
        when(angleCnec.getState()).thenReturn(state);

        RaoResult raoResult = Mockito.mock(RaoResult.class);
        MonitoringResult angleMonitoringResult = Mockito.mock(MonitoringResult.class);
        CnecResult cnecResult = new CnecResult(angleCnec, Unit.DEGREE, new AngleCnecValue(10.0), 10, Cnec.SecurityStatus.SECURE);
        when(angleMonitoringResult.getCnecResults()).thenReturn(Set.of(cnecResult));

        RaoResultWithAngleMonitoring raoResultWithAngleMonitoring = new RaoResultWithAngleMonitoring(raoResult, angleMonitoringResult);

        assertEquals(cnecResult, raoResultWithAngleMonitoring.getCnecResult(cnecInstant, angleCnec).get());

        // if optimizationInstant == null, throw an error
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> raoResultWithAngleMonitoring.getCnecResult(null, angleCnec).get());
        assertEquals("Unexpected optimization instant for angle monitoring result: initial. Only optimization instant equal to angle cnec's instant is accepted: preventive", exception.getMessage());

        // if optimizationInstant != cnec's instant, throw an error
        Instant optimizationInstant = Mockito.mock(Instant.class);
        when(optimizationInstant.getId()).thenReturn("curative");
        exception = assertThrows(OpenRaoException.class, () -> raoResultWithAngleMonitoring.getCnecResult(optimizationInstant, angleCnec).get());
        assertEquals("Unexpected optimization instant for angle monitoring result: curative. Only optimization instant equal to angle cnec's instant is accepted: preventive", exception.getMessage());

        // If we give an angle cnec that was not monitored ex. an outage cnec or if monitoring didn't return a result for the cnec for some reason
        // => should return an empty optional (if optimizationInstant == CNEC instant)
        AngleCnec angleCnecNotMonitored = Mockito.mock(AngleCnec.class);
        when(angleCnecNotMonitored.getState()).thenReturn(state);
        when(angleCnecNotMonitored.getId()).thenReturn("angleCnecNotMonitored");
        assertFalse(raoResultWithAngleMonitoring.getCnecResult(cnecInstant, angleCnecNotMonitored).isPresent());
    }
}
