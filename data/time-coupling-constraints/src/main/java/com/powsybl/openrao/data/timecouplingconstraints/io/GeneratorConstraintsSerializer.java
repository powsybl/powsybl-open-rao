/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.timecouplingconstraints.io;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.openrao.data.timecouplingconstraints.GeneratorConstraints;

import java.io.IOException;
import java.util.Optional;

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
        jsonGenerator.writeStringField(JsonTimeCouplingConstraints.GENERATOR_ID, generatorConstraints.getGeneratorId());

        Optional<Double> leadTime = generatorConstraints.getLeadTime();
        if (leadTime.isPresent()) {
            jsonGenerator.writeNumberField(JsonTimeCouplingConstraints.LEAD_TIME, leadTime.get());
        }

        Optional<Double> lagTime = generatorConstraints.getLagTime();
        if (lagTime.isPresent()) {
            jsonGenerator.writeNumberField(JsonTimeCouplingConstraints.LAG_TIME, lagTime.get());
        }

        Optional<Double> upwardPowerGradient = generatorConstraints.getUpwardPowerGradient();
        if (upwardPowerGradient.isPresent()) {
            jsonGenerator.writeNumberField(JsonTimeCouplingConstraints.UPWARD_POWER_GRADIENT, upwardPowerGradient.get());
        }

        Optional<Double> downwardPowerGradient = generatorConstraints.getDownwardPowerGradient();
        if (downwardPowerGradient.isPresent()) {
            jsonGenerator.writeNumberField(JsonTimeCouplingConstraints.DOWNWARD_POWER_GRADIENT, downwardPowerGradient.get());
        }

        jsonGenerator.writeEndObject();
    }
}
