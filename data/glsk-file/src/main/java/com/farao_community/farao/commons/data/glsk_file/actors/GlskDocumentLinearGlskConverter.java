/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.glsk_file.actors;

import com.farao_community.farao.commons.chronology.DataChronology;
import com.farao_community.farao.commons.chronology.DataChronologyImpl;
import com.farao_community.farao.commons.data.glsk_file.GlskDocument;
import com.farao_community.farao.commons.data.glsk_file.GlskPoint;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Import and Convert a CIM type GlskDocument to a map:
 * Key: country, Value: DataChronology of LinearGlsk
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class GlskDocumentLinearGlskConverter {

    /**
     * @param filepath file path in Path
     * @param network iidm network
     * @return map <country, LinearGlsk's datachronology>
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<LinearGlsk>> convertGlskDocumentToLinearGlskDataChronologyFromFilePath(Path filepath, Network network) throws ParserConfigurationException, SAXException, IOException {
        InputStream data = new FileInputStream(filepath.toFile());
        return convertGlskDocumentToLinearGlskDataChronologyFromInputStream(data, network);
    }

    /**
     * @param filepathstring file full path in string
     * @param network iidm network
     * @return map <country, LinearGlsk's datachronology>
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<LinearGlsk>> convertGlskDocumentToLinearGlskDataChronologyFromFilePathString(String filepathstring, Network network) throws ParserConfigurationException, SAXException, IOException {
        InputStream data = new FileInputStream(filepathstring);
        return convertGlskDocumentToLinearGlskDataChronologyFromInputStream(data, network);
    }

    /**
     * @param filename file name in src..resources
     * @param network iidm network
     * @return map <country, LinearGlsk's datachronology>
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<LinearGlsk>> convertGlskDocumentToLinearGlskDataChronologyFromFileName(String filename, Network network) throws ParserConfigurationException, SAXException, IOException {
        InputStream data = getClass().getResourceAsStream(filename);
        return convertGlskDocumentToLinearGlskDataChronologyFromInputStream(data, network);
    }

    /**
     * @param data InputStream
     * @param network iidm network
     * @return map <country, LinearGlsk's datachronology>
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<LinearGlsk>> convertGlskDocumentToLinearGlskDataChronologyFromInputStream(InputStream data, Network network) throws ParserConfigurationException, SAXException, IOException {
        return convertGlskDocumentToLinearGlskDataChronology(new GlskDocumentImporter().importGlskDocumentFromInputStream(data), network);
    }

    /**
     * @param glskDocument glsk document object
     * @param network iidm network
     * @return map <country, LinearGlsk's datachronology>
     */
    public Map<String, DataChronology<LinearGlsk>> convertGlskDocumentToLinearGlskDataChronology(GlskDocument glskDocument, Network network) {

        List<String> countries = glskDocument.getCountries();

        Map<String, DataChronology<LinearGlsk>> countryLinearGlskDataChronologyMap = new HashMap<>();

        for (String country : countries) {
            DataChronology<LinearGlsk> dataChronology = DataChronologyImpl.create();

            //mapping with DataChronology
            List<GlskPoint> glskPointList = glskDocument.getMapGlskTimeSeries().get(country).getGlskPointListInGlskTimeSeries();
            for (GlskPoint point : glskPointList) {
                LinearGlsk linearGlsk = new GlskPointLinearGlskConverter().convertGlskPointToLinearGlsk(network, point);
                dataChronology.storeDataOnInterval(linearGlsk, point.getPointInterval());
            }
            countryLinearGlskDataChronologyMap.put(country, dataChronology);
        }

        return countryLinearGlskDataChronologyMap;
    }


    /**
     * Converter a glsk document to map < country, datachronology<glskpoint>>
     * @param filename GLSK document filename
     * @return Map Key: country, Value: DataChronology of GLSK Point
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<GlskPoint>> convertGlskDocumentToGlskPointDataChronologyFromFileName(String filename) throws ParserConfigurationException, SAXException, IOException {
        GlskDocumentImporter importer = new GlskDocumentImporter();
        GlskDocument glskDocument = importer.importGlskDocumentWithFilename(filename);
        return convertGlskDocumentToGlskPointDataChronology(glskDocument);
    }

    /**
     * @param glskDocument GlskDocument object
     * @return Map Key: country, Value: DataChronology of GLSK Point
     */
    public Map<String, DataChronology<GlskPoint>> convertGlskDocumentToGlskPointDataChronology(GlskDocument glskDocument) {
        List<String> countries = glskDocument.getCountries();

        Map<String, DataChronology<GlskPoint>> countryGlskDataChronologyMap = new HashMap<>();

        for (String country : countries) {
            DataChronology<GlskPoint> dataChronology = DataChronologyImpl.create();

            //mapping with DataChronology
            List<GlskPoint> glskPointList = glskDocument.getMapGlskTimeSeries().get(country).getGlskPointListInGlskTimeSeries();
            for (GlskPoint point : glskPointList) {
                dataChronology.storeDataOnInterval(point, point.getPointInterval());
            }
            countryGlskDataChronologyMap.put(country, dataChronology);
        }

        return countryGlskDataChronologyMap;
    }
}
