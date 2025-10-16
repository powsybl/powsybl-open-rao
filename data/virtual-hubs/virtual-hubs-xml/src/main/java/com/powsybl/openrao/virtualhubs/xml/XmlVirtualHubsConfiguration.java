/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs.xml;

import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;

import java.io.InputStream;
import java.util.Objects;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class XmlVirtualHubsConfiguration {
    private XmlVirtualHubsConfiguration() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static VirtualHubsConfiguration importConfiguration(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "Virtual hubs configuration import on null input stream is invalid");
        VirtualHubsConfigurationImporter importer = new VirtualHubsConfigurationImporter();
        return importer.importConfiguration(inputStream);
    }
}
