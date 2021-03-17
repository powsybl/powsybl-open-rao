/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.native_crac_io_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.native_crac_api.NativeCrac;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static junit.framework.TestCase.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NativeCracImportersTest {

    @Test
    public void testFindImporterFromFile() {
        NativeCracImporter importer = NativeCracImporters.findImporter("empty.txt", getClass().getResourceAsStream("/empty.txt"));
        assertNotNull(importer);
        assertTrue(importer instanceof NativeCracImporterMock);
    }

    @Test
    public void testFindImporterFromFormat() {
        NativeCracImporter importer = NativeCracImporters.findImporter("MockedNativeCracFormat");
        assertNotNull(importer);
        assertTrue(importer instanceof NativeCracImporterMock);
    }

    @Test
    public void testFindImporterFromUnknownFormat() {
        NativeCracImporter importer = NativeCracImporters.findImporter("Unknown format");
        assertNull(importer);
    }

    @Test
    public void testImportWithInstant() {
        NativeCrac nativeCrac = NativeCracImporters.importData("empty.txt", getClass().getResourceAsStream("/empty.txt"));
        assertNotNull(nativeCrac);
        assertEquals("MockedNativeCracFormat", nativeCrac.getFormat());
    }

    @Test
    public void testImportFromPath() {
        NativeCrac nativeCrac = NativeCracImporters.importData(Paths.get(new File(getClass().getResource("/empty.txt").getFile()).getAbsolutePath()));
        assertNotNull(nativeCrac);
        assertEquals("MockedNativeCracFormat", nativeCrac.getFormat());

    }

    @Test(expected = FaraoException.class)
    public void testImportFileNotFound() {
        NativeCracImporters.importData(Paths.get("not_found", "file"));
    }
}
