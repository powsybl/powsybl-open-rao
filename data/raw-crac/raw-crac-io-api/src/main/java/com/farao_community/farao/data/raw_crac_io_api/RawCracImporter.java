/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.raw_crac_io_api;

import com.farao_community.farao.data.raw_crac_api.RawCrac;

import java.io.InputStream;

/**
 * Common interface for importers of RawCrac objects.
 *
 * @see RawCracImporters
 * @see RawCrac
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface RawCracImporter<T extends RawCrac> {

    /**
     * Get a unique identifier of the format handled by the RawCracImporter.
     */
    String getFormat();

    /**
     * Import a RawCrac from an input stream.
     */
    T importRawCrac(InputStream inputStream);

    /**
     * Check if a file is importable.
     * @param fileName the file name
     * @param inputStream the input stream of the file
     * @return true if the inputStream is importable, false otherwise
     */
    boolean exists(String fileName, InputStream inputStream);
}
