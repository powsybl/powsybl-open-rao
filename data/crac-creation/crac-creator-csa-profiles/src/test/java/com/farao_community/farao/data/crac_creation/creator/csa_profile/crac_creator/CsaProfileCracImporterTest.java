/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.importer.CsaProfileCracImporter;
import com.powsybl.triplestore.api.PropertyBags;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
public class CsaProfileCracImporterTest {

    @Test
    void getFormat() {
        CsaProfileCracImporter csaProfileCracImporter = new CsaProfileCracImporter();
        assertEquals("CsaProfileCrac", csaProfileCracImporter.getFormat());
    }

    @Test
    void testImportNativeCrac() {
        CsaProfileCracImporter csaProfileCracImporter = new CsaProfileCracImporter();
        InputStream is1 = getClass().getResourceAsStream("/TestConfiguration_TC1_v29Mar2023/ELIA_CO.xml");
        CsaProfileCrac csaProfileCrac = csaProfileCracImporter.importNativeCrac(is1);
        assertNotNull(csaProfileCrac);

        PropertyBags contingenciesPb = csaProfileCrac.getContingencies();
        assertNotNull(contingenciesPb);
        assertEquals(1, contingenciesPb.size());
        assertEquals("CO1", contingenciesPb.get(0).get(CsaProfileConstants.REQUEST_CONTINGENCIES_NAME));

        PropertyBags contingencyEquipmentsPb = csaProfileCrac.getContingencyEquipments();
        assertNotNull(contingencyEquipmentsPb);
        assertEquals(1, contingencyEquipmentsPb.size());
        assertEquals("17086487-56ba-4979-b8de-064025a6b4da", contingencyEquipmentsPb.get(0).getId(CsaProfileConstants.REQUEST_CONTINGENCIES_EQUIPMENT_ID));
    }

    @Test
    void testExists() {
        InputStream is1 = getClass().getResourceAsStream("/TestConfiguration_TC1_v29Mar2023/ELIA_CO.xml");
        CsaProfileCracImporter importer = new CsaProfileCracImporter();
        assertTrue(importer.exists("ELIA_CO.xml", is1));
    }
}
