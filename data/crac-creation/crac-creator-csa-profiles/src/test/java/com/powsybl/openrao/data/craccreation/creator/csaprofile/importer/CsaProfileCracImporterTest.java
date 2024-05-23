/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile.importer;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.CsaProfileCrac;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.CurrentLimit;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.Contingency;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 */
class CsaProfileCracImporterTest {
    private static ReportNode buildNewRootNode() {
        return ReportNode.newRootReportNode().withMessageTemplate("Test report node", "This is a parent report node for report tests").build();
    }

    @Test
    void getFormat() {
        CsaProfileCracImporter csaProfileCracImporter = new CsaProfileCracImporter();
        assertEquals("CsaProfileCrac", csaProfileCracImporter.getFormat());
    }

    @Test
    void testExists() {
        InputStream is1 = getClass().getResourceAsStream("/profiles/contingencies/Contingencies.zip");
        CsaProfileCracImporter importer = new CsaProfileCracImporter();
        assertTrue(importer.exists("/profiles/contingencies/Contingencies.zip", is1));
    }

    @Test
    void testImportNativeCracWithoutSubdirectory() {
        CsaProfileCracImporter csaProfileCracImporter = new CsaProfileCracImporter();
        InputStream is1 = getClass().getResourceAsStream("/profiles/TestCaseWithoutSubdirectory.zip");
        CsaProfileCrac csaProfileCrac = csaProfileCracImporter.importNativeCrac(is1);
        assertNotNull(csaProfileCrac);

        Set<Contingency> contingencies = csaProfileCrac.getContingencies();
        assertEquals(1, contingencies.size());

        Set<CurrentLimit> currentLimits = csaProfileCrac.getCurrentLimits();
        assertEquals(52, currentLimits.size());
    }

    @Test
    void testGeneratedReportNode() throws IOException, URISyntaxException {
        ReportNode reportNode = buildNewRootNode();

        CsaProfileCracImporter csaProfileCracImporter = new CsaProfileCracImporter();
        InputStream is1 = getClass().getResourceAsStream("/profiles/TestCaseWithoutSubdirectory.zip");
        CsaProfileCrac csaProfileCrac = csaProfileCracImporter.importNativeCrac(is1, reportNode);

        String expected = Files.readString(Path.of(getClass().getResource("/reports/expectedReportNodeContentCsaProfileImporter.txt").toURI()));
        try (StringWriter writer = new StringWriter()) {
            reportNode.print(writer);
            String actual = writer.toString();
            assertEquals(expected, actual);
        }
    }
}
