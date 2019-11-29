/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

/**
 * Some methods in the Crac require other information contained in the network.
 * So these methods require that the Crac has been previously synchronized with the network
 * When one of these methods is called with no previous synchronization of the Crac
 * this exception will be raised. The final user could deal with this exception
 * differently considering the cases.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SynchronizationException extends Exception {

    public SynchronizationException(String message) {
        super(message);
    }
}
