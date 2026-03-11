/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs.xml;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class VirtualHubsConfigurationImporterTest {
    @Test
    void checkThatConfigurationImportWithNullInputStreamThrows() {
        final VirtualHubsConfigurationImporter importer = new VirtualHubsConfigurationImporter();
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> importer.importConfiguration(null))
            .withMessage("Cannot import configuration from null input stream");
    }
}
