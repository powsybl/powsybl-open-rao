/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.json.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoutreParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.FORCED_NETWORK_ACTIONS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.LOUTRE_PARAMETERS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonLoutreParameters implements JsonRaoParameters.ExtensionSerializer<LoutreParameters> {

    @Override
    public void serialize(LoutreParameters loutreParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeArrayFieldStart(FORCED_NETWORK_ACTIONS);
        for (String networkActionId : loutreParameters.getForcedNetworkActions()) {
            jsonGenerator.writeString(networkActionId);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }

    @Override
    public LoutreParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        LoutreParameters loutreParameters = new LoutreParameters();
        while (!jsonParser.nextToken().isStructEnd()) {
            if (FORCED_NETWORK_ACTIONS.equals(jsonParser.currentName())) {
                jsonParser.nextToken();
                List<String> networkActionIds = new ArrayList<>();
                while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                    networkActionIds.add(jsonParser.getValueAsString());
                }
                loutreParameters.setForcedNetworkActions(networkActionIds);
            } else {
                throw new OpenRaoException(String.format("Cannot deserialize loutre parameters: unexpected field in %s (%s)", LOUTRE_PARAMETERS, jsonParser.currentName()));
            }
        }
        return loutreParameters;
    }

    @Override
    public String getExtensionName() {
        return LOUTRE_PARAMETERS;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super LoutreParameters> getExtensionClass() {
        return LoutreParameters.class;
    }
}
