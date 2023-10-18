/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.InstantImpl;
import com.farao_community.farao.data.crac_impl.utils.ExhaustiveCracCreation;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.NetworkActionCombination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class NetworkActionParametersTest {
    private static final Instant INSTANT_PREV = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);

    private Crac crac;

    @BeforeEach
    public void setUp() {
        crac = ExhaustiveCracCreation.create();
        crac.addInstant(INSTANT_PREV);
    }

    @Test
    void buildFromRaoParametersTestOk() {
        RaoParameters raoParameters = new RaoParameters();

        raoParameters.getTopoOptimizationParameters().setPredefinedCombinations(Collections.singletonList(List.of("complexNetworkActionId", "switchPairRaId")));
        raoParameters.getTopoOptimizationParameters().setAbsoluteMinImpactThreshold(20.);
        raoParameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(0.01);
        raoParameters.getTopoOptimizationParameters().setSkipActionsFarFromMostLimitingElement(true);
        raoParameters.getTopoOptimizationParameters().setMaxNumberOfBoundariesForSkippingActions(4);

        NetworkActionParameters nap = NetworkActionParameters.buildFromRaoParameters(raoParameters.getTopoOptimizationParameters(), crac);

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

    @Test
    void testNetworkActionCombinations() {

        Crac crac = CracFactory.findDefault().create("crac");

        crac.newNetworkAction()
            .withId("topological-action-1")
            .withOperator("operator-1")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("any-network-element").add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstantId(INSTANT_PREV.getId()).add()
            .add();

        crac.newNetworkAction()
            .withId("topological-action-2")
            .withOperator("operator-2")
            .newTopologicalAction().withActionType(ActionType.CLOSE).withNetworkElement("any-other-network-element").add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstantId(INSTANT_PREV.getId()).add()
            .add();

        crac.newNetworkAction()
            .withId("pst-setpoint")
            .withOperator("operator-2")
            .newPstSetPoint().withSetpoint(10).withNetworkElement("any-other-network-element").add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstantId(INSTANT_PREV.getId()).add()
            .add();

        // test list
        RaoParameters parameters = new RaoParameters();

        parameters.getTopoOptimizationParameters().setPredefinedCombinations(List.of(
            List.of("topological-action-1", "topological-action-2"), // OK
            List.of("topological-action-1", "topological-action-2", "pst-setpoint"), // OK
            List.of("topological-action-1", "unknown-na-id"), // should be filtered
            List.of("topological-action-1"), // should be filtered (one action only)
            new ArrayList<>())); // should be filtered

        List<NetworkActionCombination> naCombinations = NetworkActionParameters.computePredefinedCombinations(crac, parameters.getTopoOptimizationParameters());

        assertEquals(5, parameters.getTopoOptimizationParameters().getPredefinedCombinations().size());
        assertEquals(2, naCombinations.size());
        assertEquals(2, naCombinations.get(0).getNetworkActionSet().size());
        assertEquals(3, naCombinations.get(1).getNetworkActionSet().size());
    }
}
