/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.virtualhubs;

import com.powsybl.openrao.virtualhubs.json.JsonVirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.xml.XmlVirtualHubsConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class VirtualHubsConfigurationTest {
    @Test
    void roundtripTest() {
        final InputStream xmlConfigInputStream = getClass().getResourceAsStream("/virtualhubsconfig.xml");
        final VirtualHubsConfiguration configuration = XmlVirtualHubsConfiguration.importConfiguration(xmlConfigInputStream);

        final ByteArrayOutputStream serializedConfigOutputStream = new ByteArrayOutputStream();
        JsonVirtualHubsConfiguration.exportConfiguration(serializedConfigOutputStream, configuration);
        System.out.println(serializedConfigOutputStream.toString());
        final VirtualHubsConfiguration deserializedConfiguration = JsonVirtualHubsConfiguration.importConfiguration(new ByteArrayInputStream(serializedConfigOutputStream.toString().getBytes()));

        Assertions.assertThat(deserializedConfiguration)
            .usingRecursiveComparison()
            .isEqualTo(configuration);
    }
}
