/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {literal <peter.mitri at rte-france.com>}
 */
class RaoResultJsonImporterTest {

    private static final RaoResultJsonImporter IMPORTER = new RaoResultJsonImporter();

    private static InputStream resourceStream(String name) {
        InputStream stream = RaoResultJsonImporterTest.class.getResourceAsStream(name);
        if (stream == null) {
            throw new AssertionError("Test resource not found: " + name);
        }
        return stream;
    }

    @Test
    void existsWithValidRaoResultHeader() {
        // only the document header (type + version) is checked
        assertTrue(IMPORTER.exists(resourceStream("/rao-result.json")));
    }

    @Test
    void existsWithWrongDocumentType() {
        String json = """
            {
              "type" : "NOT_A_RAO_RESULT",
              "version" : "2.0"
            }
            """;
        assertFalse(IMPORTER.exists(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void existsWithNotJsonObject() {
        assertFalse(IMPORTER.exists(new ByteArrayInputStream("[1, 2, 3]".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void existsWithEmptyJsonObject() {
        assertFalse(IMPORTER.exists(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void existsWithNotJson() {
        assertFalse(IMPORTER.exists(new ByteArrayInputStream("this is not json".getBytes(StandardCharsets.UTF_8))));
    }
}
