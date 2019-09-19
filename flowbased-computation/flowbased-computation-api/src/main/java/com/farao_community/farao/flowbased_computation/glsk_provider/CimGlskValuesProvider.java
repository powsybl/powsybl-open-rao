/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.glsk_provider;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.chronology.DataChronology;
import com.farao_community.farao.commons.data.glsk_file.actors.GlskDocumentLinearGlskConverter;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Map;

/**
 * FlowBased Glsk Values Provider, for Cim Glsk format
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public class CimGlskValuesProvider extends GlskValuesProvider{

    /**
     * constructor
     */
    public CimGlskValuesProvider() {
        super();
    }

    /**
     * Constructor
     * @param network network
     * @param filePathString glsk file name
     */
    public CimGlskValuesProvider(Network network, String filePathString) {
        super(network, filePathString);
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
    public Map<String, DataChronology<LinearGlsk> > createDataChronologyLinearGlskMap(Network network, String filePathString) throws FaraoException {
        try {
            return new GlskDocumentLinearGlskConverter().convertGlskDocumentToLinearGlskDataChronologyFromFilePathString(filePathString, network);
        } catch (IOException | SAXException | ParserConfigurationException e){
            throw new FaraoException(e);
        }
    }
}
