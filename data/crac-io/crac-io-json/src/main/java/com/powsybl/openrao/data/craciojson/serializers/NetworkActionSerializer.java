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
import java.util.Arrays;
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
        serializeElementaryActions(value, TOPOLOGICAL_ACTIONS, Arrays.asList(TerminalsConnectionAction.class, SwitchAction.class), gen);
        serializeElementaryActions(value, PST_SETPOINTS, List.of(PhaseTapChangerTapPositionAction.class), gen);
        serializeElementaryActions(value, INJECTION_SETPOINTS, Arrays.asList(GeneratorAction.class, LoadAction.class, DanglingLineAction.class, ShuntCompensatorPositionAction.class), gen);
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
