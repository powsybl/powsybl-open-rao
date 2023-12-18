/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.virtual_hubs.json;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
class VirtualHubsConfigurationDeserializationException extends RuntimeException {
    public VirtualHubsConfigurationDeserializationException(String message) {
        super(message);
    }

    public VirtualHubsConfigurationDeserializationException(Throwable cause) {
        super(cause);
    }
}
