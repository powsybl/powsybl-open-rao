/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile.importer;

import com.powsybl.openrao.data.craccreation.creator.csaprofile.CsaProfileCrac;
import com.powsybl.triplestore.api.PropertyBags;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
class CsaProfileCracImporterTest {

    @Test
    void getFormat() {
        CsaProfileCracImporter csaProfileCracImporter = new CsaProfileCracImporter();
        assertEquals("CsaProfileCrac", csaProfileCracImporter.getFormat());
    }

    @Test
    void testExists() {
        InputStream is1 = getClass().getResourceAsStream("/profiles/contingencies/Contingencies.zip");
        CsaProfileCracImporter importer = new CsaProfileCracImporter();
        assertTrue(importer.exists("/profiles/contingencies/Contingencies.zip", is1));
    }


    @Test
    void testImportNativeCracWithoutSubdirectory() {
        CsaProfileCracImporter csaProfileCracImporter = new CsaProfileCracImporter();
        InputStream is1 = getClass().getResourceAsStream("/TestCaseWithoutSubdirectory.zip");
        CsaProfileCrac csaProfileCrac = csaProfileCracImporter.importNativeCrac(is1);
        assertNotNull(csaProfileCrac);

        PropertyBags contingencies = csaProfileCrac.getContingencies();
        assertEquals(1, contingencies.size());

        PropertyBags currentLimits = csaProfileCrac.getCurrentLimits();
        assertEquals(52, currentLimits.size());
    }
}
