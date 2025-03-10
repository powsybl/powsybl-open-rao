/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class GeneratorConstraintsFillerTest {
    /*
    TODO:
    CHECK DEFINED VARIABLES AND CONSTRAINTS
    - use all 4 combinations of LEAD/LAG <> timestampDuration
    - check constant timestamp duration
    - test with and without each optional attribute

    TEST BEHAVIOR:
    - gradients
    - min/max up time
    - min off time
    - ramping time (up or down)
     */

    // linear problem content

    @Test
    void testWithBasicConstraints() {
        // TODO
        assertTrue(false);
    }

    // linear problem solutions
}
