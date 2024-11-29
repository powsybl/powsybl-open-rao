/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.json.extensions;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParameters;

import java.io.IOException;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class JsonRelativeMarginsParameters {

    private JsonRelativeMarginsParameters() {
    }

    static void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        Optional<RelativeMarginsParameters> optionalRelativeMarginsParameters = parameters.getRelativeMarginsParameters();
        if (optionalRelativeMarginsParameters.isPresent()) {
            jsonGenerator.writeObjectFieldStart(RELATIVE_MARGINS);
            jsonGenerator.writeObjectField(PTDF_APPROXIMATION, optionalRelativeMarginsParameters.get().getPtdfApproximation());
            jsonGenerator.writeNumberField(PTDF_SUM_LOWER_BOUND, optionalRelativeMarginsParameters.get().getPtdfSumLowerBound());
            jsonGenerator.writeEndObject();
        }
    }

    static void deserialize(JsonParser jsonParser, OpenRaoSearchTreeParameters searchTreeParameters) throws IOException {
        RelativeMarginsParameters relativeMarginsParameters = new RelativeMarginsParameters();
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case PTDF_APPROXIMATION:
                    relativeMarginsParameters.setPtdfApproximation(stringToPtdfApproximation(jsonParser.nextTextValue()));
                    break;
                case PTDF_SUM_LOWER_BOUND:
                    jsonParser.nextToken();
                    relativeMarginsParameters.setPtdfSumLowerBound(jsonParser.getDoubleValue());
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize relative margins parameters: unexpected field in %s (%s)", RELATIVE_MARGINS, jsonParser.getCurrentName()));
            }
            searchTreeParameters.setRelativeMarginsParameters(relativeMarginsParameters);
        }
    }
}
