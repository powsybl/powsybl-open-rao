/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api;

import com.powsybl.openrao.data.raoresult.api.extension.Metadata;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MetadataTest {
    @Test
    public void testMetadataExtension() {
        RaoResult raoResult = new MockRaoResult();
        Metadata metadata = new Metadata();
        raoResult.addExtension(Metadata.class, metadata);

        assertNotNull(raoResult.getExtension(Metadata.class));
        assertTrue(raoResult.getExtension(Metadata.class).getComputationStart().isEmpty());
        assertTrue(raoResult.getExtension(Metadata.class).getComputationEnd().isEmpty());
        assertTrue(raoResult.getExtension(Metadata.class).getComputationDuration().isEmpty());

        metadata.setComputationStart(OffsetDateTime.of(2026, 3, 2, 10, 19, 0, 0, ZoneOffset.UTC));
        metadata.setComputationEnd(OffsetDateTime.of(2026, 3, 2, 10, 20, 30, 0, ZoneOffset.UTC));

        assertTrue(raoResult.getExtension(Metadata.class).getComputationStart().isPresent());
        assertTrue(raoResult.getExtension(Metadata.class).getComputationEnd().isPresent());
        assertTrue(raoResult.getExtension(Metadata.class).getComputationDuration().isPresent());

        assertEquals(OffsetDateTime.of(2026, 3, 2, 10, 19, 0, 0, ZoneOffset.UTC), raoResult.getExtension(Metadata.class).getComputationStart().get());
        assertEquals(OffsetDateTime.of(2026, 3, 2, 10, 20, 30, 0, ZoneOffset.UTC), raoResult.getExtension(Metadata.class).getComputationEnd().get());
        assertEquals(Duration.of(90, ChronoUnit.SECONDS), raoResult.getExtension(Metadata.class).getComputationDuration().get());
    }
}
