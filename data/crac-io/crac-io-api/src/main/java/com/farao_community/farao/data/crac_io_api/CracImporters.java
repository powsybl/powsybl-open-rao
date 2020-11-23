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

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CracImporters {

    private static final Supplier<List<CracImporter>> CRAC_IMPORTERS
        = Suppliers.memoize(() -> new ServiceLoaderCache<>(CracImporter.class).getServices())::get;

    private CracImporters() {
    }

    public static void cracAliasesUtil(Crac crac, Network network) {
        Set<String> cnecIds = new HashSet<>();
        crac.getCnecs().forEach(cnec -> cnecIds.add(cnec.getNetworkElement().getId()));

        cnecIds.forEach(cnecId -> {
            Optional<Identifiable<?>> correspondingElement = network.getIdentifiables().stream().filter(identifiable -> matchesOne(identifiable, cnecId)).findAny();
            correspondingElement.ifPresent(identifiable -> identifiable.addAlias(cnecId));
        });
    }

    private static boolean matchesOne(Identifiable<?> identifiable, String cnecId) {
        if (identifiable.getId().matches(changeCharacters(cnecId))) {
            return true;
        }
        return identifiable.getAliases().stream().anyMatch(alias -> alias.matches(changeCharacters(cnecId)));
    }

    private static String changeCharacters(String string) {
        return string.substring(0, 7) + ".*" + string.substring(8, 16) + ".*" + string.substring(17);
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
