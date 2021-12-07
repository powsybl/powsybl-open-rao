/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.network_action.SwitchPairAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Map;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class SwitchPairArrayDeserializer {
    private SwitchPairArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, NetworkActionAdder ownerAdder, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new FaraoException(String.format("Cannot deserialize %s before %s", SWITCH_PAIRS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            SwitchPairAdder adder = ownerAdder.newSwitchPair();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case OPEN_ACTION:
                        String networkElementId = jsonParser.nextTextValue();
                        if (networkElementsNamesPerId.containsKey(networkElementId)) {
                            adder.withSwitchToOpen(networkElementId, networkElementsNamesPerId.get(networkElementId));
                        } else {
                            adder.withSwitchToOpen(networkElementId);
                        }
                        break;
                    case CLOSE_ACTION:
                        String networkElementId2 = jsonParser.nextTextValue();
                        if (networkElementsNamesPerId.containsKey(networkElementId2)) {
                            adder.withSwitchToClose(networkElementId2, networkElementsNamesPerId.get(networkElementId2));
                        } else {
                            adder.withSwitchToClose(networkElementId2);
                        }
                        break;
                    default:
                        throw new FaraoException("Unexpected field in SwitchPair: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }
}
