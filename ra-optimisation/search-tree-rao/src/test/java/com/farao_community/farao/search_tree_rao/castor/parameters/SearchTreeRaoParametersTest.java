/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.castor.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.NetworkActionCombination;
import com.powsybl.commons.config.PlatformConfig;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

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

        assertNotNull(parameters.getUnoptimizedCnecsInSeriesWithPstsIds());
        assertTrue(parameters.getUnoptimizedCnecsInSeriesWithPstsIds().isEmpty());

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

        parameters.setUnoptimizedCnecsInSeriesWithPstsIds(Map.of("cnec1", "pst1"));
        parameters.setUnoptimizedCnecsInSeriesWithPstsIds(null);
        assertNotNull(parameters.getUnoptimizedCnecsInSeriesWithPstsIds());
        assertTrue(parameters.getUnoptimizedCnecsInSeriesWithPstsIds().isEmpty());
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
    public void testUnoptimizedCnecsInSeriesWithPsts() {

        Crac crac = CracFactory.findDefault().create("crac");

        crac.newFlowCnec().withId("flowCnec-1")
                .withNetworkElement("ne1Id")
                .withInstant(Instant.PREVENTIVE)
                .withOperator("operator1")
                .withOptimized()
                .newThreshold().withRule(BranchThresholdRule.ON_RIGHT_SIDE).withUnit(Unit.AMPERE).withMin(-500.).add()
                .withIMax(1000., Side.RIGHT)
                .withNominalVoltage(220.)
                .add();

        crac.newContingency().withId("co2").withNetworkElement("ne22").add();

        crac.newFlowCnec().withId("flowCnec-2")
                .withNetworkElement("ne2Id")
                .withInstant(Instant.CURATIVE)
                .withContingency("co2")
                .withOperator("operator1")
                .withOptimized()
                .newThreshold().withRule(BranchThresholdRule.ON_RIGHT_SIDE).withUnit(Unit.AMPERE).withMin(-500.).add()
                .withIMax(1000., Side.RIGHT)
                .withNominalVoltage(220.)
                .add();

        crac.newPstRangeAction().withId("pstRange1Id")
                .withName("pstRange1Name")
                .withOperator("RTE")
                .withNetworkElement("pst1")
                .withInitialTap(2)
                .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
                .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(1).withMaxTap(7).add()
                .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
                .newFreeToUseUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
                .add();

        crac.newPstRangeAction().withId("pstRange2Id")
                .withName("pstRange2Name")
                .withOperator("RTE")
                .withNetworkElement("pst2")
                .withGroupId("group-1-pst")
                .withInitialTap(1)
                .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
                .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(1).withMaxTap(7).add()
                .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
                .add();

        SearchTreeRaoParameters parameters = new SearchTreeRaoParameters();
        parameters.setUnoptimizedCnecsInSeriesWithPstsIds(Map.of("ne1Id", "pst1",
                "ne2Id", "fakeId",
                "fakeId", "pst2"));
        Map<FlowCnec, PstRangeAction> map = parameters.getUnoptimizedCnecsInSeriesWithPsts(crac);
        assertEquals(3, parameters.getUnoptimizedCnecsInSeriesWithPstsIds().size());
        assertEquals(1, map.size());
        assertTrue(map.containsKey(crac.getFlowCnec("flowCnec-1")));
        assertEquals(crac.getPstRangeAction("pstRange1Id"), map.get(crac.getFlowCnec("flowCnec-1")));
        assertFalse(map.containsKey(crac.getFlowCnec("flowCnec-2")));

        // Add pst with same networkElement to crac
        crac.newPstRangeAction().withId("pstRange3Id")
                .withName("pstRange3Name")
                .withOperator("RTE")
                .withNetworkElement("pst2")
                .withGroupId("group-1-pst")
                .withInitialTap(1)
                .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
                .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(1).withMaxTap(7).add()
                .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
                .add();
        SearchTreeRaoParameters newParameters = new SearchTreeRaoParameters();
        newParameters.setUnoptimizedCnecsInSeriesWithPstsIds(Map.of("ne1Id", "pst2"));
        Map<FlowCnec, PstRangeAction> newMap = newParameters.getUnoptimizedCnecsInSeriesWithPsts(crac);
        assertEquals(0, newMap.size());
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

    @Test (expected = FaraoException.class)
    public void testIncompatibleParameters1() {
        SearchTreeRaoParameters parameters = new SearchTreeRaoParameters();
        parameters.setUnoptimizedCnecsInSeriesWithPstsIds(Map.of("cnec1", "pst1"));
        parameters.setCurativeRaoOptimizeOperatorsNotSharingCras(true);
    }

    @Test (expected = FaraoException.class)
    public void testIncompatibleParameters2() {
        SearchTreeRaoParameters parameters = new SearchTreeRaoParameters();
        parameters.setCurativeRaoOptimizeOperatorsNotSharingCras(false);
        parameters.setUnoptimizedCnecsInSeriesWithPstsIds(Map.of("cnec1", "pst1"));
    }

    @Test
    public void testIncompatibleParameters3() {
        SearchTreeRaoParameters parameters = new SearchTreeRaoParameters();
        parameters.setCurativeRaoOptimizeOperatorsNotSharingCras(true);
        parameters.setUnoptimizedCnecsInSeriesWithPstsIds(Map.of("cnec1", "pst1"));
        assertEquals(Map.of("cnec1", "pst1"), parameters.getUnoptimizedCnecsInSeriesWithPstsIds());
    }

    @Test
    public void testIncompatibleParameters4() {
        SearchTreeRaoParameters parameters = new SearchTreeRaoParameters();
        parameters.setUnoptimizedCnecsInSeriesWithPstsIds(null);
        parameters.setCurativeRaoOptimizeOperatorsNotSharingCras(false);
        assertEquals(Collections.emptyMap(), parameters.getUnoptimizedCnecsInSeriesWithPstsIds());

    }
}
