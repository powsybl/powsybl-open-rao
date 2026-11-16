/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoTopoOptimizationParameters.DcSolutionUsage;
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
        CastorSearchTreeHelper.injectDcPreSolveCombination(searchTreeParameters, Set.of(), DcSolutionUsage.HINT);

        assertTrue(networkActionParameters.getNetworkActionCombinations().isEmpty());
        assertTrue(networkActionParameters.getForcedCombination().isEmpty());
    }

    @Test
    void hintAddsPredefinedCombination() {
        Set<NetworkAction> dcCombination = Set.of(networkAction1, networkAction2);

        CastorSearchTreeHelper.injectDcPreSolveCombination(searchTreeParameters, dcCombination, DcSolutionUsage.HINT);

        assertEquals(1, networkActionParameters.getNetworkActionCombinations().size());
        NetworkActionCombination addedCombination = networkActionParameters.getNetworkActionCombinations().get(0);
        assertEquals(dcCombination, addedCombination.getNetworkActionSet());
        assertTrue(addedCombination.isDetectedDuringRao());
        assertTrue(networkActionParameters.getForcedCombination().isEmpty());
    }

    @Test
    void forcedSetsForcedCombination() {
        Set<NetworkAction> dcCombination = Set.of(networkAction1, networkAction2);

        CastorSearchTreeHelper.injectDcPreSolveCombination(searchTreeParameters, dcCombination, DcSolutionUsage.FORCED);

        assertTrue(networkActionParameters.getNetworkActionCombinations().isEmpty());
        assertTrue(networkActionParameters.getForcedCombination().isPresent());
        assertEquals(dcCombination, networkActionParameters.getForcedCombination().get().getNetworkActionSet());
        assertTrue(networkActionParameters.getForcedCombination().get().isDetectedDuringRao());
    }
}
