/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.openrao.raoapi.parameters.extensions.PtdfApproximation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class PtdfApproximationTest {
    @Test
    void testUpdatePtdfWithTopo() {
        assertFalse(PtdfApproximation.FIXED_PTDF.shouldUpdatePtdfWithTopologicalChange());
        assertTrue(PtdfApproximation.UPDATE_PTDF_WITH_TOPO.shouldUpdatePtdfWithTopologicalChange());
        assertTrue(PtdfApproximation.UPDATE_PTDF_WITH_TOPO_AND_PST.shouldUpdatePtdfWithTopologicalChange());
    }

    @Test
    void testUpdatePtdfWithPst() {
        assertFalse(PtdfApproximation.FIXED_PTDF.shouldUpdatePtdfWithPstChange());
        assertFalse(PtdfApproximation.UPDATE_PTDF_WITH_TOPO.shouldUpdatePtdfWithPstChange());
        assertTrue(PtdfApproximation.UPDATE_PTDF_WITH_TOPO_AND_PST.shouldUpdatePtdfWithPstChange());
    }
}
