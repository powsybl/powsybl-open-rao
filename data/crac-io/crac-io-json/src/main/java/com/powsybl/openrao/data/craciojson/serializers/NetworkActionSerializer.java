/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.serializers;

import com.powsybl.action.Action;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.action.*;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.SwitchPair;

import java.io.IOException;
import java.util.List;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;

public class NetworkActionSerializer extends AbstractJsonSerializer<NetworkAction> {
    @Override
    public void serialize(NetworkAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(ID, value.getId());
        gen.writeStringField(NAME, value.getName());
        gen.writeStringField(OPERATOR, value.getOperator());
        UsageRulesSerializer.serializeUsageRules(value, gen);
        serializeElementaryActions(value, TERMINALS_CONNECTION_ACTIONS, List.of(TerminalsConnectionAction.class), gen);
        serializeElementaryActions(value, SWITCH_ACTIONS, List.of(SwitchAction.class), gen);
        serializeElementaryActions(value, PHASETAPCHANGER_TAPPOSITION_ACTIONS, List.of(PhaseTapChangerTapPositionAction.class), gen);
        serializeElementaryActions(value, GENERATOR_ACTIONS, List.of(GeneratorAction.class), gen);
        serializeElementaryActions(value, LOAD_ACTIONS, List.of(LoadAction.class), gen);
        serializeElementaryActions(value, DANGLINGLINE_ACTIONS, List.of(DanglingLineAction.class), gen);
        serializeElementaryActions(value, SHUNTCOMPENSATOR_POSITION_ACTIONS, List.of(ShuntCompensatorPositionAction.class), gen);
        serializeElementaryActions(value, SWITCH_PAIRS, List.of(SwitchPair.class), gen);
        serializeRemedialActionSpeed(value, gen);
        gen.writeEndObject();
    }

    private void serializeElementaryActions(NetworkAction networkAction, String arrayName, List<Class<? extends Action>> elementaryActionTypes, JsonGenerator gen) throws IOException {
        List<Action> actions = networkAction.getElementaryActions().stream().filter(action -> elementaryActionTypes.stream().anyMatch(cls -> cls.isAssignableFrom(action.getClass()))).toList();
        if (!actions.isEmpty()) {
            gen.writeArrayFieldStart(arrayName);
            for (Action ea : actions) {
                gen.writeObject(ea);
            }
            gen.writeEndArray();
        }
    }
}
