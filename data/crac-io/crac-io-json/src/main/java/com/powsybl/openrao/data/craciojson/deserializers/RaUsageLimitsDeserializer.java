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
        crac.addRaUsageLimits(raUsageLimits);
    }
}
