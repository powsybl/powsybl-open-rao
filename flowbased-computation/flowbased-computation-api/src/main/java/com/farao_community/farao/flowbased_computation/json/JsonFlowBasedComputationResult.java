/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.json;

import com.farao_community.farao.flowbased_computation.FlowBasedComputationResult;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationResultImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

public final class JsonFlowBasedComputationResult {

    private JsonFlowBasedComputationResult() {
        throw new IllegalStateException("Utility class");
    }

    public static void write(FlowBasedComputationResultImpl result, OutputStream os) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(os, result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static FlowBasedComputationResult read(InputStream is) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            return objectMapper.readValue(is, FlowBasedComputationResultImpl.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ObjectMapper createObjectMapper() {
        return JsonUtil.createObjectMapper();
    }
}
