/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.OptimizationStepsExecuted;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.commons.Unit.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RaoResultImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private RaoResultImpl raoResult;
    private Crac crac;
    private FlowCnec cnec;
    private PstRangeAction pst;
    private NetworkAction na;
    private Instant preventiveInstant;
    private Instant outageInstant;
    private Instant autoInstant;
    private Instant curativeInstant;

    private void setUp() {
        crac = CommonCracCreation.createWithPreventiveAndCurativePstRange();
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        cnec = crac.getFlowCnec("cnec1basecase");
        pst = crac.getPstRangeAction("pst");
        na = crac.newNetworkAction().withId("na-id")
            .newSwitchAction().withNetworkElement("any").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
            .newOnContingencyStateUsageRule().withContingency("Contingency FR1 FR3").withInstant(AUTO_INSTANT_ID).add()
            .newOnContingencyStateUsageRule().withContingency("Contingency FR1 FR2").withInstant(AUTO_INSTANT_ID).add()
            .newOnInstantUsageRule().withInstant(CURATIVE_INSTANT_ID).add()
            .add();

        raoResult = new RaoResultImpl(crac);

        FlowCnecResult flowCnecResult = raoResult.getAndCreateIfAbsentFlowCnecResult(cnec);

        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(null);
        ElementaryFlowCnecResult elementaryFlowCnecResult = flowCnecResult.getResult(null);

        elementaryFlowCnecResult.setFlow(TwoSides.ONE, 100., MEGAWATT);
        elementaryFlowCnecResult.setMargin(101., MEGAWATT);
        elementaryFlowCnecResult.setRelativeMargin(102., MEGAWATT);
        elementaryFlowCnecResult.setLoopFlow(TwoSides.ONE, 103., MEGAWATT);
        elementaryFlowCnecResult.setCommercialFlow(TwoSides.ONE, 104., MEGAWATT);

        elementaryFlowCnecResult.setFlow(TwoSides.ONE, 110., AMPERE);
        elementaryFlowCnecResult.setMargin(111., AMPERE);
        elementaryFlowCnecResult.setRelativeMargin(112., AMPERE);
        elementaryFlowCnecResult.setLoopFlow(TwoSides.ONE, 113., AMPERE);
        elementaryFlowCnecResult.setCommercialFlow(TwoSides.ONE, 114., AMPERE);

        elementaryFlowCnecResult.setPtdfZonalSum(TwoSides.ONE, 0.1);

        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(preventiveInstant);
        elementaryFlowCnecResult = flowCnecResult.getResult(preventiveInstant);

        elementaryFlowCnecResult.setFlow(TwoSides.ONE, 200., MEGAWATT);
        elementaryFlowCnecResult.setMargin(201., MEGAWATT);
        elementaryFlowCnecResult.setRelativeMargin(202., MEGAWATT);
        elementaryFlowCnecResult.setLoopFlow(TwoSides.ONE, 203., MEGAWATT);

        elementaryFlowCnecResult.setFlow(TwoSides.ONE, 210., AMPERE);
        elementaryFlowCnecResult.setMargin(211., AMPERE);
        elementaryFlowCnecResult.setRelativeMargin(212., AMPERE);
        elementaryFlowCnecResult.setLoopFlow(TwoSides.ONE, 213., AMPERE);

        elementaryFlowCnecResult.setPtdfZonalSum(TwoSides.ONE, 0.1);

        raoResult.getAndCreateIfAbsentNetworkActionResult(na).addActivationForState(crac.getState("Contingency FR1 FR3", autoInstant));
        raoResult.getAndCreateIfAbsentNetworkActionResult(na).addActivationForState(crac.getState("Contingency FR1 FR2", curativeInstant));

        RangeActionResult pstRangeActionResult = raoResult.getAndCreateIfAbsentRangeActionResult(pst);
        pstRangeActionResult.setInitialSetpoint(2.3); // tap = 6
        pstRangeActionResult.addActivationForState(crac.getPreventiveState(), -3.1); // tap = -8

        CostResult costResult = raoResult.getAndCreateIfAbsentCostResult(RaoResult.INITIAL_INSTANT_ID);
        costResult.setFunctionalCost(100.);
        costResult.setVirtualCost("loopFlow", 0.);
        costResult.setVirtualCost("MNEC", 0.);

        costResult = raoResult.getAndCreateIfAbsentCostResult(CURATIVE_INSTANT_ID);
        costResult.setFunctionalCost(-50.);
        costResult.setVirtualCost("loopFlow", 10.);
        costResult.setVirtualCost("MNEC", 2.);

        raoResult.setComputationStatus(ComputationStatus.DEFAULT);
    }

    private void getResultAtAGivenState(Instant optimizedInstant) {
        assertEquals(200., raoResult.getFlow(optimizedInstant, cnec, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(201., raoResult.getMargin(optimizedInstant, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(202., raoResult.getRelativeMargin(optimizedInstant, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(203., raoResult.getLoopFlow(optimizedInstant, cnec, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(optimizedInstant, cnec, TwoSides.ONE, MEGAWATT)));

        assertEquals(210., raoResult.getFlow(optimizedInstant, cnec, TwoSides.ONE, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(211., raoResult.getMargin(optimizedInstant, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(212., raoResult.getRelativeMargin(optimizedInstant, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(213., raoResult.getLoopFlow(optimizedInstant, cnec, TwoSides.ONE, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(optimizedInstant, cnec, TwoSides.ONE, AMPERE)));

        assertEquals(0.1, raoResult.getPtdfZonalSum(optimizedInstant, cnec, TwoSides.ONE), DOUBLE_TOLERANCE);
    }

    @Test
    void testPreventiveCnecResults() {
        setUp();

        assertEquals(100., raoResult.getFlow(null, cnec, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(101., raoResult.getMargin(null, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(102., raoResult.getRelativeMargin(null, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(103., raoResult.getLoopFlow(null, cnec, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(104., raoResult.getCommercialFlow(null, cnec, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(110., raoResult.getFlow(null, cnec, TwoSides.ONE, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(111., raoResult.getMargin(null, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(112., raoResult.getRelativeMargin(null, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(113., raoResult.getLoopFlow(null, cnec, TwoSides.ONE, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(114., raoResult.getCommercialFlow(null, cnec, TwoSides.ONE, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(0.1, raoResult.getPtdfZonalSum(null, cnec, TwoSides.ONE), DOUBLE_TOLERANCE);

        // should always return after pra results because the cnec is Preventive
        getResultAtAGivenState(preventiveInstant);
        getResultAtAGivenState(autoInstant);
        getResultAtAGivenState(curativeInstant);
    }

    @Test
    void testPstRangeActionResults() {
        setUp();
        assertEquals(6, raoResult.getPreOptimizationTapOnState(crac.getPreventiveState(), pst));
        assertEquals(2.3, raoResult.getPreOptimizationSetPointOnState(crac.getPreventiveState(), pst), DOUBLE_TOLERANCE);
        assertTrue(raoResult.isActivatedDuringState(crac.getPreventiveState(), pst));
        assertEquals(-8, raoResult.getOptimizedTapOnState(crac.getPreventiveState(), pst));
        assertEquals(Map.of(pst, -8), raoResult.getOptimizedTapsOnState(crac.getPreventiveState()));
        assertEquals(-3.1, raoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), pst), DOUBLE_TOLERANCE);
        assertEquals(Map.of(pst, -3.1), raoResult.getOptimizedSetPointsOnState(crac.getPreventiveState()));
        assertEquals(Set.of(pst), raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(), raoResult.getActivatedRangeActionsDuringState(crac.getState("Contingency FR1 FR3", autoInstant)));
    }

    @Test
    void testNetworkActionResults() {
        setUp();
        assertFalse(raoResult.wasActivatedBeforeState(crac.getPreventiveState(), na));
        assertFalse(raoResult.isActivatedDuringState(crac.getPreventiveState(), na));
        assertEquals(Set.of(), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));

        State state = crac.getState("Contingency FR1 FR3", autoInstant);
        assertFalse(raoResult.wasActivatedBeforeState(state, na));
        assertTrue(raoResult.isActivated(state, na));
        assertTrue(raoResult.isActivatedDuringState(state, na));
        assertEquals(Set.of(na), raoResult.getActivatedNetworkActionsDuringState(state));
        state = crac.getState("Contingency FR1 FR3", curativeInstant);
        assertTrue(raoResult.wasActivatedBeforeState(state, na));
        assertTrue(raoResult.isActivated(state, na));
        assertFalse(raoResult.isActivatedDuringState(state, na));
        assertEquals(Set.of(), raoResult.getActivatedNetworkActionsDuringState(state));

        state = crac.getState("Contingency FR1 FR2", autoInstant);
        assertFalse(raoResult.wasActivatedBeforeState(state, na));
        assertFalse(raoResult.isActivated(state, na));
        assertFalse(raoResult.isActivatedDuringState(state, na));
        assertEquals(Set.of(), raoResult.getActivatedNetworkActionsDuringState(state));
        state = crac.getState("Contingency FR1 FR2", curativeInstant);
        assertFalse(raoResult.wasActivatedBeforeState(state, na));
        assertTrue(raoResult.isActivated(state, na));
        assertTrue(raoResult.isActivatedDuringState(state, na));
        assertEquals(Set.of(na), raoResult.getActivatedNetworkActionsDuringState(state));
    }

    @Test
    void testCostResults() {
        setUp();

        assertEquals(Set.of("loopFlow", "MNEC"), raoResult.getVirtualCostNames());

        assertEquals(100., raoResult.getFunctionalCost(null), DOUBLE_TOLERANCE);
        assertEquals(0., raoResult.getVirtualCost(null, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., raoResult.getVirtualCost(null, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., raoResult.getVirtualCost(null), DOUBLE_TOLERANCE);
        assertEquals(100., raoResult.getCost(null), DOUBLE_TOLERANCE);

        assertEquals(-50., raoResult.getFunctionalCost(curativeInstant), DOUBLE_TOLERANCE);
        assertEquals(10., raoResult.getVirtualCost(curativeInstant, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(2., raoResult.getVirtualCost(curativeInstant, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(12., raoResult.getVirtualCost(curativeInstant), DOUBLE_TOLERANCE);
        assertEquals(-38, raoResult.getCost(curativeInstant), DOUBLE_TOLERANCE);

        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus());
    }

    @Test
    void testExecutionDetails() {
        setUp();
        assertEquals(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY, raoResult.getExecutionDetails());
        raoResult.setExecutionDetails(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION);
        assertEquals(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION, raoResult.getExecutionDetails());
    }

    @Test
    void testSensitivityStatus() {
        setUp();
        raoResult.setComputationStatus(crac.getState("Contingency FR1 FR3", autoInstant), ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus(crac.getState("Contingency FR1 FR3", autoInstant)));
    }

    @Test
    void testIsSecureFlowCnecs() {
        setUp();
        assertTrue(raoResult.isSecure(preventiveInstant, PhysicalParameter.FLOW));
        assertTrue(raoResult.isSecure(autoInstant, PhysicalParameter.FLOW));
        assertTrue(raoResult.isSecure(curativeInstant, PhysicalParameter.FLOW));
        assertTrue(raoResult.isSecure(preventiveInstant, PhysicalParameter.FLOW, PhysicalParameter.ANGLE));
        assertTrue(raoResult.isSecure(preventiveInstant, PhysicalParameter.FLOW, PhysicalParameter.VOLTAGE));
        assertTrue(raoResult.isSecure(preventiveInstant, PhysicalParameter.FLOW, PhysicalParameter.ANGLE, PhysicalParameter.VOLTAGE));
    }

    @Test
    void testIsNotSecureIfComputationStatusIsFailure() {
        setUp();
        raoResult.setComputationStatus(ComputationStatus.FAILURE);
        assertFalse(raoResult.isSecure(preventiveInstant, PhysicalParameter.FLOW));
    }

    @Test
    void testIsSecureIfNoCnecOfGivenParameterType() {
        setUp();
        assertTrue(raoResult.isSecure(preventiveInstant, PhysicalParameter.ANGLE));
    }

    @Test
    void testIsNotSecureCheckPhysicalParameterKind2() {
        setUp();
        AngleCnec angleCnec = crac.newAngleCnec()
                .withId("AngleCnec")
                .withInstant(AUTO_INSTANT_ID)
                .withExportingNetworkElement("ExportingNE")
                .withImportingNetworkElement("ImportingNE")
                .withContingency("Contingency FR1 FR3")
                .newThreshold()
                .withMin(0.)
                .withMax(30.)
                .withUnit(DEGREE)
                .add()
                .add();

        AngleCnecResult result = raoResult.getAndCreateIfAbsentAngleCnecResult(angleCnec);
        ElementaryAngleCnecResult elementaryAngleCnecResult = result.getAndCreateIfAbsentResultForOptimizationState(autoInstant);
        elementaryAngleCnecResult.setAngle(35., DEGREE);
        elementaryAngleCnecResult.setMargin(-5., DEGREE);
        assertTrue(raoResult.isSecure(autoInstant, PhysicalParameter.FLOW));
        assertFalse(raoResult.isSecure(autoInstant, PhysicalParameter.FLOW, PhysicalParameter.ANGLE));
    }

    @Test
    void testIsSecureDependsOnOptimizationState() {
        setUp();
        VoltageCnec voltageCnec = crac.newVoltageCnec()
                .withId("VoltageCnec")
                .withInstant(AUTO_INSTANT_ID)
                .withNetworkElement("NetworkElement")
                .withContingency("Contingency FR1 FR3")
                .newThreshold()
                .withMin(180.)
                .withMax(250.)
                .withUnit(KILOVOLT)
                .add()
                .add();

        VoltageCnecResult result = raoResult.getAndCreateIfAbsentVoltageCnecResult(voltageCnec);
        ElementaryVoltageCnecResult elementaryVoltageCnecResult = result.getAndCreateIfAbsentResultForOptimizationState(preventiveInstant);
        elementaryVoltageCnecResult.setMinVoltage(200., KILOVOLT);
        elementaryVoltageCnecResult.setMaxVoltage(220., KILOVOLT);
        elementaryVoltageCnecResult.setMargin(20., KILOVOLT);
        elementaryVoltageCnecResult = result.getAndCreateIfAbsentResultForOptimizationState(autoInstant);
        elementaryVoltageCnecResult.setMinVoltage(175., KILOVOLT);
        elementaryVoltageCnecResult.setMaxVoltage(195., KILOVOLT);
        elementaryVoltageCnecResult.setMargin(-5., KILOVOLT);
        elementaryVoltageCnecResult = result.getAndCreateIfAbsentResultForOptimizationState(curativeInstant);
        elementaryVoltageCnecResult.setMinVoltage(200., KILOVOLT);
        elementaryVoltageCnecResult.setMaxVoltage(220., KILOVOLT);
        elementaryVoltageCnecResult.setMargin(20., KILOVOLT);
        assertTrue(raoResult.isSecure(preventiveInstant, PhysicalParameter.FLOW, PhysicalParameter.ANGLE, PhysicalParameter.VOLTAGE));
        assertFalse(raoResult.isSecure(autoInstant, PhysicalParameter.FLOW, PhysicalParameter.ANGLE, PhysicalParameter.VOLTAGE));
        assertTrue(raoResult.isSecure(curativeInstant, PhysicalParameter.FLOW, PhysicalParameter.ANGLE, PhysicalParameter.VOLTAGE));
        assertTrue(raoResult.isSecure());
    }

    @Test
    void comprehensiveRaoResultWithAllThreeTypesOfCnecs() {
        setUp();
        addOutageFlowCnec();
        addAngleCnecs();
        addVoltageCnecs();

        AngleCnecResult angleResult1 = raoResult.getAndCreateIfAbsentAngleCnecResult(crac.getAngleCnec("angleCnecPreventive"));
        ElementaryAngleCnecResult elementaryAngleCnecResult1 = angleResult1.getAndCreateIfAbsentResultForOptimizationState(preventiveInstant);
        elementaryAngleCnecResult1.setAngle(50., DEGREE);
        elementaryAngleCnecResult1.setMargin(10., DEGREE);

        AngleCnecResult angleResult2 = raoResult.getAndCreateIfAbsentAngleCnecResult(crac.getAngleCnec("angleCnecStateOutageContingency1"));
        ElementaryAngleCnecResult elementaryAngleCnecResult2 = angleResult2.getAndCreateIfAbsentResultForOptimizationState(preventiveInstant);
        elementaryAngleCnecResult2.setAngle(90., DEGREE);
        elementaryAngleCnecResult2.setMargin(30., DEGREE);

        AngleCnecResult angleResult3 = raoResult.getAndCreateIfAbsentAngleCnecResult(crac.getAngleCnec("angleCnecStateCurativeContingency1"));
        ElementaryAngleCnecResult elementaryAngleCnecResult3 = angleResult3.getAndCreateIfAbsentResultForOptimizationState(preventiveInstant);
        elementaryAngleCnecResult3.setAngle(35., DEGREE);
        elementaryAngleCnecResult3.setMargin(-.5, DEGREE);

        VoltageCnecResult voltageResult1 = raoResult.getAndCreateIfAbsentVoltageCnecResult(crac.getVoltageCnec("voltageCnecPreventive"));
        ElementaryVoltageCnecResult elementaryVoltageCnecResult1 = voltageResult1.getAndCreateIfAbsentResultForOptimizationState(preventiveInstant);
        elementaryVoltageCnecResult1.setMinVoltage(400., KILOVOLT);
        elementaryVoltageCnecResult1.setMaxVoltage(420., KILOVOLT);
        elementaryVoltageCnecResult1.setMargin(40., KILOVOLT);

        VoltageCnecResult voltageResult2 = raoResult.getAndCreateIfAbsentVoltageCnecResult(crac.getVoltageCnec("voltageCnecStateOutageContingency1"));
        ElementaryVoltageCnecResult elementaryVoltageCnecResult2 = voltageResult2.getAndCreateIfAbsentResultForOptimizationState(preventiveInstant);
        elementaryVoltageCnecResult2.setMinVoltage(415., KILOVOLT);
        elementaryVoltageCnecResult2.setMaxVoltage(435., KILOVOLT);
        elementaryVoltageCnecResult2.setMargin(5., KILOVOLT);

        VoltageCnecResult voltageResult3 = raoResult.getAndCreateIfAbsentVoltageCnecResult(crac.getVoltageCnec("voltageCnecStateCurativeContingency1"));
        ElementaryVoltageCnecResult elementaryVoltageCnecResult3 = voltageResult3.getAndCreateIfAbsentResultForOptimizationState(preventiveInstant);
        elementaryVoltageCnecResult3.setMinVoltage(400., KILOVOLT);
        elementaryVoltageCnecResult3.setMaxVoltage(420., KILOVOLT);
        elementaryVoltageCnecResult3.setMargin(40., KILOVOLT);

        assertFalse(raoResult.isSecure(preventiveInstant, PhysicalParameter.FLOW, PhysicalParameter.ANGLE, PhysicalParameter.VOLTAGE));
        assertTrue(raoResult.isSecure(preventiveInstant, PhysicalParameter.FLOW, PhysicalParameter.VOLTAGE));

        assertEquals(50.0, raoResult.getAngle(preventiveInstant, crac.getAngleCnec("angleCnecPreventive"), DEGREE));
        assertEquals(10.0, raoResult.getMargin(preventiveInstant, crac.getAngleCnec("angleCnecPreventive"), DEGREE));

        assertEquals(90.0, raoResult.getAngle(preventiveInstant, crac.getAngleCnec("angleCnecStateOutageContingency1"), DEGREE));
        assertEquals(30.0, raoResult.getMargin(preventiveInstant, crac.getAngleCnec("angleCnecStateOutageContingency1"), DEGREE));

        assertEquals(35.0, raoResult.getAngle(preventiveInstant, crac.getAngleCnec("angleCnecStateCurativeContingency1"), DEGREE));
        assertEquals(-.5, raoResult.getMargin(preventiveInstant, crac.getAngleCnec("angleCnecStateCurativeContingency1"), DEGREE));

        assertEquals(400.0, raoResult.getMinVoltage(preventiveInstant, crac.getVoltageCnec("voltageCnecPreventive"), KILOVOLT));
        assertEquals(420.0, raoResult.getMaxVoltage(preventiveInstant, crac.getVoltageCnec("voltageCnecPreventive"), KILOVOLT));
        assertEquals(40.0, raoResult.getMargin(preventiveInstant, crac.getVoltageCnec("voltageCnecPreventive"), KILOVOLT));

        assertEquals(415.0, raoResult.getMinVoltage(preventiveInstant, crac.getVoltageCnec("voltageCnecStateOutageContingency1"), KILOVOLT));
        assertEquals(435.0, raoResult.getMaxVoltage(preventiveInstant, crac.getVoltageCnec("voltageCnecStateOutageContingency1"), KILOVOLT));
        assertEquals(5.0, raoResult.getMargin(preventiveInstant, crac.getVoltageCnec("voltageCnecStateOutageContingency1"), KILOVOLT));

        assertEquals(400.0, raoResult.getMinVoltage(preventiveInstant, crac.getVoltageCnec("voltageCnecStateCurativeContingency1"), KILOVOLT));
        assertEquals(420.0, raoResult.getMaxVoltage(preventiveInstant, crac.getVoltageCnec("voltageCnecStateCurativeContingency1"), KILOVOLT));
        assertEquals(40.0, raoResult.getMargin(preventiveInstant, crac.getVoltageCnec("voltageCnecStateCurativeContingency1"), KILOVOLT));

        assertEquals("RaoResult does not contain angle values for all AngleCNECs, security status for physical parameter ANGLE is unknown", assertThrows(OpenRaoException.class, () -> raoResult.isSecure(outageInstant, PhysicalParameter.FLOW, PhysicalParameter.ANGLE)).getMessage());
        assertEquals("RaoResult does not contain angle values for all AngleCNECs, security status for physical parameter ANGLE is unknown", assertThrows(OpenRaoException.class, () -> raoResult.isSecure(curativeInstant, PhysicalParameter.FLOW, PhysicalParameter.ANGLE)).getMessage());
        assertEquals("RaoResult does not contain voltage values for all VoltageCNECs, security status for physical parameter VOLTAGE is unknown", assertThrows(OpenRaoException.class, () -> raoResult.isSecure(outageInstant, PhysicalParameter.FLOW, PhysicalParameter.VOLTAGE)).getMessage());
        assertEquals("RaoResult does not contain voltage values for all VoltageCNECs, security status for physical parameter VOLTAGE is unknown", assertThrows(OpenRaoException.class, () -> raoResult.isSecure(curativeInstant, PhysicalParameter.FLOW, PhysicalParameter.VOLTAGE)).getMessage());
    }

    private void addVoltageCnecs() {
        crac.newVoltageCnec()
            .withId("voltageCnecPreventive")
            .withNetworkElement("BBE2AA1 ")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withMonitored(true)
            .withOperator("operator1")
            .newThreshold()
                .withUnit(KILOVOLT)
                .withMax(440.)
                .add()
            .add();

        crac.newVoltageCnec()
            .withId("voltageCnecStateOutageContingency1")
            .withNetworkElement("BBE2AA1 ")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withMonitored(true)
            .withOperator("operator1")
            .newThreshold()
                .withUnit(KILOVOLT)
                .withMax(420.)
                .add()
            .add();

        crac.newVoltageCnec()
            .withId("voltageCnecStateCurativeContingency1")
            .withNetworkElement("BBE2AA1 ")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withMonitored(true)
            .withOperator("operator1")
            .newThreshold()
                .withUnit(KILOVOLT)
                .withMax(440.)
                .add()
            .add();
    }

    private void addAngleCnecs() {
        crac.newAngleCnec()
            .withId("angleCnecPreventive")
            .withExportingNetworkElement("BBE2AA1 ")
            .withImportingNetworkElement("FFR3AA1 ")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withOperator("operator1")
            .withMonitored(true)
            .newThreshold()
                .withUnit(DEGREE)
                .withMin(-60.)
                .withMax(60.)
                .add()
            .add();

        crac.newAngleCnec()
            .withId("angleCnecStateOutageContingency1")
            .withExportingNetworkElement("BBE2AA1 ")
            .withImportingNetworkElement("FFR3AA1 ")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withMonitored(true)
            .withOperator("operator1")
            .newThreshold()
                .withUnit(DEGREE)
                .withMin(-120.)
                .withMax(120.)
                .add()
            .add();

        crac.newAngleCnec()
            .withId("angleCnecStateCurativeContingency1")
            .withExportingNetworkElement("BBE2AA1 ")
            .withImportingNetworkElement("FFR3AA1 ")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withMonitored(true)
            .withOperator("operator1")
            .newThreshold()
                .withUnit(DEGREE)
                .withMin(-30.)
                .withMax(30.)
                .add()
            .add();
    }

    private void addOutageFlowCnec() {
        crac.newFlowCnec()
            .withId("cnec1stateOutageContingency1")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator1")
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withSide(TwoSides.ONE)
                .withMin(-2000.)
                .withMax(2000.)
                .add()
            .newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withSide(TwoSides.TWO)
                .withMin(-2000.)
                .withMax(2000.)
                .add()
            .add();
    }
}
