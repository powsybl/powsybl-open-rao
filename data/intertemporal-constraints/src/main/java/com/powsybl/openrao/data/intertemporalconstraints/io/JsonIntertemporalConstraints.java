/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.intertemporalconstraints.io;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.intertemporalconstraints.GeneratorConstraints;
import com.powsybl.openrao.data.intertemporalconstraints.TimeCouplingConstraints;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class JsonIntertemporalConstraints {

    private JsonIntertemporalConstraints() {
    }

    // General data

    public static final String TYPE = "type";
    public static final String DESCRIPTION = "OpenRAO Intertemporal Constraints";
    public static final String VERSION = "version";
    public static final String CURRENT_VERSION = "1.0";

    /**
     * CHANGELOG v1
     * ------------
     */

    // Generator constraints

    public static final String GENERATOR_CONSTRAINTS = "generatorConstraints";
    public static final String GENERATOR_ID = "generatorId";
    public static final String LEAD_TIME = "leadTime";
    public static final String LAG_TIME = "lagTime";
    public static final String UPWARD_POWER_GRADIENT = "upwardPowerGradient";
    public static final String DOWNWARD_POWER_GRADIENT = "downwardPowerGradient";

    // IO

    public static void write(TimeCouplingConstraints timeCouplingConstraints, OutputStream outputStream) throws IOException {
        ObjectMapper objectMapper = createObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule module = new SimpleModule();
        module.addSerializer(TimeCouplingConstraints.class, new IntertemporalConstraintsSerializer(TimeCouplingConstraints.class));
        module.addSerializer(GeneratorConstraints.class, new GeneratorConstraintsSerializer(GeneratorConstraints.class));
        objectMapper.registerModule(module);
        ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
        writer.writeValue(outputStream, timeCouplingConstraints);
    }

    public static TimeCouplingConstraints read(InputStream inputStream) throws IOException {
        ObjectMapper objectMapper = createObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(TimeCouplingConstraints.class, new IntertemporalConstraintsDeserializer(TimeCouplingConstraints.class));
        module.addDeserializer(GeneratorConstraints.class, new GeneratorConstraintsDeserializer(GeneratorConstraints.class));
        objectMapper.registerModule(module);
        try {
            return objectMapper.readValue(inputStream, TimeCouplingConstraints.class);
        } catch (JsonMappingException e) {
            throw new OpenRaoException(extractDeserializationErrorMessage(e.getMessage()));
        }
    }

    /**
     * Remove the suffixes in automatic deserialization error messages.
     */
    private static String extractDeserializationErrorMessage(String originalErrorMessage) {
        String nestedErrorToken = " (through reference chain";
        if (originalErrorMessage.contains(nestedErrorToken)) {
            return originalErrorMessage.substring(0, originalErrorMessage.indexOf(nestedErrorToken));
        }
        return originalErrorMessage;
    }

}
