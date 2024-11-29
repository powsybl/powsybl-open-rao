/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.parameters;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracimpl.utils.ExhaustiveCracCreation;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class NetworkActionParametersTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";

    private Crac crac;

    @Test
    void buildFromRaoParametersTestOk() {
        crac = ExhaustiveCracCreation.create();
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        OpenRaoSearchTreeParameters searchTreeParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class);

        searchTreeParameters.getTopoOptimizationParameters().setPredefinedCombinations(Collections.singletonList(List.of("complexNetworkActionId", "switchPairRaId")));
        raoParameters.getTopoOptimizationParameters().setAbsoluteMinImpactThreshold(20.);
        raoParameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(0.01);
        searchTreeParameters.getTopoOptimizationParameters().setSkipActionsFarFromMostLimitingElement(true);
        searchTreeParameters.getTopoOptimizationParameters().setMaxNumberOfBoundariesForSkippingActions(4);

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

    @Test
    void testNetworkActionCombinations() {

        crac = CracFactory.findDefault().create("crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);

        crac.newNetworkAction()
                .withId("topological-action-1")
                .withOperator("operator-1")
                .newSwitchAction().withActionType(ActionType.OPEN).withNetworkElement("any-network-element").add()
                .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT_ID).add()
                .add();

        crac.newNetworkAction()
                .withId("topological-action-2")
                .withOperator("operator-2")
                .newTerminalsConnectionAction().withActionType(ActionType.CLOSE).withNetworkElement("any-other-network-element").add()
                .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT_ID).add()
                .add();

        crac.newNetworkAction()
                .withId("pst-setpoint")
                .withOperator("operator-2")
                .newPhaseTapChangerTapPositionAction().withTapPosition(10).withNetworkElement("any-other-network-element").add()
                .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT_ID).add()
                .add();

        // test list
        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        OpenRaoSearchTreeParameters searchTreeParameters = parameters.getExtension(OpenRaoSearchTreeParameters.class);

        searchTreeParameters.getTopoOptimizationParameters().setPredefinedCombinations(List.of(
                List.of("topological-action-1", "topological-action-2"), // OK
                List.of("topological-action-1", "topological-action-2", "pst-setpoint"), // OK
                List.of("topological-action-1", "unknown-na-id"), // should be filtered
                List.of("topological-action-1"), // should be filtered (one action only)
                new ArrayList<>())); // should be filtered

        List<NetworkActionCombination> naCombinations = NetworkActionParameters.computePredefinedCombinations(crac, parameters);

        assertEquals(5, searchTreeParameters.getTopoOptimizationParameters().getPredefinedCombinations().size());
        assertEquals(2, naCombinations.size());
        assertEquals(2, naCombinations.get(0).getNetworkActionSet().size());
        assertEquals(3, naCombinations.get(1).getNetworkActionSet().size());
    }
}
