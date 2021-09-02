/*
 * Copyright (c) 2021, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creator_api.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracCreatorParametersSerializer extends StdSerializer<CracCreatorParameters> {

    CracCreatorParametersSerializer() {
        super(CracCreatorParameters.class);
    }

    @Override
    public void serialize(CracCreatorParameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("crac-factory", parameters.getCracFactoryName());
        JsonUtil.writeExtensions(parameters, jsonGenerator, serializerProvider, JsonCracCreatorParameters.getExtensionSerializers());
        jsonGenerator.writeEndObject();
    }
}
