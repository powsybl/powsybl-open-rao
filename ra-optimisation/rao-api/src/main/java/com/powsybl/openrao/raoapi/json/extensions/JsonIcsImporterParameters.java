/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.json.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.extensions.IcsImporterParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;

import java.io.IOException;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class JsonIcsImporterParameters {

    private JsonIcsImporterParameters() {
    }

    static void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        Optional<IcsImporterParameters> icsImporterParameters = parameters.getIcsImporterParameters();
        if (icsImporterParameters.isPresent()) {
            jsonGenerator.writeObjectFieldStart(ICS_IMPORTER_PARAMETERS);
            jsonGenerator.writeObjectField(COST_DOWN, icsImporterParameters.get().getCostDown());
            jsonGenerator.writeNumberField(COST_UP, icsImporterParameters.get().getCostUp());
            jsonGenerator.writeEndObject();
        }

    }

    static void deserialize(JsonParser jsonParser, OpenRaoSearchTreeParameters searchTreeParameters) throws IOException {
        IcsImporterParameters icsImporterParameters = new IcsImporterParameters();
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case COST_DOWN:
                    icsImporterParameters.setCostDown(jsonParser.getValueAsDouble());
                    break;
                case COST_UP:
                    jsonParser.nextToken();
                    icsImporterParameters.setCostUp(jsonParser.getDoubleValue());
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize ics importer parameters: unexpected field in %s (%s)", ICS_IMPORTER_PARAMETERS, jsonParser.getCurrentName()));
            }
            searchTreeParameters.setIcsImporterParameters(icsImporterParameters);
        }
    }
}
