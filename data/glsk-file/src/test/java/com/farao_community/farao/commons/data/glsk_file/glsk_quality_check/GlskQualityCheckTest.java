package com.farao_community.farao.commons.data.glsk_file.glsk_quality_check;

import com.farao_community.farao.commons.data.glsk_file.UcteGlskDocument;
import com.farao_community.farao.commons.data.glsk_file.actors.UcteGlskDocumentImporter;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static org.junit.Assert.assertEquals;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public class GlskQualityCheckTest {

    private static final String COUNTRYTEST = "/20170322_1844_SN3_FR2_GLSK_test.xml";
    private static final String FIRST_ERROR = "/20170322_1844_SN3_FR2_GLSK_error_1.xml";

    private Path getResourceAsPath(String resource) {
        return Paths.get(getResourceAsPathString(resource));
    }

    private String getResourceAsPathString(String resource) {
        return getClass().getResource(resource).getPath();
    }

    private InputStream getResourceAsInputStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    GlskQualityCheck glskQualityCheck;

    @Before
    public void setUp() {

    }

    @Test
    public void qualityCheckWithCorrectValue() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocumentImporter.importGlsk(getResourceAsInputStream(COUNTRYTEST));
        glskQualityCheck = new GlskQualityCheck();
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskQualityCheckImporter glskQualityCheckImporter = GlskQualityCheckImporter.checkFromObject(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"));

        assertEquals(0, glskQualityCheckImporter.getQualityReports().size());
    }

    @Test
    public void qualityCheckWithError1() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocumentImporter.importGlsk(getResourceAsInputStream(FIRST_ERROR));
        glskQualityCheck = new GlskQualityCheck();
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskQualityCheckImporter glskQualityCheckImporter = GlskQualityCheckImporter.checkFromObject(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"));

        assertEquals(1, glskQualityCheckImporter.getQualityReports().size());
        assertEquals("GSK node is not found in CGM", glskQualityCheckImporter.getQualityReports().get(0).getMessage());
        assertEquals("FFR2AA2 ", glskQualityCheckImporter.getQualityReports().get(0).getNodeId());
        assertEquals("10YFR-RTE------C", glskQualityCheckImporter.getQualityReports().get(0).getTso());
    }

    @Test
    public void qualityCheckWithError2() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocumentImporter.importGlsk(getResourceAsInputStream(COUNTRYTEST));
        glskQualityCheck = new GlskQualityCheck();
        Network network = Importers.loadNetwork("testCase_error_2.xiidm", getClass().getResourceAsStream("/testCase_error_2.xiidm"));
        GlskQualityCheckImporter glskQualityCheckImporter = GlskQualityCheckImporter.checkFromObject(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"));

        assertEquals(1, glskQualityCheckImporter.getQualityReports().size());
        assertEquals("The GSK node is present but it's not representing a Generator or Load", glskQualityCheckImporter.getQualityReports().get(0).getMessage());
        assertEquals("FFR2AA1 ", glskQualityCheckImporter.getQualityReports().get(0).getNodeId());
        assertEquals("10YFR-RTE------C", glskQualityCheckImporter.getQualityReports().get(0).getTso());
    }

}
