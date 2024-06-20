/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.RemedialActionAdder;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerConditionAdder;

import java.io.IOException;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.CNEC_ID;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.CONTINGENCY_ID;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.COUNTRY;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.INSTANT;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.USAGE_METHOD;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.deserializeUsageMethod;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class TriggerConditionDeserializer {
    private TriggerConditionDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, RemedialActionAdder<?> ownerAdder) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            TriggerConditionAdder<?> adder = ownerAdder.newTriggerCondition();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case INSTANT:
                        adder.withInstant(jsonParser.nextTextValue());
                        break;
                    case CONTINGENCY_ID:
                        adder.withContingency(jsonParser.nextTextValue());
                        break;
                    case CNEC_ID:
                        adder.withCnec(jsonParser.nextTextValue());
                        break;
                    case COUNTRY:
                        adder.withCountry(Country.valueOf(jsonParser.nextTextValue()));
                        break;
                    case USAGE_METHOD:
                        adder.withUsageMethod(deserializeUsageMethod(jsonParser.nextTextValue()));
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in TriggerCondition: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }
}
