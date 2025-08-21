/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.virtualhubs.json;

import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class VirtualHubsConfigurationJsonModule extends SimpleModule {
    public VirtualHubsConfigurationJsonModule() {
        super();
        addSerializer(VirtualHubsConfiguration.class, new VirtualHubsConfigurationSerializer());
        addDeserializer(VirtualHubsConfiguration.class, new VirtualHubsConfigurationDeserializer());
    }
}
