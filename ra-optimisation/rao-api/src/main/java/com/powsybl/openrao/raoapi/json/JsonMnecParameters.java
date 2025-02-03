/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.MnecParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.io.IOException;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class JsonMnecParameters {

    private JsonMnecParameters() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        Optional<MnecParameters> optionalMnecParameters = parameters.getMnecParameters();
        if (optionalMnecParameters.isPresent()) {
            jsonGenerator.writeObjectFieldStart(MNEC_PARAMETERS);
            jsonGenerator.writeNumberField(ACCEPTABLE_MARGIN_DECREASE, optionalMnecParameters.get().getAcceptableMarginDecrease());
            jsonGenerator.writeEndObject();
        }
    }

    static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        MnecParameters mnecParameters = new MnecParameters();
        while (!jsonParser.nextToken().isStructEnd()) {
            if (jsonParser.getCurrentName().equals(ACCEPTABLE_MARGIN_DECREASE)) {
                jsonParser.nextToken();
                mnecParameters.setAcceptableMarginDecrease(jsonParser.getDoubleValue());
            } else {
                throw new OpenRaoException(String.format("Cannot deserialize mnec parameters: unexpected field in %s (%s)", MNEC_PARAMETERS, jsonParser.getCurrentName()));
            }
        }
        raoParameters.setMnecParameters(mnecParameters);
    }

}
