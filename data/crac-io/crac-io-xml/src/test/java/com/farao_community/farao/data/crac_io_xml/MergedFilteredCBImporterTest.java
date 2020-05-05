/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_xml;

import com.farao_community.farao.data.crac_io_api.CracImporter;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class MergedFilteredCBImporterTest {
    @Test
    public void testExistTrue() {
        InputStream is = getClass().getResourceAsStream("/MergedFilteredCriticalBranchesNoRA.xml");
        CracImporter importer = new MergedFilteredCBImporter();
        assertTrue(importer.exists("MergedFilteredCriticalBranchesNoRA.xml", is));
    }


    @Test
    public void testExistFalse() {
        InputStream is = getClass().getResourceAsStream("/SLwithoutRACorrupted.xml");
        CracImporter importer = new MergedFilteredCBImporter();
        assertFalse(importer.exists("MergedFilteredCriticalBranchesNoRACorrupted.xml", is));
    }
}
