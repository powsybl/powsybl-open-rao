/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.api.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracCreationParametersSerializer extends StdSerializer<CracCreationParameters> {

    CracCreationParametersSerializer() {
        super(CracCreationParameters.class);
    }

    @Override
    public void serialize(CracCreationParameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("crac-factory", parameters.getCracFactoryName());
        JsonUtil.writeExtensions(parameters, jsonGenerator, serializerProvider, JsonCracCreationParameters.getExtensionSerializers());
        jsonGenerator.writeEndObject();
    }
}
