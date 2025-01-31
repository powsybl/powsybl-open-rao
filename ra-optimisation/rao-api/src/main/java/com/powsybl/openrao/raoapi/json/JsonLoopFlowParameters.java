/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class JsonLoopFlowParameters {

    private JsonLoopFlowParameters() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        Optional<LoopFlowParameters> optionalLoopFlowParameters = parameters.getLoopFlowParameters();
        if (optionalLoopFlowParameters.isPresent()) {
            jsonGenerator.writeObjectFieldStart(LOOP_FLOW_PARAMETERS);
            jsonGenerator.writeNumberField(ACCEPTABLE_INCREASE, optionalLoopFlowParameters.get().getAcceptableIncrease());
            jsonGenerator.writeFieldName(COUNTRIES);
            jsonGenerator.writeStartArray();
            optionalLoopFlowParameters.get().getCountries().stream().map(Enum::toString).sorted().forEach(s -> {
                try {
                    jsonGenerator.writeString(s);
                } catch (IOException e) {
                    throw new OpenRaoException("error while serializing loopflow countries", e);
                }
            });
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
    }

    static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        LoopFlowParameters loopFlowParameters = new LoopFlowParameters();
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case ACCEPTABLE_INCREASE:
                    jsonParser.nextToken();
                    loopFlowParameters.setAcceptableIncrease(jsonParser.getDoubleValue());
                    break;
                case COUNTRIES:
                    jsonParser.nextToken();
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode arrayNode = objectMapper.readTree(jsonParser);
                    List<String> countryStrings = objectMapper.readValue(arrayNode.traverse(), new TypeReference<ArrayList<String>>() { });
                    loopFlowParameters.setCountries(countryStrings);
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize loop flow parameters: unexpected field in %s (%s)", LOOP_FLOW_PARAMETERS, jsonParser.getCurrentName()));
            }
        }
        raoParameters.setLoopFlowParameters(loopFlowParameters);
    }
}
