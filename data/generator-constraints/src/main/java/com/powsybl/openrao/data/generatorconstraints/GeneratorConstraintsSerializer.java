/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.generatorconstraints;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class GeneratorConstraintsSerializer extends StdSerializer<GeneratorConstraints> {

    public GeneratorConstraintsSerializer(Class<GeneratorConstraints> t) {
        super(t);
    }

    @Override
    public void serialize(GeneratorConstraints generatorConstraints, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(JsonGeneratorConstraints.ID, generatorConstraints.getGeneratorId());
        if (generatorConstraints.getPMin().isPresent()) {
            jsonGenerator.writeNumberField(JsonGeneratorConstraints.P_MIN, generatorConstraints.getPMin().get());
        }
        if (generatorConstraints.getPMax().isPresent()) {
            jsonGenerator.writeNumberField(JsonGeneratorConstraints.P_MAX, generatorConstraints.getPMax().get());
        }
        if (generatorConstraints.getLeadTime().isPresent()) {
            jsonGenerator.writeNumberField(JsonGeneratorConstraints.LEAD_TIME, generatorConstraints.getLeadTime().get());
        }
        if (generatorConstraints.getLagTime().isPresent()) {
            jsonGenerator.writeNumberField(JsonGeneratorConstraints.LAG_TIME, generatorConstraints.getLagTime().get());
        }
        if (generatorConstraints.getUpwardPowerGradient().isPresent()) {
            jsonGenerator.writeNumberField(JsonGeneratorConstraints.UPWARD_POWER_GRADIENT, generatorConstraints.getUpwardPowerGradient().get());
        }
        if (generatorConstraints.getDownwardPowerGradient().isPresent()) {
            jsonGenerator.writeNumberField(JsonGeneratorConstraints.DOWNWARD_POWER_GRADIENT, generatorConstraints.getDownwardPowerGradient().get());
        }
        if (generatorConstraints.getMinUpTime().isPresent()) {
            jsonGenerator.writeNumberField(JsonGeneratorConstraints.MIN_UP_TIME, generatorConstraints.getMinUpTime().get());
        }
        if (generatorConstraints.getMaxUpTime().isPresent()) {
            jsonGenerator.writeNumberField(JsonGeneratorConstraints.MAX_UP_TIME, generatorConstraints.getMaxUpTime().get());
        }
        if (generatorConstraints.getMinOffTime().isPresent()) {
            jsonGenerator.writeNumberField(JsonGeneratorConstraints.MIN_OFF_TIME, generatorConstraints.getMinOffTime().get());
        }
        jsonGenerator.writeEndObject();
    }
}
