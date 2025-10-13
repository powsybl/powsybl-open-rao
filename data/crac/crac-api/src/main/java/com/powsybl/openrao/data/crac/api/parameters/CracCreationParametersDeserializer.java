/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.parameters;

import com.powsybl.openrao.commons.OpenRaoException;
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
public class CracCreationParametersDeserializer extends StdDeserializer<CracCreationParameters> {

    CracCreationParametersDeserializer() {
        super(CracCreationParameters.class);
    }

    @Override
    public CracCreationParameters deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        return deserialize(parser, deserializationContext, new CracCreationParameters());
    }

    @Override
    public CracCreationParameters deserialize(JsonParser parser, DeserializationContext deserializationContext, CracCreationParameters parameters) throws IOException {

        List<Extension<CracCreationParameters>> extensions = Collections.emptyList();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.getCurrentName()) {
                case JsonCracCreationParametersConstants.CRAC_FACTORY:
                    parameters.setCracFactoryName(parser.nextTextValue());
                    break;
                case JsonCracCreationParametersConstants.DEFAULT_MONITORED_LINE_SIDE:
                    parameters.setDefaultMonitoredLineSide(JsonCracCreationParametersConstants.deserializeMonitoredLineSide(parser.nextTextValue()));
                    break;
                case JsonCracCreationParametersConstants.RA_USAGE_LIMITS_PER_INSTANT:
                    parser.nextToken();
                    JsonCracCreationParametersConstants.deserializeRaUsageLimitsAndUpdateParameters(parser, parameters);
                    break;
                case "extensions":
                    parser.nextToken();
                    if (parameters.getExtensions().isEmpty()) {
                        extensions = JsonUtil.readExtensions(parser, deserializationContext, JsonCracCreationParameters.getExtensionSerializers());
                    } else {
                        JsonUtil.updateExtensions(parser, deserializationContext, JsonCracCreationParameters.getExtensionSerializers(), parameters);
                    }
                    break;
                default:
                    throw new OpenRaoException("Unexpected field: " + parser.getCurrentName());
            }
        }

        JsonCracCreationParameters.getExtensionSerializers().addExtensions(parameters, extensions);
        return parameters;
    }

}
