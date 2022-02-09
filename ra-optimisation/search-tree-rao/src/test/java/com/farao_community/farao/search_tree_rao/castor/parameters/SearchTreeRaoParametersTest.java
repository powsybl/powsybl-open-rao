/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.castor.parameters;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.NetworkActionCombination;
import com.powsybl.commons.config.PlatformConfig;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SearchTreeRaoParametersTest {

    @Test
    public void testExtensionRecognition() {
        PlatformConfig config = Mockito.mock(PlatformConfig.class);
        RaoParameters parameters = RaoParameters.load(config);
        assertTrue(parameters.getExtensionByName("SearchTreeRaoParameters") instanceof SearchTreeRaoParameters);
        assertNotNull(parameters.getExtension(SearchTreeRaoParameters.class));
    }

    @Test
    public void testRelativeNetworkActionMinimumImpactThresholdBounds() {
        SearchTreeRaoParameters params = new SearchTreeRaoParameters();
        params.setRelativeNetworkActionMinimumImpactThreshold(-0.5);
        assertEquals(0, params.getRelativeNetworkActionMinimumImpactThreshold(), 1e-6);
        params.setRelativeNetworkActionMinimumImpactThreshold(1.1);
        assertEquals(1, params.getRelativeNetworkActionMinimumImpactThreshold(), 1e-6);
    }

    @Test
    public void testMaxNumberOfBoundariesForSkippingNetworkActionsBounds() {
        SearchTreeRaoParameters params = new SearchTreeRaoParameters();
        params.setMaxNumberOfBoundariesForSkippingNetworkActions(300);
        assertEquals(300, params.getMaxNumberOfBoundariesForSkippingNetworkActions());
        params.setMaxNumberOfBoundariesForSkippingNetworkActions(-2);
        assertEquals(0, params.getMaxNumberOfBoundariesForSkippingNetworkActions());
    }

    @Test
    public void testNegativeCurativeRaoMinObjImprovement() {
        SearchTreeRaoParameters parameters = new SearchTreeRaoParameters();
        parameters.setCurativeRaoMinObjImprovement(100);
        assertEquals(100, parameters.getCurativeRaoMinObjImprovement(), 1e-6);
        parameters.setCurativeRaoMinObjImprovement(-100);
        assertEquals(100, parameters.getCurativeRaoMinObjImprovement(), 1e-6);
    }

    @Test
    public void testNonNullMaps() {
        SearchTreeRaoParameters parameters = new SearchTreeRaoParameters();

        // default
        assertNotNull(parameters.getMaxCurativeRaPerTso());
        assertTrue(parameters.getMaxCurativeRaPerTso().isEmpty());

        assertNotNull(parameters.getMaxCurativePstPerTso());
        assertTrue(parameters.getMaxCurativePstPerTso().isEmpty());

        assertNotNull(parameters.getMaxCurativeTopoPerTso());
        assertTrue(parameters.getMaxCurativeTopoPerTso().isEmpty());

        // using setters
        parameters.setMaxCurativeRaPerTso(Map.of("fr", 2));
        parameters.setMaxCurativeRaPerTso(null);
        assertNotNull(parameters.getMaxCurativeRaPerTso());
        assertTrue(parameters.getMaxCurativeRaPerTso().isEmpty());

        parameters.setMaxCurativePstPerTso(Map.of("fr", 2));
        parameters.setMaxCurativePstPerTso(null);
        assertNotNull(parameters.getMaxCurativePstPerTso());
        assertTrue(parameters.getMaxCurativePstPerTso().isEmpty());

        parameters.setMaxCurativeTopoPerTso(Map.of("fr", 2));
        parameters.setMaxCurativeTopoPerTso(null);
        assertNotNull(parameters.getMaxCurativeTopoPerTso());
        assertTrue(parameters.getMaxCurativeTopoPerTso().isEmpty());
    }

    @Test
    public void testNetworkActionCombinations() {

        Crac crac = CracFactory.findDefault().create("crac");

        crac.newNetworkAction()
            .withId("topological-action-1")
            .withOperator("operator-1")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("any-network-element").add()
            .newFreeToUseUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
            .add();

        crac.newNetworkAction()
            .withId("topological-action-2")
            .withOperator("operator-2")
            .newTopologicalAction().withActionType(ActionType.CLOSE).withNetworkElement("any-other-network-element").add()
            .newFreeToUseUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
            .add();

        crac.newNetworkAction()
            .withId("pst-setpoint")
            .withOperator("operator-2")
            .newPstSetPoint().withSetpoint(10).withNetworkElement("any-other-network-element").add()
            .newFreeToUseUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
            .add();

        // test list
        SearchTreeRaoParameters parameters = new SearchTreeRaoParameters();
        parameters.setNetworkActionIdCombinations(List.of(
            List.of("topological-action-1", "topological-action-2"), // OK
            List.of("topological-action-1", "topological-action-2", "pst-setpoint"), // OK
            List.of("topological-action-1", "unknown-na-id"), // should be filtered
            List.of("topological-action-1"), // should be filtered (one action only)
            new ArrayList<>())); // should be filtered

        List<NetworkActionCombination> naCombinations = parameters.getNetworkActionCombinations(crac);

        assertEquals(5, parameters.getNetworkActionIdCombinations().size());
        assertEquals(2, naCombinations.size());
        assertEquals(2, naCombinations.get(0).getNetworkActionSet().size());
        assertEquals(3, naCombinations.get(1).getNetworkActionSet().size());
    }

    @Test
    public void testIllegalValues() {
        SearchTreeRaoParameters parameters = new SearchTreeRaoParameters();

        parameters.setMaxCurativeRa(2);
        parameters.setMaxCurativeRa(-2);
        assertEquals(0, parameters.getMaxCurativeRa());

        parameters.setMaxCurativeTso(2);
        parameters.setMaxCurativeTso(-2);
        assertEquals(0, parameters.getMaxCurativeTso());
    }
}
