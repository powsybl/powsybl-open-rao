/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.CracImpl;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class UnoptimizedCnecParametersTest {

    private Crac crac;

    @BeforeEach
    public void setUp() {
        crac = new CracImpl("test-crac");
    }

    @Test
    void buildWithoutOptimizingOperatorsNotSharingCras() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCurativeCnecsForTsosWithoutCras(true);

        UnoptimizedCnecParameters ocp = UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), Set.of("BE"), crac);

        assertNotNull(ocp);
        assertEquals(Set.of("BE"), ocp.getOperatorsNotToOptimize());
    }

    @Test
    void buildWhileOptimizingOperatorsNotSharingCras() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCurativeCnecsForTsosWithoutCras(false);

        UnoptimizedCnecParameters ocp = UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), Set.of("BE"), crac);
        assertNull(ocp);
    }

    @Test
    void testUnoptimizedCnecsInSeriesWithPsts() {

        Crac crac = CracFactory.findDefault().create("crac");

        crac.newFlowCnec().withId("flowCnec-1")
                .withNetworkElement("ne1Id")
                .withInstant(Instant.PREVENTIVE)
                .withOperator("operator1")
                .withOptimized()
                .newThreshold().withSide(Side.RIGHT).withUnit(Unit.AMPERE).withMin(-500.).add()
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
                .newThreshold().withSide(Side.RIGHT).withUnit(Unit.AMPERE).withMin(-500.).add()
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
                .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
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

        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCnecsSecuredByTheirPst(Map.of("ne1Id", "pst1",
                "ne2Id", "fakeId",
                "fakeId", "pst2"));
        UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), Set.of("BE"), crac);

        Map<FlowCnec, PstRangeAction> map = UnoptimizedCnecParameters.getUnoptimizedCnecsInSeriesWithPsts(raoParameters.getNotOptimizedCnecsParameters(), crac);
        assertEquals(3,  raoParameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCnecsSecuredByTheirPst().size());
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
        RaoParameters newRaoParameters = new RaoParameters();
        newRaoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCnecsSecuredByTheirPst(Map.of("ne1Id", "pst2"));
        Map<FlowCnec, PstRangeAction> newMap = UnoptimizedCnecParameters.getUnoptimizedCnecsInSeriesWithPsts(newRaoParameters.getNotOptimizedCnecsParameters(), crac);
        assertEquals(0, newMap.size());
    }

}
