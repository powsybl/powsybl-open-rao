/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package test.java.com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator;

import com.farao_community.farao.data.crac_creation.creator.cim.importer.CimCracImporter;
import main.java.com.farao_community.farao.data.crac_creation.creator.csa_profile.importer.CsaProfileCracImporter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void testrdf() {

    }
}
