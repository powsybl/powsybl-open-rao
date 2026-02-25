/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.powsybl.openrao.commons.OpenRaoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class CracExportTest {

    private MockCrac crac;

    @BeforeEach
    void setUp() {
        crac = new MockCrac("crac");
    }

    @Test
    void testExportWithUnknownExporter() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> crac.write("unknownFormat", null));
        assertEquals("Export format unknownFormat not supported", exception.getMessage());
        assertFalse(crac.wasExportSuccessful());
    }

    @Test
    void testExportCracWithValidExporter() {
        crac.write("Mock", null);
        assertTrue(crac.wasExportSuccessful());
    }
}
