/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.intertemporalconstraints.io;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.intertemporalconstraints.GeneratorConstraints;

import java.io.IOException;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class GeneratorConstraintsDeserializer extends StdDeserializer<GeneratorConstraints> {

    public GeneratorConstraintsDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public GeneratorConstraints deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        GeneratorConstraints.GeneratorConstraintsBuilder builder = GeneratorConstraints.create();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.currentName()) {
                case JsonIntertemporalConstraints.GENERATOR_ID -> {
                    jsonParser.nextToken();
                    builder.withGeneratorId(jsonParser.getValueAsString());
                }
                case JsonIntertemporalConstraints.LEAD_TIME -> {
                    jsonParser.nextToken();
                    builder.withLeadTime(jsonParser.getValueAsDouble());
                }
                case JsonIntertemporalConstraints.LAG_TIME -> {
                    jsonParser.nextToken();
                    builder.withLagTime(jsonParser.getValueAsDouble());
                }
                case JsonIntertemporalConstraints.UPWARD_POWER_GRADIENT -> {
                    jsonParser.nextToken();
                    builder.withUpwardPowerGradient(jsonParser.getValueAsDouble());
                }
                case JsonIntertemporalConstraints.DOWNWARD_POWER_GRADIENT -> {
                    jsonParser.nextToken();
                    builder.withDownwardPowerGradient(jsonParser.getValueAsDouble());
                }
                default ->
                    throw new OpenRaoException("Unexpected field '%s' in JSON generator constraints.".formatted(jsonParser.currentName()));
            }
        }
        return builder.build();
    }
}
