/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.serializers;

import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.networkaction.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;

public class NetworkActionSerializer extends AbstractJsonSerializer<NetworkAction> {
    @Override
    public void serialize(NetworkAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(ID, value.getId());
        gen.writeStringField(NAME, value.getName());
        gen.writeStringField(OPERATOR, value.getOperator());
        serializeTriggerConditions(value, TRIGGER_CONDITIONS, gen);
        serializeElementaryActions(value, TopologicalAction.class, TOPOLOGICAL_ACTIONS, gen);
        serializeElementaryActions(value, PstSetpoint.class, PST_SETPOINTS, gen);
        serializeElementaryActions(value, InjectionSetpoint.class, INJECTION_SETPOINTS, gen);
        serializeElementaryActions(value, SwitchPair.class, SWITCH_PAIRS, gen);
        serializeRemedialActionSpeed(value, gen);
        gen.writeEndObject();
    }

    private void serializeElementaryActions(NetworkAction networkAction, Class<? extends ElementaryAction> elementaryActionType, String arrayName, JsonGenerator gen) throws IOException {
        List<ElementaryAction> actions = networkAction.getElementaryActions().stream().filter(action -> elementaryActionType.isAssignableFrom(action.getClass()))
                .sorted(Comparator.comparing(this::buildElementaryActionId)).toList();
        if (!actions.isEmpty()) {
            gen.writeArrayFieldStart(arrayName);
            for (ElementaryAction ea : actions) {
                gen.writeObject(ea);
            }
            gen.writeEndArray();
        }
    }

    private String buildElementaryActionId(ElementaryAction elementaryAction) {
        List<String> sortedElements = elementaryAction.getNetworkElements().stream().map(Identifiable::getId).sorted(String::compareTo).toList();
        return String.join(" + ", sortedElements);
    }
}
