/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.glsk_file.actors;

import com.farao_community.farao.commons.chronology.DataChronology;
import com.farao_community.farao.commons.chronology.DataChronologyImpl;
import com.farao_community.farao.commons.data.glsk_file.GlskPoint;
import com.farao_community.farao.commons.data.glsk_file.UcteGlskDocument;
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
 * Import and Convert a UCTE GLSK document to LinearGlsk
 * return a map :
 * Key: country, Value: DataChronology of LinearGlsk
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class UcteGlskDocumentLinearGlskConverter {

    /**
     * @param filepath file path as Path
     * @param network iidm network
     * @return map <country, Datachronology's LinearGlsk>
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<LinearGlsk>> convertUcteGlskDocumentToLinearGlskDataChronologyFromFilePath(Path filepath, Network network) throws ParserConfigurationException, SAXException, IOException {
        InputStream data = new FileInputStream(filepath.toFile());
        return convertUcteGlskDocumentToLinearGlskDataChronologyFromInputStream(data, network);
    }

    /**
     * @param filepathstring file full path in string
     * @param network iidm network
     * @return map <country, Datachronology's LinearGlsk>
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<LinearGlsk>> convertUcteGlskDocumentToLinearGlskDataChronologyFromFilePathString(String filepathstring, Network network) throws ParserConfigurationException, SAXException, IOException {
        InputStream data = new FileInputStream(filepathstring);
        return convertUcteGlskDocumentToLinearGlskDataChronologyFromInputStream(data, network);
    }

    /**
     * @param filename file name in src..resources
     * @param network iidm network
     * @return map <country, Datachronology's LinearGlsk>
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<LinearGlsk>> convertUcteGlskDocumentToLinearGlskDataChronologyFromFileName(String filename, Network network) throws ParserConfigurationException, SAXException, IOException {
        InputStream data = getClass().getResourceAsStream(filename);
        return convertUcteGlskDocumentToLinearGlskDataChronologyFromInputStream(data, network);
    }

    /**
     * @param data InputStream
     * @param network iidm network
     * @return map <country, Datachronology's LinearGlsk>
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<LinearGlsk>> convertUcteGlskDocumentToLinearGlskDataChronologyFromInputStream(InputStream data, Network network) throws ParserConfigurationException, SAXException, IOException {
        return convertUcteGlskDocuementToLinearGlskDataChronology(new UcteGlskDocumentImporter().importUcteGlskDocumentFromInputStream(data), network);
    }

    /**
     * @param ucteGlskDocument UcteGlskDocument object
     * @param network iidm network
     * @return map <country, Datachronology's LinearGlsk>
     */
    public Map<String, DataChronology<LinearGlsk>> convertUcteGlskDocuementToLinearGlskDataChronology(UcteGlskDocument ucteGlskDocument, Network network) {
        Map<String, DataChronology<LinearGlsk>> chronologyLinearGlskMap = new HashMap<>();

        List<String> countries = ucteGlskDocument.getCountries();

        for (String country : countries) {
            DataChronology<LinearGlsk> dataChronology = DataChronologyImpl.create();
            List<GlskPoint> glskPointList = ucteGlskDocument.getUcteGlskPointsByCountry().get(country);
            for (GlskPoint point : glskPointList) {
                LinearGlsk linearGlsk = new GlskPointLinearGlskConverter().convertGlskPointToLinearGlsk(network, point);
                dataChronology.storeDataOnInterval(linearGlsk, point.getPointInterval());
            }
            chronologyLinearGlskMap.put(country, dataChronology);
        }

        return chronologyLinearGlskMap;

    }

    /**
     * @param glskFilename file name in src..resources
     * @return map < country, datachronology<glskpoint>>
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public Map<String, DataChronology<GlskPoint>> convertUcteGlskDocuementToGlskPointDataChronology(String glskFilename) throws IOException, ParserConfigurationException, SAXException {

        UcteGlskDocument ucteGlskDocument = new UcteGlskDocumentImporter().importUcteGlskDocumentWithFilename(glskFilename);

        Map<String, DataChronology<GlskPoint>> chronologyGlskPointMap = new HashMap<>();

        List<String> countries = ucteGlskDocument.getCountries();

        for (String country : countries) {
            DataChronology<GlskPoint> dataChronology = DataChronologyImpl.create();
            List<GlskPoint> glskPointList = ucteGlskDocument.getUcteGlskPointsByCountry().get(country);
            for (GlskPoint point : glskPointList) {
                dataChronology.storeDataOnInterval(point, point.getPointInterval());
            }
            chronologyGlskPointMap.put(country, dataChronology);
        }

        return chronologyGlskPointMap;

    }

}
