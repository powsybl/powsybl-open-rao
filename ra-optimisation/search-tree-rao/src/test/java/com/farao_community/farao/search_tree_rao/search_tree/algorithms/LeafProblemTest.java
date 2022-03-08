/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.search_tree.algorithms;

import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_api.parameters.MaxMinMarginParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.search_tree.parameters.TreeParameters;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class LeafProblemTest {

    private NetworkAction mockNetworkAction(String operator) {
        NetworkAction na = mock(NetworkAction.class);
        when(na.getOperator()).thenReturn(operator);
        return na;
    }

    @Test
    public void testRaUsageLimitsFiller() {
        TreeParameters treeParameters = mock(TreeParameters.class);
        when(treeParameters.getMaxRa()).thenReturn(9);
        when(treeParameters.getMaxTso()).thenReturn(4);
        when(treeParameters.getMaxPstPerTso()).thenReturn(Map.of("opA", 2, "opB", 3));
        when(treeParameters.getMaxRaPerTso()).thenReturn(Map.of("opA", 6, "opB", 5));

        Set<NetworkAction> activatedNetworkActions = Set.of(
            mockNetworkAction("opA"),
            mockNetworkAction("opA"),
            mockNetworkAction("opB"),
            mockNetworkAction("opC")
        );

        LinearOptimizerParameters linearOptimizerParameters = mock(LinearOptimizerParameters.class);
        when(linearOptimizerParameters.getObjectiveFunction()).thenReturn(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT);
        when(linearOptimizerParameters.getMaxMinMarginParameters()).thenReturn(mock(MaxMinMarginParameters.class));
        when(linearOptimizerParameters.getPstOptimizationApproximation()).thenReturn(RaoParameters.PstOptimizationApproximation.CONTINUOUS);
        when(linearOptimizerParameters.getSolver()).thenReturn(RaoParameters.Solver.CBC);

        LeafProblem leafProblem =
            new LeafProblem(mock(FlowResult.class),
                mock(FlowResult.class),
                mock(RangeActionActivationResult.class),
                Set.of(),
                Set.of(),
                linearOptimizerParameters,
                treeParameters,
                Set.of(),
                activatedNetworkActions);

        // Initial maxRA = 9, 4 network actions used, so 5 range actions can still be used
        assertEquals(5, leafProblem.getMaxRa().intValue());

        // Initial maxTso = 4. opA, opB and opC already have activated RAs. So they should be counted out:
        // maxTso for TSOs other than these three = 1
        assertEquals(1, leafProblem.getMaxTso().intValue());
        assertEquals(Set.of("opA", "opB", "opC"), leafProblem.getMaxTsoExclusions());

        // Max PST per TSO is not impacted by activated network actions
        assertEquals(Map.of("opA", 2, "opB", 3), leafProblem.getMaxPstPerTso());

        // Max RA per TSO should be impacted by the number of activated network actions
        assertEquals(Map.of("opA", 4, "opB", 4), leafProblem.getMaxRaPerTso());
    }
}
