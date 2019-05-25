/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
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

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowBasedGlskValuesProvider {

    public FlowBasedGlskValuesProvider() {
    }

    Map<String, LinearGlsk> getLinearGlskMap(Network network, String filepathstring, Instant instant) throws IOException, SAXException, ParserConfigurationException {

        Map<String, DataChronology<LinearGlsk>> mapGlskDocLinearGlsk = new GlskDocumentLinearGlskConverter().convertGlskDocumentToLinearGlskDataChronologyFromFilePathString(filepathstring, network);

        Map<String, LinearGlsk> linearGlskMap = new HashMap<>();
        for (String country : mapGlskDocLinearGlsk.keySet()) {
            DataChronology<LinearGlsk> dataChronology = mapGlskDocLinearGlsk.get(country);
            LinearGlsk linearGlsk = dataChronology.getDataForInstant(instant).get();
//                    () -> new FaraoException("No LinearGlsk found for instant " + instant.toString() + "in " + filepathstring));
            linearGlskMap.put(country, linearGlsk);
        }

        return linearGlskMap;
    }

    Map<String, DataChronology<LinearGlsk>> getDataChronologyLinearGlskMap(Network network, String filepathstring) throws IOException, SAXException, ParserConfigurationException {
        return new GlskDocumentLinearGlskConverter().convertGlskDocumentToLinearGlskDataChronologyFromFilePathString(filepathstring, network);
    }

    LinearGlsk getCountryLinearGlsk(Network network, String filepathstring, Instant instant, String country) throws IOException, SAXException, ParserConfigurationException {

        Map<String, DataChronology<LinearGlsk>> mapGlskDocLinearGlsk = new GlskDocumentLinearGlskConverter().convertGlskDocumentToLinearGlskDataChronologyFromFilePathString(filepathstring, network);

        if (!mapGlskDocLinearGlsk.containsKey(country)) {
            throw new FaraoException("No LinearGlsk found for country " + country + " in " + filepathstring);
        } else {
            DataChronology<LinearGlsk> dataChronology = mapGlskDocLinearGlsk.get(country);
            return dataChronology.getDataForInstant(instant).get();
//                    () -> new FaraoException("No LinearGlsk found for instant " + instant.toString() + " in " + filepathstring));
        }

    }

}
