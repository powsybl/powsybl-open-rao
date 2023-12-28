/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraintInCountryAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.iidm.network.Country;

import java.io.IOException;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class OnFlowConstraintInCountryArrayDeserializer {
    private OnFlowConstraintInCountryArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, RemedialActionAdder<?> ownerAdder, String version) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            OnFlowConstraintInCountryAdder<?> adder = ownerAdder.newOnFlowConstraintInCountryUsageRule();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case INSTANT:
                        String instantId = jsonParser.nextTextValue();
                        adder.withInstant(instantId);
                        if (getPrimaryVersionNumber(version) < 2 ) {
                            adder.withUsageMethod(instant.equals(Instant.AUTO) ? UsageMethod.FORCED : UsageMethod.AVAILABLE);
                        }
                        break;
                    case USAGE_METHOD:
                        adder.withUsageMethod(deserializeUsageMethod(jsonParser.nextTextValue()));
                        break;
                    case COUNTRY:
                        adder.withCountry(Country.valueOf(jsonParser.nextTextValue()));
                        break;
                    default:
                        throw new FaraoException("Unexpected field in OnFlowConstraintInCountry: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }
}
