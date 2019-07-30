/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.chronology.DataChronology;
import com.farao_community.farao.commons.data.glsk_file.actors.GlskDocumentLinearGlskConverter;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * FlowBased Glsk Values Provider
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public class FlowBasedGlskValuesProvider {
    /**
     * Network we need a network to import Glsk document
     */
    private Network network;

    /**
     * Glsk file path in String
     */
    private String filePathString;

    /**
     * map of country code and DataChronology of LinearGlsk created from GlskFile
     */
    private Map<String, DataChronology<LinearGlsk> > mapCountryDataChronologyLinearGlsk;

    /**
     * constructor
     */
    public FlowBasedGlskValuesProvider() {
        network = null;
        filePathString = "";
        mapCountryDataChronologyLinearGlsk = null;
    }

    /**
     * Constructor
     * @param network network
     * @param filePathString glsk file name
     */
    public FlowBasedGlskValuesProvider(Network network, String filePathString) throws ParserConfigurationException, SAXException, IOException {
        this.network = network;
        this.filePathString = filePathString;

        mapCountryDataChronologyLinearGlsk = createDataChronologyLinearGlskMap(network, filePathString);
    }

    /**
     * Create map from Glsk file
     * @param network Network
     * @param filePathString Glsk File name
     * @return map of data chronology of linear Glsk
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    Map<String, DataChronology<LinearGlsk> > createDataChronologyLinearGlskMap(Network network, String filePathString) throws IOException, SAXException, ParserConfigurationException {
        return new GlskDocumentLinearGlskConverter().convertGlskDocumentToLinearGlskDataChronologyFromFilePathString(filePathString, network);
    }

    /**
     * @param instant Flowbased domain is time dependent. Instant give the time stamp t of the flowbased domain
     * @return LinearGlsk map of instant
     */
    Map<String, LinearGlsk> getCountryLinearGlskMap(Instant instant) {

        Map<String, LinearGlsk> linearGlskMap = new HashMap<>();
        for (Map.Entry<String, DataChronology<LinearGlsk> > entry: mapCountryDataChronologyLinearGlsk.entrySet()) {
            DataChronology<LinearGlsk> dataChronology = entry.getValue();
            Optional<LinearGlsk> linearGlskOptional = dataChronology.getDataForInstant(instant);
            if (linearGlskOptional.isPresent()) {
                linearGlskMap.put(entry.getKey(), linearGlskOptional.get());
            } else {
                throw new FaraoException("No LinearGlsk found for instant " + instant.toString() + "in " + filePathString);
            }
        }

        return linearGlskMap;
    }

    /**
     * @param instant Instant time stamp t of the flowbased domain
     * @param country country EIC code
     * @return Linear Glsk: linear Glsk of country of instant t
     */
    LinearGlsk getCountryLinearGlsk(Instant instant, String country) {

        if (!mapCountryDataChronologyLinearGlsk.containsKey(country)) {
            throw new FaraoException("No LinearGlsk found for country " + country + " in " + filePathString);
        } else {
            DataChronology<LinearGlsk> dataChronology = mapCountryDataChronologyLinearGlsk.get(country);
            Optional<LinearGlsk> linearGlskOptional = dataChronology.getDataForInstant(instant);
            if (linearGlskOptional.isPresent()) {
                return linearGlskOptional.get();
            } else {
                throw new FaraoException("No LinearGlsk found for instant " + instant.toString() + "in " + filePathString);
            }
        }
    }

    /**
     * @return Network reference network
     */
    public Network getNetwork() {
        return network;
    }

    /**
     * @param network setter
     */
    public void setNetwork(Network network) {
        this.network = network;
    }

    /**
     * @return getter file path string
     */
    public String getFilePathString() {
        return filePathString;
    }

    /**
     * @param filePathString set glsk document file path
     */
    public void setFilePathString(String filePathString) {
        this.filePathString = filePathString;
    }

    /**
     * @param mapCountryDataChronologyLinearGlsk set linear glsk 's data chronology map
     */
    public void setMapCountryDataChronologyLinearGlsk(Map<String, DataChronology<LinearGlsk> > mapCountryDataChronologyLinearGlsk) {
        this.mapCountryDataChronologyLinearGlsk = mapCountryDataChronologyLinearGlsk;
    }

    /**
     * @return getter mapCountryDataChronologyLinearGlsk
     */
    public Map<String, DataChronology<LinearGlsk>> getMapCountryDataChronologyLinearGlsk() {
        return mapCountryDataChronologyLinearGlsk;
    }

}
