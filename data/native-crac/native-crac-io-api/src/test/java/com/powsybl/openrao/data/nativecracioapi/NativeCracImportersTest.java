/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.nativecracioapi;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.nativecracapi.NativeCrac;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class NativeCracImportersTest {

    @Test
    void testFindImporterFromFile() {
        NativeCracImporter importer = NativeCracImporters.findImporter("empty.txt", getClass().getResourceAsStream("/empty.txt"));
        assertNotNull(importer);
        assertTrue(importer instanceof NativeCracImporterMock);
    }

    @Test
    void testFindImporterFromFormat() {
        NativeCracImporter importer = NativeCracImporters.findImporter("MockedNativeCracFormat");
        assertNotNull(importer);
        assertTrue(importer instanceof NativeCracImporterMock);
    }

    @Test
    void testFindImporterFromUnknownFormat() {
        NativeCracImporter importer = NativeCracImporters.findImporter("Unknown format");
        assertNull(importer);
    }

    @Test
    void testImportWithInstant() {
        NativeCrac nativeCrac = NativeCracImporters.importData("empty.txt", getClass().getResourceAsStream("/empty.txt"));
        assertNotNull(nativeCrac);
        assertEquals("MockedNativeCracFormat", nativeCrac.getFormat());
    }

    @Test
    void testImportFromPath() {
        NativeCrac nativeCrac = NativeCracImporters.importData(Paths.get(new File(getClass().getResource("/empty.txt").getFile()).getAbsolutePath()));
        assertNotNull(nativeCrac);
        assertEquals("MockedNativeCracFormat", nativeCrac.getFormat());

    }

    @Test
    void testImportFileNotFound() {
        Path path = Paths.get("not_found", "file");
        assertThrows(OpenRaoException.class, () -> NativeCracImporters.importData(path));
    }
}
