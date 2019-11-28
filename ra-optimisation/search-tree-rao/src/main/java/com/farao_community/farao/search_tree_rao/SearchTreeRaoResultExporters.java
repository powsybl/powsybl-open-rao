/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.google.common.base.Suppliers;
import com.powsybl.commons.util.ServiceLoaderCache;

import java.io.OutputStream;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public final class SearchTreeRaoResultExporters {

    private static final Supplier<List<SearchTreeRaoResultExporter>> SEARCH_TREE_RAO_RESULTS_EXPORTERS
            = Suppliers.memoize(() -> new ServiceLoaderCache<>(SearchTreeRaoResultExporter.class).getServices())::get;

    private SearchTreeRaoResultExporters() {
    }

    public static void exportSearchTreeRaoResult(SearchTreeRaoResult result, String format, OutputStream outputStream) {
        SearchTreeRaoResultExporter exporter = findSearchTreeRaoResultExporter(format, SEARCH_TREE_RAO_RESULTS_EXPORTERS.get());
        exporter.export(result, outputStream);
    }

    static SearchTreeRaoResultExporter findSearchTreeRaoResultExporter(String name, List<SearchTreeRaoResultExporter> searchTreeRaoResultExporters) {
        Objects.requireNonNull(searchTreeRaoResultExporters);
        if (searchTreeRaoResultExporters.size() > 1 && name == null) {
            throw new FaraoException("Several Search Tree Rao result exporters implementations found (" + searchTreeRaoResultExporters.stream().map(SearchTreeRaoResultExporter::getFormat).collect(Collectors.toList()) + "), you must specify an explicit exporter name");
        }
        return searchTreeRaoResultExporters.stream()
                .filter(ns -> ns.getFormat().equals(name))
                .findFirst()
                .orElseThrow(() ->
                        new FaraoException("SearchTreeRaoResult export '" + name + "' not found")
                );
    }
}
