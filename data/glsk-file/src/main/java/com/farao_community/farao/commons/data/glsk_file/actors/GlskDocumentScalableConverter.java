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
 * Import and Convert a CIM type GlskDocument to Scalable
 * create a map:
 * Key: country, Value: DataChronology of Scalable
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class GlskDocumentScalableConverter {

    /**
     * @param filepath file path in Path
     * @param network iidm network
     * @return map <country, Scalable's datachronology>
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<Scalable>> convertGlskDocumentToScalableDataChronologyFromFilePath(Path filepath, Network network) throws ParserConfigurationException, SAXException, IOException {
        InputStream data = new FileInputStream(filepath.toFile());
        return convertGlskDocumentToScalableDataChronologyFromInputStream(data, network);
    }

    /**
     * @param filepathstring file full path in string
     * @param network iidm network
     * @return map <country, Scalable's datachronology>
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<Scalable>> convertGlskDocumentToScalableDataChronologyFromFilePathString(String filepathstring, Network network) throws ParserConfigurationException, SAXException, IOException {
        InputStream data = new FileInputStream(filepathstring);
        return convertGlskDocumentToScalableDataChronologyFromInputStream(data, network);
    }

    /**
     * @param filename file name in src..resources
     * @param network iidm network
     * @return map <country, Scalable's datachronology>
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<Scalable>> convertGlskDocumentToScalableDataChronologyFromFileName(String filename, Network network) throws ParserConfigurationException, SAXException, IOException {
        InputStream data = getClass().getResourceAsStream(filename);
        return convertGlskDocumentToScalableDataChronologyFromInputStream(data, network);
    }

    /**
     * @param data InputStream
     * @param network iidm network
     * @return map <country, Scalable's datachronology>
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Map<String, DataChronology<Scalable>> convertGlskDocumentToScalableDataChronologyFromInputStream(InputStream data, Network network) throws ParserConfigurationException, SAXException, IOException {
        return convertGlskDocumentToScalableDataChronology(new GlskDocumentImporter().importGlskDocumentFromInputStream(data), network);
    }

    /**
     * @param glskDocument glsk document object
     * @param network iidm network
     * @return map <country, Scalable's datachronology>
     */
    public Map<String, DataChronology<Scalable>> convertGlskDocumentToScalableDataChronology(GlskDocument glskDocument, Network network) {

        List<String> countries = glskDocument.getCountries();

        Map<String, DataChronology<Scalable>> countryScalableDataChronologyMap = new HashMap<>();

        for (String country : countries) {
            DataChronology<Scalable> dataChronology = DataChronologyImpl.create();

            //mapping with DataChronology
            List<GlskPoint> glskPointList = glskDocument.getMapGlskTimeSeries().get(country).getGlskPointListInGlskTimeSeries();
            for (GlskPoint point : glskPointList) {
                Scalable scalable = new GlskPointScalableConverter().convertGlskPointToScalable(network, point, TypeGlskFile.CIM);
                dataChronology.storeDataOnInterval(scalable, point.getPointInterval());
            }
            countryScalableDataChronologyMap.put(country, dataChronology);
        }

        return countryScalableDataChronologyMap;
    }

}
