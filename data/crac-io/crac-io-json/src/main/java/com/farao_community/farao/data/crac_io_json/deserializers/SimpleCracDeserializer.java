/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.*;

import static com.farao_community.farao.data.crac_io_json.deserializers.DeserializerNames.*;
import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SimpleCracDeserializer extends JsonDeserializer<SimpleCrac> {

    @Override
    public SimpleCrac deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
       /*
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node.get(ID) == null || node.get(NAME) == null) {
            throw new FaraoException("Json crac has no field 'id' or no field 'name'");
        }
        SimpleCrac simpleCrac = new SimpleCrac(node.get(ID).asText(), node.get(NAME).asText());

        // todo : find a cleaner way to reset the parser or to initialize the Crac
        jsonParser.nextToken();

        */
       SimpleCrac simpleCrac = new SimpleCrac("id");

        while (jsonParser.currentToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.getCurrentName()) {

                case DeserializerNames.TYPE:
                case ID:
                case NAME:
                    jsonParser.nextToken();
                    break;

                case NETWORK_ELEMENTS:
                    jsonParser.nextToken();
                    Set<NetworkElement> networkElements = jsonParser.readValueAs(new TypeReference<Set<NetworkElement>>() {
                    });
                    networkElements.forEach(simpleCrac::addNetworkElement);
                    break;

                case INSTANTS:
                    jsonParser.nextToken();
                    Set<Instant> instants = jsonParser.readValueAs(new TypeReference<Set<Instant>>() {
                    });
                    instants.forEach(simpleCrac::addInstant);
                    break;

                case CONTINGENCIES:
                    jsonParser.nextToken();
                    ContingencyDeserializer.deserialize(jsonParser, simpleCrac);
                    break;

                case STATES:
                    jsonParser.nextToken();
                    StateDeserializer.deserialize(jsonParser, simpleCrac);
                    break;

                case CNECS:
                    jsonParser.nextToken();
                    CnecDeserializer.deserialize(jsonParser, deserializationContext, simpleCrac);
                    break;

                case RANGE_ACTIONS:
                    jsonParser.nextToken();
                    RangeActionDeserializer.deserialize(jsonParser, simpleCrac);
                    break;

                case NETWORK_ACTIONS:
                    jsonParser.nextToken();
                    Set<NetworkAction> networkActions = NetworkActionDeserializer.deserialize(jsonParser, simpleCrac);
                    networkActions.forEach(simpleCrac::addNetworkAction);
                    break;

                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());

            }
            jsonParser.nextToken();
        }
        return simpleCrac;
    }
}
