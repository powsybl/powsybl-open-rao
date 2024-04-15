/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.openrao.data.cracio.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.networkaction.GeneratorActionAdder;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;

import java.io.IOException;
import java.util.Map;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
public final class GeneratorActionArrayDeserializer {
    private GeneratorActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, NetworkActionAdder ownerAdder, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", GENERATOR_ACTIONS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            GeneratorActionAdder adder = ownerAdder.newGeneratorAction();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case NETWORK_ELEMENT_ID:
                        String networkElementId = jsonParser.nextTextValue();
                        if (networkElementsNamesPerId.containsKey(networkElementId)) {
                            adder.withNetworkElement(networkElementId, networkElementsNamesPerId.get(networkElementId));
                        } else {
                            adder.withNetworkElement(networkElementId);
                        }
                        break;
                    case ACTIVE_POWER_VALUE:
                        jsonParser.nextToken();
                        adder.withActivePowerValue(jsonParser.getDoubleValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in GeneratorAction: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }
}
