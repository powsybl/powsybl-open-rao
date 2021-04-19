/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracDeserializer extends JsonDeserializer<Crac> {

    private Set<DeserializedNetworkElement> deserializedNetworkElements;
    private Map<String, String> deserializedNetworkElementsNamesPerId;

    @Override
    public Crac deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        jsonParser.nextToken();
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

        jsonParser.nextToken();
        if (!jsonParser.getCurrentName().equals(NETWORK_ELEMENTS)) {
            throw new FaraoException(String.format("'%s' field is expected in the fourth line of the json file.", NETWORK_ELEMENTS));
        }
        jsonParser.nextToken();
        deserializedNetworkElements = jsonParser.readValueAs(new TypeReference<Set<DeserializedNetworkElement>>() {
        });
        deserializedNetworkElementsNamesPerId = new HashMap<>();
        deserializedNetworkElements.forEach(ne -> deserializedNetworkElementsNamesPerId.put(ne.getId(), ne.getName()));

        Crac crac = CracFactory.findDefault().create(id, name);

        // deserialize the following lines of the CracImpl
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.getCurrentName()) {
                case CONTINGENCIES:
                    jsonParser.nextToken();
                    ContingencyArrayDeserializer.deserialize(jsonParser, crac, deserializedNetworkElementsNamesPerId);
                    break;

                /*case CNECS:
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
                    break;*/

                default:
                    //throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
                    break;
            }
        }
        return crac;
    }
}
