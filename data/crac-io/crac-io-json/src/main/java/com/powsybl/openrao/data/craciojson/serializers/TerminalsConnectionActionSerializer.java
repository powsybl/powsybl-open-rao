/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.action.TerminalsConnectionAction;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;

import java.io.IOException;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class TerminalsConnectionActionSerializer extends AbstractJsonSerializer<TerminalsConnectionAction> {
    @Override
    public void serialize(TerminalsConnectionAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(NETWORK_ELEMENT_ID, value.getElementId());
        gen.writeStringField(ACTION_TYPE, serializeActionType(value.isOpen() ? ActionType.OPEN : ActionType.CLOSE));
        gen.writeEndObject();
    }
}
