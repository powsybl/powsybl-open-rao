/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.google.common.base.Suppliers;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

import java.io.*;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CracImporters {

    private static final Supplier<List<CracImporter>> CRAC_IMPORTERS
        = Suppliers.memoize(() -> new ServiceLoaderCache<>(CracImporter.class).getServices())::get;

    private CracImporters() {
    }

    public static void cracAliasesUtil(Crac crac, Network network) {
        // List (without duplicates) all the crac elements that need to be found in the network
        Set<String> elementIds = new HashSet<>();
        crac.getCnecs().forEach(cnec -> elementIds.add(cnec.getNetworkElement().getId()));
        crac.getContingencies().forEach(contingency -> contingency.getNetworkElements().forEach(networkElement -> elementIds.add(networkElement.getId())));

        // Try to find a corresponding element in the network, and add elementId as an alias
        elementIds.forEach(elementId -> {
            Optional<Identifiable<?>> correspondingElement = network.getIdentifiables().stream().filter(identifiable -> anyMatch(identifiable, elementId)).findAny();
            correspondingElement.ifPresent(identifiable -> identifiable.addAlias(elementId));
        });
    }

    private static boolean anyMatch(Identifiable<?> identifiable, String cnecId) {
        return nameMatches(identifiable, cnecId, false) ||
            aliasMatches(identifiable, cnecId, false) ||
            nameMatches(identifiable, cnecId, true) ||
            aliasMatches(identifiable, cnecId, true);
    }

    private static boolean nameMatches(Identifiable<?> identifiable, String cnecId, boolean reverse) {
        return identifiable.getId().trim().matches(checkWithPattern(cnecId, reverse));
    }

    private static boolean aliasMatches(Identifiable<?> identifiable, String cnecId, boolean reverse) {
        return identifiable.getAliases().stream().anyMatch(alias -> alias.trim().matches(checkWithPattern(cnecId, reverse)));
    }

    private static String checkWithPattern(String string, boolean reverse) {
        int first = reverse ? 9 : 0;
        int second = reverse ? 0 : 9;
        return Pattern.quote(string.substring(first, first + 7)) + ".*" + " " + Pattern.quote(string.substring(second, second + 7)) + ".*" + Pattern.quote(string.substring(17)).trim();
    }

    public static Crac importCrac(Path cracPath) {
        return importCrac(cracPath, null);
    }

    public static Crac importCrac(Path cracPath, OffsetDateTime timeStampFilter) {
        try (InputStream is = new FileInputStream(cracPath.toFile())) {
            return importCrac(cracPath.getFileName().toString(), is, timeStampFilter);
        } catch (FileNotFoundException e) {
            throw new FaraoException("File not found.");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        org.apache.commons.io.IOUtils.copy(inputStream, baos);
        return baos.toByteArray();
    }

    public static Crac importCrac(String fileName, InputStream inputStream) {
        return importCrac(fileName, inputStream, null);
    }

    public static Crac importCrac(String fileName, InputStream inputStream, OffsetDateTime timeStampFilter) {
        try {
            byte[] bytes = getBytesFromInputStream(inputStream);

            CracImporter importer = findImporter(fileName, new ByteArrayInputStream(bytes));
            if (importer == null) {
                throw new FaraoException("No importer found for this file");
            }
            return importer.importCrac(new ByteArrayInputStream(bytes), timeStampFilter);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static CracImporter findImporter(String fileName, InputStream inputStream) {
        try {
            byte[] bytes = getBytesFromInputStream(inputStream);

            for (CracImporter importer : CRAC_IMPORTERS.get()) {
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                if (importer.exists(fileName, bais)) {
                    return importer;
                }
            }
            return null;

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
