/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api;

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
        MockMetadata metadata = new MockMetadata();
        raoResult.addExtension(MockMetadata.class, metadata);

        assertNotNull(raoResult.getExtension(MockMetadata.class));
        assertTrue(raoResult.getExtension(MockMetadata.class).getComputationStart().isEmpty());
        assertTrue(raoResult.getExtension(MockMetadata.class).getComputationEnd().isEmpty());
        assertTrue(raoResult.getExtension(MockMetadata.class).getComputationDuration().isEmpty());
        assertEquals(ComputationStatus.DEFAULT, metadata.getComputationStatus());

        metadata.setComputationStart(OffsetDateTime.of(2026, 3, 2, 10, 19, 0, 0, ZoneOffset.UTC));
        metadata.setComputationEnd(OffsetDateTime.of(2026, 3, 2, 10, 20, 30, 0, ZoneOffset.UTC));
        metadata.setExecutionDetails(ComputationStatus.FAILURE);

        assertTrue(raoResult.getExtension(MockMetadata.class).getComputationStart().isPresent());
        assertTrue(raoResult.getExtension(MockMetadata.class).getComputationEnd().isPresent());
        assertTrue(raoResult.getExtension(MockMetadata.class).getComputationDuration().isPresent());

        assertEquals(OffsetDateTime.of(2026, 3, 2, 10, 19, 0, 0, ZoneOffset.UTC), raoResult.getExtension(MockMetadata.class).getComputationStart().get());
        assertEquals(OffsetDateTime.of(2026, 3, 2, 10, 20, 30, 0, ZoneOffset.UTC), raoResult.getExtension(MockMetadata.class).getComputationEnd().get());
        assertEquals(Duration.of(90, ChronoUnit.SECONDS), raoResult.getExtension(MockMetadata.class).getComputationDuration().get());
        assertEquals(ComputationStatus.FAILURE, raoResult.getExtension(MockMetadata.class).getComputationStatus());
    }
}
