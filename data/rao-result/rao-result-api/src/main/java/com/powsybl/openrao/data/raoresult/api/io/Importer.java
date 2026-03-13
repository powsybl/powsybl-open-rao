/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.io;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.io.utils.BufferSize;
import com.powsybl.openrao.data.crac.api.io.utils.SafeFileReader;
import com.powsybl.openrao.data.crac.api.io.utils.TmpFile;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface Importer {
    /**
     * Get a unique identifier of the format.
     */
    String getFormat();

    boolean exists(SafeFileReader inputFile);

    @Deprecated
    default boolean exists(String filename, InputStream inputStream) {
        try (var tmp = TmpFile.create(filename, inputStream, BufferSize.MEDIUM)) {
            return exists(SafeFileReader.create(tmp.getTempFile().toFile(), BufferSize.MEDIUM));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Create a RaoResult.
     *
     * @param inputFile RaoResult data
     * @param crac        the crac on which the RaoResult data is based
     * @return the model
     */
    RaoResult importData(SafeFileReader inputFile, Crac crac);

    @Deprecated
    default RaoResult importData(InputStream inputStream, Crac crac) {
        try (var tmp = TmpFile.create("importData", inputStream, BufferSize.MEDIUM)) {
            return importData(SafeFileReader.create(tmp.getTempFile().toFile(), BufferSize.MEDIUM), crac);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
