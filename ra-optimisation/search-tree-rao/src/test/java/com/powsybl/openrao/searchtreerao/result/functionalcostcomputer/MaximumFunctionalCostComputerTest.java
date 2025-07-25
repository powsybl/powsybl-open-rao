/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.functionalcostcomputer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class MaximumFunctionalCostComputerTest extends FunctionalCostComputerTestUtils {
    private MaximumFunctionalCostComputer functionalCostComputer;

    @BeforeEach
    void setUp() {
        init();
        functionalCostComputer = new MaximumFunctionalCostComputer(initialResult, secondPreventivePerimeterResult, postContingencyResults);
    }

    @Test
    void testPreventiveTotalFunctionalCost() {
        assertEquals(100.0, functionalCostComputer.computeFunctionalCost(crac.getInstant("preventive")));
    }

    @Test
    void testOutageTotalFunctionalCost() {
        assertEquals(100.0, functionalCostComputer.computeFunctionalCost(crac.getInstant("outage")));
    }

    @Test
    void testAutoTotalFunctionalCost() {
        assertEquals(100.0, functionalCostComputer.computeFunctionalCost(crac.getInstant("auto")));
    }

    @Test
    void testCurativeTotalFunctionalCost() {
        assertEquals(250.0, functionalCostComputer.computeFunctionalCost(crac.getInstant("curative")));
    }
}
