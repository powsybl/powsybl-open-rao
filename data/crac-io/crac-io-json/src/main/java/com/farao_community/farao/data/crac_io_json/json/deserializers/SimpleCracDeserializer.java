/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_impl.NetworkElementImpl;
import com.farao_community.farao.data.crac_impl.CracImpl;
import com.farao_community.farao.data.crac_api.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.*;

import static com.farao_community.farao.data.crac_io_json.json.JsonSerializationNames.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SimpleCracDeserializer extends JsonDeserializer<CracImpl> {

    @Override
    public CracImpl deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        // the Json file should start with the id and the name of the CracImpl
        if (!jsonParser.getCurrentName().equals(ID)) {
            throw new FaraoException("'id' field is expected in the third line of the json file.");
        }
        String id = jsonParser.nextTextValue();

        jsonParser.nextToken();
        if (!jsonParser.getCurrentName().equals(NAME)) {
            throw new FaraoException("'name' field is expected in the fourth line of the json file.");
        }
        String name = jsonParser.nextTextValue();

        CracImpl simpleCrac = new CracImpl(id, name);

        // deserialize the following lines of the CracImpl
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.getCurrentName()) {

                case NETWORK_DATE:
                    DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    DateTime time = format.parseDateTime(jsonParser.nextTextValue());
                    simpleCrac.setNetworkDate(time);
                    break;

                case NETWORK_ELEMENTS:
                    jsonParser.nextToken();
                    Set<NetworkElementImpl> networkElements = jsonParser.readValueAs(new TypeReference<Set<NetworkElementImpl>>() {
                    });
                    networkElements.forEach(simpleCrac::addNetworkElement);
                    break;

                case CONTINGENCIES:
                    jsonParser.nextToken();
                    ContingencyDeserializer.deserialize(jsonParser, simpleCrac);
                    break;

                case CNECS:
                    jsonParser.nextToken();
                    CnecDeserializer.deserialize(jsonParser, deserializationContext, simpleCrac);
                    break;

                case RANGE_ACTIONS:
                    jsonParser.nextToken();
                    RangeActionDeserializer.deserialize(jsonParser, simpleCrac, deserializationContext);
                    break;

                case NETWORK_ACTIONS:
                    jsonParser.nextToken();
                    Set<NetworkAction> networkActions = NetworkActionDeserializer.deserialize(jsonParser, simpleCrac, deserializationContext);
                    networkActions.forEach(simpleCrac::addNetworkAction);
                    break;

                case EXTENSIONS:
                    jsonParser.nextToken();
                    List<Extension<Crac>> extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                    ExtensionsHandler.getExtensionsSerializers().addExtensions(simpleCrac, extensions);
                    break;

                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());

            }
        }
        return simpleCrac;
    }
}
