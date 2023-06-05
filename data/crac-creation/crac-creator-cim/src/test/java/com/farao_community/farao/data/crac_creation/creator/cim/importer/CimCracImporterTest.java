/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.importer;

import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class CimCracImporterTest {

    @Test
    void testGetFormat() {
        CimCracImporter cimCracImporter = new CimCracImporter();
        assertEquals("CimCrac", cimCracImporter.getFormat());
    }

    @Test
    void testExists() {
        InputStream is = getClass().getResourceAsStream("/cracs/CIM_21_1_1.xml");
        CimCracImporter importer = new CimCracImporter();
        CimCrac cimCrac = importer.importNativeCrac(is);
        assertEquals("CIM_CRAC_DOCUMENT", cimCrac.getCracDocument().getMRID());
    }
}
