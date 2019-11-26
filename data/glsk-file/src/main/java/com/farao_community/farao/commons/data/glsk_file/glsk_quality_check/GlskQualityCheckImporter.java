package com.farao_community.farao.commons.data.glsk_file.glsk_quality_check;

import com.farao_community.farao.commons.data.glsk_file.GlskPoint;
import com.farao_community.farao.commons.data.glsk_file.UcteGlskDocument;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public class GlskQualityCheckImporter {

    private UcteGlskDocument ucteGlskDocument;

    private Network network;

    private Instant instant;

    private Map<String, GlskPoint> glskPointMap;

    private List<QualityReport> qualityReports;

    public UcteGlskDocument getUcteGlskDocument() {
        return ucteGlskDocument;
    }

    public Network getNetwork() {
        return network;
    }

    public Instant getInstant() {
        return instant;
    }

    public GlskQualityCheckImporter(String name, InputStream glsk, InputStream cgm, Instant localDate) throws ParserConfigurationException, SAXException, IOException {
        this(UcteGlskDocument.importGlskFromFile(glsk), Importers.loadNetwork(name, cgm), localDate);
    }

    public GlskQualityCheckImporter(UcteGlskDocument ucteGlskDocument, Network network, Instant instant) {
        this.ucteGlskDocument = ucteGlskDocument;
        this.network = network;
        this.instant = instant;
        this.glskPointMap = ucteGlskDocument.getGlskPointForInstant(instant);
        qualityReports = new GlskQualityCheck().gskQualityCheck(this);
    }

    public List<QualityReport> getQualityReports() {
        return qualityReports;
    }

    public static GlskQualityCheckImporter checkFromFiles(String name, InputStream glsk, InputStream cgm, Instant localDate) throws IOException, SAXException, ParserConfigurationException {
        return new GlskQualityCheckImporter(name, glsk, cgm, localDate);
    }

    public static GlskQualityCheckImporter checkFromObject(UcteGlskDocument ucteGlskDocument, Network network, Instant instant) {
        return new GlskQualityCheckImporter(ucteGlskDocument, network, instant);
    }
}
