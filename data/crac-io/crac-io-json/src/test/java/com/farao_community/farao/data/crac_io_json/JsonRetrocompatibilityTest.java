/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.InjectionSetpoint;
import com.farao_community.farao.data.crac_api.network_action.PstSetpoint;
import com.farao_community.farao.data.crac_api.network_action.SwitchPair;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range.StandardRange;
import com.farao_community.farao.data.crac_api.range.TapRange;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.threshold.Threshold;
import com.farao_community.farao.data.crac_api.usage_rule.*;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.data.crac_api.Instant.*;
import static com.farao_community.farao.data.crac_api.usage_rule.UsageMethod.AVAILABLE;
import static com.farao_community.farao.data.crac_api.usage_rule.UsageMethod.FORCED;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class JsonRetrocompatibilityTest {

    /*
    The goal of this test class is to ensure that former JSON CRAC files are still
    importable, even when modifications are brought to the JSON importer.
     */

    /*
    CARE: the existing json file used in this test case SHOULD NOT BE MODIFIED. If
    the current tests do not pass, it means that formerly generated JSON CRAC will
    not be compatible anymore with the next version of farao-core -> This is NOT
    desirable.

    Instead, we need to ensure that the JSON CRAC files used in this class can still
    be imported as is. Using versioning of the importer if needed.
     */

    @Test
    void importV1Point0Test() {

        // JSON file of farao-core v3.4.3
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.0.json");

        Crac crac = new JsonImport().importCrac(cracFile);

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(3, crac.getNetworkActions().size());
        assertEquals(2, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        testContentOfV1Point0Crac(crac);
    }

    @Test
    void importV1Point1Test() {

        // JSON file of farao-core v3.5
        // addition of switch pairs
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.1.json");

        Crac crac = new JsonImport().importCrac(cracFile);

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(2, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        testContentOfV1Point1Crac(crac);
    }

    @Test
    void importV1Point2Test() {

        // JSON file of farao-core v3.6
        // addition of injection range action
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.2.json");

        Crac crac = new JsonImport().importCrac(cracFile);

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(2, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getInjectionRangeActions().size());
        testContentOfV1Point2Crac(crac);
    }

    @Test
    void importV1Point3Test() {

        // JSON file of farao-core v3.9
        // addition of initial setpoints for InjectionRangeActions and HvdcRangeActions
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.3.json");

        Crac crac = new JsonImport().importCrac(cracFile);

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(2, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getInjectionRangeActions().size());
        testContentOfV1Point3Crac(crac);
    }

    @Test
    void importV1Point4Test() {

        // JSON file of farao-core v4.0
        // addition of angle cnecs
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.4.json");

        Crac crac = new JsonImport().importCrac(cracFile);

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(1, crac.getAngleCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(3, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getInjectionRangeActions().size());
        testContentOfV1Point4Crac(crac);
    }

    @Test
    void importV1Point5Test() {

        // JSON file of farao-core v4.1
        // addition of voltage cnecs
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.5.json");

        Crac crac = new JsonImport().importCrac(cracFile);

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(1, crac.getAngleCnecs().size());
        assertEquals(1, crac.getVoltageCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(3, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getInjectionRangeActions().size());
        testContentOfV1Point5Crac(crac);
    }

    @Test
    void importV1Point6Test() {

        // renaming usage rules
        // Branch threshold rule no longer handled

        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.6.json");

        Crac crac = new JsonImport().importCrac(cracFile);

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(1, crac.getAngleCnecs().size());
        assertEquals(1, crac.getVoltageCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(3, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getInjectionRangeActions().size());
        testContentOfV1Point6Crac(crac);
    }

    @Test
    void importV1Point7Test() {

        // renaming usage rules
        // Branch threshold rule no longer handled

        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.7.json");

        Crac crac = new JsonImport().importCrac(cracFile);

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(1, crac.getAngleCnecs().size());
        assertEquals(1, crac.getVoltageCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(4, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getInjectionRangeActions().size());
        testContentOfV1Point7Crac(crac);
    }

    @Test
    void importV1Point8Test() {

        // renaming usage rules
        // Branch threshold rule no longer handled

        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1/crac-v1.8.json");

        Crac crac = new JsonImport().importCrac(cracFile);

        assertEquals(2, crac.getContingencies().size());
        assertEquals(7, crac.getFlowCnecs().size());
        assertEquals(1, crac.getAngleCnecs().size());
        assertEquals(1, crac.getVoltageCnecs().size());
        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(4, crac.getPstRangeActions().size());
        assertEquals(2, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getInjectionRangeActions().size());
        testContentOfV1Point8Crac(crac);
    }

    private void testContentOfV1Point0Crac(Crac crac) {

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
        assertEquals(Side.LEFT, threshold.getSide());
        assertTrue(threshold.min().isEmpty());
        assertEquals(500., threshold.max().orElse(0.0), 1e-3);
        assertEquals(3, crac.getFlowCnec("cnec2prevId").getThresholds().size());
        assertTrue(crac.getFlowCnec("cnec2prevId").getThresholds().stream()
            .anyMatch(thr -> thr.getSide().equals(Side.LEFT) && thr.getUnit().equals(Unit.AMPERE) && thr.min().orElse(-999.).equals(-800.)));
        assertTrue(crac.getFlowCnec("cnec2prevId").getThresholds().stream()
            .anyMatch(thr -> thr.getSide().equals(Side.LEFT) && thr.getUnit().equals(Unit.PERCENT_IMAX) && thr.min().orElse(-999.).equals(-0.3)));
        assertTrue(crac.getFlowCnec("cnec2prevId").getThresholds().stream()
            .anyMatch(thr -> thr.getSide().equals(Side.RIGHT) && thr.getUnit().equals(Unit.AMPERE) && thr.max().orElse(-999.).equals(1200.)));

        // ---------------------------
        // --- test NetworkActions ---
        // ---------------------------

        // check that NetworkAction are present
        assertNotNull(crac.getNetworkAction("pstSetpointRaId"));
        assertNotNull(crac.getNetworkAction("injectionSetpointRaId"));
        assertNotNull(crac.getNetworkAction("complexNetworkActionId"));

        // check elementaryActions
        assertEquals(1, crac.getNetworkAction("pstSetpointRaId").getElementaryActions().size());
        assertTrue(crac.getNetworkAction("pstSetpointRaId").getElementaryActions().iterator().next() instanceof PstSetpoint);
        assertEquals(1, crac.getNetworkAction("injectionSetpointRaId").getElementaryActions().size());
        assertTrue(crac.getNetworkAction("injectionSetpointRaId").getElementaryActions().iterator().next() instanceof InjectionSetpoint);
        assertEquals(2, crac.getNetworkAction("complexNetworkActionId").getElementaryActions().size());

        // check onInstant usage rule
        assertEquals(1, crac.getNetworkAction("complexNetworkActionId").getUsageRules().size());
        assertTrue(crac.getNetworkAction("complexNetworkActionId").getUsageRules().get(0) instanceof OnInstant);
        OnInstant onInstant = (OnInstant) crac.getNetworkAction("complexNetworkActionId").getUsageRules().get(0);
        assertEquals(PREVENTIVE, onInstant.getInstant());
        assertEquals(AVAILABLE, onInstant.getUsageMethod());

        // check several usage rules
        assertEquals(2, crac.getNetworkAction("pstSetpointRaId").getUsageRules().size());

        // check onContingencyState usage Rule
        OnContingencyState onContingencyState = crac.getNetworkAction("pstSetpointRaId").getUsageRules().stream()
                .filter(ur -> ur instanceof OnContingencyState)
                .map(ur -> (OnContingencyState) ur)
                .findAny().orElse(null);
        assertNotNull(onContingencyState);
        assertEquals("contingency1Id", onContingencyState.getContingency().getId());
        assertEquals(CURATIVE, onContingencyState.getInstant());
        assertEquals(FORCED, onContingencyState.getUsageMethod());

        // check automaton OnFlowConstraint usage rule
        assertEquals(1, crac.getNetworkAction("injectionSetpointRaId").getUsageRules().size());
        assertTrue(crac.getNetworkAction("injectionSetpointRaId").getUsageRules().get(0) instanceof OnFlowConstraint);
        OnFlowConstraint onFlowConstraint1 = (OnFlowConstraint) crac.getNetworkAction("injectionSetpointRaId").getUsageRules().get(0);
        assertEquals("cnec3autoId", onFlowConstraint1.getFlowCnec().getId());
        assertEquals(AUTO, onFlowConstraint1.getInstant());

        // ----------------------------
        // --- test PstRangeActions ---
        // ----------------------------

        // check that RangeActions are present
        assertNotNull(crac.getRangeAction("pstRange1Id"));
        assertNotNull(crac.getRangeAction("pstRange2Id"));

        // check groupId
        assertTrue(crac.getRangeAction("pstRange1Id").getGroupId().isEmpty());
        assertEquals("group-1-pst", crac.getRangeAction("pstRange2Id").getGroupId().orElseThrow());

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

        // check usage rules
        assertEquals(4, (int) crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(List::stream).filter(OnInstant.class::isInstance).count());
    }

    void testContentOfV1Point1Crac(Crac crac) {

        testContentOfV1Point0Crac(crac);

        // test SwitchPair
        assertNotNull(crac.getNetworkAction("switchPairRaId"));

        assertEquals(1, crac.getNetworkAction("switchPairRaId").getElementaryActions().size());
        assertTrue(crac.getNetworkAction("switchPairRaId").getElementaryActions().iterator().next() instanceof SwitchPair);

        SwitchPair switchPair = (SwitchPair) crac.getNetworkAction("switchPairRaId").getElementaryActions().iterator().next();
        assertEquals("to-open", switchPair.getSwitchToOpen().getId());
        assertEquals("to-open", switchPair.getSwitchToOpen().getName());
        assertEquals("to-close", switchPair.getSwitchToClose().getId());
        assertEquals("to-close-name", switchPair.getSwitchToClose().getName());
    }

    void testContentOfV1Point2Crac(Crac crac) {

        testContentOfV1Point1Crac(crac);

        // test injection range action
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

        // check usage rules
        assertEquals(3, (int) crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(List::stream).filter(OnContingencyState.class::isInstance).count());
        assertEquals(3, (int) crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(List::stream).filter(OnFlowConstraint.class::isInstance).count());
    }

    void testContentOfV1Point3Crac(Crac crac) {

        testContentOfV1Point2Crac(crac);

        assertEquals(100, crac.getHvdcRangeAction("hvdcRange1Id").getInitialSetpoint(), 1e-3);
        assertEquals(-100, crac.getHvdcRangeAction("hvdcRange2Id").getInitialSetpoint(), 1e-3);
        assertEquals(50, crac.getInjectionRangeAction("injectionRange1Id").getInitialSetpoint(), 1e-3);
    }

    void testContentOfV1Point4Crac(Crac crac) {

        testContentOfV1Point3Crac(crac);

        // test angle cnec
        AngleCnec angleCnec = crac.getAngleCnec("angleCnecId");
        assertNotNull(angleCnec);

        assertEquals("eneId", angleCnec.getExportingNetworkElement().getId());
        assertEquals("ineId", angleCnec.getImportingNetworkElement().getId());
        assertEquals(CURATIVE, angleCnec.getState().getInstant());
        assertEquals("contingency1Id", angleCnec.getState().getContingency().get().getId());
        assertFalse(angleCnec.isOptimized());
        assertTrue(angleCnec.isMonitored());
        assertEquals("operator1", angleCnec.getOperator());
        assertEquals(1, angleCnec.getThresholds().size());
        Threshold threshold = angleCnec.getThresholds().iterator().next();
        assertEquals(Unit.DEGREE, threshold.getUnit());
        assertTrue(threshold.max().isEmpty());
        assertEquals(-100., threshold.min().orElse(0.0), 1e-3);

        //test onAngleCnec range action
        RangeAction rangeAction = crac.getRangeAction("pstRange3Id");
        assertEquals(1, rangeAction.getUsageRules().size());
        assertTrue(rangeAction.getUsageRules().get(0) instanceof OnAngleConstraint);
        OnAngleConstraint onAngleConstraint = (OnAngleConstraint) rangeAction.getUsageRules().get(0);
        assertEquals("angleCnecId", onAngleConstraint.getAngleCnec().getId());
        assertEquals(CURATIVE, onAngleConstraint.getInstant());

        // check usage rules
        assertEquals(1, (int) crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(List::stream).filter(OnAngleConstraint.class::isInstance).count());
    }

    void testContentOfV1Point5Crac(Crac crac) {

        testContentOfV1Point4Crac(crac);

        // test voltage cnec
        VoltageCnec voltageCnec = crac.getVoltageCnec("voltageCnecId");
        assertNotNull(voltageCnec);

        assertEquals("voltageCnecNeId", voltageCnec.getNetworkElement().getId());
        assertEquals(CURATIVE, voltageCnec.getState().getInstant());
        assertEquals("contingency1Id", voltageCnec.getState().getContingency().get().getId());
        assertFalse(voltageCnec.isOptimized());
        assertTrue(voltageCnec.isMonitored());
        assertEquals("operator1", voltageCnec.getOperator());
        assertEquals(1, voltageCnec.getThresholds().size());
        Threshold threshold = voltageCnec.getThresholds().iterator().next();
        assertEquals(Unit.KILOVOLT, threshold.getUnit());
        assertTrue(threshold.max().isEmpty());
        assertEquals(380., threshold.min().orElse(0.0), 1e-3);
    }

    void testContentOfV1Point6Crac(Crac crac) {

        testContentOfV1Point5Crac(crac);
        // test usage rules
        assertEquals(4, (int) crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(List::stream).filter(OnInstant.class::isInstance).count());
        assertEquals(3, (int) crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(List::stream).filter(OnContingencyState.class::isInstance).count());
        assertEquals(3, (int) crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(List::stream).filter(OnFlowConstraint.class::isInstance).count());
        assertEquals(1, (int) crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(List::stream).filter(OnAngleConstraint.class::isInstance).count());
        // test speed
        assertEquals(10, crac.getPstRangeAction("pstRange1Id").getSpeed().get().intValue());
        assertEquals(20, crac.getHvdcRangeAction("hvdcRange1Id").getSpeed().get().intValue());
        assertEquals(30, crac.getInjectionRangeAction("injectionRange1Id").getSpeed().get().intValue());
        assertEquals(40, crac.getNetworkAction("complexNetworkActionId").getSpeed().get().intValue());
    }

    void testContentOfV1Point7Crac(Crac crac) {

        testContentOfV1Point6Crac(crac);
        // test new voltage constraint usage rules
        assertEquals(1, crac.getRemedialActions().stream().map(RemedialAction::getUsageRules).flatMap(List::stream).filter(OnVoltageConstraint.class::isInstance).count());
    }

    void testContentOfV1Point8Crac(Crac crac) {

        testContentOfV1Point7Crac(crac);
        // test new injection setpoint unit
        assertEquals(Unit.MEGAWATT, ((InjectionSetpoint) crac.getNetworkAction("injectionSetpointRaId").getElementaryActions().stream().filter(InjectionSetpoint.class::isInstance).findFirst().orElseThrow()).getUnit());
    }
}
