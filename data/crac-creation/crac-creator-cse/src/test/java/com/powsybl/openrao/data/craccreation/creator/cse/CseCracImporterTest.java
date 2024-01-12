/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.cse;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
class CseCracImporterTest {

    @Test
    void getFormat() {
        CseCracImporter cseCracImporter = new CseCracImporter();
        assertEquals("CseCrac", cseCracImporter.getFormat());
    }

    @Test
    void importNativeCrac() {
        InputStream is = getClass().getResourceAsStream("/cracs/cse_crac_1.xml");
        CseCracImporter importer = new CseCracImporter();
        CseCrac cseCrac = importer.importNativeCrac(is);
        assertEquals("ruleToBeDefined", cseCrac.getCracDocument().getDocumentIdentification().getV());
    }

    @Test
    void importNativeCracWithMNE() {
        InputStream is = getClass().getResourceAsStream("/cracs/cse_crac_with_MNE.xml");
        CseCracImporter importer = new CseCracImporter();
        CseCrac cseCrac = importer.importNativeCrac(is);
        assertEquals(100, cseCrac.getCracDocument().getCRACSeries().get(0).getMonitoredElements().getMonitoredElement().get(0).getBranch().get(0).getIlimitMNE().getV());
    }
}
