/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUseImpl;
import com.farao_community.farao.data.crac_impl.usage_rule.OnStateImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.farao_community.farao.data.crac_impl.json.JsonSerializationNames.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class UsageRuleDeserializer {

    private UsageRuleDeserializer() { }

    static List<UsageRule> deserialize(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard UsageRule deserializer as it requires the simpleCrac to compare
        // the state of the OnState UsageRules with the states in the Crac
        List<UsageRule> usageRules = new ArrayList<>();

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {

            UsageRule usageRule;

            // first json Token should be the type of the range action
            jsonParser.nextToken();
            if (!jsonParser.getCurrentName().equals(TYPE)) {
                throw new FaraoException("Type of usage rule is missing");
            }

            // use the deserializer suited to the usage rule type
            String type = jsonParser.nextTextValue();
            switch (type) {
                case FREE_TO_USE_TYPE:
                    usageRule = deserializeFreeToUseUsageRule(jsonParser);
                    break;

                case ON_STATE_TYPE:
                    usageRule = deserializeOnStateUsageRule(jsonParser, simpleCrac);
                    break;

                default:
                    throw new FaraoException(String.format("Type of range action [%s] not handled by SimpleCrac deserializer.", type));
            }

            usageRules.add(usageRule);
        }
        return usageRules;

    }

    private static FreeToUseImpl deserializeFreeToUseUsageRule(JsonParser jsonParser) throws IOException {

        UsageMethod usageMethod = null;
        Instant instant = null;

        while (!jsonParser.nextToken().isStructEnd()) {

            switch (jsonParser.getCurrentName()) {
                case USAGE_METHOD:
                    jsonParser.nextToken();
                    usageMethod = jsonParser.readValueAs(UsageMethod.class);
                    break;

                case INSTANT:
                    jsonParser.nextToken();
                    instant = jsonParser.readValueAs(Instant.class);
                    break;

                default:
                    throw new FaraoException(UNEXPECTED_FIELD + jsonParser.getCurrentName());
            }
        }
        return new FreeToUseImpl(usageMethod, instant);
    }

    private static OnStateImpl deserializeOnStateUsageRule(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {

        UsageMethod usageMethod = null;
        String stateId = null;

        while (!jsonParser.nextToken().isStructEnd()) {

            switch (jsonParser.getCurrentName()) {
                case USAGE_METHOD:
                    jsonParser.nextToken();
                    usageMethod = jsonParser.readValueAs(UsageMethod.class);
                    break;

                case STATE:
                    stateId = jsonParser.nextTextValue();
                    break;

                default:
                    throw new FaraoException(UNEXPECTED_FIELD + jsonParser.getCurrentName());
            }
        }

        State state = simpleCrac.getState(stateId);
        if (state == null) {
            throw new FaraoException(String.format("The state [%s] mentioned in the on-contingency usage rule is not defined", stateId));
        }

        return new OnStateImpl(usageMethod, state);
    }
}
