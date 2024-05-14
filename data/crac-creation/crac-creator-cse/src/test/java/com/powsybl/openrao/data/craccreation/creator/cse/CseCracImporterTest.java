/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.cse;

import com.powsybl.commons.report.ReportNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        CseCrac cseCrac = importer.importNativeCrac(is, ReportNode.NO_OP);
        assertEquals("ruleToBeDefined", cseCrac.getCracDocument().getDocumentIdentification().getV());
    }

    @Test
    void importNativeCracWithMNE() {
        InputStream is = getClass().getResourceAsStream("/cracs/cse_crac_with_MNE.xml");
        CseCracImporter importer = new CseCracImporter();
        CseCrac cseCrac = importer.importNativeCrac(is, ReportNode.NO_OP);
        assertEquals(100, cseCrac.getCracDocument().getCRACSeries().get(0).getMonitoredElements().getMonitoredElement().get(0).getBranch().get(0).getIlimitMNE().getV());
    }

    @Test
    @Disabled("TODO find a valid CSE crac file...")
    void testGeneratedReportNode() throws IOException, URISyntaxException {
        ReportNode reportNode = ReportNode.newRootReportNode()
            .withMessageTemplate("Test report node", "This is a parent report node for report tests")
            .build();

        String filename = "/cracs/cse_crac_valid.xml";
        InputStream is = getClass().getResourceAsStream(filename);
        CseCracImporter importer = new CseCracImporter();
        importer.exists(filename, is, reportNode);
        String expected = Files.readString(Path.of(getClass().getResource("/expectedReportNodeContent.txt").toURI()));
        try (StringWriter writer = new StringWriter()) {
            reportNode.print(writer);
            String actual = writer.toString();
            assertEquals(expected, actual);
        }
    }
}
