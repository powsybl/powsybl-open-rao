/*
 *  Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json.serializers;

import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class LoadFlowAndSensitivityComputationParametersSerializer {

    private LoadFlowAndSensitivityComputationParametersSerializer() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeObjectFieldStart(LOAD_FLOW_AND_SENSITIVITY_COMPUTATION);
        jsonGenerator.writeStringField(LOAD_FLOW_PROVIDER, parameters.getLoadFlowAndSensitivityParameters().getLoadFlowProvider());
        jsonGenerator.writeStringField(SENSITIVITY_PROVIDER, parameters.getLoadFlowAndSensitivityParameters().getSensitivityProvider());
        jsonGenerator.writeNumberField(SENSITIVITY_FAILURE_OVERCOST, parameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost());
        jsonGenerator.writeFieldName(SENSITIVITY_PARAMETERS);
        serializerProvider.defaultSerializeValue(parameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters(), jsonGenerator);
        jsonGenerator.writeEndObject();
    }
}
