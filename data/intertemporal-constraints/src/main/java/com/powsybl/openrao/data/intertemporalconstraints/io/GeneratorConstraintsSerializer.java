/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.intertemporalconstraints.io;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.openrao.data.intertemporalconstraints.GeneratorConstraints;

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
        jsonGenerator.writeStringField(JsonIntertemporalConstraints.GENERATOR_ID, generatorConstraints.getGeneratorId());

        Optional<Double> pMin = generatorConstraints.getPMin();
        if (pMin.isPresent()) {
            jsonGenerator.writeNumberField(JsonIntertemporalConstraints.P_MIN, pMin.get());
        }

        Optional<Double> pMax = generatorConstraints.getPMax();
        if (pMax.isPresent()) {
            jsonGenerator.writeNumberField(JsonIntertemporalConstraints.P_MAX, pMax.get());
        }

        Optional<Double> leadTime = generatorConstraints.getLeadTime();
        if (leadTime.isPresent()) {
            jsonGenerator.writeNumberField(JsonIntertemporalConstraints.LEAD_TIME, leadTime.get());
        }

        Optional<Double> lagTime = generatorConstraints.getLagTime();
        if (lagTime.isPresent()) {
            jsonGenerator.writeNumberField(JsonIntertemporalConstraints.LAG_TIME, lagTime.get());
        }

        Optional<Double> upwardPowerGradient = generatorConstraints.getUpwardPowerGradient();
        if (upwardPowerGradient.isPresent()) {
            jsonGenerator.writeNumberField(JsonIntertemporalConstraints.UPWARD_POWER_GRADIENT, upwardPowerGradient.get());
        }

        Optional<Double> downwardPowerGradient = generatorConstraints.getDownwardPowerGradient();
        if (downwardPowerGradient.isPresent()) {
            jsonGenerator.writeNumberField(JsonIntertemporalConstraints.DOWNWARD_POWER_GRADIENT, downwardPowerGradient.get());
        }

        Optional<Double> minUpTime = generatorConstraints.getMinUpTime();
        if (minUpTime.isPresent()) {
            jsonGenerator.writeNumberField(JsonIntertemporalConstraints.MIN_UP_TIME, minUpTime.get());
        }

        Optional<Double> maxUpTime = generatorConstraints.getMaxUpTime();
        if (maxUpTime.isPresent()) {
            jsonGenerator.writeNumberField(JsonIntertemporalConstraints.MAX_UP_TIME, maxUpTime.get());
        }

        Optional<Double> minOffTime = generatorConstraints.getMinOffTime();
        if (minOffTime.isPresent()) {
            jsonGenerator.writeNumberField(JsonIntertemporalConstraints.MIN_OFF_TIME, minOffTime.get());
        }

        jsonGenerator.writeEndObject();
    }
}
