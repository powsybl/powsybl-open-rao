/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.RemedialActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnAngleConstraintAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.ANGLE_CNEC_ID;
import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.INSTANT;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class OnAngleConstraintArrayDeserializer {
    private OnAngleConstraintArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, RemedialActionAdder<?> ownerAdder, Crac crac) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            OnAngleConstraintAdder<?> adder = ownerAdder.newOnAngleConstraintUsageRule();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case INSTANT:
                        adder.withInstant(crac.getInstant(jsonParser.nextTextValue()));
                        break;
                    case ANGLE_CNEC_ID:
                        adder.withAngleCnec(jsonParser.nextTextValue());
                        break;
                    default:
                        throw new FaraoException("Unexpected field in OnAngleConstraint: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }
}
