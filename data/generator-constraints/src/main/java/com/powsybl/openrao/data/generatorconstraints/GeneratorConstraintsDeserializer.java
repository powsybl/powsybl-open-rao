/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.generatorconstraints;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.openrao.commons.OpenRaoException;

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
                case JsonGeneratorConstraints.ID -> {
                    jsonParser.nextToken();
                    builder.withGeneratorId(jsonParser.getValueAsString());
                }
                case JsonGeneratorConstraints.P_MIN -> {
                    jsonParser.nextToken();
                    builder.withPMin(jsonParser.getValueAsDouble());
                }
                case JsonGeneratorConstraints.P_MAX -> {
                    jsonParser.nextToken();
                    builder.withPMax(jsonParser.getValueAsDouble());
                }
                case JsonGeneratorConstraints.LEAD_TIME -> {
                    jsonParser.nextToken();
                    builder.withLeadTime(jsonParser.getValueAsDouble());
                }
                case JsonGeneratorConstraints.LAG_TIME -> {
                    jsonParser.nextToken();
                    builder.withLagTime(jsonParser.getValueAsDouble());
                }
                case JsonGeneratorConstraints.UPWARD_POWER_GRADIENT -> {
                    jsonParser.nextToken();
                    builder.withUpwardPowerGradient(jsonParser.getValueAsDouble());
                }
                case JsonGeneratorConstraints.DOWNWARD_POWER_GRADIENT -> {
                    jsonParser.nextToken();
                    builder.withDownwardPowerGradient(jsonParser.getValueAsDouble());
                }
                case JsonGeneratorConstraints.MIN_UP_TIME -> {
                    jsonParser.nextToken();
                    builder.withMinUpTime(jsonParser.getValueAsDouble());
                }
                case JsonGeneratorConstraints.MAX_UP_TIME -> {
                    jsonParser.nextToken();
                    builder.withMaxUpTime(jsonParser.getValueAsDouble());
                }
                case JsonGeneratorConstraints.MIN_OFF_TIME -> {
                    jsonParser.nextToken();
                    builder.withMinOffTime(jsonParser.getValueAsDouble());
                }
                default ->
                    throw new OpenRaoException("Unexpected field %s in JSON generator constraints.".formatted(jsonParser.currentName()));
            }
        }
        return builder.build();
    }
}
