/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.networkaction.InjectionSetpointAdder;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Map;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class InjectionSetpointArrayDeserializer {
    private InjectionSetpointArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, NetworkActionAdder ownerAdder, Map<String, String> networkElementsNamesPerId, String version) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", INJECTION_SETPOINTS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            InjectionSetpointAdder adder = ownerAdder.newInjectionSetPoint();
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
                    case SETPOINT:
                        jsonParser.nextToken();
                        adder.withSetpoint(jsonParser.getIntValue());
                        break;
                    case UNIT:
                        adder.withUnit(deserializeUnit(jsonParser.nextTextValue()));
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in InjectionSetpoint: " + jsonParser.getCurrentName());
                }
            }
            if (getPrimaryVersionNumber(version) < 2 && getSubVersionNumber(version) < 8) {
                adder.withUnit(Unit.MEGAWATT);
            }
            adder.add();
        }
    }
}
