/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.virtual_hubs.json;

import com.farao_community.farao.virtual_hubs.VirtualHubsConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Objects;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public final class JsonVirtualHubsConfiguration {
    private JsonVirtualHubsConfiguration() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static VirtualHubsConfiguration importConfiguration(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "Virtual hubs configuration import on null input stream is invalid");
        try {
            ObjectMapper objectMapper = preparedObjectMapper();
            return objectMapper.readValue(inputStream, VirtualHubsConfiguration.class);
        } catch (IOException e) {
            throw new VirtualHubsConfigurationDeserializationException(e);
        }
    }

    public static void exportConfiguration(OutputStream outputStream, VirtualHubsConfiguration configuration) {
        Objects.requireNonNull(outputStream, "Virtual hubs configuration export on null output stream is invalid");
        Objects.requireNonNull(configuration, "Virtual hubs configuration export on null configuration is invalid");
        try {
            ObjectMapper objectMapper = preparedObjectMapper();
            objectMapper.writeValue(outputStream, configuration);
        } catch (IOException e) {
            throw new VirtualHubsConfigurationSerializationException(e);
        }
    }

    public static void exportConfiguration(Writer writer, VirtualHubsConfiguration configuration) {
        Objects.requireNonNull(writer, "Virtual hubs configuration export on null writer is invalid");
        Objects.requireNonNull(configuration, "Virtual hubs configuration export on null configuration is invalid");
        try {
            ObjectMapper objectMapper = preparedObjectMapper();
            objectMapper.writeValue(writer, configuration);
        } catch (IOException e) {
            throw new VirtualHubsConfigurationSerializationException(e);
        }
    }

    private static ObjectMapper preparedObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new VirtualHubsConfigurationJsonModule());
        return objectMapper;
    }
}
