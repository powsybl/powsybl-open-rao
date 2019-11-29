/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.flow_decomposition_results.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.powsybl.commons.json.JsonUtil;
import com.farao_community.farao.data.flow_decomposition_results.FlowDecompositionResults;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

/**
 * Utility class for flow decomposition result JSON IO
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class JsonFlowDecompositionResults {

    private JsonFlowDecompositionResults() {
    }

    public static void write(FlowDecompositionResults result, OutputStream os) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(os, result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static FlowDecompositionResults read(InputStream is) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            return objectMapper.readValue(is, FlowDecompositionResults.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ObjectMapper createObjectMapper() {
        return JsonUtil.createObjectMapper()
                .registerModule(new GuavaModule())
                .registerModule(new FlowDecompositionResultsJsonModule());
    }
}
