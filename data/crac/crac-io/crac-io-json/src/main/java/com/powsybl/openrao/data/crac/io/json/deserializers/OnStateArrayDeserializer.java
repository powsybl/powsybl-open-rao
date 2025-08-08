/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.api.RemedialActionAdder;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyStateAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.USAGE_METHOD;
import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.getPrimaryVersionNumber;
import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.getSubVersionNumber;

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
                switch (jsonParser.getCurrentName()) {
                    case JsonSerializationConstants.INSTANT:
                        adder.withInstant(jsonParser.nextTextValue());
                        break;
                    case USAGE_METHOD:
                        if (getPrimaryVersionNumber(version) < 2 || getPrimaryVersionNumber(version) == 2 && getSubVersionNumber(version) < 8) {
                            BUSINESS_WARNS.warn("Usage methods are no longer read since they are redundant with the usage rule's instant.");
                            break;
                        } else {
                            throw new OpenRaoException("Unexpected field in OnContingencyState: " + jsonParser.getCurrentName());
                        }
                    case JsonSerializationConstants.CONTINGENCY_ID:
                        adder.withContingency(jsonParser.nextTextValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in OnContingencyState: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }

}
