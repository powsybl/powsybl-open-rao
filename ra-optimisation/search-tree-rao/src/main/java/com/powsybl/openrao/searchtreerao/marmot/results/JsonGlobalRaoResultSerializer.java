/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.powsybl.openrao.searchtreerao.marmot.results;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.GlobalRaoResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants;

import java.io.IOException;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class JsonGlobalRaoResultSerializer extends JsonSerializer<GlobalRaoResult> {
    private static final String GLOBAL_RAO_RESULT = "GLOBAL_RAO_RESULT";
    private static final String VERSION = "1.0";

    @Override
    public void serialize(GlobalRaoResult globalRaoResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        // type and version
        jsonGenerator.writeStringField(JsonSerializationConstants.TYPE, GLOBAL_RAO_RESULT);
        jsonGenerator.writeStringField(JsonSerializationConstants.VERSION, VERSION);
        jsonGenerator.writeStringField(JsonSerializationConstants.INFO, RaoResultJsonConstants.RAO_RESULT_INFO);

        // computation status
        ComputationStatus computationStatus = globalRaoResult.getComputationStatus();
        jsonGenerator.writeStringField(COMPUTATION_STATUS, serializeStatus(computationStatus));

        jsonGenerator.writeEndObject();
    }
}
