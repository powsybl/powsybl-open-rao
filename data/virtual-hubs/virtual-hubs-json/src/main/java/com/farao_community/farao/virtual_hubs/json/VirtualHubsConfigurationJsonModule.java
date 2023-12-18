/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.virtual_hubs.json;

import com.farao_community.farao.virtual_hubs.VirtualHubsConfiguration;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
class VirtualHubsConfigurationJsonModule extends SimpleModule {
    public VirtualHubsConfigurationJsonModule() {
        super();
        addSerializer(VirtualHubsConfiguration.class, new VirtualHubsConfigurationSerializer());
        addDeserializer(VirtualHubsConfiguration.class, new VirtualHubsConfigurationDeserializer());
    }
}
