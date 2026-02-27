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
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AngleExtensionTest {
    @Test
    void testAngleExtension() {
        AngleCnec angleCnec = Mockito.mock(AngleCnec.class);
        Mockito.when(angleCnec.getUpperBound(Unit.DEGREE)).thenReturn(Optional.of(30.0));
        Mockito.when(angleCnec.getLowerBound(Unit.DEGREE)).thenReturn(Optional.of(0.0));

        Instant preventiveInstant = Mockito.mock(Instant.class);
        Mockito.when(preventiveInstant.getOrder()).thenReturn(0);
        Instant curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.getOrder()).thenReturn(1);

        AngleExtension angleExtension = new AngleExtension();
        assertEquals("angle-results", angleExtension.getName());

        // initial results

        assertEquals(Double.NaN, angleExtension.getAngle(null, angleCnec, Unit.DEGREE));
        assertEquals(Double.NaN, angleExtension.getMargin(null, angleCnec, Unit.DEGREE));

        assertEquals(Double.NaN, angleExtension.getAngle(preventiveInstant, angleCnec, Unit.DEGREE));
        assertEquals(Double.NaN, angleExtension.getMargin(preventiveInstant, angleCnec, Unit.DEGREE));

        assertEquals(Double.NaN, angleExtension.getAngle(curativeInstant, angleCnec, Unit.DEGREE));
        assertEquals(Double.NaN, angleExtension.getMargin(curativeInstant, angleCnec, Unit.DEGREE));

        // manually add results

        angleExtension.addAngle(25.0, null, angleCnec, Unit.DEGREE);
        assertEquals(25.0, angleExtension.getAngle(null, angleCnec, Unit.DEGREE));
        assertEquals(5.0, angleExtension.getMargin(null, angleCnec, Unit.DEGREE));

        angleExtension.addAngle(17.0, preventiveInstant, angleCnec, Unit.DEGREE);
        assertEquals(17.0, angleExtension.getAngle(preventiveInstant, angleCnec, Unit.DEGREE));
        assertEquals(13.0, angleExtension.getMargin(preventiveInstant, angleCnec, Unit.DEGREE));

        angleExtension.addAngle(-5.0, curativeInstant, angleCnec, Unit.DEGREE);
        assertEquals(-5.0, angleExtension.getAngle(curativeInstant, angleCnec, Unit.DEGREE));
        assertEquals(-5.0, angleExtension.getMargin(curativeInstant, angleCnec, Unit.DEGREE));

        // invalid unit

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> angleExtension.addAngle(25.0, null, angleCnec, Unit.MEGAWATT));
        assertEquals("AngleCNEC results are only allowed for degrees.", exception.getMessage());
    }
}
