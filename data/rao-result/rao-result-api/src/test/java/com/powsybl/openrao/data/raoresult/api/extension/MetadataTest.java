/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class MetadataTest {
    private Metadata metadata;

    @Test
    void testGetName() {
        assertEquals("metadata", Metadata.build().build().getName());
    }

    @Test
    void testGetComputationStatus() {
        // Case 1: Empty map -> DEFAULT
        metadata = Metadata.build().build();
        assertEquals(ComputationStatus.DEFAULT, metadata.getComputationStatus());

        State preventive = Mockito.mock(State.class);
        when(preventive.isPreventive()).thenReturn(true);
        State curative = Mockito.mock(State.class);
        when(curative.isPreventive()).thenReturn(false);

        // Case 2: Only DEFAULT statuses -> DEFAULT
        metadata = Metadata.build()
            .addComputationStatus(preventive, ComputationStatus.DEFAULT)
            .addComputationStatus(curative, ComputationStatus.DEFAULT)
            .build();
        assertEquals(ComputationStatus.DEFAULT, metadata.getComputationStatus());

        // Case 3: Only PARTIAL_FAILURE statuses -> DEFAULT
        // (The implementation currently only looks for ComputationStatus.FAILURE)
        metadata = Metadata.build()
            .addComputationStatus(preventive, ComputationStatus.PARTIAL_FAILURE)
            .addComputationStatus(curative, ComputationStatus.PARTIAL_FAILURE)
            .build();
        assertEquals(ComputationStatus.DEFAULT, metadata.getComputationStatus());

        // Case 4: One non-preventive FAILURE -> PARTIAL_FAILURE
        metadata = Metadata.build()
            .addComputationStatus(curative, ComputationStatus.FAILURE)
            .build();
        assertEquals(ComputationStatus.PARTIAL_FAILURE, metadata.getComputationStatus());

        // Case 5: One preventive FAILURE -> FAILURE
        metadata = Metadata.build()
            .addComputationStatus(preventive, ComputationStatus.FAILURE)
            .build();
        assertEquals(ComputationStatus.FAILURE, metadata.getComputationStatus());

        // Case 6: Mixed statuses (preventive FAILURE + others) -> FAILURE
        metadata = Metadata.build()
            .addComputationStatus(preventive, ComputationStatus.FAILURE)
            .addComputationStatus(curative, ComputationStatus.FAILURE)
            .build();
        assertEquals(ComputationStatus.FAILURE, metadata.getComputationStatus());

        // Case 7: Mixed statuses (non-preventive FAILURE + DEFAULT) -> PARTIAL_FAILURE
        metadata = Metadata.build()
            .addComputationStatus(preventive, ComputationStatus.DEFAULT)
            .addComputationStatus(curative, ComputationStatus.FAILURE)
            .build();
        assertEquals(ComputationStatus.PARTIAL_FAILURE, metadata.getComputationStatus());
    }

    @Test
    void testSetAndGetComputationStatusPerState() {
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);

        // Default value for unknown state
        assertEquals(ComputationStatus.DEFAULT, Metadata.build().build().getComputationStatus(state1));

        metadata = Metadata.build()
            .addComputationStatus(state1, ComputationStatus.FAILURE)
            .addComputationStatus(state2, ComputationStatus.PARTIAL_FAILURE)
            .build();

        assertEquals(ComputationStatus.FAILURE, metadata.getComputationStatus(state1));
        assertEquals(ComputationStatus.PARTIAL_FAILURE, metadata.getComputationStatus(state2));

        // Unknown state still returns DEFAULT
        State state3 = Mockito.mock(State.class);
        assertEquals(ComputationStatus.DEFAULT, metadata.getComputationStatus(state3));
    }

    @Test
    void testSetAndGetExecutionDetails() {
        metadata = Metadata.build().build();

        String details = "Step 1: Success\nStep 2: Warning";
        metadata.setExecutionDetails(details);
        assertEquals(Optional.of(details), metadata.getExecutionDetails());

        metadata.setExecutionDetails(null);
        assertTrue(metadata.getExecutionDetails().isEmpty());
    }
}
