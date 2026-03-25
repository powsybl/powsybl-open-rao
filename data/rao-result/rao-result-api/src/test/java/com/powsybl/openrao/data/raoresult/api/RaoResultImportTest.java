/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class RaoResultImportTest {

    @Test
    void testImportFromInputStream() throws IOException {
        assertTrue(
            RaoResult.read(getResourceAsFile("raoResult.txt"), null) instanceof MockRaoResult);
    }

    public File getResourceAsFile(String file) {
        try {
            return Path.of(getClass().getClassLoader().getResource(file).toURI()).toFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error loading resource: " + file, e);
        }
    }

}

