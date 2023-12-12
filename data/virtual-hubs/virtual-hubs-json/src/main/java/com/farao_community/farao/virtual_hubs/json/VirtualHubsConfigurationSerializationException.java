/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.virtual_hubs.json;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
class VirtualHubsConfigurationSerializationException extends RuntimeException {
    public VirtualHubsConfigurationSerializationException(String message) {
        super(message);
    }

    public VirtualHubsConfigurationSerializationException(Throwable cause) {
        super(cause);
    }
}
