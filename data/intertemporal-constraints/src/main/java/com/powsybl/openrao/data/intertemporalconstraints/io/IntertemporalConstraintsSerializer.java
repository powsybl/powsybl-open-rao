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
import com.powsybl.openrao.data.intertemporalconstraints.IntertemporalConstraints;

import java.io.IOException;
import java.util.Comparator;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class IntertemporalConstraintsSerializer extends StdSerializer<IntertemporalConstraints> {

    protected IntertemporalConstraintsSerializer(Class<IntertemporalConstraints> t) {
        super(t);
    }

    @Override
    public void serialize(IntertemporalConstraints intertemporalConstraints, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(JsonIntertemporalConstraints.TYPE, JsonIntertemporalConstraints.DESCRIPTION);
        jsonGenerator.writeStringField(JsonIntertemporalConstraints.VERSION, JsonIntertemporalConstraints.CURRENT_VERSION);
        if (!intertemporalConstraints.getGeneratorConstraints().isEmpty()) {
            serializeGeneratorConstraints(intertemporalConstraints.getGeneratorConstraints(), jsonGenerator);
        }
        jsonGenerator.writeEndObject();
    }

    private static void serializeGeneratorConstraints(Set<GeneratorConstraints> generatorConstraints, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeArrayFieldStart(JsonIntertemporalConstraints.GENERATOR_CONSTRAINTS);
        for (GeneratorConstraints individualGeneratorConstraints : generatorConstraints.stream().sorted(Comparator.comparing(GeneratorConstraints::getGeneratorId)).toList()) {
            jsonGenerator.writeObject(individualGeneratorConstraints);
        }
        jsonGenerator.writeEndArray();
    }
}
