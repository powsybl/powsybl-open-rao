/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationNames.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class ContingencyArrayDeserializer {

    private ContingencyArrayDeserializer() { }

    static void deserialize(JsonParser jsonParser, Crac crac) throws IOException {

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {

            // String type = null;
            String id = null;
            String name = null;
            Set<String> networkElementsId = new HashSet<>();

            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {

                    /*case TYPE:
                        type = jsonParser.nextTextValue();
                        if (!type.equals(COMPLEX_CONTINGENCY_TYPE) && !type.equals(XNODE_CONTINGENCY_TYPE)) {
                            throw new FaraoException(String.format("CracImpl can only deserialize %s and %s contingency types", COMPLEX_CONTINGENCY_TYPE, XNODE_CONTINGENCY_TYPE));
                        }
                        break;*/

                    case ID:
                        id = jsonParser.nextTextValue();
                        break;

                    case NAME:
                        name = jsonParser.nextTextValue();
                        break;

                    case NETWORK_ELEMENTS:
                        jsonParser.nextToken();
                        networkElementsIds = jsonParser.readValueAs(new TypeReference<HashSet<String>>() {
                        });
                        break;

                    case XNODE_IDS:
                        jsonParser.nextToken();
                        xnodeIds = jsonParser.readValueAs(new TypeReference<HashSet<String>>() {
                        });
                        break;

                    default:
                        throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
                }
            }

            //add contingency in Crac

        }
    }
}
