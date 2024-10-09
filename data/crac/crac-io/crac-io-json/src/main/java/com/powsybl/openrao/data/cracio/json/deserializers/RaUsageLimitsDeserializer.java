/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import com.powsybl.openrao.data.cracapi.parameters.JsonCracCreationParametersConstants;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */
public final class RaUsageLimitsDeserializer {
    private RaUsageLimitsDeserializer() {
        // should not be used
    }

    public static void deserialize(JsonParser jsonParser, Crac crac) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            Pair<String, RaUsageLimits> raUsageLimitsPair = JsonCracCreationParametersConstants.deserializeRaUsageLimits(jsonParser);
            RaUsageLimits raUsageLimits = raUsageLimitsPair.getRight();
            crac.newRaUsageLimits(raUsageLimitsPair.getLeft())
                .withMaxRa(raUsageLimits.getMaxRa())
                .withMaxTso(raUsageLimits.getMaxTso())
                .withMaxRaPerTso(raUsageLimits.getMaxRaPerTso())
                .withMaxPstPerTso(raUsageLimits.getMaxPstPerTso())
                .withMaxTopoPerTso(raUsageLimits.getMaxTopoPerTso())
                .withMaxElementaryActionPerTso(raUsageLimits.getMaxElementaryActionsPerTso())
                .add();
        }
    }
}
