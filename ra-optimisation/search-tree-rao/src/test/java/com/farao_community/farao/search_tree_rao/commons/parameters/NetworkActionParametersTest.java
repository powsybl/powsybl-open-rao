/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_impl.utils.ExhaustiveCracCreation;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.farao_community.farao.search_tree_rao.commons.NetworkActionCombination;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NetworkActionParametersTest {

    private Crac crac;

    @Before
    public void setUp() {
        crac = ExhaustiveCracCreation.create();
    }

    @Test
    public void buildFromRaoParametersTestOk() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.addExtension(SearchTreeRaoParameters.class, new SearchTreeRaoParameters());

        raoParameters.getExtension(SearchTreeRaoParameters.class).setNetworkActionIdCombinations(Collections.singletonList(List.of("complexNetworkActionId", "switchPairRaId")));
        raoParameters.getExtension(SearchTreeRaoParameters.class).setAbsoluteNetworkActionMinimumImpactThreshold(20.);
        raoParameters.getExtension(SearchTreeRaoParameters.class).setRelativeNetworkActionMinimumImpactThreshold(0.01);
        raoParameters.getExtension(SearchTreeRaoParameters.class).setSkipNetworkActionsFarFromMostLimitingElement(true);
        raoParameters.getExtension(SearchTreeRaoParameters.class).setMaxNumberOfBoundariesForSkippingNetworkActions(4);

        NetworkActionParameters nap = NetworkActionParameters.buildFromRaoParameters(raoParameters, crac);

        assertEquals(1, nap.getNetworkActionCombinations().size());
        assertEquals(2, nap.getNetworkActionCombinations().get(0).getNetworkActionSet().size());
        assertTrue(nap.getNetworkActionCombinations().get(0).getNetworkActionSet().contains(crac.getNetworkAction("complexNetworkActionId")));
        assertTrue(nap.getNetworkActionCombinations().get(0).getNetworkActionSet().contains(crac.getNetworkAction("switchPairRaId")));

        assertEquals(20., nap.getAbsoluteNetworkActionMinimumImpactThreshold(), 1e-6);
        assertEquals(0.01, nap.getRelativeNetworkActionMinimumImpactThreshold(), 1e-6);
        assertTrue(nap.skipNetworkActionFarFromMostLimitingElements());
        assertEquals(4, nap.getMaxNumberOfBoundariesForSkippingNetworkActions());

        Set<NetworkAction> naSet = Set.of(Mockito.mock(NetworkAction.class), Mockito.mock(NetworkAction.class));
        NetworkActionCombination naCombination = new NetworkActionCombination(naSet);
        nap.addNetworkActionCombination(naCombination);
        assertEquals(2, nap.getNetworkActionCombinations().size());
        assertTrue(nap.getNetworkActionCombinations().contains(naCombination));

        // Test unicity
        NetworkActionCombination naCombinationDetectedInRao = new NetworkActionCombination(naSet, true);
        nap.addNetworkActionCombination(naCombinationDetectedInRao);
        assertEquals(2, nap.getNetworkActionCombinations().size());
        assertTrue(nap.getNetworkActionCombinations().contains(naCombinationDetectedInRao));
        assertFalse(nap.getNetworkActionCombinations().contains(naCombination));
    }

    @Test (expected = FaraoException.class)
    public void buildFromRaoParametersWithMissingSearchTreeRaoParametersTest() {
        RaoParameters raoParameters = new RaoParameters();
        NetworkActionParameters.buildFromRaoParameters(raoParameters, crac);
    }
}
