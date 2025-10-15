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
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.networkaction.PhaseTapChangerTapPositionActionAdder;

import java.io.IOException;
import java.util.Map;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class PstSetpointArrayDeserializer {
    private PstSetpointArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, NetworkActionAdder ownerAdder, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", JsonSerializationConstants.PST_SETPOINTS, JsonSerializationConstants.NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            PhaseTapChangerTapPositionActionAdder adder = ownerAdder.newPhaseTapChangerTapPositionAction();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case JsonSerializationConstants.NETWORK_ELEMENT_ID:
                        JsonSerializationConstants.deserializeNetworkElement(jsonParser.nextTextValue(), networkElementsNamesPerId, adder);
                        break;
                    case JsonSerializationConstants.SETPOINT:
                        jsonParser.nextToken();
                        adder.withTapPosition(jsonParser.getIntValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in PstSetpoint: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }
}
