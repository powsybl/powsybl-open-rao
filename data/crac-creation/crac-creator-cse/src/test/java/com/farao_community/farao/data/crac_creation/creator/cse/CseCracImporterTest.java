/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse;

import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CseCracImporterTest {

    @Test
    public void getFormat() {
        CseCracImporter cseCracImporter = new CseCracImporter();
        assertEquals("CseCrac", cseCracImporter.getFormat());
    }

    @Test
    public void importNativeCrac() {
        InputStream is = getClass().getResourceAsStream("/cracs/cse_crac_1.xml");
        CseCracImporter importer = new CseCracImporter();
        CseCrac cseCrac = importer.importNativeCrac(is);
        assertEquals("ruleToBeDefined", cseCrac.getCracDocument().getDocumentIdentification().getV());
    }

    @Test
    public void importNativeCracWithMNE() {
        InputStream is = getClass().getResourceAsStream("/cracs/cse_crac_with_MNE(cnec1become_mne).xml");
        CseCracImporter importer = new CseCracImporter();
        CseCrac cseCrac = importer.importNativeCrac(is);
        assertEquals(cseCrac.getCracDocument().getCRACSeries().get(0).getMonitoredElements().getMonitoredElement().get(0).getTimeInterval().getV(), "2021-04-30T22:00Z/2021-04-30T23:00ZZ");
        assertEquals(cseCrac.getCracDocument().getCRACSeries().get(0).getMonitoredElements().getMonitoredElement().get(0).getBranch().get(0).getImax().getV(), 100);
    }

    @Test
    public void importNativeCracwithMNEUs() {
        InputStream is = getClass().getResourceAsStream("/cracs/20220530_1930_2D1_ce_CRAC_Transit_CSE2.xml");
        CseCracImporter importer = new CseCracImporter();
        CseCrac cseCrac = importer.importNativeCrac(is);
        assertEquals(cseCrac.getCracDocument().getCRACSeries().get(0).getMonitoredElements().getMonitoredElement().size(), 30);
    }
}
