/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.json.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.MarmotParameters;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.MARGIN_WINDOW_TO_CONSIDER;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.MARMOT_PARAMETERS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.MAX_MIP_ITERATIONS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.MIN_RELATIVE_IMPROVEMENT_ON_MARGIN;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.NUMBER_OF_CNECS_TO_ADD_PER_VIRTUAL_COST_NAME;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.NUMBER_OF_THREADS;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonMarmotParameters implements JsonRaoParameters.ExtensionSerializer<MarmotParameters> {

    @Override
    public void serialize(MarmotParameters marmotParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField(NUMBER_OF_CNECS_TO_ADD_PER_VIRTUAL_COST_NAME, marmotParameters.getNumberOfCnecsToAddPerVirtualCostName());
        jsonGenerator.writeNumberField(MIN_RELATIVE_IMPROVEMENT_ON_MARGIN, marmotParameters.getMinRelativeImprovementOnMargin());
        jsonGenerator.writeNumberField(MARGIN_WINDOW_TO_CONSIDER, marmotParameters.getMarginWindowToConsider());
        jsonGenerator.writeNumberField(MAX_MIP_ITERATIONS, marmotParameters.getMaxMipIterations());
        jsonGenerator.writeNumberField(NUMBER_OF_THREADS, marmotParameters.getNumberOfThreads());
        jsonGenerator.writeEndObject();
    }

    @Override
    public MarmotParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        MarmotParameters marmotParameters = new MarmotParameters();
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case NUMBER_OF_CNECS_TO_ADD_PER_VIRTUAL_COST_NAME -> {
                    jsonParser.nextToken();
                    marmotParameters.setNumberOfCnecsToAddPerVirtualCostName(jsonParser.getIntValue());
                }
                case MIN_RELATIVE_IMPROVEMENT_ON_MARGIN -> {
                    jsonParser.nextToken();
                    marmotParameters.setMinRelativeImprovementOnMargin(jsonParser.getDoubleValue());
                }
                case MARGIN_WINDOW_TO_CONSIDER -> {
                    jsonParser.nextToken();
                    marmotParameters.setMarginWindowToConsider(jsonParser.getDoubleValue());
                }
                case MAX_MIP_ITERATIONS -> {
                    jsonParser.nextToken();
                    marmotParameters.setMaxMipIterations(jsonParser.getIntValue());
                }
                case NUMBER_OF_THREADS -> {
                    jsonParser.nextToken();
                    marmotParameters.setNumberOfThreads(jsonParser.getIntValue());
                }
                default -> throw new OpenRaoException(String.format("Cannot deserialize marmot parameters: unexpected field in %s (%s)", MARMOT_PARAMETERS, jsonParser.currentName()));
            }
        }
        return marmotParameters;
    }

    @Override
    public String getExtensionName() {
        return "marmot-parameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super MarmotParameters> getExtensionClass() {
        return MarmotParameters.class;
    }
}
