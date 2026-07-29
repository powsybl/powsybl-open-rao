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
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.networkaction.SwitchPairAdder;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class SwitchPairArrayDeserializer {
    private SwitchPairArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, NetworkActionAdder ownerAdder) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            SwitchPairAdder adder = ownerAdder.newSwitchPair();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.currentName()) {
                    case JsonSerializationConstants.OPEN_ACTION:
                        adder.withSwitchToOpen(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.CLOSE_ACTION:
                        adder.withSwitchToClose(jsonParser.nextTextValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in SwitchPair: " + jsonParser.currentName());
                }
            }
            adder.add();
        }
    }
}
