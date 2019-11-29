/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.flow_decomposition_results.json;

import com.powsybl.commons.AbstractConverterTest;
import com.farao_community.farao.data.flow_decomposition_results.FlowDecompositionResults;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class JsonFlowDecompositionResultsTest extends AbstractConverterTest {

    private static FlowDecompositionResults create() {
        return JsonFlowDecompositionResults.read(JsonFlowDecompositionResults.class.getResourceAsStream("/flowDecompositionResults.json"));
    }

    private static FlowDecompositionResults read(Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (InputStream is = Files.newInputStream(jsonFile)) {
            return JsonFlowDecompositionResults.read(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(FlowDecompositionResults results, Path jsonFile) {
        Objects.requireNonNull(results);
        Objects.requireNonNull(jsonFile);

        try (OutputStream os = Files.newOutputStream(jsonFile)) {
            JsonFlowDecompositionResults.write(results, os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    public void roundTripTest() throws IOException {
        roundTripTest(create(), JsonFlowDecompositionResultsTest::write, JsonFlowDecompositionResultsTest::read, "/flowDecompositionResults.json");
    }
}
