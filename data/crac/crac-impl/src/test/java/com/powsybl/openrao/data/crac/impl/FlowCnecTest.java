/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.powsybl.openrao.commons.Unit.MEGAWATT;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class FlowCnecTest {

    @Test
    void testGetMonitoredSides() {
        // Ensures getMonitoredSides() returns sides in natural enum order (ONE, TWO),
        // regardless of the addition order of thresholds, due to internal TreeSet usage.
        FlowCnec cnec = new FlowCnecMock(
            Set.of(
                new BranchThresholdImpl(TwoSides.TWO, MEGAWATT, -1000d, 1000d),
                new BranchThresholdImpl(TwoSides.ONE, MEGAWATT, -1000d, 1000d)
            ));

        assertIterableEquals(java.util.List.of(TwoSides.ONE, TwoSides.TWO), cnec.getMonitoredSides());
    }
}