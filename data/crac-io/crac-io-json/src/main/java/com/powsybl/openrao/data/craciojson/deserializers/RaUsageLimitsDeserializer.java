package com.powsybl.openrao.data.craciojson.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;

import java.io.IOException;

import static com.powsybl.openrao.data.craccreation.creator.api.parameters.JsonCracCreationParametersConstants.*;
import static com.powsybl.openrao.data.craccreation.creator.api.parameters.JsonCracCreationParametersConstants.INSTANT;

public final class RaUsageLimitsDeserializer {
    private RaUsageLimitsDeserializer() {
        // should not be used
    }

    public static void deserialize(JsonParser jsonParser, Crac crac) throws IOException {
        RaUsageLimits raUsageLimits = new RaUsageLimits();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String instantName = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case INSTANT:
                        jsonParser.nextToken();
                        instantName = jsonParser.getValueAsString();
                        break;
                    case MAX_RA:
                        jsonParser.nextToken();
                        raUsageLimits.setMaxRa(jsonParser.getIntValue());
                        break;
                    case MAX_TSO:
                        jsonParser.nextToken();
                        raUsageLimits.setMaxTso(jsonParser.getIntValue());
                        break;
                    case MAX_TOPO_PER_TSO:
                        jsonParser.nextToken();
                        raUsageLimits.setMaxTopoPerTso(readStringToPositiveIntMap(jsonParser));
                        break;
                    case MAX_PST_PER_TSO:
                        jsonParser.nextToken();
                        raUsageLimits.setMaxPstPerTso(readStringToPositiveIntMap(jsonParser));
                        break;
                    case MAX_RA_PER_TSO:
                        jsonParser.nextToken();
                        raUsageLimits.setMaxRaPerTso(readStringToPositiveIntMap(jsonParser));
                        break;
                    default:
                        throw new OpenRaoException(String.format("Cannot deserialize ra-usage-limits-per-instant parameters: unexpected field in %s (%s)", RA_USAGE_LIMITS_PER_INSTANT, jsonParser.getCurrentName()));
                }
            }
            crac.addRaUsageLimitsForAGivenInstant(instantName, raUsageLimits);
        }
    }
}
