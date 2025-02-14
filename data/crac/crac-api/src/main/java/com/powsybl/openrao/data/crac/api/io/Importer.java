/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.io;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;

import java.io.InputStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface Importer {

    /**
     * Get a unique identifier of the format.
     */
    String getFormat();

    boolean exists(String filename, InputStream inputStream);

    /**
     * Create a model.
     *
     * @param inputStream data input stream
     * @param cracCreationParameters extra CRAC creation parameters
     * @param network     network upon which the CRAC is based
     * @return the model
     */
    CracCreationContext importData(InputStream inputStream, CracCreationParameters cracCreationParameters, Network network);
}
