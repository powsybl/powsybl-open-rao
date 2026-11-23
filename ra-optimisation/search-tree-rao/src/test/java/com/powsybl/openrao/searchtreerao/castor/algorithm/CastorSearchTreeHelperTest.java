/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoPreSolveParameters.SolutionUsage;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.commons.parameters.NetworkActionParameters;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Maxence Fleury {@literal <maxence.fleury at artelys.com>}
 */
class CastorSearchTreeHelperTest {

    private SearchTreeParameters searchTreeParameters;
    private NetworkActionParameters networkActionParameters;
    private NetworkAction networkAction1;
    private NetworkAction networkAction2;

    @BeforeEach
    void setUp() {
        networkActionParameters = new NetworkActionParameters(new ArrayList<>(), 0., 0., false, 0, false);
        searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        when(searchTreeParameters.getNetworkActionParameters()).thenReturn(networkActionParameters);
        networkAction1 = Mockito.mock(NetworkAction.class);
        networkAction2 = Mockito.mock(NetworkAction.class);
    }

    @Test
    void emptyCombinationIsNoOp() {
        CastorSearchTreeHelper.injectPreSolveCombination(searchTreeParameters, Set.of(), SolutionUsage.HINT);

        assertTrue(networkActionParameters.getNetworkActionCombinations().isEmpty());
        assertTrue(networkActionParameters.getForcedCombinations().isEmpty());
    }

    @Test
    void hintAddsPredefinedCombination() {
        Set<NetworkAction> combination = Set.of(networkAction1, networkAction2);

        CastorSearchTreeHelper.injectPreSolveCombination(searchTreeParameters, combination, SolutionUsage.HINT);

        assertEquals(1, networkActionParameters.getNetworkActionCombinations().size());
        NetworkActionCombination addedCombination = networkActionParameters.getNetworkActionCombinations().getFirst();
        assertEquals(combination, addedCombination.getNetworkActionSet());
        assertTrue(addedCombination.isDetectedDuringRao());
        assertTrue(networkActionParameters.getForcedCombinations().isEmpty());
    }

    @Test
    void forcedSetsForcedCombination() {
        Set<NetworkAction> combination = Set.of(networkAction1, networkAction2);

        CastorSearchTreeHelper.injectPreSolveCombination(searchTreeParameters, combination, SolutionUsage.FORCED);

        assertTrue(networkActionParameters.getNetworkActionCombinations().isEmpty());
        assertEquals(1, networkActionParameters.getForcedCombinations().size());
        assertEquals(combination, networkActionParameters.getForcedCombinations().getFirst().getNetworkActionSet());
        assertTrue(networkActionParameters.getForcedCombinations().getFirst().isDetectedDuringRao());
    }
}
