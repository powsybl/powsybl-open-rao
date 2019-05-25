/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.flowbased_computation.json.JsonFlowBasedComputationResult;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A utility class to work with FlowBased optimisation result exporters
 *
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public final class FlowBasedComputationResultExporters {

    private FlowBasedComputationResultExporters() {
    }

    public static void export(FlowBasedComputationResult result, Path path) {
        Objects.requireNonNull(path);

        try (OutputStream os = Files.newOutputStream(path)) {
            export(result, os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void export(FlowBasedComputationResult result, OutputStream os) {
        JsonFlowBasedComputationResult.write(result, os);
    }
}
