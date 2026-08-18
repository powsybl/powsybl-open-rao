/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class MetadataTest {

    private Metadata metadata;

    @BeforeEach
    void setUp() {
        metadata = new Metadata();
    }

    @Test
    void testConstructor() {
        assertEquals(ComputationStatus.DEFAULT, metadata.getComputationStatus());
        assertEquals("Not provided.", metadata.getExecutionDetails());
    }

    @Test
    void testGetName() {
        assertEquals("metadata", metadata.getName());
    }

    @Test
    void testSetAndGetComputationStatus() {
        metadata.setComputationStatus(ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, metadata.getComputationStatus());

        metadata.setComputationStatus(ComputationStatus.PARTIAL_FAILURE);
        assertEquals(ComputationStatus.PARTIAL_FAILURE, metadata.getComputationStatus());

        metadata.setComputationStatus(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, metadata.getComputationStatus());
    }

    @Test
    void testSetAndGetComputationStatusPerState() {
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);

        // Default value for unknown state
        assertEquals(ComputationStatus.DEFAULT, metadata.getComputationStatus(state1));

        metadata.setComputationStatusPerState(state1, ComputationStatus.FAILURE);
        metadata.setComputationStatusPerState(state2, ComputationStatus.PARTIAL_FAILURE);

        assertEquals(ComputationStatus.FAILURE, metadata.getComputationStatus(state1));
        assertEquals(ComputationStatus.PARTIAL_FAILURE, metadata.getComputationStatus(state2));

        // Unknown state still returns DEFAULT
        State state3 = Mockito.mock(State.class);
        assertEquals(ComputationStatus.DEFAULT, metadata.getComputationStatus(state3));
    }

    @Test
    void testSetAndGetExecutionDetails() {
        String details = "Step 1: Success\nStep 2: Warning";
        metadata.setExecutionDetails(details);
        assertEquals(details, metadata.getExecutionDetails());

        metadata.setExecutionDetails(null);
        assertNull(metadata.getExecutionDetails());
    }
}
