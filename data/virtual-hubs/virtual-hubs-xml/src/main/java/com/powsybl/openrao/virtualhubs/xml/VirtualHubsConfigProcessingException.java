/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs.xml;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class VirtualHubsConfigProcessingException extends RuntimeException {
    VirtualHubsConfigProcessingException(Throwable cause) {
        super(cause);
    }
}
