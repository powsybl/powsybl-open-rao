/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.network_action.ElementaryAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_impl.NetworkActionImpl;
import com.farao_community.farao.data.crac_impl.CracImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationNames.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class NetworkActionDeserializer {

    private NetworkActionDeserializer() {
    }

    static Set<NetworkAction> deserialize(JsonParser jsonParser, CracImpl simpleCrac, DeserializationContext deserializationContext) throws IOException {
        // cannot be done in a standard NetworkAction deserializer as it requires the simpleCrac to compare
        // the networkElement ids of the NetworkAction with the NetworkElements of the Crac

        Set<NetworkAction> networkActions = new HashSet<>();

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {

            String id = null;
            String name = null;
            String operator = null;
            List<UsageRule> usageRules = new ArrayList<>();
            Set<ElementaryAction> elementaryActions = new HashSet<>();
            List<Extension<NetworkAction>> extensions = new ArrayList<>();

            while (!jsonParser.nextToken().isStructEnd()) {

                switch (jsonParser.getCurrentName()) {

                    case TYPE:
                        String type = jsonParser.nextTextValue();
                        if (!type.equals(NETWORK_ACTION_IMPL_TYPE)) {
                            throw new FaraoException(String.format("Type of network action [%s] not handled by CracImpl deserializer.", type));
                        }
                        break;

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

                    case ELEMENTARY_ACTIONS:
                        jsonParser.nextToken();
                        elementaryActions = ElementaryActionsDeserializer.deserialize(jsonParser, simpleCrac);
                        break;

                    case EXTENSIONS:
                        jsonParser.nextToken();
                        jsonParser.nextToken();
                        extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                        break;

                    default:
                        throw new FaraoException(UNEXPECTED_FIELD + jsonParser.getCurrentName());
                }
            }

            NetworkAction networkAction = new NetworkActionImpl(id, name, operator, usageRules, elementaryActions);
            if (!extensions.isEmpty()) {
                ExtensionsHandler.getExtensionsSerializers().addExtensions(networkAction, extensions);
            }
            networkActions.add(networkAction);
        }
        return networkActions;
    }
}

