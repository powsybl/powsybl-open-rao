/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.powsybl.iidm.network.Network;

/**
 * This interface enables to enhance the Crac objects with network information
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface Synchronizable {

    /**
     * Enhance the Crac object with network information.
     * Some Crac methods will become available thanks to this action.
     *
     * @param network: PowSyBl network associated to the Crac object
     */
    void synchronize(Network network);

    /**
     * Erase the additional data created by synchronize method.
     * The aim is to be able to get back to the original Crac object,
     * mainly for export purpose.
     */
    void desynchronize();

    /**
     * Notify the user if the Synchronizable object has already been synchronized or not.
     * @return True if its already synchronized.
     */
    boolean isSynchronized();
}
