/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.ContingencyAdder;
import com.powsybl.openrao.data.cracapi.Crac;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class ContingencyArrayDeserializer {

    private ContingencyArrayDeserializer() { }

    static void deserialize(JsonParser jsonParser, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", CONTINGENCIES, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            ContingencyAdder adder = crac.newContingency();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case ID:
                        adder.withId(jsonParser.nextTextValue());
                        break;
                    case NAME:
                        adder.withName(jsonParser.nextTextValue());
                        break;
                    case NETWORK_ELEMENTS_IDS:
                        jsonParser.nextToken();
                        Set<String> networkElementIds = jsonParser.readValueAs(new TypeReference<HashSet<String>>() {
                        });
                        networkElementIds.forEach(neId -> {
                                if (networkElementsNamesPerId.containsKey(neId)) {
                                    adder.withNetworkElement(neId, networkElementsNamesPerId.get(neId));
                                } else {
                                    adder.withNetworkElement(neId);
                                }
                            }
                        );
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in Contingency: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }
}
