/*
 *  Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.json.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.sensitivity.json.JsonSensitivityAnalysisParameters;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class JsonLoadFlowAndSensitivityComputationParameters {

    private JsonLoadFlowAndSensitivityComputationParameters() {
    }

    static void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeObjectFieldStart(LOAD_FLOW_AND_SENSITIVITY_COMPUTATION);
        jsonGenerator.writeStringField(LOAD_FLOW_PROVIDER, parameters.getLoadFlowAndSensitivityParameters().getLoadFlowProvider());
        jsonGenerator.writeStringField(SENSITIVITY_PROVIDER, parameters.getLoadFlowAndSensitivityParameters().getSensitivityProvider());
        jsonGenerator.writeNumberField(SENSITIVITY_FAILURE_OVERCOST, parameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost());
        jsonGenerator.writeFieldName(SENSITIVITY_PARAMETERS);
        serializerProvider.defaultSerializeValue(parameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters(), jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    static void deserialize(JsonParser jsonParser, OpenRaoSearchTreeParameters searchTreeParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case LOAD_FLOW_PROVIDER:
                    jsonParser.nextToken();
                    searchTreeParameters.getLoadFlowAndSensitivityParameters().setLoadFlowProvider(jsonParser.getValueAsString());
                    break;
                case SENSITIVITY_PROVIDER:
                    jsonParser.nextToken();
                    searchTreeParameters.getLoadFlowAndSensitivityParameters().setSensitivityProvider(jsonParser.getValueAsString());
                    break;
                case SENSITIVITY_FAILURE_OVERCOST:
                    jsonParser.nextToken();
                    searchTreeParameters.getLoadFlowAndSensitivityParameters().setSensitivityFailureOvercost(jsonParser.getValueAsDouble());
                    break;
                case SENSITIVITY_PARAMETERS:
                    jsonParser.nextToken();
                    searchTreeParameters.getLoadFlowAndSensitivityParameters().setSensitivityWithLoadFlowParameters(JsonSensitivityAnalysisParameters.createObjectMapper().readerForUpdating(searchTreeParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters()).readValue(jsonParser));
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize load flow and sensitivity parameters: unexpected field in %s (%s)", LOAD_FLOW_AND_SENSITIVITY_COMPUTATION, jsonParser.getCurrentName()));
            }
        }
    }
}
