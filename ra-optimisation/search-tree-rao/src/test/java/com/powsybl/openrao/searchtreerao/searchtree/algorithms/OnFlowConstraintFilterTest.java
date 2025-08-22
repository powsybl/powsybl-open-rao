/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_BE_NL;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_3_FR;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_3_FR_NL_BE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.CRAC;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.NETWORK;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.P_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class OnFlowConstraintFilterTest {
    @Test
    void testNetworkActionWithUnsatisfiedOnFlowConstraintFilteredOut() {
        FlowCnec flowCnec = CRAC.getFlowCnec("cnecBe");
        OnFlowConstraintFilter filter = new OnFlowConstraintFilter(P_STATE, Set.of(flowCnec), NETWORK, Unit.MEGAWATT);
        Set<NetworkActionCombination> naCombinations = Set.of(COMB_3_FR, COMB_2_BE_NL, COMB_3_FR_NL_BE);
        OptimizationResult fromLeaf = Mockito.mock(OptimizationResult.class);
        Mockito.when(fromLeaf.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(200.0);
        Set<NetworkActionCombination> filteredNaCombinations = filter.filter(naCombinations, fromLeaf);
        assertEquals(Set.of(COMB_3_FR, COMB_2_BE_NL), filteredNaCombinations);
    }
}
