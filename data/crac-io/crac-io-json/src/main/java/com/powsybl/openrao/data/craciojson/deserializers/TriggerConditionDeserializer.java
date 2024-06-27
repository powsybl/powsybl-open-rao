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
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.RemedialActionAdder;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerConditionAdder;
import com.powsybl.openrao.data.cracapi.triggercondition.UsageMethod;

import java.io.IOException;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.ANGLE_CNEC_ID;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.CNEC_ID;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.CONTINGENCY_ID;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.COUNTRY;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.FLOW_CNEC_ID;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.INSTANT;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.USAGE_METHOD;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.VOLTAGE_CNEC_ID;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.deseralizeInstantKind;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.deserializeUsageMethod;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.getPrimaryVersionNumber;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.getSubVersionNumber;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class TriggerConditionDeserializer {

    private TriggerConditionDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, RemedialActionAdder<?> ownerAdder, String version) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            TriggerConditionAdder<?> adder = ownerAdder.newTriggerCondition();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case INSTANT:
                        String instantId = jsonParser.nextTextValue();
                        adder.withInstant(instantId);
                        if (getPrimaryVersionNumber(version) < 2) {
                            adder.withUsageMethod(deseralizeInstantKind(instantId).equals(InstantKind.AUTO) ? UsageMethod.FORCED : UsageMethod.AVAILABLE);
                        }
                        break;
                    case CONTINGENCY_ID:
                        adder.withContingency(jsonParser.nextTextValue());
                        break;
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

    private static void deserializeOlderOnConstraintUsageRules(JsonParser jsonParser, String keyword, String version, TriggerConditionAdder<?> adder) throws IOException {
        if (getPrimaryVersionNumber(version) < 2 || getPrimaryVersionNumber(version) == 2 && getSubVersionNumber(version) < 4) {
            adder.withCnec(jsonParser.nextTextValue());
        } else {
            throw new OpenRaoException("Unsupported field %s for TriggerCondition with CNEC in CRAC version >= 2.4".formatted(keyword));
        }
    }
}
