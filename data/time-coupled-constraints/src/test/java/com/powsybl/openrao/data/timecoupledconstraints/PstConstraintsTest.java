/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.timecoupledconstraints;

import com.powsybl.openrao.commons.OpenRaoException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Atena Amnache {@literal <atena.amnache at rte-france.com>}
 */
class PstConstraintsTest {

    @Test
    void testBuildPstConstraints() {
        PstConstraints pstConstraints = PstConstraints.create()
            .withPstId("pst")
            .withUpwardTapGradient(2)
            .withDownwardTapGradient(-1)
            .build();
        assertEquals("pst", pstConstraints.getPstId());
        assertEquals(Optional.of(2), pstConstraints.getUpwardTapGradient());
        assertEquals(Optional.of(-1), pstConstraints.getDownwardTapGradient());
    }

    @Test
    void testBuildPstConstraintsWithOnlyUpwardTapGradient() {
        PstConstraints pstConstraints = PstConstraints.create()
            .withPstId("pst")
            .withUpwardTapGradient(2)
            .build();
        assertEquals("pst", pstConstraints.getPstId());
        assertEquals(Optional.of(2), pstConstraints.getUpwardTapGradient());
        assertTrue(pstConstraints.getDownwardTapGradient().isEmpty());
    }

    @Test
    void testBuildPstConstraintsWithOnlyDownwardTapGradient() {
        PstConstraints pstConstraints = PstConstraints.create()
            .withPstId("pst")
            .withDownwardTapGradient(-2)
            .build();
        assertEquals("pst", pstConstraints.getPstId());
        assertEquals(Optional.of(-2), pstConstraints.getDownwardTapGradient());
        assertTrue(pstConstraints.getUpwardTapGradient().isEmpty());
    }

    @Test
    void testBuildWithMissingId() {
        OpenRaoException exception = assertThrows(OpenRaoException.class,
            () -> PstConstraints.create().withUpwardTapGradient(1).withDownwardTapGradient(-1).build()
        );
        assertEquals("The id of the pst is mandatory.", exception.getMessage());
    }

    @Test
    void testNegativeUpwardGradient() {
        OpenRaoException exception = assertThrows(OpenRaoException.class,
            () -> PstConstraints.create().withPstId("pst").withUpwardTapGradient(-1).withDownwardTapGradient(-3).build()
        );
        assertEquals("The upward tap gradient of the pst must be positive.", exception.getMessage());
    }

    @Test
    void testPositiveDownwardGradient() {
        OpenRaoException exception = assertThrows(OpenRaoException.class,
            () -> PstConstraints.create().withPstId("pst").withUpwardTapGradient(1).withDownwardTapGradient(2).build()
        );
        assertEquals("The downward tap gradient of the pst must be negative.", exception.getMessage());
    }
}
