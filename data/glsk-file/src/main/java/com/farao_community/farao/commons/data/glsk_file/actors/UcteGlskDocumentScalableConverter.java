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
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.network.Network;
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
 * Import and Convert a UCTE type GLSK document to Scalable
 * return a map:
 * Key: country
 * Value: DataChronology of Scalable
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class UcteGlskDocumentScalableConverter {

    /**
     * @param filepath file path as Path
     * @param network iidm network
     * @return A map associating a DataChronology of Scalable for each country
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<Scalable>> convertUcteGlskDocumentToScalableDataChronologyFromFilePath(Path filepath, Network network) throws ParserConfigurationException, SAXException, IOException {
        InputStream data = new FileInputStream(filepath.toFile());
        return convertUcteGlskDocumentToScalableDataChronologyFromInputStream(data, network);
    }

    /**
     * @param filepathstring file full path in string
     * @param network iidm network
     * @return A map associating a DataChronology of Scalable for each country
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<Scalable>> convertUcteGlskDocumentToScalableDataChronologyFromFilePathString(String filepathstring, Network network) throws ParserConfigurationException, SAXException, IOException {
        InputStream data = new FileInputStream(filepathstring);
        return convertUcteGlskDocumentToScalableDataChronologyFromInputStream(data, network);
    }

    /**
     * @param filename file name in src..resources
     * @param network iidm network
     * @return A map associating a DataChronology of Scalable for each country
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<Scalable>> convertUcteGlskDocumentToScalableDataChronologyFromFileName(String filename, Network network) throws ParserConfigurationException, SAXException, IOException {
        InputStream data = getClass().getResourceAsStream(filename);
        return convertUcteGlskDocumentToScalableDataChronologyFromInputStream(data, network);
    }

    /**
     * @param data InputStream
     * @param network iidm network
     * @return A map associating a DataChronology of Scalable for each country
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<Scalable>> convertUcteGlskDocumentToScalableDataChronologyFromInputStream(InputStream data, Network network) throws ParserConfigurationException, SAXException, IOException {
        return convertUcteGlskDocuementToScalableDataChronology(new UcteGlskDocumentImporter().importUcteGlskDocumentFromInputStream(data), network);
    }

    /**
     * @param ucteGlskDocument UcteGlskDocument object
     * @param network iidm network
     * @return A map associating a DataChronology of Scalable for each country
     */
    public Map<String, DataChronology<Scalable>> convertUcteGlskDocuementToScalableDataChronology(UcteGlskDocument ucteGlskDocument, Network network) {
        Map<String, DataChronology<Scalable>> chronologyScalableMap = new HashMap<>();

        List<String> countries = ucteGlskDocument.getCountries();

        for (String country : countries) {
            DataChronology<Scalable> dataChronology = DataChronologyImpl.create();
            List<GlskPoint> glskPointList = ucteGlskDocument.getUcteGlskPointsByCountry().get(country);
            for (GlskPoint point : glskPointList) {
                Scalable scalable = new GlskPointScalableConverter().convertGlskPointToScalable(network, point, TypeGlskFile.UCTE);
                dataChronology.storeDataOnInterval(scalable, point.getPointInterval());
            }
            chronologyScalableMap.put(country, dataChronology);
        }

        return chronologyScalableMap;

    }

}
