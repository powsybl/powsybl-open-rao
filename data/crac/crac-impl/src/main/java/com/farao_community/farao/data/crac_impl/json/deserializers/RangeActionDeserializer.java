/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.AlignedRangeAction;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.farao_community.farao.data.crac_impl.json.deserializers.DeserializerNames.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class RangeActionDeserializer {

    private RangeActionDeserializer() { }

    static void deserialize(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard RangeAction deserializer as it requires the simpleCrac to compare
        // the networkElement ids of the RangeAction with the NetworkElements of the Crac

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            // first json Token should be the type of the range action
            jsonParser.nextToken();
            if (!jsonParser.getCurrentName().equals(TYPE)) {
                throw new FaraoException("Type of range action is missing");
            }

            // use the deserializer suited to range action type
            String type = jsonParser.nextTextValue();
            RangeAction rangeAction = deserializeRangeAction(type, jsonParser, simpleCrac);

            simpleCrac.addRangeAction(rangeAction);
        }
    }

    private static RangeAction deserializeRangeAction(String type, JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        String id = null;
        String name = null;
        String operator = null;
        List<UsageRule> usageRules = new ArrayList<>();
        List<Range> ranges = new ArrayList<>();
        Set<String> networkElementsIds = new HashSet<>();

        while (!jsonParser.nextToken().isStructEnd()) {

            switch (jsonParser.getCurrentName()) {

                case ID:
                    id = jsonParser.nextTextValue();
                    break;

                case NAME:
                    name = jsonParser.nextTextValue();
                    break;

                case OPERATOR:
                    operator = jsonParser.nextTextValue();
                    break;

                case USAGE_RULES:
                    jsonParser.nextToken();
                    usageRules = UsageRuleDeserializer.deserialize(jsonParser, simpleCrac);
                    break;

                case RANGES:
                    jsonParser.nextToken();
                    ranges = jsonParser.readValueAs(new TypeReference<List<Range>>() {
                    });
                    break;

                case NETWORK_ELEMENTS:
                    jsonParser.nextToken();
                    networkElementsIds = jsonParser.readValueAs(new TypeReference<HashSet<String>>() {
                    });
                    break;

                default:
                    throw new FaraoException(UNEXPECTED_FIELD + jsonParser.getCurrentName());
            }

        }

        //add contingency in Crac
        Set<NetworkElement> networkElements = DeserializerUtils.getNetworkElementsFromIds(networkElementsIds, simpleCrac);
        switch (type) {
            case PST_WITH_RANGE_TYPE:
                return new PstWithRange(id, name, operator, usageRules, ranges, networkElements.iterator().next());
            case ALIGNED_RANGE_ACTIONS_TYPE:
                return new AlignedRangeAction(id, name, operator, usageRules, ranges, networkElements);
            default:
                throw new FaraoException(String.format("Type of range action [%s] not handled by SimpleCrac deserializer.", type));
        }
    }
}
