/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.POST_PROCESSING;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.REMOVE_ADDED_VARIANTS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class JsonPostProcessingParameters {

    private JsonPostProcessingParameters() {
    }

    public static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(POST_PROCESSING);
        jsonGenerator.writeBooleanField(REMOVE_ADDED_VARIANTS, parameters.getPostProcessingParameters().mustRemoveAddedVariants());
        jsonGenerator.writeEndObject();
    }

    public static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            if (REMOVE_ADDED_VARIANTS.equals(jsonParser.currentName())) {
                jsonParser.nextToken();
                raoParameters.getPostProcessingParameters().setRemoveAddedVariants(jsonParser.getBooleanValue());
            } else {
                throw new OpenRaoException(String.format(
                    "Cannot deserialize post-processing parameters: unexpected field in %s (%s)",
                    POST_PROCESSING,
                    jsonParser.currentName()
                ));
            }
        }
    }

}
