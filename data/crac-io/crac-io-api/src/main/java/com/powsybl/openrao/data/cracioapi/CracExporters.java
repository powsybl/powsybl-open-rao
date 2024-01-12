/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracioapi;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;

import java.io.*;
import java.nio.file.Path;
import java.util.function.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Objects;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CracExporters {

    private static final Supplier<List<CracExporter>> CRAC_EXPORTERS
        = Suppliers.memoize(() -> new ServiceLoaderCache<>(CracExporter.class).getServices())::get;

    private CracExporters() {
    }

    public static void exportCrac(Crac crac, String format, Path cracPath) {
        try (OutputStream os = new FileOutputStream(cracPath.toFile())) {
            exportCrac(crac, format, os);
        } catch (FileNotFoundException e) {
            throw new OpenRaoException("File not found.");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void exportCrac(Crac crac, String format, OutputStream outputStream) {
        CracExporter exporter = findCracExporter(format, CRAC_EXPORTERS.get());
        exporter.exportCrac(crac, outputStream);
    }

    public static void exportCrac(Crac crac, Network network, String format, OutputStream outputStream) {
        CracExporter exporter = findCracExporter(format, CRAC_EXPORTERS.get());
        exporter.exportCrac(crac, network, outputStream);
    }

    static CracExporter findCracExporter(String name, List<CracExporter> cracExporters) {
        Objects.requireNonNull(cracExporters);

        if (cracExporters.size() == 1 && name == null) {
            // no information to select the implementation but only one crac exporter, so we can use it by default
            // (that is be the most common use case)
            return cracExporters.get(0);
        } else {
            if (cracExporters.size() > 1 && name == null) {
                // several crac exporters and no information to select which one to choose, we can only throw
                // an exception
                List<String> exportersNames = cracExporters.stream().map(CracExporter::getFormat).toList();
                throw new OpenRaoException("Several crac exporters implementations found (" + exportersNames
                    + "), you must specify an explicit exporter name");
            }
            return cracExporters.stream()
                .filter(ns -> ns.getFormat().equals(name))
                .findFirst()
                .orElseThrow(() -> new OpenRaoException("Crac exporter '" + name + "' not found"));
        }
    }
}
