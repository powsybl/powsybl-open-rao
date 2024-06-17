/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresultapi;

import com.powsybl.openrao.commons.OpenRaoException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class RaoResultImportTest {
    @Test
    void testImportWithNoSuitableImporter() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> RaoResult.read(List.of(), null, null));
        assertEquals("No suitable RaoResult importer found.", exception.getMessage());
    }

    @Test
    void testImportFromInputStream() {
        assertTrue(RaoResult.read(getClass().getResourceAsStream("/raoResult.txt"), null) instanceof MockRaoResult);
    }
}
