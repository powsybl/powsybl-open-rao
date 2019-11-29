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

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CracImporters {

    private static final Supplier<List<CracImporter>> CRAC_IMPORTERS
        = Suppliers.memoize(() -> new ServiceLoaderCache<>(CracImporter.class).getServices())::get;

    private CracImporters() {
    }

    public static Crac importCrac(Path cracPath) {
        try (InputStream is = new FileInputStream(cracPath.toFile())) {
            return importCrac(is);
        } catch (FileNotFoundException e) {
            throw new FaraoException("File not found.");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Crac importCrac(InputStream inputStream) {
        CracImporter importer = findImporter(inputStream);
        if (importer == null) {
            throw new FaraoException("No importer found for this file");
        }
        return importer.importCrac(inputStream);
    }

    public static CracImporter findImporter(InputStream inputStream) {
        for (CracImporter importer : CRAC_IMPORTERS.get()) {
            if (importer.exists(inputStream)) {
                return importer;
            }
        }
        return null;
    }
}
