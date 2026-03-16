/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.timecoupledconstraints.io;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.openrao.data.timecoupledconstraints.GeneratorConstraints;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;

import java.io.IOException;
import java.util.Comparator;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class TimeCoupledConstraintsSerializer extends StdSerializer<TimeCoupledConstraints> {

    protected TimeCoupledConstraintsSerializer(Class<TimeCoupledConstraints> t) {
        super(t);
    }

    @Override
    public void serialize(TimeCoupledConstraints timeCoupledConstraints, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(JsonTimeCoupledConstraints.TYPE, JsonTimeCoupledConstraints.DESCRIPTION);
        jsonGenerator.writeStringField(JsonTimeCoupledConstraints.VERSION, JsonTimeCoupledConstraints.CURRENT_VERSION);
        if (!timeCoupledConstraints.getGeneratorConstraints().isEmpty()) {
            serializeGeneratorConstraints(timeCoupledConstraints.getGeneratorConstraints(), jsonGenerator);
        }
        jsonGenerator.writeEndObject();
    }

    private static void serializeGeneratorConstraints(Set<GeneratorConstraints> generatorConstraints, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeArrayFieldStart(JsonTimeCoupledConstraints.GENERATOR_CONSTRAINTS);
        for (GeneratorConstraints individualGeneratorConstraints : generatorConstraints.stream().sorted(Comparator.comparing(GeneratorConstraints::getGeneratorId)).toList()) {
            jsonGenerator.writeObject(individualGeneratorConstraints);
        }
        jsonGenerator.writeEndArray();
    }
}
