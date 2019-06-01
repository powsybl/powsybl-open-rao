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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowBasedGlskValuesProvider.class);

    private Network network;
    private String filePathString;
    private Map<String, DataChronology<LinearGlsk> > mapCountryDataChronologyLinearGlsk;

    public FlowBasedGlskValuesProvider() {
        network = null;
        filePathString = "";
        mapCountryDataChronologyLinearGlsk = null;
    }

    public FlowBasedGlskValuesProvider(Network network, String filePathString) {
        this.network = network;
        this.filePathString = filePathString;
        try {
            mapCountryDataChronologyLinearGlsk = createDataChronologyLinearGlskMap(network, filePathString);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            LOGGER.error(String.valueOf(e));
        }
    }

    Map<String, DataChronology<LinearGlsk> > createDataChronologyLinearGlskMap(Network network, String filePathString) throws IOException, SAXException, ParserConfigurationException {
        return new GlskDocumentLinearGlskConverter().convertGlskDocumentToLinearGlskDataChronologyFromFilePathString(filePathString, network);
    }

    Map<String, DataChronology<LinearGlsk> > getCountryDataChronologyLinearGlskMap() {
        return mapCountryDataChronologyLinearGlsk;
    }

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

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

}
