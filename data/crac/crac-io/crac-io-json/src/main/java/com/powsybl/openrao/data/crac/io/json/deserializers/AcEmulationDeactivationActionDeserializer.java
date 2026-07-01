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

import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.NETWORK_ELEMENT_ID;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class AcEmulationDeactivationActionDeserializer {
    private AcEmulationDeactivationActionDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, NetworkActionAdder ownerAdder) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            AcEmulationDeactivationActionAdder adder = ownerAdder.newAcEmulationDeactivationAction();
            while (!jsonParser.nextToken().isStructEnd()) {
                if (jsonParser.currentName().equals(NETWORK_ELEMENT_ID)) {
                    adder.withNetworkElement(jsonParser.nextTextValue());
                } else {
                    throw new OpenRaoException("Unexpected field in AcEmulationDeactivationAction: " + jsonParser.currentName());
                }
            }
            adder.add();
        }
    }
}
