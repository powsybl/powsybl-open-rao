/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.cracapi.networkaction.SwitchActionAdder;
import com.powsybl.openrao.data.cracapi.networkaction.TerminalsConnectionActionAdder;
import com.powsybl.openrao.data.cracio.json.JsonSerializationConstants;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.deserializeNetworkElement;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class TopologicalActionArrayDeserializer {
    private TopologicalActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, NetworkActionAdder ownerAdder, Map<String, String> networkElementsNamesPerId, Network network) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", JsonSerializationConstants.TOPOLOGICAL_ACTIONS, JsonSerializationConstants.NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String networkElementId = null;
            ActionType actionType = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case JsonSerializationConstants.NETWORK_ELEMENT_ID:
                        networkElementId = jsonParser.nextTextValue();
                        break;
                    case JsonSerializationConstants.ACTION_TYPE:
                        actionType = JsonSerializationConstants.deserializeActionType(jsonParser.nextTextValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in TopologicalAction: " + jsonParser.getCurrentName());
                }
            }
            Identifiable<?> identifiable = network.getIdentifiable(networkElementId);
            if (Objects.isNull(identifiable)) {
                throw new OpenRaoException("Network element id " + networkElementId + " does not exist in network " + network.getId());
            }
            if (identifiable.getType() == IdentifiableType.SWITCH) {
                SwitchActionAdder switchActionAdder = ownerAdder.newSwitchAction();
                deserializeNetworkElement(networkElementId, networkElementsNamesPerId, switchActionAdder);
                switchActionAdder.withActionType(actionType);
                switchActionAdder.add();
            } else {
                TerminalsConnectionActionAdder terminalsConnectionActionAdder = ownerAdder.newTerminalsConnectionAction();
                deserializeNetworkElement(networkElementId, networkElementsNamesPerId, terminalsConnectionActionAdder);
                terminalsConnectionActionAdder.withActionType(actionType);
                terminalsConnectionActionAdder.add();
            }
        }
    }
}
