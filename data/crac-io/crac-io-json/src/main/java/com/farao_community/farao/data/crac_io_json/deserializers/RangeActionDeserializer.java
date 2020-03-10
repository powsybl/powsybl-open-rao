/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

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
import java.util.List;
import java.util.Set;

import static com.farao_community.farao.data.crac_io_json.deserializers.DeserializerNames.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class RangeActionDeserializer {

    private RangeActionDeserializer() { }

    static void deserialize(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard RangeAction deserializer as it requires the simpleCrac to compare
        // the networkElement ids of the RangeAction with the NetworkElements of the Crac

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {

            RangeAction rangeAction;

            // first json Token should be the type of the range action
            jsonParser.nextToken();
            if (!jsonParser.getCurrentName().equals(TYPE)) {
                throw new FaraoException("Type of range action is missing");
            }

            // use the deserializer suited to range action type
            String type = jsonParser.nextTextValue();
            switch (type) {
                case PST_WITH_RANGE_TYPE:
                    rangeAction = deserializePstWithRange(jsonParser, simpleCrac);
                    break;

                case ALIGNED_RANGE_ACTIONS_TYPE:
                    rangeAction = deserializeAlignedRangeAction(jsonParser, simpleCrac);
                    break;

                default:
                    throw new FaraoException(String.format("Type of range action [%s] not handled by SimpleCrac deserializer.", type));
            }

            simpleCrac.addRangeAction(rangeAction);
        }
    }

    private static AlignedRangeAction deserializeAlignedRangeAction(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard AlignedRangeAction deserializer as it requires the simpleCrac to compare
        // the networkElement ids of the AlignedRangeAction with the NetworkElements of the Crac

        String id = null;
        String name = null;
        String operator = null;
        List<UsageRule> usageRules = new ArrayList<>();
        List<Range> ranges = new ArrayList<>();
        List<String> networkElementsIds = new ArrayList<>();

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
                    networkElementsIds = jsonParser.readValueAs(new TypeReference<ArrayList<String>>() {
                    });
                    break;

                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }

        }

        //add contingency in Crac
        Set<NetworkElement> networkElements = DeserializerUtils.getNetworkElementsFromIds(networkElementsIds, simpleCrac);
        return new AlignedRangeAction(id, name, operator, usageRules, ranges, networkElements);
    }

    private static PstWithRange deserializePstWithRange(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard PstWithRange deserializer as it requires the simpleCrac to compare
        // the networkElement ids of the PstWithRange with the NetworkElements of the Crac

        String id = null;
        String name = null;
        String operator = null;
        List<UsageRule> usageRules = new ArrayList<>();
        List<Range> ranges = new ArrayList<>();
        String networkElementId = null;

        while (!jsonParser.nextToken().isStructEnd()) {

            switch (jsonParser.getCurrentName()) {

                case OPERATOR:
                    operator = jsonParser.nextTextValue();
                    break;

                case ID:
                    id = jsonParser.nextTextValue();
                    break;

                case USAGE_RULES:
                    jsonParser.nextToken();
                    usageRules = UsageRuleDeserializer.deserialize(jsonParser, simpleCrac);
                    break;

                case NAME:
                    name = jsonParser.nextTextValue();
                    break;

                case NETWORK_ELEMENT:
                    networkElementId = jsonParser.nextTextValue();
                    break;

                case RANGES:
                    jsonParser.nextToken();
                    ranges = jsonParser.readValueAs(new TypeReference<List<Range>>() {
                    });
                    break;

                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }

        }

        NetworkElement ne = simpleCrac.getNetworkElement(networkElementId);
        if (ne == null) {
            throw new FaraoException(String.format("The network element [%s] mentioned in the pst-with-range is not defined", networkElementId));
        }

        return new PstWithRange(id, name, operator, usageRules, ranges, ne);
    }
}
