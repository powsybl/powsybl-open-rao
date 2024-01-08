/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_creation.creator.fb_constraint.importer;

import com.powsybl.open_rao.data.crac_creation.creator.fb_constraint.FbConstraint;
import com.powsybl.open_rao.data.native_crac_io_api.NativeCracImporter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
class FbConstraintImporterTest {

    @Test
    void testExistSchemaVersion18True() {
        NativeCracImporter importer = new FbConstraintImporter();
        assertTrue(importer.exists("without_RA.xml", getClass().getResourceAsStream("/merged_cb/without_RA.xml")));
    }

    @Test
    void testImportSchemaVersion18() {
        FbConstraint fbConstraint = new FbConstraintImporter().importNativeCrac(getClass().getResourceAsStream("/merged_cb/without_RA.xml"));
        assertEquals(17, fbConstraint.getFlowBasedDocumentVersion());
    }

    @Test
    void testExistFalseErrorInSchema() {
        NativeCracImporter importer = new FbConstraintImporter();
        assertFalse(importer.exists("corrupted.xml", getClass().getResourceAsStream("/merged_cb/corrupted.xml")));
    }

    @Test
    void testExistFalseUnknownVersion() {
        NativeCracImporter importer = new FbConstraintImporter();
        assertFalse(importer.exists("schema_v99.xml", getClass().getResourceAsStream("/merged_cb/schema_v99.xml")));
    }
}
