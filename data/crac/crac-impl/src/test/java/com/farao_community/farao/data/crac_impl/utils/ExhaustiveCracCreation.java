/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.utils;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;

import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class ExhaustiveCracCreation {

    /*
    Small CRAC used in I/O unit tests of farao-core

    The idea of this CRAC is to be quite exhaustive regarding the diversity of the CRAC objects.
    It contains numerous variations of the CRAC objects, to ensure that they are all tested in
    the manipulations of the CRAC.
     */

    private ExhaustiveCracCreation() {
    }

    public static Crac create() {
        return create(CracFactory.findDefault());
    }

    public static Crac create(CracFactory cracFactory) {

        Crac crac = cracFactory.create("exhaustiveCracId", "exhaustiveCracName");

        String contingency1Id = "contingency1Id";
        crac.newContingency().withId(contingency1Id).withNetworkElement("ne1Id").add();

        String contingency2Id = "contingency2Id";
        crac.newContingency().withId(contingency2Id).withNetworkElement("ne2Id", "ne2Name").withNetworkElement("ne3Id").add();

        crac.newFlowCnec().withId("cnec1prevId")
                .withNetworkElement("ne4Id")
                .withInstant(Instant.PREVENTIVE)
                .withOperator("operator1")
                .withOptimized()
                .newThreshold().withRule(BranchThresholdRule.ON_RIGHT_SIDE).withUnit(Unit.AMPERE).withMin(-500.).add()
                .withIMax(1000., Side.RIGHT)
                .withNominalVoltage(220.)
                .add();

        crac.newFlowCnec().withId("cnec1outageId")
                .withNetworkElement("ne4Id")
                .withInstant(Instant.OUTAGE)
                .withContingency(contingency1Id)
                .withOperator("operator1")
                .withOptimized()
                .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMin(-800.).add()
                .withNominalVoltage(220.)
                .add();

        crac.newFlowCnec().withId("cnec2prevId")
                .withNetworkElement("ne5Id", "ne5Name")
                .withInstant(Instant.PREVENTIVE)
                .withOperator("operator2")
                .withOptimized()
                .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.PERCENT_IMAX).withMin(-0.3).add()
                .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMin(-800.).add()
                .newThreshold().withRule(BranchThresholdRule.ON_HIGH_VOLTAGE_LEVEL).withUnit(Unit.AMPERE).withMin(-800.).add()
                .newThreshold().withRule(BranchThresholdRule.ON_LOW_VOLTAGE_LEVEL).withUnit(Unit.AMPERE).withMax(1200.).add()
                .withNominalVoltage(220., Side.RIGHT)
                .withNominalVoltage(380., Side.LEFT)
                .withIMax(2000.)
                .add();

        crac.newFlowCnec().withId("cnec3prevId")
                .withName("cnec3prevName")
                .withNetworkElement("ne2Id", "ne2Name")
                .withInstant(Instant.PREVENTIVE)
                .withOperator("operator3")
                .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
                .withReliabilityMargin(20.)
                .withMonitored()
                .add();

        crac.newFlowCnec().withId("cnec3autoId")
                .withName("cnec3curBis")
                .withNetworkElement("ne2Id", "ne2Name")
                .withInstant(Instant.AUTO)
                .withContingency(contingency2Id)
                .withOperator("operator3")
                .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
                .withReliabilityMargin(20.)
                .withMonitored()
                .add();

        crac.newFlowCnec().withId("cnec3curId")
                .withName("cnec3curBis")
                .withNetworkElement("ne2Id", "ne2Name")
                .withInstant(Instant.CURATIVE)
                .withContingency(contingency2Id)
                .withOperator("operator3")
                .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
                .withReliabilityMargin(20.)
                .withMonitored()
                .add();

        crac.newFlowCnec().withId("cnec4prevId")
                .withName("cnec4prevName")
                .withNetworkElement("ne3Id")
                .withInstant(Instant.PREVENTIVE)
                .withOperator("operator4")
                .newThreshold().withUnit(Unit.MEGAWATT).withMax(500.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
                .withReliabilityMargin(0.)
                .withOptimized()
                .withMonitored()
                .add();

        // network action with one pst set point
        crac.newNetworkAction().withId("pstSetpointRaId")
                .withName("pstSetpointRaName")
                .withOperator("RTE")
                .newPstSetPoint().withSetpoint(15).withNetworkElement("pst").add()
                .newFreeToUseUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
                .newOnStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency(contingency1Id).withInstant(Instant.CURATIVE).add()
                .add();

        // complex network action with one pst set point and one topology
        crac.newNetworkAction().withId("complexNetworkActionId")
                .withName("complexNetworkActionName")
                .withOperator("RTE")
                .newPstSetPoint().withSetpoint(5).withNetworkElement("pst").add()
                .newTopologicalAction().withActionType(ActionType.CLOSE).withNetworkElement("ne1Id").add()
                .newFreeToUseUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
                .add();

        // network action with one injection set point
        crac.newNetworkAction().withId("injectionSetpointRaId")
                .withName("injectionSetpointRaName")
                .withOperator("RTE")
                .newInjectionSetPoint().withSetpoint(260).withNetworkElement("injection").add()
                .newOnFlowConstraintUsageRule().withFlowCnec("cnec3autoId").withInstant(Instant.AUTO).add()
                .add();

        // network action with one switch pair
        crac.newNetworkAction().withId("switchPairRaId")
                .withName("switchPairRaName")
                .withOperator("RTE")
                .newSwitchPair().withSwitchToOpen("to-open").withSwitchToClose("to-close", "to-close-name").add()
                .newOnStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency(contingency2Id).withInstant(Instant.CURATIVE).add()
                .add();

        // range actions
        crac.newPstRangeAction().withId("pstRange1Id")
                .withName("pstRange1Name")
                .withOperator("RTE")
                .withNetworkElement("pst")
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
                .newOnFlowConstraintUsageRule().withInstant(Instant.PREVENTIVE).withFlowCnec("cnec3prevId").add()
                .add();

        crac.newHvdcRangeAction().withId("hvdcRange1Id")
                .withName("hvdcRange1Name")
                .withOperator("RTE")
                .withNetworkElement("hvdc")
                .newHvdcRange().withMin(-1000).withMax(1000).add()
                .newFreeToUseUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
                .add();

        crac.newHvdcRangeAction().withId("hvdcRange2Id")
                .withName("hvdcRange2Name")
                .withOperator("RTE")
                .withNetworkElement("hvdc2")
                .withGroupId("group-1-hvdc")
                .newHvdcRange().withMin(-1000).withMax(1000).add()
                .newOnFlowConstraintUsageRule().withInstant(Instant.PREVENTIVE).withFlowCnec("cnec3curId").add()
                .add();

        return crac;
    }
}
