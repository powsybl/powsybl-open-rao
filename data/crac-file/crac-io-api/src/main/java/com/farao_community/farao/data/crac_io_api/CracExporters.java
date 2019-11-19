/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.function.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.commons.util.ServiceLoaderCache;

import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CracExporters {

    private static final Supplier<List<CracExporter>> NAMING_STRATEGY_SUPPLIERS
        = Suppliers.memoize(() -> new ServiceLoaderCache<>(CracExporter.class).getServices())::get;

    private CracExporters() {

    }

    public static void exportCrac(Crac crac, String format, Path cracPath) {
        try {
            OutputStream os = new FileOutputStream(new File(cracPath.toUri()));
            exportCrac(crac, format, os);
        } catch (FileNotFoundException e) {
            throw new FaraoException("File not found.");
        }
    }

    public static void exportCrac(Crac crac, String format, OutputStream outputStream) {
        CracExporter availableExporter = findNamingStrategy(format, NAMING_STRATEGY_SUPPLIERS.get());
        availableExporter.exportCrac(crac, outputStream);
    }

    static CracExporter findNamingStrategy(String name, List<CracExporter> namingStrategies) {
        Objects.requireNonNull(namingStrategies);

        if (namingStrategies.size() == 1 && name == null) {
            // no information to select the implementation but only one naming strategy, so we can use it by default
            // (that is be the most common use case)
            return namingStrategies.get(0);
        } else {
            if (namingStrategies.size() > 1 && name == null) {
                // several naming strategies and no information to select which one to choose, we can only throw
                // an exception
                List<String> namingStrategyNames = namingStrategies.stream().map(CracExporter::getFormat).collect(Collectors.toList());
                throw new FaraoException("Several naming strategy implementations found (" + namingStrategyNames
                    + "), you must add properties to select the implementation");
            }
            return namingStrategies.stream()
                .filter(ns -> ns.getFormat().equals(name))
                .findFirst()
                .orElseThrow(() -> new FaraoException("NamingStrategy '" + name + "' not found"));
        }
    }
}
