/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.InjectionSetpoint;
import com.farao_community.farao.data.crac_api.network_action.PstSetpoint;
import com.farao_community.farao.data.crac_api.network_action.SwitchPair;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range.StandardRange;
import com.farao_community.farao.data.crac_api.range.TapRange;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.*;
import com.farao_community.farao.data.crac_impl.utils.ExhaustiveCracCreation;
import com.powsybl.iidm.network.Country;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.farao_community.farao.data.crac_api.Instant.*;
import static com.farao_community.farao.data.crac_api.usage_rule.UsageMethod.AVAILABLE;
import static com.farao_community.farao.data.crac_api.usage_rule.UsageMethod.FORCED;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracImportExportTest {

    @Test
    public void roundTripTest() {
        Crac crac = ExhaustiveCracCreation.create();

        Crac importedCrac = RoundTripUtil.roundTrip(crac);

        // check overall content
        assertNotNull(importedCrac);
        assertEquals(5, importedCrac.getStates().size());
        assertEquals(2, importedCrac.getContingencies().size());
        assertEquals(7, importedCrac.getFlowCnecs().size());
        assertEquals(1, importedCrac.getAngleCnecs().size());
        assertEquals(1, importedCrac.getVoltageCnecs().size());
        assertEquals(6, importedCrac.getRangeActions().size());
        assertEquals(4, importedCrac.getNetworkActions().size());

        // --------------------------
        // --- test Contingencies ---
        // --------------------------

        // check that Contingencies are present
        assertNotNull(crac.getContingency("contingency1Id"));
        assertNotNull(crac.getContingency("contingency2Id"));

        // check network elements
        assertEquals(1, crac.getContingency("contingency1Id").getNetworkElements().size());
        assertEquals("ne1Id", crac.getContingency("contingency1Id").getNetworkElements().iterator().next().getId());
        assertEquals(2, crac.getContingency("contingency2Id").getNetworkElements().size());

        // ----------------------
        // --- test FlowCnecs ---
        // ----------------------

        // check that Cnecs are present
        assertNotNull(crac.getFlowCnec("cnec1prevId"));
        assertNotNull(crac.getFlowCnec("cnec1outageId"));
        assertNotNull(crac.getFlowCnec("cnec2prevId"));
        assertNotNull(crac.getFlowCnec("cnec3prevId"));
        assertNotNull(crac.getFlowCnec("cnec3autoId"));
        assertNotNull(crac.getFlowCnec("cnec3curId"));
        assertNotNull(crac.getFlowCnec("cnec4prevId"));

        // check network element
        assertEquals("ne2Id", crac.getFlowCnec("cnec3prevId").getNetworkElement().getId());
        assertEquals("ne2Name", crac.getFlowCnec("cnec3prevId").getNetworkElement().getName());
        assertEquals("ne4Id", crac.getFlowCnec("cnec1outageId").getNetworkElement().getId());
        assertEquals("ne4Id", crac.getFlowCnec("cnec1outageId").getNetworkElement().getName());

        // check instants and contingencies
        assertEquals(PREVENTIVE, crac.getFlowCnec("cnec1prevId").getState().getInstant());
        assertTrue(crac.getFlowCnec("cnec1prevId").getState().getContingency().isEmpty());
        assertEquals(CURATIVE, crac.getFlowCnec("cnec3curId").getState().getInstant());
        assertEquals("contingency2Id", crac.getFlowCnec("cnec3curId").getState().getContingency().get().getId());
        assertEquals(AUTO, crac.getFlowCnec("cnec3autoId").getState().getInstant());
        assertEquals("contingency2Id", crac.getFlowCnec("cnec3autoId").getState().getContingency().get().getId());

        // check monitored and optimized
        assertFalse(crac.getFlowCnec("cnec3prevId").isOptimized());
        assertTrue(crac.getFlowCnec("cnec3prevId").isMonitored());
        assertTrue(crac.getFlowCnec("cnec4prevId").isOptimized());
        assertTrue(crac.getFlowCnec("cnec4prevId").isMonitored());

        // check operators
        assertEquals("operator1", crac.getFlowCnec("cnec1prevId").getOperator());
        assertEquals("operator1", crac.getFlowCnec("cnec1outageId").getOperator());
        assertEquals("operator2", crac.getFlowCnec("cnec2prevId").getOperator());
        assertEquals("operator3", crac.getFlowCnec("cnec3prevId").getOperator());
        assertEquals("operator4", crac.getFlowCnec("cnec4prevId").getOperator());

        // check iMax and nominal voltage
        assertEquals(2000., crac.getFlowCnec("cnec2prevId").getIMax(Side.LEFT), 1e-3);
        assertEquals(2000., crac.getFlowCnec("cnec2prevId").getIMax(Side.RIGHT), 1e-3);
        assertEquals(380., crac.getFlowCnec("cnec2prevId").getNominalVoltage(Side.LEFT), 1e-3);
        assertEquals(220., crac.getFlowCnec("cnec2prevId").getNominalVoltage(Side.RIGHT), 1e-3);
        assertEquals(Double.NaN, crac.getFlowCnec("cnec1prevId").getIMax(Side.LEFT), 1e-3);
        assertEquals(1000., crac.getFlowCnec("cnec1prevId").getIMax(Side.RIGHT), 1e-3);
        assertEquals(220., crac.getFlowCnec("cnec1prevId").getNominalVoltage(Side.LEFT), 1e-3);
        assertEquals(220., crac.getFlowCnec("cnec1prevId").getNominalVoltage(Side.RIGHT), 1e-3);

        // check threshold
        assertEquals(1, crac.getFlowCnec("cnec4prevId").getThresholds().size());
        BranchThreshold threshold = crac.getFlowCnec("cnec4prevId").getThresholds().iterator().next();
        assertEquals(Unit.MEGAWATT, threshold.getUnit());
        assertEquals(BranchThresholdRule.ON_LEFT_SIDE, threshold.getRule());
        assertTrue(threshold.min().isEmpty());
        assertEquals(500., threshold.max().orElse(0.0), 1e-3);
        assertEquals(4, crac.getFlowCnec("cnec2prevId").getThresholds().size());

        // ----------------------
        // --- test AngleCnec ---
        // ----------------------
        AngleCnec angleCnec = crac.getAngleCnec("angleCnecId");
        assertNotNull(angleCnec);
        assertEquals("eneId", angleCnec.getExportingNetworkElement().getId());
        assertEquals("ineId", angleCnec.getImportingNetworkElement().getId());
        assertEquals(CURATIVE, angleCnec.getState().getInstant());
        assertEquals("contingency1Id", angleCnec.getState().getContingency().get().getId());
        assertFalse(angleCnec.isOptimized());
        assertTrue(angleCnec.isMonitored());
        assertEquals("operator1", angleCnec.getOperator());
        assertEquals(-90., angleCnec.getLowerBound(Unit.DEGREE).orElseThrow(), 1e-3);
        assertEquals(90., angleCnec.getUpperBound(Unit.DEGREE).orElseThrow(), 1e-3);

        // ----------------------
        // --- test VoltageCnec ---
        // ----------------------
        VoltageCnec voltageCnec = crac.getVoltageCnec("voltageCnecId");
        assertNotNull(voltageCnec);
        assertEquals("voltageCnecNeId", voltageCnec.getNetworkElement().getId());
        assertEquals(CURATIVE, voltageCnec.getState().getInstant());
        assertEquals("contingency1Id", voltageCnec.getState().getContingency().get().getId());
        assertFalse(voltageCnec.isOptimized());
        assertTrue(voltageCnec.isMonitored());
        assertEquals("operator1", voltageCnec.getOperator());
        assertEquals(381, voltageCnec.getLowerBound(Unit.KILOVOLT).orElseThrow(), 1e-3);

        // ---------------------------
        // --- test NetworkActions ---
        // ---------------------------

        // check that NetworkAction are present
        assertNotNull(crac.getNetworkAction("pstSetpointRaId"));
        assertNotNull(crac.getNetworkAction("injectionSetpointRaId"));
        assertNotNull(crac.getNetworkAction("complexNetworkActionId"));
        assertNotNull(crac.getNetworkAction("switchPairRaId"));

        // check elementaryActions
        assertEquals(1, crac.getNetworkAction("pstSetpointRaId").getElementaryActions().size());
        assertTrue(crac.getNetworkAction("pstSetpointRaId").getElementaryActions().iterator().next() instanceof PstSetpoint);
        assertEquals(1, crac.getNetworkAction("injectionSetpointRaId").getElementaryActions().size());
        assertTrue(crac.getNetworkAction("injectionSetpointRaId").getElementaryActions().iterator().next() instanceof InjectionSetpoint);
        assertEquals(2, crac.getNetworkAction("complexNetworkActionId").getElementaryActions().size());

        // check freeToUse usage rule
        assertEquals(2, crac.getNetworkAction("complexNetworkActionId").getUsageRules().size());
        FreeToUse freeToUse = crac.getNetworkAction("complexNetworkActionId").getUsageRules().stream()
            .filter(ur -> ur instanceof FreeToUse)
            .map(ur -> (FreeToUse) ur)
            .findAny().orElse(null);
        assertNotNull(freeToUse);
        assertEquals(PREVENTIVE, freeToUse.getInstant());
        assertEquals(AVAILABLE, freeToUse.getUsageMethod());

        // check several usage rules
        assertEquals(2, crac.getNetworkAction("pstSetpointRaId").getUsageRules().size());

        // check onState usage Rule (curative)
        OnState onState = crac.getNetworkAction("pstSetpointRaId").getUsageRules().stream()
                .filter(ur -> ur instanceof OnState)
                .map(ur -> (OnState) ur)
                .findAny().orElse(null);
        assertNotNull(onState);
        assertEquals("contingency1Id", onState.getContingency().getId());
        assertEquals(CURATIVE, onState.getInstant());
        assertEquals(FORCED, onState.getUsageMethod());

        // check onState usage Rule (preventive)
        onState = crac.getNetworkAction("complexNetworkActionId").getUsageRules().stream()
            .filter(ur -> ur instanceof OnState)
            .map(ur -> (OnState) ur)
            .findAny().orElse(null);
        assertNotNull(onState);
        assertNull(onState.getContingency());
        assertEquals(crac.getPreventiveState(), onState.getState());
        assertEquals(PREVENTIVE, onState.getInstant());
        assertEquals(FORCED, onState.getUsageMethod());

        // check automaton OnFlowConstraint usage rule
        assertEquals(1, crac.getNetworkAction("injectionSetpointRaId").getUsageRules().size());
        assertTrue(crac.getNetworkAction("injectionSetpointRaId").getUsageRules().get(0) instanceof OnFlowConstraint);
        OnFlowConstraint onFlowConstraint1 = (OnFlowConstraint) crac.getNetworkAction("injectionSetpointRaId").getUsageRules().get(0);
        assertEquals("cnec3autoId", onFlowConstraint1.getFlowCnec().getId());
        assertEquals(AUTO, onFlowConstraint1.getInstant());

        // test SwitchPair

        assertEquals(1, crac.getNetworkAction("switchPairRaId").getElementaryActions().size());
        assertTrue(crac.getNetworkAction("switchPairRaId").getElementaryActions().iterator().next() instanceof SwitchPair);

        SwitchPair switchPair = (SwitchPair) crac.getNetworkAction("switchPairRaId").getElementaryActions().iterator().next();
        assertEquals("to-open", switchPair.getSwitchToOpen().getId());
        assertEquals("to-open", switchPair.getSwitchToOpen().getName());
        assertEquals("to-close", switchPair.getSwitchToClose().getId());
        assertEquals("to-close-name", switchPair.getSwitchToClose().getName());

        // ----------------------------
        // --- test PstRangeActions ---
        // ----------------------------

        // check that RangeActions are present
        assertNotNull(crac.getRangeAction("pstRange1Id"));
        assertNotNull(crac.getRangeAction("pstRange2Id"));
        assertNotNull(crac.getRangeAction("pstRange3Id"));

        // check groupId
        assertTrue(crac.getRangeAction("pstRange1Id").getGroupId().isEmpty());
        assertEquals("group-1-pst", crac.getRangeAction("pstRange2Id").getGroupId().orElseThrow());
        assertEquals("group-3-pst", crac.getRangeAction("pstRange3Id").getGroupId().orElseThrow());

        // check taps
        assertEquals(2, crac.getPstRangeAction("pstRange1Id").getInitialTap());
        assertEquals(0.5, crac.getPstRangeAction("pstRange1Id").convertTapToAngle(-2));
        assertEquals(2.5, crac.getPstRangeAction("pstRange1Id").convertTapToAngle(2));
        assertEquals(2, crac.getPstRangeAction("pstRange1Id").convertAngleToTap(2.5));

        // check Tap Range
        assertEquals(2, crac.getPstRangeAction("pstRange1Id").getRanges().size());

        TapRange absRange = crac.getPstRangeAction("pstRange1Id").getRanges().stream()
                .filter(tapRange -> tapRange.getRangeType().equals(RangeType.ABSOLUTE))
                .findAny().orElse(null);
        TapRange relRange = crac.getPstRangeAction("pstRange1Id").getRanges().stream()
                .filter(tapRange -> tapRange.getRangeType().equals(RangeType.RELATIVE_TO_INITIAL_NETWORK))
                .findAny().orElse(null);

        assertNotNull(absRange);
        assertEquals(1, absRange.getMinTap());
        assertEquals(7, absRange.getMaxTap());
        assertNotNull(relRange);
        assertEquals(-3, relRange.getMinTap());
        assertEquals(3, relRange.getMaxTap());
        assertEquals(Unit.TAP, relRange.getUnit());

        // check OnFlowConstraint usage rule
        assertEquals(1, crac.getPstRangeAction("pstRange2Id").getUsageRules().size());
        assertTrue(crac.getPstRangeAction("pstRange2Id").getUsageRules().get(0) instanceof OnFlowConstraint);
        OnFlowConstraint onFlowConstraint2 = (OnFlowConstraint) crac.getPstRangeAction("pstRange2Id").getUsageRules().get(0);
        assertEquals(PREVENTIVE, onFlowConstraint2.getInstant());
        assertSame(crac.getCnec("cnec3prevId"), onFlowConstraint2.getFlowCnec());

        // check OnAngleConstraint usage rule
        assertEquals(1, crac.getPstRangeAction("pstRange3Id").getUsageRules().size());
        assertTrue(crac.getPstRangeAction("pstRange3Id").getUsageRules().get(0) instanceof OnAngleConstraint);
        OnAngleConstraint onAngleConstraint = (OnAngleConstraint) crac.getPstRangeAction("pstRange3Id").getUsageRules().get(0);
        assertEquals(CURATIVE, onAngleConstraint.getInstant());
        assertSame(crac.getCnec("angleCnecId"), onAngleConstraint.getAngleCnec());

        // -----------------------------
        // --- test HvdcRangeActions ---
        // -----------------------------

        assertNotNull(crac.getRangeAction("hvdcRange1Id"));
        assertNotNull(crac.getRangeAction("hvdcRange2Id"));

        // check groupId
        assertTrue(crac.getRangeAction("hvdcRange1Id").getGroupId().isEmpty());
        assertEquals("group-1-hvdc", crac.getRangeAction("hvdcRange2Id").getGroupId().orElseThrow());

        // check preventive OnFlowConstraint usage rule
        assertEquals(1, crac.getHvdcRangeAction("hvdcRange2Id").getUsageRules().size());
        assertTrue(crac.getHvdcRangeAction("hvdcRange2Id").getUsageRules().get(0) instanceof OnFlowConstraint);
        OnFlowConstraint onFlowConstraint3 = (OnFlowConstraint) crac.getHvdcRangeAction("hvdcRange2Id").getUsageRules().get(0);
        assertEquals(PREVENTIVE, onFlowConstraint3.getInstant());
        assertSame(crac.getCnec("cnec3curId"), onFlowConstraint3.getFlowCnec());

        // check Hvdc range
        assertEquals(1, crac.getHvdcRangeAction("hvdcRange1Id").getRanges().size());
        StandardRange hvdcRange = crac.getHvdcRangeAction("hvdcRange1Id").getRanges().get(0);
        assertEquals(-1000, hvdcRange.getMin(), 1e-3);
        assertEquals(1000, hvdcRange.getMax(), 1e-3);
        assertEquals(Unit.MEGAWATT, hvdcRange.getUnit());

        // Check OnFlowConstraintInCountry usage rules
        List<UsageRule> usageRules = crac.getRemedialAction("hvdcRange1Id").getUsageRules();
        assertEquals(1, usageRules.size());
        assertTrue(usageRules.get(0) instanceof OnFlowConstraintInCountry);
        OnFlowConstraintInCountry ur = (OnFlowConstraintInCountry) usageRules.get(0);
        assertEquals(PREVENTIVE, ur.getInstant());
        assertEquals(Country.FR, ur.getCountry());

        // ---------------------------------
        // --- test InjectionRangeAction ---
        // ---------------------------------

        assertNotNull(crac.getInjectionRangeAction("injectionRange1Id"));

        assertEquals("injectionRange1Name", crac.getInjectionRangeAction("injectionRange1Id").getName());
        assertNull(crac.getInjectionRangeAction("injectionRange1Id").getOperator());
        assertTrue(crac.getInjectionRangeAction("injectionRange1Id").getGroupId().isEmpty());
        Map<NetworkElement, Double> networkElementAndKeys = crac.getInjectionRangeAction("injectionRange1Id").getInjectionDistributionKeys();
        assertEquals(2, networkElementAndKeys.size());
        assertEquals(1., networkElementAndKeys.entrySet().stream().filter(e -> e.getKey().getId().equals("generator1Id")).findAny().orElseThrow().getValue(), 1e-3);
        assertEquals(-1., networkElementAndKeys.entrySet().stream().filter(e -> e.getKey().getId().equals("generator2Id")).findAny().orElseThrow().getValue(), 1e-3);
        assertEquals("generator2Name", networkElementAndKeys.entrySet().stream().filter(e -> e.getKey().getId().equals("generator2Id")).findAny().orElseThrow().getKey().getName());
        assertEquals(2, crac.getInjectionRangeAction("injectionRange1Id").getRanges().size());

        // Check OnFlowConstraintInCountry usage rules
        usageRules = crac.getRemedialAction("injectionRange1Id").getUsageRules();
        assertEquals(1, usageRules.size());
        assertTrue(usageRules.get(0) instanceof OnFlowConstraintInCountry);
        ur = (OnFlowConstraintInCountry) usageRules.get(0);
        assertEquals(CURATIVE, ur.getInstant());
        assertEquals(Country.ES, ur.getCountry());
    }
}
