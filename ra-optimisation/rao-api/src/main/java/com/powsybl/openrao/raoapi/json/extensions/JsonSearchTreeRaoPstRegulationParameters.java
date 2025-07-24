/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.json.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoPstRegulationParameters;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.PSTS_TO_REGULATE;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.PST_REGULATION_PARAMETERS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class JsonSearchTreeRaoPstRegulationParameters {
    private JsonSearchTreeRaoPstRegulationParameters() {
    }

    static void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        Optional<SearchTreeRaoPstRegulationParameters> pstRegulationParameters = parameters.getPstRegulationParameters();
        if (pstRegulationParameters.isPresent()) {
            jsonGenerator.writeObjectFieldStart(PST_REGULATION_PARAMETERS);
            jsonGenerator.writeArrayFieldStart(PSTS_TO_REGULATE);
            for (String pstToRegulate : pstRegulationParameters.get().getPstsToRegulate()) {
                jsonGenerator.writeString(pstToRegulate);
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }

    }

    static void deserialize(JsonParser jsonParser, OpenRaoSearchTreeParameters searchTreeParameters) throws IOException {
        SearchTreeRaoPstRegulationParameters pstRegulationParameters = new SearchTreeRaoPstRegulationParameters();
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case PSTS_TO_REGULATE:
                    jsonParser.nextToken();
                    pstRegulationParameters.setPstsToRegulate(List.of(jsonParser.readValueAs(String[].class)));
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize PST regulation parameters: unexpected field in %s (%s)", PST_REGULATION_PARAMETERS, jsonParser.getCurrentName()));
            }
            searchTreeParameters.setPstRegulationParameters(pstRegulationParameters);
        }
    }
}
