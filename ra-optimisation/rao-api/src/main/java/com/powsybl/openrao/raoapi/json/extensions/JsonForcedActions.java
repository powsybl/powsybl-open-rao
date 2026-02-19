/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.json.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.action.ActionList;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.ForcedActions;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * ForcedActions extension json serializer & deserializer.
 * Depends on PowSyBl's ActionList serializer & deserializer.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonForcedActions implements JsonRaoParameters.ExtensionSerializer<ForcedActions> {

    @Override
    public void serialize(ForcedActions forcedActions, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField(PREVENTIVE_ACTION_LIST, new ActionList(forcedActions.getPreventiveActions()));
        jsonGenerator.writeEndObject();
    }

    @Override
    public ForcedActions deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ForcedActions forcedActions = new ForcedActions();
        while (!jsonParser.nextToken().isStructEnd()) {
            if (jsonParser.currentName().equals(PREVENTIVE_ACTION_LIST)) {
                jsonParser.nextToken();
                ActionList actionList = JsonUtil.readValue(deserializationContext, jsonParser, ActionList.class);
                forcedActions.setPreventiveActions(actionList.getActions());
            } else {
                throw new OpenRaoException("Unexpected token: " + jsonParser.currentName());
            }
        }
        return forcedActions;
    }

    @Override
    public String getExtensionName() {
        return FORCED_ACTIONS_PARAMETERS;
    }

    @Override
    public String getCategoryName() {
        return RAO_PARAMETERS_CATEGORY;
    }

    @Override
    public Class<? super ForcedActions> getExtensionClass() {
        return ForcedActions.class;
    }
}
