/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.io;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.io.utils.SafeFileReader;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface Importer {
    /**
     * Get a unique identifier of the format.
     */
    String getFormat();

    // @Deprecated
    //TODO Lui deprecated

    boolean exists(SafeFileReader inputFile);

    /**
     * Create a RaoResult.
     *
     * @param inputStream RaoResult data
     * @param crac        the crac on which the RaoResult data is based
     * @return the model
     */
    // @Deprecated
    //TODO Lui  deprecated

    RaoResult importData(SafeFileReader inputFile, Crac crac);
}
