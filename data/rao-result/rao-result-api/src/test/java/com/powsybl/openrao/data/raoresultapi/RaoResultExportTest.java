/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresultapi;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.CracCreationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class RaoResultExportTest {

    private MockRaoResult raoResult;

    @BeforeEach
    void setUp() {
        raoResult = new MockRaoResult();
    }

    @Test
    void testExportWithUnknownExporter() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> raoResult.write("unknownFormat", (CracCreationContext) null, null, null));
        assertEquals("Export format unknownFormat not supported", exception.getMessage());
        assertFalse(raoResult.wasExportSuccessful());
    }

    @Test
    void testExportCracWithValidExporter() {
        raoResult.write("Mock", (CracCreationContext) null, new Properties(), null);
        assertTrue(raoResult.wasExportSuccessful());
    }
}
