/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.CracImpl;
import com.farao_community.farao.data.crac_impl.InjectionSetpointImpl;
import com.farao_community.farao.data.crac_impl.OnFlowConstraintImpl;
import com.farao_community.farao.data.crac_impl.PstSetpointImpl;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracImportExportTest {

    @Test
    public void cracTest() {
        CracImpl crac = new CracImpl("cracId");

        String contingency1Id = "contingency1Id";
        crac.newContingency().withId(contingency1Id).withNetworkElement("ne1Id").add();

        String contingency2Id = "contingency2Id";
        crac.newContingency().withId(contingency2Id).withNetworkElement("ne2Id", "ne2Name").withNetworkElement("ne3Id").add();

        crac.newFlowCnec().withId("cnec1prev")
                .withNetworkElement("ne4Id")
                .withInstant(Instant.PREVENTIVE)
                .withOperator("operator1")
                .withOptimized()
                .newThreshold().withRule(BranchThresholdRule.ON_RIGHT_SIDE).withUnit(Unit.AMPERE).withMin(-500.).add()
                .withIMax(1000., Side.RIGHT)
                .withNominalVoltage(220.)
            .add();

        crac.newFlowCnec().withId("cnec2prev")
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

        crac.newFlowCnec().withId("cnec1cur")
                .withNetworkElement("ne4Id")
                .withInstant(Instant.OUTAGE)
                .withContingency(contingency1Id)
                .withOperator("operator1")
                .withOptimized()
                .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withUnit(Unit.AMPERE).withMin(-800.).add()
                .withNominalVoltage(220.)
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
                .newFreeToUseUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
                .newOnStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency(contingency1Id).withInstant(Instant.CURATIVE).add()
                .add();

        // range actions
        crac.newPstRangeAction().withId("pstRangeId")
                .withName("pstRangeName")
                .withOperator("RTE")
                .withNetworkElement("pst")
                .newFreeToUseUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
                .withInitialTap(2)
                .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
                .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(1).withMaxTap(7).add()
                .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
                .add();

        crac.newPstRangeAction().withId("pstRangeId2")
                .withName("pstRangeName2")
                .withOperator("RTE")
                .withNetworkElement("pst2")
                .withGroupId("group-1")
                .withInitialTap(1)
                .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
                .newOnFlowConstraintUsageRule().withInstant(Instant.PREVENTIVE).withFlowCnec("cnec3prevId").add()
                .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(1).withMaxTap(7).add()
                .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
                .add();

        Crac importedCrac = RoundTripUtil.roundTrip(crac);

        assertEquals(3, importedCrac.getStates().size());
        assertEquals(2, importedCrac.getContingencies().size());
        assertEquals(5, importedCrac.getFlowCnecs().size());
        assertEquals(2, importedCrac.getRangeActions().size());
        assertEquals(3, importedCrac.getNetworkActions().size());
        assertEquals(4, importedCrac.getFlowCnec("cnec2prev").getThresholds().size());
        assertFalse(importedCrac.getFlowCnec("cnec3prevId").isOptimized());
        assertTrue(importedCrac.getFlowCnec("cnec4prevId").isMonitored());
        assertEquals(1, importedCrac.getNetworkAction("pstSetpointRaId").getElementaryActions().size());
        assertTrue(importedCrac.getNetworkAction("pstSetpointRaId").getElementaryActions().iterator().next() instanceof PstSetpointImpl);
        assertEquals(1, importedCrac.getNetworkAction("injectionSetpointRaId").getElementaryActions().size());
        assertTrue(importedCrac.getNetworkAction("injectionSetpointRaId").getElementaryActions().iterator().next() instanceof InjectionSetpointImpl);
        assertEquals(2, importedCrac.getNetworkAction("complexNetworkActionId").getElementaryActions().size());
        assertEquals("group-1", importedCrac.getRangeAction("pstRangeId2").getGroupId().orElseThrow());
        assertTrue(importedCrac.getRangeAction("pstRangeId").getGroupId().isEmpty());

        assertEquals("operator1", importedCrac.getFlowCnec("cnec1prev").getOperator());
        assertEquals("operator1", importedCrac.getFlowCnec("cnec1cur").getOperator());
        assertEquals("operator2", importedCrac.getFlowCnec("cnec2prev").getOperator());
        assertEquals("operator3", importedCrac.getFlowCnec("cnec3prevId").getOperator());
        assertEquals("operator4", importedCrac.getFlowCnec("cnec4prevId").getOperator());

        assertEquals(2, importedCrac.getPstRangeAction("pstRangeId").getInitialTap());
        assertEquals(0.5, importedCrac.getPstRangeAction("pstRangeId").convertTapToAngle(-2));
        assertEquals(2.5, importedCrac.getPstRangeAction("pstRangeId").convertTapToAngle(2));
        assertEquals(2, importedCrac.getPstRangeAction("pstRangeId").convertAngleToTap(2.5));

        assertEquals(1, importedCrac.getPstRangeAction("pstRangeId2").getUsageRules().size());
        assertTrue(importedCrac.getPstRangeAction("pstRangeId2").getUsageRules().get(0) instanceof OnFlowConstraintImpl);
        OnFlowConstraint onFlowConstraint = (OnFlowConstraint) importedCrac.getPstRangeAction("pstRangeId2").getUsageRules().get(0);
        assertEquals(Instant.PREVENTIVE, onFlowConstraint.getInstant());
        assertSame(importedCrac.getCnec("cnec3prevId"), onFlowConstraint.getFlowCnec());
    }
}
