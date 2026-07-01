/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.roda.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.action.ActionList;
import com.powsybl.action.json.ActionJsonModule;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;

import java.io.IOException;
import java.util.List;

/**
 * RODA parameters extension json serializer & deserializer.
 * Depends on PowSyBl's ActionList serializer & deserializer.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonRodaParameters implements JsonRaoParameters.ExtensionSerializer<RodaParameters> {

    private static final String PREVENTIVE_ACTION_LIST = "forced-preventive-actions-list";

    @Override
    public void serialize(RodaParameters rodaParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeFieldName(PREVENTIVE_ACTION_LIST);
        createObjectMapper().writeValue(jsonGenerator, new ActionList(rodaParameters.getForcedPreventiveActions()));
        jsonGenerator.writeEndObject();
    }

    private static ObjectMapper createObjectMapper() {
        return JsonUtil.createObjectMapper()
            .registerModule(new ActionJsonModule());
    }

    @Override
    public RodaParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ActionList actionList = null;
        while (!jsonParser.nextToken().isStructEnd()) {
            if (jsonParser.currentName().equals(PREVENTIVE_ACTION_LIST)) {
                jsonParser.nextToken();
                actionList = createObjectMapper().readValue(jsonParser, ActionList.class);
            } else {
                throw new OpenRaoException("Unexpected token: " + jsonParser.currentName());
            }
        }
        if (actionList == null) {
            return new RodaParameters(List.of());
        }
        return new RodaParameters(actionList.getActions());
    }

    @Override
    public String getExtensionName() {
        return RodaParameters.EXTENSION_NAME;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super RodaParameters> getExtensionClass() {
        return RodaParameters.class;
    }
}
