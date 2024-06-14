/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi;

import com.powsybl.openrao.commons.OpenRaoException;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class CracImportTest {
    @Test
    void testImportWithNoSuitableImporter() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> Crac.read(List.of(), null, null, null));
        assertEquals("No suitable CRAC importer found.", exception.getMessage());
    }

    @Test
    void testImportFromInputStream() {
        assertNull(Crac.read(getClass().getResourceAsStream("/crac.example"), null));
    }

    @Test
    void testImportFromPath() throws FileNotFoundException {
        assertNull(Crac.read(Path.of(getClass().getResource("/crac.example").getPath()), null));
    }

    @Test
    void testImportFromString() throws FileNotFoundException {
        assertNull(Crac.read(getClass().getResource("/crac.example").getPath(), null));
    }
}
