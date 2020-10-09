/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.json;

import com.farao_community.farao.flowbased_computation.FlowbasedComputationResult;
import com.farao_community.farao.flowbased_computation.FlowbasedComputationResultImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.powsybl.commons.json.JsonUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class JsonFlowbasedComputationResult {

    private JsonFlowbasedComputationResult() {
        throw new IllegalStateException("Utility class");
    }

    public static void write(FlowbasedComputationResult result, OutputStream os) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(os, result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void write(FlowbasedComputationResult result, Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (OutputStream os = Files.newOutputStream(jsonFile)) {
            ObjectMapper objectMapper = createObjectMapper();
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(os, result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static FlowbasedComputationResult read(InputStream is) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            return objectMapper.readValue(is, FlowbasedComputationResultImpl.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static FlowbasedComputationResult read(Path jsonFile) {
        try (InputStream is = Files.newInputStream(jsonFile)) {
            return read(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ObjectMapper createObjectMapper() {
        return JsonUtil.createObjectMapper();
    }
}
