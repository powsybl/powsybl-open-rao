/*
 * Copyright (c) 2021, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creator_api.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracCreatorParametersDeserializer extends StdDeserializer<CracCreatorParameters> {

    CracCreatorParametersDeserializer() {
        super(CracCreatorParameters.class);
    }

    @Override
    public CracCreatorParameters deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        return deserialize(parser, deserializationContext, new CracCreatorParameters());
    }

    @Override
    public CracCreatorParameters deserialize(JsonParser parser, DeserializationContext deserializationContext, CracCreatorParameters parameters) throws IOException {

        List<Extension<CracCreatorParameters>> extensions = Collections.emptyList();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.getCurrentName()) {
                case "crac-factory":
                    parameters.setCracFactoryName(parser.nextTextValue());
                    break;
                case "extensions":
                    parser.nextToken();
                    if (parameters.getExtensions().isEmpty()) {
                        extensions = JsonUtil.readExtensions(parser, deserializationContext, JsonCracCreatorParameters.getExtensionSerializers());
                    } else {
                        JsonUtil.updateExtensions(parser, deserializationContext, JsonCracCreatorParameters.getExtensionSerializers(), parameters);
                    }
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + parser.getCurrentName());
            }
        }

        JsonCracCreatorParameters.getExtensionSerializers().addExtensions(parameters, extensions);
        return parameters;
    }

}
