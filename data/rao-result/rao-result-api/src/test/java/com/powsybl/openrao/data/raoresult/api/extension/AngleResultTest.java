/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AngleResultTest {
    @Test
    void testAngleExtension() {
        AngleCnec angleCnec = Mockito.mock(AngleCnec.class);
        Mockito.when(angleCnec.getUpperBound(Unit.DEGREE)).thenReturn(Optional.of(30.0));
        Mockito.when(angleCnec.getLowerBound(Unit.DEGREE)).thenReturn(Optional.of(0.0));

        Instant preventiveInstant = Mockito.mock(Instant.class);
        Mockito.when(preventiveInstant.getOrder()).thenReturn(0);
        Instant curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.getOrder()).thenReturn(1);

        // initial results

        AngleResult emptyAngleResult = AngleResult.build().build();
        assertEquals("angle-results", emptyAngleResult.getName());

        assertEquals(Double.NaN, emptyAngleResult.getAngle(null, angleCnec, Unit.DEGREE));
        assertEquals(Double.NaN, emptyAngleResult.getMargin(null, angleCnec, Unit.DEGREE));

        assertEquals(Double.NaN, emptyAngleResult.getAngle(preventiveInstant, angleCnec, Unit.DEGREE));
        assertEquals(Double.NaN, emptyAngleResult.getMargin(preventiveInstant, angleCnec, Unit.DEGREE));

        assertEquals(Double.NaN, emptyAngleResult.getAngle(curativeInstant, angleCnec, Unit.DEGREE));
        assertEquals(Double.NaN, emptyAngleResult.getMargin(curativeInstant, angleCnec, Unit.DEGREE));

        // manually add results

        AngleResult.AngleResultBuilder angleResultBuilder = AngleResult.build();

        angleResultBuilder.addMeasurement(25.0, null, angleCnec, Unit.DEGREE)
            .addMeasurement(17.0, preventiveInstant, angleCnec, Unit.DEGREE)
            .addMeasurement(-5.0, curativeInstant, angleCnec, Unit.DEGREE);

        AngleResult angleResult = angleResultBuilder.build();

        assertEquals(25.0, angleResult.getAngle(null, angleCnec, Unit.DEGREE));
        assertEquals(5.0, angleResult.getMargin(null, angleCnec, Unit.DEGREE));

        assertEquals(17.0, angleResult.getAngle(preventiveInstant, angleCnec, Unit.DEGREE));
        assertEquals(13.0, angleResult.getMargin(preventiveInstant, angleCnec, Unit.DEGREE));

        assertEquals(-5.0, angleResult.getAngle(curativeInstant, angleCnec, Unit.DEGREE));
        assertEquals(-5.0, angleResult.getMargin(curativeInstant, angleCnec, Unit.DEGREE));

        // invalid unit

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> AngleResult.build().addMeasurement(25.0, null, angleCnec, Unit.MEGAWATT));
        assertEquals("AngleCNEC results are only allowed for degrees.", exception.getMessage());
    }

    private static AngleCnec mockAngleCnec(Instant instant) {
        AngleCnec angleCnec = Mockito.mock(AngleCnec.class);
        State state = Mockito.mock(State.class);
        Mockito.when(state.getInstant()).thenReturn(instant);
        Mockito.when(angleCnec.getState()).thenReturn(state);
        Mockito.when(angleCnec.getUpperBound(Unit.DEGREE)).thenReturn(Optional.of(30.0));
        Mockito.when(angleCnec.getLowerBound(Unit.DEGREE)).thenReturn(Optional.of(0.0));
        return angleCnec;
    }

    @Test
    void testIsSecureWithoutResults() {
        assertTrue(AngleResult.build().build().isSecure());
    }

    @Test
    void testIsSecure() {
        Instant preventiveInstant = Mockito.mock(Instant.class);
        Instant curativeInstant = Mockito.mock(Instant.class);
        AngleCnec preventiveCnec = mockAngleCnec(preventiveInstant);
        AngleCnec curativeCnec = mockAngleCnec(curativeInstant);

        // only the margin at the CNEC's own instant matters (other instants are negative)
        AngleResult angleResult = AngleResult.build()
            .addMeasurement(-10.0, null, preventiveCnec, Unit.DEGREE)
            .addMeasurement(20.0, preventiveInstant, preventiveCnec, Unit.DEGREE)
            .addMeasurement(50.0, preventiveInstant, curativeCnec, Unit.DEGREE)
            .addMeasurement(10.0, curativeInstant, curativeCnec, Unit.DEGREE)
            .build();
        assertTrue(angleResult.isSecure());
    }

    @Test
    void testIsNotSecureWithNegativeMargin() {
        Instant preventiveInstant = Mockito.mock(Instant.class);
        AngleCnec secureCnec = mockAngleCnec(preventiveInstant);
        AngleCnec unsecureCnec = mockAngleCnec(preventiveInstant);

        AngleResult angleResult = AngleResult.build()
            .addMeasurement(15.0, preventiveInstant, secureCnec, Unit.DEGREE)
            .addMeasurement(35.0, preventiveInstant, unsecureCnec, Unit.DEGREE)
            .build();
        assertFalse(angleResult.isSecure());
    }

    @Test
    void testIsSecureWithNullMargin() {
        Instant preventiveInstant = Mockito.mock(Instant.class);
        AngleCnec angleCnec = mockAngleCnec(preventiveInstant);

        AngleResult angleResult = AngleResult.build().addMeasurement(30.0, preventiveInstant, angleCnec, Unit.DEGREE).build();
        assertTrue(angleResult.isSecure());
    }
}
