/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.importer;

import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class CimCracImporterTest {

    @Test
    public void getFormat() {
        CimCracImporter cimCracImporter = new CimCracImporter();
        assertEquals("CimCrac", cimCracImporter.getFormat());
    }

    @Test
    public void importNativeCrac() {
        InputStream is = getClass().getResourceAsStream("/cracs/CIM_21_1_1.xml");
        CimCracImporter importer = new CimCracImporter();
        CimCrac cimCrac = importer.importNativeCrac(is);
        assert cimCrac.getCracDocument().getMRID().equals("CIM_CRAC_DOCUMENT");
    }
}
