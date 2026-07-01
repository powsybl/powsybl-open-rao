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
import com.powsybl.openrao.data.crac.api.RemedialActionAdder;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyStateAdder;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;

import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.USAGE_METHOD;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class OnStateArrayDeserializer {
    private OnStateArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, RemedialActionAdder<?> ownerAdder, String version) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            OnContingencyStateAdder<?> adder = ownerAdder.newOnContingencyStateUsageRule();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.currentName()) {
                    case JsonSerializationConstants.INSTANT:
                        adder.withInstant(jsonParser.nextTextValue());
                        break;
                    case USAGE_METHOD:
                        JsonSerializationConstants.logDeprecatedField(
                            2, 8,
                            "Usage methods are no longer used.",
                            jsonParser, String.class, version
                        );
                        break;
                    case JsonSerializationConstants.CONTINGENCY_ID:
                        adder.withContingency(jsonParser.nextTextValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in OnContingencyState: " + jsonParser.currentName());
                }
            }
            adder.add();
        }
    }

}
