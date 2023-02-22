/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.sensitivity.json.JsonSensitivityAnalysisParameters;

import java.io.IOException;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class LoadFlowAndSensitivityComputationParametersDeserializer {

    private LoadFlowAndSensitivityComputationParametersDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case LOAD_FLOW_PROVIDER:
                    jsonParser.nextToken();
                    raoParameters.getLoadFlowAndSensitivityParameters().setLoadFlowProvider(jsonParser.getValueAsString());
                    break;
                case SENSITIVITY_PROVIDER:
                    jsonParser.nextToken();
                    raoParameters.getLoadFlowAndSensitivityParameters().setSensitivityProvider(jsonParser.getValueAsString());
                    break;
                case SENSITIVITY_PARAMETERS:
                    jsonParser.nextToken();
                    raoParameters.getLoadFlowAndSensitivityParameters().setSensitivityWithLoadFlowParameters(JsonSensitivityAnalysisParameters.createObjectMapper().readerForUpdating(raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters()).readValue(jsonParser));
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize load flow and sensitivity parameters: unexpected field in %s (%s)", LOAD_FLOW_AND_SENSITIVITY_COMPUTATION, jsonParser.getCurrentName()));
            }
        }
    }
}
