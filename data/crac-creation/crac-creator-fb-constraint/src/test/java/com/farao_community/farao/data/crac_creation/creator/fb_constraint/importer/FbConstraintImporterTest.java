/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.fb_constraint.importer;

import com.farao_community.farao.data.crac_creation.creator.fb_constraint.FbConstraint;
import com.farao_community.farao.data.native_crac_io_api.NativeCracImporter;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class FbConstraintImporterTest {

    @Test
    public void testExistSchemaVersion18True() {
        NativeCracImporter importer = new FbConstraintImporter();
        assertTrue(importer.exists("without_RA.xml", getClass().getResourceAsStream("/merged_cb/without_RA.xml")));
    }

    @Test
    public void testImportSchemaVersion18() {
        FbConstraint fbConstraint = new FbConstraintImporter().importNativeCrac(getClass().getResourceAsStream("/merged_cb/without_RA.xml"));
        assertEquals(17, fbConstraint.getFlowBasedDocumentVersion());
    }

    @Test
    public void testExistFalseErrorInSchema() {
        NativeCracImporter importer = new FbConstraintImporter();
        assertFalse(importer.exists("corrupted.xml", getClass().getResourceAsStream("/merged_cb/corrupted.xml")));
    }

    @Test
    public void testExistFalseUnknownVersion() {
        NativeCracImporter importer = new FbConstraintImporter();
        assertFalse(importer.exists("schema_v99.xml", getClass().getResourceAsStream("/merged_cb/schema_v99.xml")));
    }
}
