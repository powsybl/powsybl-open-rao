/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.RemedialActionAdder;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountryAdder;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;

import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.CONTINGENCY_ID;
import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.COUNTRY;
import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.INSTANT;
import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.USAGE_METHOD;

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
                switch (jsonParser.currentName()) {
                    case INSTANT:
                        String instantId = jsonParser.nextTextValue();
                        adder.withInstant(instantId);
                        break;
                    case CONTINGENCY_ID:
                        adder.withContingency(jsonParser.nextTextValue());
                        break;
                    case USAGE_METHOD:
                        JsonSerializationConstants.logDeprecatedField(
                            jsonParser.currentName(), 2, 8,
                            "Usage methods are no longer used.",
                            jsonParser, String.class, version
                        );
                        break;
                    case COUNTRY:
                        adder.withCountry(Country.valueOf(jsonParser.nextTextValue()));
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in OnFlowConstraintInCountry: " + jsonParser.currentName());
                }
            }
            adder.add();
        }
    }
}
