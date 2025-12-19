/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring;

import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.CnecValue;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.impl.VoltageCnecValue;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.results.CnecResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class VoltageMonitoringTest {
    private static final double VOLTAGE_TOLERANCE = 0.5;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Network network;
    private Crac crac;
    private RaoResult raoResult;
    private LoadFlowParameters loadFlowParameters;
    private MonitoringResult voltageMonitoringResult;
    private NetworkAction naOpenL1;
    private NetworkAction naCloseL1;
    private NetworkAction naOpenL2;
    private NetworkAction naCloseL2;
    private PstRangeAction pst;
    private VoltageCnec vcPrev;
    private Instant curativeInstant;

    @BeforeEach
    void setUp() {
        network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);

        naOpenL1 = crac.newNetworkAction()
            .withId("Open L1")
            .newTerminalsConnectionAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
            .add();
        naCloseL1 = crac.newNetworkAction()
            .withId("Close L1")
            .newTerminalsConnectionAction().withNetworkElement("L1").withActionType(ActionType.CLOSE).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
            .add();
        naOpenL2 = crac.newNetworkAction()
            .withId("Open L2")
            .newTerminalsConnectionAction().withNetworkElement("L2").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
            .add();
        naCloseL2 = crac.newNetworkAction()
            .withId("Close L2")
            .newTerminalsConnectionAction().withNetworkElement("L2").withActionType(ActionType.CLOSE).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
            .add();
        pst = crac.newPstRangeAction()
            .withId("pst")
            .withNetworkElement("PS1")
            .withInitialTap(2).withTapToAngleConversionMap(Map.of(1, -20., 2, 0., 3, 20.))
            .newTapRange().withMinTap(1).withMaxTap(3).withRangeType(RangeType.ABSOLUTE).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
            .add();

        crac.newContingency().withId("coL1").withContingencyElement("L1", ContingencyElementType.LINE).add();
        crac.newContingency().withId("coL2").withContingencyElement("L2", ContingencyElementType.LINE).add();
        crac.newContingency().withId("coL1L2").withContingencyElement("L1", ContingencyElementType.LINE).withContingencyElement("L2", ContingencyElementType.LINE).add();

        loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(false);

        raoResult = Mockito.mock(RaoResult.class);
        when(raoResult.getActivatedNetworkActionsDuringState(any())).thenReturn(Collections.emptySet());
        when(raoResult.getActivatedRangeActionsDuringState(any())).thenReturn(Collections.emptySet());
    }

    private VoltageCnec addVoltageCnec(String id, String instantId, String contingency, String networkElement, Double min, Double max) {
        return crac.newVoltageCnec()
            .withId(id)
            .withInstant(instantId)
            .withContingency(contingency)
            .withNetworkElement(networkElement)
            .withMonitored()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(min).withMax(max).add()
            .add();
    }

    private void runVoltageMonitoring() {
        MonitoringInput monitoringInput = new MonitoringInput.MonitoringInputBuilder().withCrac(crac).withNetwork(network).withRaoResult(raoResult).withPhysicalParameter(PhysicalParameter.VOLTAGE).build();
        voltageMonitoringResult = new Monitoring("OpenLoadFlow", loadFlowParameters).runMonitoring(monitoringInput, 1);
    }

    @Test
    void testOneSecurePreventiveCnec() {
        addVoltageCnec("vc", PREVENTIVE_INSTANT_ID, null, "VL1", null, 500.);
        runVoltageMonitoring();
        VoltageCnecValue voltageCnecValue = (VoltageCnecValue) voltageMonitoringResult.getCnecResults().stream().filter(cnec -> cnec.getId().equals("vc")).map(CnecResult::getValue).findFirst().get();
        assertEquals(400., voltageCnecValue.minValue(), VOLTAGE_TOLERANCE);
        assertEquals(400., voltageCnecValue.maxValue(), VOLTAGE_TOLERANCE);
        assertEquals(Cnec.SecurityStatus.SECURE, voltageMonitoringResult.getStatus());
        assertTrue(voltageMonitoringResult.getCnecResults().stream().noneMatch(cr -> cr.getMargin() < 0));
        assertEquals(List.of("All VOLTAGE Cnecs are secure."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testTwoSecurePreventiveCnecs() {
        addVoltageCnec("vc1", PREVENTIVE_INSTANT_ID, null, "VL1", 400., 400.);
        addVoltageCnec("vc2", PREVENTIVE_INSTANT_ID, null, "VL2", 385., null);
        runVoltageMonitoring();
        assertEquals(Cnec.SecurityStatus.SECURE, voltageMonitoringResult.getStatus());
        assertTrue(voltageMonitoringResult.getCnecResults().stream().noneMatch(cr -> cr.getMargin() < 0));
        assertEquals(List.of("All VOLTAGE Cnecs are secure."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testOneHighVoltagePreventiveCnec() {
        addVoltageCnec("vc", PREVENTIVE_INSTANT_ID, null, "VL1", 300., 350.);
        runVoltageMonitoring();
        assertEquals(Cnec.SecurityStatus.HIGH_CONSTRAINT, voltageMonitoringResult.getStatus());
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc")).anyMatch(cr -> cr.getMargin() < 0));
        assertEquals(List.of("Some VOLTAGE Cnecs are not secure:",
            "Network element VL1 at state preventive has a min voltage of 400.0 kV and a max voltage of 400.0 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testOneLowVoltagePreventiveCnec() {
        addVoltageCnec("vc", PREVENTIVE_INSTANT_ID, null, "VL1", 401., 410.);
        runVoltageMonitoring();
        assertEquals(Cnec.SecurityStatus.LOW_CONSTRAINT, voltageMonitoringResult.getStatus());
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc")).anyMatch(cr -> cr.getMargin() < 0));
        assertEquals(List.of("Some VOLTAGE Cnecs are not secure:",
            "Network element VL1 at state preventive has a min voltage of 400.0 kV and a max voltage of 400.0 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testPrevNetworkActionMakesVoltageLowOn1Cnec() {
        when(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState())).thenReturn(Set.of(naOpenL1));

        // Before NA, VL2 = 386kV, VL3 = 393kV
        // After applying NA, VL2 = 368kV, VL3 = 383kV
        addVoltageCnec("vc1", PREVENTIVE_INSTANT_ID, null, "VL2", 375., null);
        addVoltageCnec("vc2", PREVENTIVE_INSTANT_ID, null, "VL3", 380., 400.);

        runVoltageMonitoring();
        assertEquals(Cnec.SecurityStatus.LOW_CONSTRAINT, voltageMonitoringResult.getStatus());
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc1")).anyMatch(cr -> cr.getMargin() < 0));
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc2")).noneMatch(cr -> cr.getMargin() < 0));
        assertEquals(List.of("Some VOLTAGE Cnecs are not secure:",
            "Network element VL2 at state preventive has a min voltage of 368.12 kV and a max voltage of 368.12 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testPrevNetworkActionMakesVoltageLowOn2Cnecs() {
        when(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState())).thenReturn(Set.of(naOpenL1));

        // Before NA, VL2 = 386kV, VL3 = 393kV
        // After applying NA, VL2 = 368kV, VL3 = 383kV
        addVoltageCnec("vc1", PREVENTIVE_INSTANT_ID, null, "VL2", 375., null);
        addVoltageCnec("vc2", PREVENTIVE_INSTANT_ID, null, "VL3", 385., 400.);

        runVoltageMonitoring();
        assertEquals(Cnec.SecurityStatus.LOW_CONSTRAINT, voltageMonitoringResult.getStatus());
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc1")).anyMatch(cr -> cr.getMargin() < 0));
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc2")).anyMatch(cr -> cr.getMargin() < 0));
        assertEquals(List.of("Some VOLTAGE Cnecs are not secure:",
            "Network element VL2 at state preventive has a min voltage of 368.12 kV and a max voltage of 368.12 kV.", "Network element VL3 at state preventive has a min voltage of 383.19 kV and a max voltage of 383.19 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testPrevNetworkActionMakesVoltageHighOn1Cnec() {
        when(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState())).thenReturn(Set.of(naOpenL2));

        // Before NA, VL2 = 386kV, VL3 = 393kV
        // After applying NA, VL2 = 368kV, VL3 = 400kV
        addVoltageCnec("vc1", PREVENTIVE_INSTANT_ID, null, "VL2", null, 395.);
        addVoltageCnec("vc2", PREVENTIVE_INSTANT_ID, null, "VL3", 375., 395.);

        runVoltageMonitoring();
        assertEquals(Cnec.SecurityStatus.HIGH_CONSTRAINT, voltageMonitoringResult.getStatus());
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc1")).noneMatch(cr -> cr.getMargin() < 0));
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc2")).anyMatch(cr -> cr.getMargin() < 0));
        assertEquals(List.of("Some VOLTAGE Cnecs are not secure:",
            "Network element VL3 at state preventive has a min voltage of 400.0 kV and a max voltage of 400.0 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testPrevNetworkActionMakesHighAndLowConstraints() {
        when(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState())).thenReturn(Set.of(naOpenL2));

        // Before NA, VL2 = 386kV, VL3 = 393kV
        // After applying NA, VL2 = 368kV, VL3 = 400kV
        addVoltageCnec("vc1", PREVENTIVE_INSTANT_ID, null, "VL2", 375., 395.);
        addVoltageCnec("vc2", PREVENTIVE_INSTANT_ID, null, "VL3", 375., 395.);

        runVoltageMonitoring();
        assertEquals(Cnec.SecurityStatus.HIGH_AND_LOW_CONSTRAINTS, voltageMonitoringResult.getStatus());
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc1")).anyMatch(cr -> cr.getMargin() < 0));
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc2")).anyMatch(cr -> cr.getMargin() < 0));
        assertEquals(List.of("Some VOLTAGE Cnecs are not secure:",
            "Network element VL2 at state preventive has a min voltage of 368.12 kV and a max voltage of 368.12 kV.", "Network element VL3 at state preventive has a min voltage of 400.0 kV and a max voltage of 400.0 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testPrevPstMakesVoltageLowOn1Cnec() {
        when(raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState())).thenReturn(Set.of(pst));
        when(raoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), pst)).thenReturn(-20.);

        // Before RA, VL2 = 386kV, VL3 = 393kV
        // After applying NA, VL2 = 379kV, VL3 = 387kV
        addVoltageCnec("vc1", PREVENTIVE_INSTANT_ID, null, "VL2", 380., 395.);
        addVoltageCnec("vc2", PREVENTIVE_INSTANT_ID, null, "VL3", 375., 395.);

        runVoltageMonitoring();
        assertEquals(Cnec.SecurityStatus.LOW_CONSTRAINT, voltageMonitoringResult.getStatus());
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc1")).anyMatch(cr -> cr.getMargin() < 0));
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc2")).noneMatch(cr -> cr.getMargin() < 0));
        assertEquals(List.of("Some VOLTAGE Cnecs are not secure:",
            "Network element VL2 at state preventive has a min voltage of 379.35 kV and a max voltage of 379.35 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testCurativeStatesConstraints() {
        // In this test, L1 and L2 are open by contingencies
        // we don't apply any remedial action, the raoresult remedial action set is empty
        // the network after applying the contingencies is unbalanced making the load flow diverge in state "coL1L2 - curative"
        addVoltageCnec("vc1", CURATIVE_INSTANT_ID, "coL1", "VL2", 375., 395.);
        addVoltageCnec("vc2", CURATIVE_INSTANT_ID, "coL2", "VL3", 375., 395.);
        addVoltageCnec("vc1b", CURATIVE_INSTANT_ID, "coL1L2", "VL2", 375., 395.);
        addVoltageCnec("vc2b", CURATIVE_INSTANT_ID, "coL1L2", "VL3", 375., 395.);

        runVoltageMonitoring();
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc1")).allMatch(cr -> cr.getMargin() < 0));
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc2")).allMatch(cr -> cr.getMargin() < 0));
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc2b")).allMatch(cr -> Double.isNaN(cr.getMargin())));
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc1b")).allMatch(cr -> Double.isNaN(cr.getMargin())));
        assertEquals(List.of("VOLTAGE monitoring failed due to a load flow divergence or an inconsistency in the crac or in the parameters."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testCurativeStatesConstraintsSolvedByCras() {
        // Same as previous case, except here applied CRAs revert the contingencies
        addVoltageCnec("vc1", CURATIVE_INSTANT_ID, "coL1", "VL2", 375., 395.);
        addVoltageCnec("vc2", CURATIVE_INSTANT_ID, "coL2", "VL3", 375., 395.);
        addVoltageCnec("vc1b", CURATIVE_INSTANT_ID, "coL1L2", "VL2", 375., 395.);
        addVoltageCnec("vc2b", CURATIVE_INSTANT_ID, "coL1L2", "VL3", 375., 395.);

        when(raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("coL1"), curativeInstant))).thenReturn(Set.of(naCloseL1));
        when(raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("coL2"), curativeInstant))).thenReturn(Set.of(naCloseL2));
        when(raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("coL1L2"), curativeInstant))).thenReturn(Set.of(naCloseL1, naCloseL2));

        runVoltageMonitoring();
        assertEquals(Cnec.SecurityStatus.SECURE, voltageMonitoringResult.getStatus());
        assertTrue(voltageMonitoringResult.getCnecResults().stream().noneMatch(cr -> cr.getMargin() < 0));
        assertEquals(List.of("All VOLTAGE Cnecs are secure."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testCurPstMakesVoltageLowOn1Cnec() {
        crac.newContingency().withId("co3").withContingencyElement("L3", ContingencyElementType.LINE).add();
        addVoltageCnec("vc", CURATIVE_INSTANT_ID, "co3", "VL2", 375., 395.);
        State state = crac.getState(crac.getContingency("co3"), curativeInstant);
        when(raoResult.getActivatedRangeActionsDuringState(state)).thenReturn(Set.of(pst));
        when(raoResult.getOptimizedSetPointOnState(state, pst)).thenReturn(-20.);

        runVoltageMonitoring();
        assertEquals(Cnec.SecurityStatus.LOW_CONSTRAINT, voltageMonitoringResult.getStatus());
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("vc")).allMatch(cr -> cr.getMargin() < 0));
        assertEquals(List.of("Some VOLTAGE Cnecs are not secure:",
            "Network element VL2 at state co3 - curative has a min voltage of 368.12 kV and a max voltage of 368.12 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testMultipleVoltageValuesPerVoltageLevel() {
        network = Network.read("ieee14.xiidm", getClass().getResourceAsStream("/ieee14.xiidm"));
        // VL45 : Min = 141.07, Max = 146.86
        // VL46 : Min = 140.96, Max = 147.66

        addVoltageCnec("VL45", PREVENTIVE_INSTANT_ID, null, "VL45", 145., 150.);
        addVoltageCnec("VL46", PREVENTIVE_INSTANT_ID, null, "VL46", 140., 145.);

        runVoltageMonitoring();

        // VL46 HIGH and VL45 LOW
        assertEquals(Cnec.SecurityStatus.HIGH_AND_LOW_CONSTRAINTS, voltageMonitoringResult.getStatus());
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("VL45")).anyMatch(cr -> cr.getMargin() < 0));
        assertTrue(voltageMonitoringResult.getCnecResults().stream().filter(cnecResult -> cnecResult.getCnec().getId().equals("VL46")).anyMatch(cr -> cr.getMargin() < 0));
        assertEquals(List.of(
                "Some VOLTAGE Cnecs are not secure:",
                "Network element VL45 at state preventive has a min voltage of 141.07 kV and a max voltage of 146.86 kV.",
                "Network element VL46 at state preventive has a min voltage of 140.96 kV and a max voltage of 147.66 kV."),
            voltageMonitoringResult.printConstraints());
    }

    public void setUpCracFactory(String networkFileName) {
        network = Network.read(networkFileName, getClass().getResourceAsStream("/" + networkFileName));
        crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
    }

    public void mockPreventiveState() {
        vcPrev = addVoltageCnec("vcPrev", PREVENTIVE_INSTANT_ID, null, "VL3", 375., 395.);
        crac.newNetworkAction()
            .withId("Open L1 - 1")
            .newTerminalsConnectionAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec(vcPrev.getId()).add()
            .add();
    }

    @Test
    void testDivergentLoadFlowDuringInitialLoadFlow() {
        setUpCracFactory("networkKO.xiidm");
        mockPreventiveState();

        runVoltageMonitoring();

        assertEquals(Cnec.SecurityStatus.FAILURE, voltageMonitoringResult.getStatus());
        assertEquals(0, voltageMonitoringResult.getAppliedRas().get(crac.getPreventiveState()).size());
    }

    @Test
    void testDivergentLoadFlowAfterApplicationOfRemedialAction() {
        setUpCracFactory("network2.xiidm");

        crac.newContingency().withId("co").withContingencyElement("L1", ContingencyElementType.LINE).add();
        VoltageCnec vc = addVoltageCnec("vc", CURATIVE_INSTANT_ID, "co", "VL1", 390., 399.);

        String networkActionName = "Open L2 - 1";
        crac.newNetworkAction()
            .withId(networkActionName)
            .newTerminalsConnectionAction().withNetworkElement("L2").withActionType(ActionType.OPEN).add()
            .newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec(vc.getId()).add()
            .add();

        runVoltageMonitoring();

        assertEquals(Cnec.SecurityStatus.FAILURE, voltageMonitoringResult.getStatus());
        assertEquals(1, voltageMonitoringResult.getAppliedRas().size());
    }

    @Test
    void testSecureInitialSituationWithAvailableRemedialActions() {
        setUpCracFactory("network.xiidm");
        mockPreventiveState();
        //Add contingency that doesn't break the loadflow
        crac.newContingency().withId("coL3").withContingencyElement("L3", ContingencyElementType.LINE).add();
        VoltageCnec vc = addVoltageCnec("vcCur1", CURATIVE_INSTANT_ID, "coL3", "VL3", 375., 395.);
        crac.newNetworkAction()
            .withId("Open L1 - 2")
            .newTerminalsConnectionAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec(vc.getId()).add()
            .add();

        runVoltageMonitoring();
        assertEquals(Cnec.SecurityStatus.SECURE, voltageMonitoringResult.getStatus());
        assertTrue(voltageMonitoringResult.getAppliedRas().values().stream().allMatch(appliedRasByState -> appliedRasByState.size() == 0));
    }

    @Test
    void testUnsecureInitialSituationWithoutAvailableRemedialActions() {
        setUpCracFactory("network.xiidm");
        vcPrev = addVoltageCnec("vcPrev", PREVENTIVE_INSTANT_ID, null, "VL1", 390., 399.);

        runVoltageMonitoring();
        assertTrue(voltageMonitoringResult.getAppliedRas().values().stream().allMatch(appliedRasByState -> appliedRasByState.size() == 0));
        assertEquals(Cnec.SecurityStatus.HIGH_CONSTRAINT, voltageMonitoringResult.getStatus());
    }

    @Test
    void testUnsecureInitialSituationWithRemedialActionThatDoNotSolveVC() {
        setUpCracFactory("network.xiidm");
        vcPrev = addVoltageCnec("vcPrev", PREVENTIVE_INSTANT_ID, null, "VL1", 390., 399.);
        crac.newNetworkAction()
            .withId("Open L1 - 1")
            .newTerminalsConnectionAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec(vcPrev.getId()).add()
            .add();

        crac.newContingency().withId("co").withContingencyElement("L1", ContingencyElementType.LINE).add();
        VoltageCnec vc = addVoltageCnec("vc", CURATIVE_INSTANT_ID, "co", "VL1", 390., 399.);
        String networkActionName = "Open L1 - 2";
        RemedialAction<?> networkAction = crac.newNetworkAction()
            .withId(networkActionName)
            .newTerminalsConnectionAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec(vc.getId()).add()
            .add();

        runVoltageMonitoring();

        assertEquals(0, voltageMonitoringResult.getAppliedRas().get(crac.getPreventiveState()).size());
        assertEquals(Set.of(networkAction), voltageMonitoringResult.getAppliedRas().get(crac.getState("co", curativeInstant)));
        assertEquals(Cnec.SecurityStatus.HIGH_CONSTRAINT, voltageMonitoringResult.getStatus());
    }

    @Test
    void testUnsecureInitialSituationWithRemedialActionThatDoNotSolveVCBis() {
        setUpCracFactory("network.xiidm");
        vcPrev = addVoltageCnec("vcPrev", PREVENTIVE_INSTANT_ID, null, "VL1", 440., 450.);
        crac.newNetworkAction()
            .withId("Open L1 - 1")
            .newTerminalsConnectionAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec(vcPrev.getId()).add()
            .add();

        crac.newContingency().withId("co").withContingencyElement("L1", ContingencyElementType.LINE).add();
        VoltageCnec vc = addVoltageCnec("vc", CURATIVE_INSTANT_ID, "co", "VL1", 440., 450.);
        String networkActionName = "Open L1 - 2";
        RemedialAction<?> networkAction = crac.newNetworkAction()
            .withId(networkActionName)
            .newTerminalsConnectionAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec(vc.getId()).add()
            .add();

        runVoltageMonitoring();

        assertEquals(0, voltageMonitoringResult.getAppliedRas().get(crac.getPreventiveState()).size());
        assertEquals(Set.of(networkAction), voltageMonitoringResult.getAppliedRas().get(crac.getState("co", curativeInstant)));
        assertEquals(Cnec.SecurityStatus.LOW_CONSTRAINT, voltageMonitoringResult.getStatus());
    }

    @Test
    void testUnsecureInitialSituationWithRemedialActionThatSolveVC() {
        setUpCracFactory("network3.xiidm");

        crac.newContingency().withId("co").withContingencyElement("L1", ContingencyElementType.LINE).add();
        VoltageCnec vc = addVoltageCnec("vc", CURATIVE_INSTANT_ID, "co", "VL2", 385., 400.);
        String networkActionName = "Close L1 - 1";
        RemedialAction<?> networkAction = crac.newNetworkAction()
            .withId(networkActionName)
            .newTerminalsConnectionAction().withNetworkElement("L1").withActionType(ActionType.CLOSE).add()
            .newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec(vc.getId()).add()
            .add();

        runVoltageMonitoring();

        assertEquals(Cnec.SecurityStatus.SECURE, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(networkAction), voltageMonitoringResult.getAppliedRas().get(crac.getState("co", curativeInstant)));
    }

    @Test
    void testUnsecureInitialSituationWithRemedialActionThatSolveVCBis() {
        setUpCracFactory("network3.xiidm");

        crac.newContingency().withId("co").withContingencyElement("L1", ContingencyElementType.LINE).add();
        VoltageCnec vc = addVoltageCnec("vc", CURATIVE_INSTANT_ID, "co", "VL2", 340., 350.);
        String networkActionName = "Close L1 - 1";
        RemedialAction<?> networkAction = crac.newNetworkAction()
            .withId(networkActionName)
            .newTerminalsConnectionAction().withNetworkElement("L2").withActionType(ActionType.OPEN).add()
            .newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec(vc.getId()).add()
            .add();

        runVoltageMonitoring();

        assertEquals(Cnec.SecurityStatus.SECURE, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(networkAction), voltageMonitoringResult.getAppliedRas().get(crac.getState("co", curativeInstant)));
    }

    @Test
    void testVoltageMonitoringWithNonValidContingency() {
        setUpCracFactory("network.xiidm");

        // Type BATTERY is put in purpose to simulate a contingency valid scenario
        crac.newContingency().withId("coL3").withContingencyElement("L1", ContingencyElementType.BATTERY).add();
        addVoltageCnec("vc", CURATIVE_INSTANT_ID, "coL3", "VL2", 340., 350.);

        addVoltageCnec("vcPrev", PREVENTIVE_INSTANT_ID, null, "VL1", 390., 399.);

        crac.newContingency().withId("co").withContingencyElement("L1", ContingencyElementType.LINE).add();

        runVoltageMonitoring();
        assertEquals(Cnec.SecurityStatus.FAILURE, voltageMonitoringResult.getStatus());
        assertEquals(2, voltageMonitoringResult.getCnecResults().size());

        Optional<CnecResult> vcCnecOpt = voltageMonitoringResult.getCnecResults().stream().filter(cr -> cr.getId().equals("vc")).findFirst();
        CnecValue vcCnecOptCnecValue = vcCnecOpt.get().getValue();
        Cnec.SecurityStatus vcCnecOptSecurityStatus = vcCnecOpt.get().getCnecSecurityStatus();
        double vcMargin = vcCnecOpt.get().getMargin();

        assertTrue(vcCnecOptCnecValue instanceof VoltageCnecValue);
        assertEquals(Double.NaN, ((VoltageCnecValue) vcCnecOptCnecValue).minValue());
        assertEquals(Double.NaN, ((VoltageCnecValue) vcCnecOptCnecValue).maxValue());
        assertEquals(Cnec.SecurityStatus.FAILURE, vcCnecOptSecurityStatus);
        assertEquals(Double.NaN, vcMargin);

        Optional<CnecResult> vcPrevCnecOpt = voltageMonitoringResult.getCnecResults().stream().filter(cr -> cr.getId().equals("vcPrev")).findFirst();
        CnecValue vcPrevCnecOptCnecValue = vcPrevCnecOpt.get().getValue();
        Cnec.SecurityStatus vcPrevCnecOptSecurityStatus = vcPrevCnecOpt.get().getCnecSecurityStatus();
        double vcPrevMargin = vcPrevCnecOpt.get().getMargin();

        assertTrue(vcPrevCnecOptCnecValue instanceof VoltageCnecValue);
        assertEquals(400., ((VoltageCnecValue) vcPrevCnecOptCnecValue).minValue(), 0.01);
        assertEquals(400., ((VoltageCnecValue) vcPrevCnecOptCnecValue).maxValue(), 0.01);
        assertEquals(Cnec.SecurityStatus.HIGH_CONSTRAINT, vcPrevCnecOptSecurityStatus);
        assertEquals(-1.0, vcPrevMargin, 0.01);
    }

    @Test
    void testWithRaoResultUpdate() {
        setUpCracFactory("network.xiidm");
        VoltageCnec vcPrev = addVoltageCnec("vcPrev", PREVENTIVE_INSTANT_ID, null, "VL1", 400., 450.);

        crac.newContingency().withId("co").withContingencyElement("L1", ContingencyElementType.LINE).add();
        VoltageCnec vcCur = addVoltageCnec("vc", CURATIVE_INSTANT_ID, "co", "VL1", 390., 399.);

        NetworkAction networkAction = crac.newNetworkAction()
            .withId("Open L1 - 1")
            .newTerminalsConnectionAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec(vcCur.getId()).add()
            .add();

        when(raoResult.getComputationStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(raoResult.isSecure()).thenReturn(true);

        MonitoringInput monitoringInput = new MonitoringInput.MonitoringInputBuilder().withCrac(crac).withNetwork(network).withRaoResult(raoResult).withPhysicalParameter(PhysicalParameter.VOLTAGE).build();
        RaoResult raoResultWithVoltageMonitoring = Monitoring.runVoltageAndUpdateRaoResult("OpenLoadFlow", loadFlowParameters, 1, monitoringInput);

        assertFalse(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.VOLTAGE));
        assertThrows(OpenRaoException.class, () -> raoResultWithVoltageMonitoring.getMinVoltage(crac.getPreventiveState().getInstant(), vcPrev, Unit.KILOVOLT));
        assertEquals(400., raoResultWithVoltageMonitoring.getMinVoltage(crac.getInstant(CURATIVE_INSTANT_ID), vcCur, Unit.KILOVOLT));
        assertEquals(400, raoResultWithVoltageMonitoring.getMaxVoltage(crac.getInstant(CURATIVE_INSTANT_ID), vcCur, Unit.KILOVOLT));
        assertEquals(-1., raoResultWithVoltageMonitoring.getMargin(crac.getInstant(CURATIVE_INSTANT_ID), vcCur, Unit.KILOVOLT));
        assertEquals(Set.of(networkAction), raoResultWithVoltageMonitoring.getActivatedNetworkActionsDuringState(crac.getState("co", crac.getInstant(CURATIVE_INSTANT_ID))));
        assertTrue(raoResultWithVoltageMonitoring.isActivatedDuringState(crac.getState("co", crac.getInstant(CURATIVE_INSTANT_ID)), networkAction));
        assertEquals(ComputationStatus.DEFAULT, raoResultWithVoltageMonitoring.getComputationStatus());
        assertFalse(raoResultWithVoltageMonitoring.isSecure(crac.getInstant(CURATIVE_INSTANT_ID), PhysicalParameter.VOLTAGE));
        assertFalse(raoResultWithVoltageMonitoring.isSecure(PhysicalParameter.VOLTAGE));
        assertFalse(raoResultWithVoltageMonitoring.isSecure());
    }

    @Test
    void testWithComputationManager() throws IOException, InterruptedException {
        setUpCracFactory("network.xiidm");
        addVoltageCnec("vcPrev", PREVENTIVE_INSTANT_ID, null, "VL1", 400., 450.);

        crac.newContingency().withId("co").withContingencyElement("L1", ContingencyElementType.LINE).add();
        final VoltageCnec vcCur = addVoltageCnec("vc", CURATIVE_INSTANT_ID, "co", "VL1", 390., 399.);

        crac.newNetworkAction()
            .withId("Open L1 - 1")
            .newTerminalsConnectionAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec(vcCur.getId()).add()
            .add();

        when(raoResult.getComputationStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(raoResult.isSecure()).thenReturn(true);

        final MonitoringInput monitoringInput = new MonitoringInput.MonitoringInputBuilder().withCrac(crac).withNetwork(network).withRaoResult(raoResult).withPhysicalParameter(PhysicalParameter.VOLTAGE).build();
        final AtomicInteger firstReferenceValue = new AtomicInteger(0);
        final AtomicInteger secondReferenceValue = new AtomicInteger(3); // Loadflow is expected to be run 3 times
        final ComputationManager computationManager = MonitoringTestUtil.getComputationManager(firstReferenceValue, secondReferenceValue);

        final RaoResult raoResultWithVoltageMonitoring = Monitoring.runVoltageAndUpdateRaoResult("OpenLoadFlow", loadFlowParameters, computationManager, 1, monitoringInput);

        assertEquals(3, firstReferenceValue.get());
        assertEquals(0, secondReferenceValue.get());
        assertFalse(raoResultWithVoltageMonitoring.isSecure());
    }
}
