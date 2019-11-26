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

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public class GlskQualityCheckTest {

    private static final String COUNTRYFULL = "/20160729_0000_GSK_allday_full.xml";
    private static final String COUNTRYTEST = "/20170322_1844_SN3_FR2_GLSK_test_quality_check.xml";

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
        UcteGlskDocument ucteGlskDocument = UcteGlskDocumentImporter.importGlsk(getResourceAsInputStream(COUNTRYTEST));
        glskQualityCheck = new GlskQualityCheck();
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskQualityCheckImporter.checkFromObject(ucteGlskDocument, network, Instant.parse("2016-07-28T23:00:00Z"));
    }

    @Test
    public void qualityCheck() {

    }

}
