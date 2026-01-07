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
import com.powsybl.openrao.data.crac.api.networkaction.AcEmulationDeactivationActionAdder;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;

import java.io.IOException;
import java.util.Map;

import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.*;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class AcEmulationDeactivationActionDeserializer {
    private AcEmulationDeactivationActionDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, NetworkActionAdder ownerAdder, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", AC_EMULATION_DEACTIVATION_ACTIONS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            AcEmulationDeactivationActionAdder adder = ownerAdder.newAcEmulationDeactivationAction();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case NETWORK_ELEMENT_ID:
                        deserializeNetworkElement(jsonParser.nextTextValue(), networkElementsNamesPerId, adder);
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in AcEmulationDeactivationAction: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }
}
