/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.networkaction.BoundaryLineActionAdder;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;

import java.io.IOException;
import java.util.Map;

import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.ACTIVE_POWER_VALUE;
import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.BOUNDARYLINE_ACTIONS;
import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.NETWORK_ELEMENTS_NAME_PER_ID;
import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.NETWORK_ELEMENT_ID;
import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.deserializeNetworkElement;

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
public final class BoundaryLineActionArrayDeserializer {
    private BoundaryLineActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, NetworkActionAdder ownerAdder, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", BOUNDARYLINE_ACTIONS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            BoundaryLineActionAdder adder = ownerAdder.newBoundaryLineAction();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.currentName()) {
                    case NETWORK_ELEMENT_ID:
                        deserializeNetworkElement(jsonParser.nextTextValue(), networkElementsNamesPerId, adder);
                        break;
                    case ACTIVE_POWER_VALUE:
                        jsonParser.nextToken();
                        adder.withActivePowerValue(jsonParser.getDoubleValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in BoundaryLineAction: " + jsonParser.currentName());
                }
            }
            adder.add();
        }
    }
}
