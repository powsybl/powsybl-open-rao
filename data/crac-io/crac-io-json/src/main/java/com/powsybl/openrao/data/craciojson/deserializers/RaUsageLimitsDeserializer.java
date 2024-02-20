/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.powsybl.openrao.data.craccreation.creator.api.parameters.JsonCracCreationParametersConstants.*;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */
public final class RaUsageLimitsDeserializer {
    private RaUsageLimitsDeserializer() {
        // should not be used
    }

    public static void deserialize(JsonParser jsonParser, Crac crac) throws IOException {
        Map<String, RaUsageLimits> raUsageLimits = new HashMap<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            Pair<String, RaUsageLimits> raUsageLimitsPair = deserializeRaUsageLimits(jsonParser);
            raUsageLimits.put(raUsageLimitsPair.getLeft(), raUsageLimitsPair.getRight());
        }
        crac.setRaUsageLimits(raUsageLimits);
    }
}
