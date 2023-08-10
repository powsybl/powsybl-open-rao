/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.csa_profile.importer;

import com.farao_community.farao.data.crac_creation.creator.csa_profile.CsaProfileCrac;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
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
    void testExists() {
        InputStream is1 = getClass().getResourceAsStream("/TestConfiguration_TC1_v29Mar2023.zip");
        CsaProfileCracImporter importer = new CsaProfileCracImporter();
        assertTrue(importer.exists("TestConfiguration_TC1_v29Mar2023.zip", is1));
    }

    @Test
    void testImportNativeCracV29Mar2023() {
        CsaProfileCracImporter csaProfileCracImporter = new CsaProfileCracImporter();
        InputStream is1 = getClass().getResourceAsStream("/TestConfiguration_TC1_v29Mar2023.zip");
        CsaProfileCrac csaProfileCrac = csaProfileCracImporter.importNativeCrac(is1);
        assertNotNull(csaProfileCrac);

        //contingencies
        PropertyBags contingenciesPb = csaProfileCrac.getContingencies();
        assertNotNull(contingenciesPb);
        assertEquals(2, contingenciesPb.size());
        assertEquals("493480ba-93c3-426e-bee5-347d8dda3749", contingenciesPb.get(0).getId(CsaProfileConstants.REQUEST_CONTINGENCY));
        assertEquals("c0a25fd7-eee0-4191-98a5-71a74469d36e", contingenciesPb.get(1).getId(CsaProfileConstants.REQUEST_CONTINGENCY));
        // contingencies equipments
        PropertyBags contingencyEquipmentsPb = csaProfileCrac.getContingencyEquipments();
        assertNotNull(contingencyEquipmentsPb);
        assertEquals(2, contingencyEquipmentsPb.size());
        assertEquals("ef11f9bd-5da0-43e3-921b-7e92d2896136", contingencyEquipmentsPb.get(0).getId(CsaProfileConstants.REQUEST_CONTINGENCY_EQUIPMENT));
        assertEquals("f19925fa-b114-48c5-97a4-42ef84372115", contingencyEquipmentsPb.get(1).getId(CsaProfileConstants.REQUEST_CONTINGENCY_EQUIPMENT));
        assertEquals(2, csaProfileCrac.getRemedialActions().size());
    }

}
