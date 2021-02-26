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
 * Interface for RawCrac object importer
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface RawCracImporter {

    RawCrac importRawCrac(InputStream inputStream);

    boolean exists(String fileName, InputStream inputStream);
}
