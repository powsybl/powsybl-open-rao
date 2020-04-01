/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.converter;

import com.farao_community.farao.rao_api.RaoResult;
import com.powsybl.commons.PowsyblException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * A utility class to work with remedial actions optimisation result exporters
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class RaoResultExporters {

    private RaoResultExporters() {
    }

    /**
     * Get all supported formats.
     *
     * @return the supported formats
     */
    public static Collection<String> getFormats() {
        List<String> formats = new ArrayList<>();
        for (RaoResultExporter e : ServiceLoader.load(RaoResultExporter.class)) {
            formats.add(e.getFormat());
        }
        return formats;
    }

    /**
     * Get the exporter for the specified format
     *
     * @param format The export format
     *
     * @return The exporter for the specified format or null if this format is not supported
     */
    public static RaoResultExporter getExporter(String format) {
        Objects.requireNonNull(format);
        for (RaoResultExporter e : ServiceLoader.load(RaoResultExporter.class)) {
            if (format.equals(e.getFormat())) {
                return e;
            }
        }
        return null;
    }

    public static void export(RaoResult result, Path path, String format) {
        Objects.requireNonNull(path);

        try (OutputStream os = Files.newOutputStream(path)) {
            export(result, os, format);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void export(RaoResult result, OutputStream os, String format) {
        RaoResultExporter exporter = getExporter(format);
        if (exporter == null) {
            throw new PowsyblException("Unsupported format: " + format + " [" + getFormats() + "]");
        }

        exporter.export(result, os);
    }
}
