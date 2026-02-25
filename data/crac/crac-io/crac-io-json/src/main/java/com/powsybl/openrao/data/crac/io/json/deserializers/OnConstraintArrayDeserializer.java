/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.RemedialActionAdder;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraintAdder;

import java.io.IOException;

import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class OnConstraintArrayDeserializer {
    private OnConstraintArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, RemedialActionAdder<?> ownerAdder, String version) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            OnConstraintAdder<?, ?> adder = ownerAdder.newOnConstraintUsageRule();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case INSTANT:
                        String instantId = jsonParser.nextTextValue();
                        adder.withInstant(instantId);
                        break;
                    case USAGE_METHOD:
                        if (getPrimaryVersionNumber(version) < 2 || getPrimaryVersionNumber(version) == 2 && getSubVersionNumber(version) < 8) {
                            CracDeserializer.LOGGER.warn("Usage methods are no longer used.");
                            break;
                        } else {
                            throw new OpenRaoException("Unexpected field in OnConstraint: " + jsonParser.getCurrentName());
                        }
                    case CNEC_ID:
                        adder.withCnec(jsonParser.nextTextValue());
                        break;
                    case ANGLE_CNEC_ID:
                        deserializeOlderOnConstraintUsageRules(jsonParser, ANGLE_CNEC_ID, version, adder);
                        break;
                    case FLOW_CNEC_ID:
                        deserializeOlderOnConstraintUsageRules(jsonParser, FLOW_CNEC_ID, version, adder);
                        break;
                    case VOLTAGE_CNEC_ID:
                        deserializeOlderOnConstraintUsageRules(jsonParser, VOLTAGE_CNEC_ID, version, adder);
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in OnConstraint: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }

    private static void deserializeOlderOnConstraintUsageRules(JsonParser jsonParser, String keyword, String version, OnConstraintAdder<?, ?> adder) throws IOException {
        if (getPrimaryVersionNumber(version) < 2 || getPrimaryVersionNumber(version) == 2 && getSubVersionNumber(version) < 4) {
            adder.withCnec(jsonParser.nextTextValue());
        } else {
            throw new OpenRaoException("Unsupported field %s for OnConstraint usage rule in CRAC version >= 2.4".formatted(keyword));
        }
    }
}
