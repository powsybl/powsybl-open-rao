/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.ConstantCostEvaluatorResult;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class ConstantCostEvaluatorResultTest {
    @Test
    void testEvaluator() {
        ConstantCostEvaluatorResult evaluatorResult = new ConstantCostEvaluatorResult(100.0);
        assertEquals(100.0, evaluatorResult.getCost(Set.of()));
        assertTrue(evaluatorResult.getCostlyElements(Set.of()).isEmpty());
    }
}
