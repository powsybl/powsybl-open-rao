/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

import static com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParametersConstants.CRAC_FACTORY;
import static com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParametersConstants.DEFAULT_MONITORED_LINE_SIDE;
import static com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParametersConstants.serializeMonitoredLineSide;
import static com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParametersConstants.serializeRaUsageLimits;

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
        jsonGenerator.writeStringField(CRAC_FACTORY, parameters.getCracFactoryName());
        jsonGenerator.writeStringField(DEFAULT_MONITORED_LINE_SIDE, serializeMonitoredLineSide(parameters.getDefaultMonitoredLineSide()));
        serializeRaUsageLimits(parameters, jsonGenerator);
        JsonUtil.writeExtensions(parameters, jsonGenerator, serializerProvider, JsonCracCreationParameters.getExtensionSerializers());
        jsonGenerator.writeEndObject();
    }
}
