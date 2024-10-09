/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.serializers;

import com.powsybl.action.Action;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.action.*;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.SwitchPair;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;

public class NetworkActionSerializer extends AbstractJsonSerializer<NetworkAction> {
    @Override
    public void serialize(NetworkAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(ID, value.getId());
        gen.writeStringField(NAME, value.getName());
        gen.writeStringField(OPERATOR, value.getOperator());
        UsageRulesSerializer.serializeUsageRules(value, gen);
        serializeElementaryActions(value, TerminalsConnectionAction.class, TERMINALS_CONNECTION_ACTIONS, gen);
        serializeElementaryActions(value, SwitchAction.class, SWITCH_ACTIONS, gen);
        serializeElementaryActions(value, PhaseTapChangerTapPositionAction.class, PHASETAPCHANGER_TAPPOSITION_ACTIONS, gen);
        serializeElementaryActions(value, GeneratorAction.class, GENERATOR_ACTIONS, gen);
        serializeElementaryActions(value, LoadAction.class, LOAD_ACTIONS, gen);
        serializeElementaryActions(value, DanglingLineAction.class, DANGLINGLINE_ACTIONS, gen);
        serializeElementaryActions(value, ShuntCompensatorPositionAction.class, SHUNTCOMPENSATOR_POSITION_ACTIONS, gen);
        serializeElementaryActions(value, SwitchPair.class, SWITCH_PAIRS, gen);
        serializeRemedialActionSpeed(value, gen);
        gen.writeEndObject();
    }

    private void serializeElementaryActions(NetworkAction networkAction, Class<? extends Action> elementaryActionType, String arrayName, JsonGenerator gen) throws IOException {
        List<Action> actions = networkAction.getElementaryActions().stream().filter(action -> elementaryActionType.isAssignableFrom(action.getClass()))
            .sorted(Comparator.comparing(Action::getId)).toList();
        if (!actions.isEmpty()) {
            gen.writeArrayFieldStart(arrayName);
            for (Action ea : actions) {
                gen.writeObject(ea);
            }
            gen.writeEndArray();
        }
    }
}
