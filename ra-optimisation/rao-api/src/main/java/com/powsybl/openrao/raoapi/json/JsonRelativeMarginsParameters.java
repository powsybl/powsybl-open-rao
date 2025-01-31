/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class JsonRelativeMarginsParameters {

    private JsonRelativeMarginsParameters() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        Optional<RelativeMarginsParameters> optionalRelativeMarginsParameters = parameters.getRelativeMarginsParameters();
        if (optionalRelativeMarginsParameters.isPresent()) {
            jsonGenerator.writeObjectFieldStart(RELATIVE_MARGINS);
            jsonGenerator.writeArrayFieldStart(PTDF_BOUNDARIES);
            for (String ptdfBoundary : optionalRelativeMarginsParameters.get().getPtdfBoundariesAsString()) {
                jsonGenerator.writeString(ptdfBoundary);
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
    }

    static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        RelativeMarginsParameters relativeMarginsParameters = new RelativeMarginsParameters();
        while (!jsonParser.nextToken().isStructEnd()) {
            if (jsonParser.getCurrentName().equals(PTDF_BOUNDARIES)) {
                if (jsonParser.getCurrentToken() == JsonToken.START_ARRAY) {
                    List<String> boundaries = new ArrayList<>();
                    while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                        boundaries.add(jsonParser.getValueAsString());
                    }
                    relativeMarginsParameters.setPtdfBoundariesFromString(boundaries);
                }
            } else {
                throw new OpenRaoException(String.format("Cannot deserialize relative margins parameters: unexpected field in %s (%s)", RELATIVE_MARGINS, jsonParser.getCurrentName()));
            }
        }
        raoParameters.setRelativeMarginsParameters(relativeMarginsParameters);
    }

}
