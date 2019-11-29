/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.json;

import com.farao_community.farao.ra_optimisation.ContingencyResult;
import com.farao_community.farao.ra_optimisation.PreContingencyResult;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RaoComputationResultDeserializer extends StdDeserializer<RaoComputationResult> {

    RaoComputationResultDeserializer() {
        super(RaoComputationResult.class);
    }

    @Override
    public RaoComputationResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        RaoComputationResult.Status status = RaoComputationResult.Status.FAILURE;
        PreContingencyResult preContingencyResult = null;
        List<ContingencyResult> contingencyResults = Collections.emptyList();
        List<Extension<RaoComputationResult>> extensions = Collections.emptyList();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.getCurrentName()) {

                case "status":

                    status = RaoComputationResult.Status.valueOf(jsonParser.nextTextValue());
                    break;

                case "preContingencyResult":
                    jsonParser.nextValue();
                    preContingencyResult = jsonParser.readValueAs(PreContingencyResult.class);
                    break;

                case "contingencyResults":
                    jsonParser.nextToken();
                    contingencyResults = jsonParser.readValueAs(new TypeReference<ArrayList<ContingencyResult>>() {
                    });
                    break;

                case "extensions":
                    jsonParser.nextToken();
                    extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, JsonRaoComputationResult.getExtensionSerializers());
                    break;

                default:
                    throw new AssertionError("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        RaoComputationResult result = new RaoComputationResult(
                status,
                preContingencyResult,
                contingencyResults
        );
        JsonRaoComputationResult.getExtensionSerializers().addExtensions(result, extensions);

        return result;
    }
}
