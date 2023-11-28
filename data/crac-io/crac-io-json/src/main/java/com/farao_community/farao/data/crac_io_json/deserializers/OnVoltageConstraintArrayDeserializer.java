/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.RemedialActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnVoltageConstraintAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.INSTANT;
import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.VOLTAGE_CNEC_ID;

/**
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
public final class OnVoltageConstraintArrayDeserializer {
    private OnVoltageConstraintArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, RemedialActionAdder<?> ownerAdder, Crac crac) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            OnVoltageConstraintAdder<?> adder = ownerAdder.newOnVoltageConstraintUsageRule();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case INSTANT:
                        adder.withInstant(crac.getInstant(jsonParser.nextTextValue()));
                        break;
                    case VOLTAGE_CNEC_ID:
                        adder.withVoltageCnec(jsonParser.nextTextValue());
                        break;
                    default:
                        throw new FaraoException("Unexpected field in OnVoltageConstraint: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }
}
