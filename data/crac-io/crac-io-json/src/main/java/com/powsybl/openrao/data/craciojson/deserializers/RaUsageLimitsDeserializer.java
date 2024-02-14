package com.powsybl.openrao.data.craciojson.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;

import static com.powsybl.openrao.data.craccreation.creator.api.parameters.JsonCracCreationParametersConstants.*;

public final class RaUsageLimitsDeserializer {
    private RaUsageLimitsDeserializer() {
        // should not be used
    }

    public static void deserialize(JsonParser jsonParser, Crac crac) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            Pair<String, RaUsageLimits> pairOfInstantAndItsRaUsageLimits = deserializeRaUsageLimits(jsonParser);
            crac.addRaUsageLimitsForAGivenInstant(pairOfInstantAndItsRaUsageLimits.getLeft(), pairOfInstantAndItsRaUsageLimits.getRight());
        }
    }
}
