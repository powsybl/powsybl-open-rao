/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.voltage_monitoring;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_io_json.JsonImport;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.monitoring.voltage_monitoring.json.VoltageMonitoringResultImporter;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.*;
import static com.farao_community.farao.commons.Unit.KILOVOLT;
import static com.farao_community.farao.data.crac_api.Instant.*;
import static com.farao_community.farao.data.crac_api.Instant.CURATIVE;
import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static com.farao_community.farao.data.crac_api.cnec.Side.RIGHT;
import static com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult.Status.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class VoltageMonitoringTest {
    private static final double VOLTAGE_TOLERANCE = 0.5;
    private static final double DOUBLE_TOLERANCE = 0.1;

    private Network network;
    private Crac crac;
    private RaoResult raoResult;
    private LoadFlowParameters loadFlowParameters;
    private VoltageMonitoringResult voltageMonitoringResult;
    private NetworkAction naOpenL1;
    private NetworkAction naCloseL1;
    private NetworkAction naOpenL2;
    private NetworkAction naCloseL2;
    private PstRangeAction pst;
    private VoltageCnec vcPrev;

    @BeforeEach
    public void setUp() {
        network = Network.read("network.xiidm", getClass().getResourceAsStream("/network.xiidm"));
        crac = CracFactory.findDefault().create("test-crac");

        naOpenL1 = (NetworkAction) crac.newNetworkAction()
            .withId("Open L1")
            .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        naCloseL1 = (NetworkAction) crac.newNetworkAction()
            .withId("Close L1")
            .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.CLOSE).add()
            .newOnInstantUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        naOpenL2 = (NetworkAction) crac.newNetworkAction()
            .withId("Open L2")
            .newTopologicalAction().withNetworkElement("L2").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        naCloseL2 = (NetworkAction) crac.newNetworkAction()
            .withId("Close L2")
            .newTopologicalAction().withNetworkElement("L2").withActionType(ActionType.CLOSE).add()
            .newOnInstantUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        pst = (PstRangeAction) crac.newPstRangeAction()
            .withId("pst")
            .withNetworkElement("PS1")
            .withInitialTap(2).withTapToAngleConversionMap(Map.of(1, -20., 2, 0., 3, 20.))
            .newTapRange().withMinTap(1).withMaxTap(3).withRangeType(RangeType.ABSOLUTE).add()
            .newOnInstantUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        crac.newContingency().withId("coL1").withNetworkElement("L1").add();
        crac.newContingency().withId("coL2").withNetworkElement("L2").add();
        crac.newContingency().withId("coL1L2").withNetworkElement("L1").withNetworkElement("L2").add();

        loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(false);

        raoResult = Mockito.mock(RaoResult.class);
        when(raoResult.getActivatedNetworkActionsDuringState(any())).thenReturn(Collections.emptySet());
        when(raoResult.getActivatedRangeActionsDuringState(any())).thenReturn(Collections.emptySet());
    }

    private VoltageCnec addVoltageCnec(String id, Instant instant, String contingency, String networkElement, Double min, Double max) {
        return crac.newVoltageCnec()
            .withId(id)
            .withInstant(instant)
            .withContingency(contingency)
            .withNetworkElement(networkElement)
            .withMonitored()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(min).withMax(max).add()
            .add();
    }

    private void runVoltageMonitoring() {
        voltageMonitoringResult = new VoltageMonitoring(crac, network, raoResult).run("OpenLoadFlow", loadFlowParameters, 2);
    }

    @Test
    void testOneSecurePreventiveCnec() {
        addVoltageCnec("vc", Instant.PREVENTIVE, null, "VL1", null, 500.);
        runVoltageMonitoring();
        assertEquals(400., voltageMonitoringResult.getMinVoltage("vc"), VOLTAGE_TOLERANCE);
        assertEquals(400., voltageMonitoringResult.getMaxVoltage("vc"), VOLTAGE_TOLERANCE);
        assertEquals(SECURE, voltageMonitoringResult.getStatus());
        assertTrue(voltageMonitoringResult.getConstrainedElements().isEmpty());
        assertEquals(List.of("All voltage CNECs are secure."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testTwoSecurePreventiveCnecs() {
        addVoltageCnec("vc1", Instant.PREVENTIVE, null, "VL1", 400., 400.);
        addVoltageCnec("vc2", Instant.PREVENTIVE, null, "VL2", 385., null);
        runVoltageMonitoring();
        assertEquals(400., voltageMonitoringResult.getMinVoltage("vc1"), VOLTAGE_TOLERANCE);
        assertEquals(386., voltageMonitoringResult.getMaxVoltage("vc2"), VOLTAGE_TOLERANCE);
        assertEquals(SECURE, voltageMonitoringResult.getStatus());
        assertTrue(voltageMonitoringResult.getConstrainedElements().isEmpty());
        assertEquals(List.of("All voltage CNECs are secure."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testOneHighVoltagePreventiveCnec() {
        addVoltageCnec("vc", Instant.PREVENTIVE, null, "VL1", 300., 350.);
        runVoltageMonitoring();
        assertEquals(400., voltageMonitoringResult.getMinVoltage("vc"), VOLTAGE_TOLERANCE);
        assertEquals(400., voltageMonitoringResult.getMaxVoltage("vc"), VOLTAGE_TOLERANCE);
        assertEquals(HIGH_VOLTAGE_CONSTRAINT, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(crac.getVoltageCnec("vc")), voltageMonitoringResult.getConstrainedElements());
        assertEquals(List.of("Some voltage CNECs are not secure:",
            "Network element VL1 at state preventive has a voltage of 400 - 400 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testOneLowVoltagePreventiveCnec() {
        addVoltageCnec("vc", Instant.PREVENTIVE, null, "VL1", 401., 410.);
        runVoltageMonitoring();
        assertEquals(400., voltageMonitoringResult.getMinVoltage("vc"), VOLTAGE_TOLERANCE);
        assertEquals(400., voltageMonitoringResult.getMaxVoltage("vc"), VOLTAGE_TOLERANCE);
        assertEquals(LOW_VOLTAGE_CONSTRAINT, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(crac.getVoltageCnec("vc")), voltageMonitoringResult.getConstrainedElements());
        assertEquals(List.of("Some voltage CNECs are not secure:",
            "Network element VL1 at state preventive has a voltage of 400 - 400 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testPrevNetworkActionMakesVoltageLowOn1Cnec() {
        when(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState())).thenReturn(Set.of(naOpenL1));

        // Before NA, VL2 = 386kV, VL3 = 393kV
        // After applying NA, VL2 = 368kV, VL3 = 383kV
        VoltageCnec vc1 = addVoltageCnec("vc1", Instant.PREVENTIVE, null, "VL2", 375., null);
        VoltageCnec vc2 = addVoltageCnec("vc2", Instant.PREVENTIVE, null, "VL3", 380., 400.);

        runVoltageMonitoring();
        assertEquals(LOW_VOLTAGE_CONSTRAINT, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(vc1), voltageMonitoringResult.getConstrainedElements());
        assertEquals(368., voltageMonitoringResult.getMinVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(368., voltageMonitoringResult.getMaxVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(383., voltageMonitoringResult.getMinVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(383., voltageMonitoringResult.getMaxVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(List.of("Some voltage CNECs are not secure:",
            "Network element VL2 at state preventive has a voltage of 368 - 368 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testPrevNetworkActionMakesVoltageLowOn2Cnecs() {
        when(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState())).thenReturn(Set.of(naOpenL1));

        // Before NA, VL2 = 386kV, VL3 = 393kV
        // After applying NA, VL2 = 368kV, VL3 = 383kV
        VoltageCnec vc1 = addVoltageCnec("vc1", Instant.PREVENTIVE, null, "VL2", 375., null);
        VoltageCnec vc2 = addVoltageCnec("vc2", Instant.PREVENTIVE, null, "VL3", 385., 400.);

        runVoltageMonitoring();
        assertEquals(LOW_VOLTAGE_CONSTRAINT, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(vc1, vc2), voltageMonitoringResult.getConstrainedElements());
        assertEquals(368., voltageMonitoringResult.getMinVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(368., voltageMonitoringResult.getMaxVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(383., voltageMonitoringResult.getMinVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(383., voltageMonitoringResult.getMaxVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(List.of("Some voltage CNECs are not secure:",
            "Network element VL2 at state preventive has a voltage of 368 - 368 kV.", "Network element VL3 at state preventive has a voltage of 383 - 383 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testPrevNetworkActionMakesVoltageHighOn1Cnec() {
        when(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState())).thenReturn(Set.of(naOpenL2));

        // Before NA, VL2 = 386kV, VL3 = 393kV
        // After applying NA, VL2 = 368kV, VL3 = 400kV
        VoltageCnec vc1 = addVoltageCnec("vc1", Instant.PREVENTIVE, null, "VL2", null, 395.);
        VoltageCnec vc2 = addVoltageCnec("vc2", Instant.PREVENTIVE, null, "VL3", 375., 395.);

        runVoltageMonitoring();
        assertEquals(HIGH_VOLTAGE_CONSTRAINT, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(vc2), voltageMonitoringResult.getConstrainedElements());
        assertEquals(368., voltageMonitoringResult.getMinVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(368., voltageMonitoringResult.getMaxVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(400., voltageMonitoringResult.getMinVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(400., voltageMonitoringResult.getMaxVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(List.of("Some voltage CNECs are not secure:",
            "Network element VL3 at state preventive has a voltage of 400 - 400 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testPrevNetworkActionMakesHighAndLowConstraints() {
        when(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState())).thenReturn(Set.of(naOpenL2));

        // Before NA, VL2 = 386kV, VL3 = 393kV
        // After applying NA, VL2 = 368kV, VL3 = 400kV
        VoltageCnec vc1 = addVoltageCnec("vc1", Instant.PREVENTIVE, null, "VL2", 375., 395.);
        VoltageCnec vc2 = addVoltageCnec("vc2", Instant.PREVENTIVE, null, "VL3", 375., 395.);

        runVoltageMonitoring();
        assertEquals(HIGH_AND_LOW_VOLTAGE_CONSTRAINTS, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(vc1, vc2), voltageMonitoringResult.getConstrainedElements());
        assertEquals(368., voltageMonitoringResult.getMinVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(368., voltageMonitoringResult.getMaxVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(400., voltageMonitoringResult.getMinVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(400., voltageMonitoringResult.getMaxVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(List.of("Some voltage CNECs are not secure:",
            "Network element VL2 at state preventive has a voltage of 368 - 368 kV.", "Network element VL3 at state preventive has a voltage of 400 - 400 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testPrevPstMakesVoltageLowOn1Cnec() {
        when(raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState())).thenReturn(Set.of(pst));
        when(raoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), pst)).thenReturn(-20.);

        // Before RA, VL2 = 386kV, VL3 = 393kV
        // After applying NA, VL2 = 379kV, VL3 = 387kV
        VoltageCnec vc1 = addVoltageCnec("vc1", Instant.PREVENTIVE, null, "VL2", 380., 395.);
        VoltageCnec vc2 = addVoltageCnec("vc2", Instant.PREVENTIVE, null, "VL3", 375., 395.);

        runVoltageMonitoring();
        assertEquals(LOW_VOLTAGE_CONSTRAINT, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(vc1), voltageMonitoringResult.getConstrainedElements());
        assertEquals(379., voltageMonitoringResult.getMinVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(379., voltageMonitoringResult.getMaxVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(387., voltageMonitoringResult.getMinVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(387., voltageMonitoringResult.getMaxVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(List.of("Some voltage CNECs are not secure:",
            "Network element VL2 at state preventive has a voltage of 379 - 379 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testCurativeStatesConstraints() {
        // In this test, L1 and L2 are open by contingencies
        // We define CNECs on these contingencies, one should have low voltage and one should have high voltage
        VoltageCnec vc1 = addVoltageCnec("vc1", Instant.CURATIVE, "coL1", "VL2", 375., 395.);
        VoltageCnec vc2 = addVoltageCnec("vc2", Instant.CURATIVE, "coL2", "VL3", 375., 395.);
        VoltageCnec vc1b = addVoltageCnec("vc1b", Instant.CURATIVE, "coL1L2", "VL2", 375., 395.);
        VoltageCnec vc2b = addVoltageCnec("vc2b", Instant.CURATIVE, "coL1L2", "VL3", 375., 395.);

        runVoltageMonitoring();
        assertEquals(HIGH_AND_LOW_VOLTAGE_CONSTRAINTS, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(vc1, vc2, vc2b), voltageMonitoringResult.getConstrainedElements());
        assertEquals(368., voltageMonitoringResult.getMinVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(368., voltageMonitoringResult.getMaxVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(400., voltageMonitoringResult.getMinVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(400., voltageMonitoringResult.getMaxVoltage(vc2), VOLTAGE_TOLERANCE);
        assertTrue(Double.isNaN(voltageMonitoringResult.getMinVoltage(vc1b)));
        assertTrue(Double.isNaN(voltageMonitoringResult.getMaxVoltage(vc1b)));
        assertEquals(400., voltageMonitoringResult.getMinVoltage(vc2b), VOLTAGE_TOLERANCE);
        assertEquals(400., voltageMonitoringResult.getMaxVoltage(vc2b), VOLTAGE_TOLERANCE);
        assertEquals(List.of("Some voltage CNECs are not secure:",
                "Network element VL2 at state coL1 - curative has a voltage of 368 - 368 kV.",
                "Network element VL3 at state coL2 - curative has a voltage of 400 - 400 kV.",
                "Network element VL3 at state coL1L2 - curative has a voltage of 400 - 400 kV."),
            voltageMonitoringResult.printConstraints());
    }

    @Test
    void testCurativeStatesConstraintsSolvedByCras() {
        // Same as previous case, except here applied CRAs revert the contingencies
        VoltageCnec vc1 = addVoltageCnec("vc1", Instant.CURATIVE, "coL1", "VL2", 375., 395.);
        VoltageCnec vc2 = addVoltageCnec("vc2", Instant.CURATIVE, "coL2", "VL3", 375., 395.);
        VoltageCnec vc1b = addVoltageCnec("vc1b", Instant.CURATIVE, "coL1L2", "VL2", 375., 395.);
        VoltageCnec vc2b = addVoltageCnec("vc2b", Instant.CURATIVE, "coL1L2", "VL3", 375., 395.);

        when(raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("coL1"), Instant.CURATIVE))).thenReturn(Set.of(naCloseL1));
        when(raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("coL2"), Instant.CURATIVE))).thenReturn(Set.of(naCloseL2));
        when(raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("coL1L2"), Instant.CURATIVE))).thenReturn(Set.of(naCloseL1, naCloseL2));

        runVoltageMonitoring();
        assertEquals(SECURE, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(), voltageMonitoringResult.getConstrainedElements());
        assertEquals(386., voltageMonitoringResult.getMinVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(386., voltageMonitoringResult.getMaxVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(393., voltageMonitoringResult.getMinVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(393., voltageMonitoringResult.getMaxVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(386., voltageMonitoringResult.getMinVoltage(vc1b), VOLTAGE_TOLERANCE);
        assertEquals(386., voltageMonitoringResult.getMaxVoltage(vc1b), VOLTAGE_TOLERANCE);
        assertEquals(393., voltageMonitoringResult.getMinVoltage(vc2b), VOLTAGE_TOLERANCE);
        assertEquals(393., voltageMonitoringResult.getMaxVoltage(vc2b), VOLTAGE_TOLERANCE);
        assertEquals(List.of("All voltage CNECs are secure."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testCurPstMakesVoltageLowOn1Cnec() {
        crac.newContingency().withId("co3").withNetworkElement("L3").add();

        VoltageCnec vc = addVoltageCnec("vc", Instant.CURATIVE, "co3", "VL2", 375., 395.);

        State state = crac.getState(crac.getContingency("co3"), Instant.CURATIVE);
        when(raoResult.getActivatedRangeActionsDuringState(state)).thenReturn(Set.of(pst));
        when(raoResult.getOptimizedSetPointOnState(state, pst)).thenReturn(-20.);

        runVoltageMonitoring();
        assertEquals(LOW_VOLTAGE_CONSTRAINT, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(vc), voltageMonitoringResult.getConstrainedElements());
        assertEquals(368., voltageMonitoringResult.getMinVoltage(vc), VOLTAGE_TOLERANCE);
        assertEquals(368., voltageMonitoringResult.getMaxVoltage(vc), VOLTAGE_TOLERANCE);
        assertEquals(List.of("Some voltage CNECs are not secure:",
            "Network element VL2 at state co3 - curative has a voltage of 368 - 368 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testMultipleVoltageValuesPerVoltageLevel() {
        network = Network.read("ieee14.xiidm", getClass().getResourceAsStream("/ieee14.xiidm"));
        // VL45 : Min = 144.38, Max = 148.41
        // VL46 : Min = 143.10, Max = 147.66

        VoltageCnec vc1 = addVoltageCnec("VL45", Instant.PREVENTIVE, null, "VL45", 145., 150.);
        VoltageCnec vc2 = addVoltageCnec("VL46", Instant.PREVENTIVE, null, "VL46", 140., 145.);

        runVoltageMonitoring();

        assertEquals(HIGH_AND_LOW_VOLTAGE_CONSTRAINTS, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(vc1, vc2), voltageMonitoringResult.getConstrainedElements());
        assertEquals(144.4, voltageMonitoringResult.getMinVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(148.4, voltageMonitoringResult.getMaxVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(143.1, voltageMonitoringResult.getMinVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(147.7, voltageMonitoringResult.getMaxVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(List.of(
                "Some voltage CNECs are not secure:",
                "Network element VL45 at state preventive has a voltage of 144 - 148 kV.",
                "Network element VL46 at state preventive has a voltage of 143 - 148 kV."),
            voltageMonitoringResult.printConstraints());
    }

    public void setUpCracFactory(String networkFileName) {
        network = Network.read(networkFileName, getClass().getResourceAsStream("/" + networkFileName));
        crac = CracFactory.findDefault().create("test-crac");
    }

    public void mockPreventiveState() {
        vcPrev = addVoltageCnec("vcPrev", Instant.PREVENTIVE, null, "VL45", 145., 150.);
        crac.newNetworkAction()
            .withId("Open L1 - 1")
            .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnVoltageConstraintUsageRule().withInstant(Instant.PREVENTIVE).withVoltageCnec(vcPrev.getId()).add()
            .add();
    }

    @Test
    void testDivergentLoadFlowDuringInitialLoadFlow() {
        setUpCracFactory("networkKO.xiidm");
        mockPreventiveState();

        runVoltageMonitoring();

        assertEquals(UNKNOWN, voltageMonitoringResult.getStatus());
        assertEquals(0, voltageMonitoringResult.getAppliedRas().size());
        assertEquals(Double.NaN, voltageMonitoringResult.getMinVoltage(vcPrev));
        assertEquals(Double.NaN, voltageMonitoringResult.getMaxVoltage(vcPrev));
    }

    @Test
    void testDivergentLoadFlowAfterApplicationOfRemedialAction() {
        setUpCracFactory("network2.xiidm");

        crac.newContingency().withId("co").withNetworkElement("L1").add();
        VoltageCnec vc = addVoltageCnec("vc", Instant.CURATIVE, "co", "VL1", 390., 399.);

        String networkActionName = "Open L2 - 1";
        crac.newNetworkAction()
            .withId(networkActionName)
            .newTopologicalAction().withNetworkElement("L2").withActionType(ActionType.OPEN).add()
            .newOnVoltageConstraintUsageRule().withInstant(Instant.CURATIVE).withVoltageCnec(vc.getId()).add()
            .add();

        runVoltageMonitoring();

        assertEquals(HIGH_VOLTAGE_CONSTRAINT, voltageMonitoringResult.getStatus());
        assertEquals(0, voltageMonitoringResult.getAppliedRas().size());
    }

    @Test
    void testSecureInitialSituationWithAvailableRemedialActions() {
        setUpCracFactory("network.xiidm");
        mockPreventiveState();
        //Add contingency that doesn't break the loadflow
        crac.newContingency().withId("coL3").withNetworkElement("L3").add();
        VoltageCnec vc = addVoltageCnec("vcCur1", Instant.CURATIVE, "coL3", "VL3", 375., 395.);
        crac.newNetworkAction()
            .withId("Open L1 - 2")
            .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnVoltageConstraintUsageRule().withInstant(Instant.PREVENTIVE).withVoltageCnec(vc.getId()).add()
            .add();

        runVoltageMonitoring();

        assertEquals(SECURE, voltageMonitoringResult.getStatus());
        assertEquals(0, voltageMonitoringResult.getAppliedRas().size());
    }

    @Test
    void testUnsecureInitialSituationWithoutAvailableRemedialActions() {
        setUpCracFactory("network.xiidm");
        vcPrev = addVoltageCnec("vcPrev", Instant.PREVENTIVE, null, "VL1", 390., 399.);

        runVoltageMonitoring();

        assertEquals(0, voltageMonitoringResult.getAppliedRas().size());
        assertEquals(HIGH_VOLTAGE_CONSTRAINT, voltageMonitoringResult.getStatus());
    }

    @Test
    void testUnsecureInitialSituationWithRemedialActionThatDoNotSolveVC() {
        setUpCracFactory("network.xiidm");
        vcPrev = addVoltageCnec("vcPrev", Instant.PREVENTIVE, null, "VL1", 390., 399.);
        crac.newNetworkAction()
            .withId("Open L1 - 1")
            .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnVoltageConstraintUsageRule().withInstant(Instant.PREVENTIVE).withVoltageCnec(vcPrev.getId()).add()
            .add();

        crac.newContingency().withId("co").withNetworkElement("L1").add();
        VoltageCnec vc = addVoltageCnec("vc", Instant.CURATIVE, "co", "VL1", 390., 399.);
        String networkActionName = "Open L1 - 2";
        RemedialAction<?> networkAction = crac.newNetworkAction()
            .withId(networkActionName)
            .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnVoltageConstraintUsageRule().withInstant(Instant.CURATIVE).withVoltageCnec(vc.getId()).add()
            .add();

        runVoltageMonitoring();

        assertEquals(1, voltageMonitoringResult.getAppliedRas().size());
        assertEquals(Set.of(networkAction), voltageMonitoringResult.getAppliedRas().get(crac.getState("co", Instant.CURATIVE)));
        assertEquals(HIGH_VOLTAGE_CONSTRAINT, voltageMonitoringResult.getStatus());
    }

    @Test
    void testUnsecureInitialSituationWithRemedialActionThatDoNotSolveVCBis() {
        setUpCracFactory("network.xiidm");
        vcPrev = addVoltageCnec("vcPrev", Instant.PREVENTIVE, null, "VL1", 440., 450.);
        crac.newNetworkAction()
            .withId("Open L1 - 1")
            .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnVoltageConstraintUsageRule().withInstant(Instant.PREVENTIVE).withVoltageCnec(vcPrev.getId()).add()
            .add();

        crac.newContingency().withId("co").withNetworkElement("L1").add();
        VoltageCnec vc = addVoltageCnec("vc", Instant.CURATIVE, "co", "VL1", 440., 450.);
        String networkActionName = "Open L1 - 2";
        RemedialAction<?> networkAction = crac.newNetworkAction()
            .withId(networkActionName)
            .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnVoltageConstraintUsageRule().withInstant(Instant.CURATIVE).withVoltageCnec(vc.getId()).add()
            .add();

        runVoltageMonitoring();

        assertEquals(1, voltageMonitoringResult.getAppliedRas().size());
        assertEquals(Set.of(networkAction), voltageMonitoringResult.getAppliedRas().get(crac.getState("co", Instant.CURATIVE)));
        assertEquals(LOW_VOLTAGE_CONSTRAINT, voltageMonitoringResult.getStatus());
    }

    @Test
    void testUnsecureInitialSituationWithRemedialActionThatSolveVC() {
        setUpCracFactory("network3.xiidm");

        crac.newContingency().withId("co").withNetworkElement("L1").add();
        VoltageCnec vc = addVoltageCnec("vc", Instant.CURATIVE, "co", "VL2", 385., 400.);
        String networkActionName = "Close L1 - 1";
        RemedialAction<?> networkAction = crac.newNetworkAction()
            .withId(networkActionName)
            .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.CLOSE).add()
            .newOnVoltageConstraintUsageRule().withInstant(Instant.CURATIVE).withVoltageCnec(vc.getId()).add()
            .add();

        runVoltageMonitoring();

        assertEquals(SECURE, voltageMonitoringResult.getStatus());
        assertEquals(1, voltageMonitoringResult.getAppliedRas().size());
        assertEquals(Set.of(networkAction), voltageMonitoringResult.getAppliedRas().get(crac.getState("co", Instant.CURATIVE)));
    }

    @Test
    void testUnsecureInitialSituationWithRemedialActionThatSolveVCBis() {
        setUpCracFactory("network3.xiidm");

        crac.newContingency().withId("co").withNetworkElement("L1").add();
        VoltageCnec vc = addVoltageCnec("vc", Instant.CURATIVE, "co", "VL2", 340., 350.);
        String networkActionName = "Close L1 - 1";
        RemedialAction<?> networkAction = crac.newNetworkAction()
            .withId(networkActionName)
            .newTopologicalAction().withNetworkElement("L2").withActionType(ActionType.OPEN).add()
            .newOnVoltageConstraintUsageRule().withInstant(Instant.CURATIVE).withVoltageCnec(vc.getId()).add()
            .add();

        runVoltageMonitoring();

        assertEquals(SECURE, voltageMonitoringResult.getStatus());
        assertEquals(1, voltageMonitoringResult.getAppliedRas().size());
        assertEquals(Set.of(networkAction), voltageMonitoringResult.getAppliedRas().get(crac.getState("co", Instant.CURATIVE)));
    }

    @Test
    void testRaoResultWithVoltageMonitoring() {
        InputStream raoResultFile = getClass().getResourceAsStream("/rao-result-v1.4.json");
        InputStream cracFile = getClass().getResourceAsStream("/crac-for-rao-result-v1.4.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus());

        assertEquals(4446.0, raoResult.getVoltage(Instant.CURATIVE, crac.getVoltageCnec("voltageCnecId"), Unit.KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4441.0, raoResult.getMargin(Instant.CURATIVE, crac.getVoltageCnec("voltageCnecId"), Unit.KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(1, raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency1Id", Instant.CURATIVE)).size());
        assertFalse(raoResult.isActivatedDuringState(crac.getState("contingency1Id", Instant.CURATIVE), crac.getNetworkAction("complexNetworkActionId")));

        VoltageMonitoringResult voltageMonitoringResult =
            new VoltageMonitoringResultImporter().importVoltageMonitoringResult(getClass().getResourceAsStream("/voltage-m-result.json"), crac);

        RaoResult raoResultWithVoltageMonitoring = new RaoResultWithVoltageMonitoring(raoResult, voltageMonitoringResult);
        assertEquals(144.38, raoResultWithVoltageMonitoring.getVoltage(Instant.CURATIVE, crac.getVoltageCnec("voltageCnecId"), Unit.KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(-236.61, raoResultWithVoltageMonitoring.getMargin(Instant.CURATIVE, crac.getVoltageCnec("voltageCnecId"), Unit.KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(2, raoResultWithVoltageMonitoring.getActivatedNetworkActionsDuringState(crac.getState("contingency1Id", Instant.CURATIVE)).size());
        assertTrue(raoResultWithVoltageMonitoring.isActivatedDuringState(crac.getState("contingency1Id", Instant.CURATIVE), crac.getNetworkAction("complexNetworkActionId")));
        assertEquals(ComputationStatus.FAILURE, raoResultWithVoltageMonitoring.getComputationStatus());
    }

    /*
        This test intend to enhance the coverage of AbstractRaoResultClone, code is copied from rao-result json ImporterRetrocompatibilityTest
     */
    @Test
    void testRaoResultClone() {
        InputStream raoResultFile = getClass().getResourceAsStream("/rao-result-v1.4.json");
        InputStream cracFile = getClass().getResourceAsStream("/crac-for-rao-result-v1.4.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);
        VoltageMonitoringResult voltageMonitoringResult = new VoltageMonitoringResultImporter().importVoltageMonitoringResult(getClass().getResourceAsStream("/voltage-m-result.json"), crac);
        RaoResult importedRaoResult = new RaoResultWithVoltageMonitoring(raoResult, voltageMonitoringResult);

        // --------------------------
        // --- test Costs results ---
        // --------------------------
        assertEquals(Set.of("loopFlow", "MNEC"), importedRaoResult.getVirtualCostNames());

        assertEquals(100., importedRaoResult.getFunctionalCost(null), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(null, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(null, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(null), DOUBLE_TOLERANCE);
        assertEquals(100., importedRaoResult.getCost(null), DOUBLE_TOLERANCE);

        assertEquals(80., importedRaoResult.getFunctionalCost(PREVENTIVE), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(PREVENTIVE, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(PREVENTIVE, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(PREVENTIVE), DOUBLE_TOLERANCE);
        assertEquals(80., importedRaoResult.getCost(PREVENTIVE), DOUBLE_TOLERANCE);

        assertEquals(-20., importedRaoResult.getFunctionalCost(AUTO), DOUBLE_TOLERANCE);
        assertEquals(15., importedRaoResult.getVirtualCost(AUTO, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(20., importedRaoResult.getVirtualCost(AUTO, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(35., importedRaoResult.getVirtualCost(AUTO), DOUBLE_TOLERANCE);
        assertEquals(15., importedRaoResult.getCost(AUTO), DOUBLE_TOLERANCE);

        assertEquals(-50., importedRaoResult.getFunctionalCost(CURATIVE), DOUBLE_TOLERANCE);
        assertEquals(10., importedRaoResult.getVirtualCost(CURATIVE, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(2., importedRaoResult.getVirtualCost(CURATIVE, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(12., importedRaoResult.getVirtualCost(CURATIVE), DOUBLE_TOLERANCE);
        assertEquals(-38, importedRaoResult.getCost(CURATIVE), DOUBLE_TOLERANCE);

        // -----------------------------
        // --- test FlowCnec results ---
        // -----------------------------

        /*
        cnec4prevId: preventive, no loop-flows, optimized
        - contains result in null and in PREVENTIVE. Results in AUTO and CURATIVE are the same as PREVENTIVE because the CNEC is preventive
        - contains result relative margin and PTDF sum but not for loop and commercial flows
         */
        FlowCnec cnecP = crac.getFlowCnec("cnec4prevId");
        assertEquals(4110., importedRaoResult.getFlow(null, cnecP, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getFlow(null, cnecP, Side.RIGHT, MEGAWATT)));
        assertEquals(4111., importedRaoResult.getMargin(null, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(4112., importedRaoResult.getRelativeMargin(null, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, Side.RIGHT, MEGAWATT)));

        assertEquals(4220., importedRaoResult.getFlow(PREVENTIVE, cnecP, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getFlow(PREVENTIVE, cnecP, Side.RIGHT, AMPERE)));
        assertEquals(4221., importedRaoResult.getMargin(PREVENTIVE, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(4222., importedRaoResult.getRelativeMargin(PREVENTIVE, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, Side.RIGHT, MEGAWATT)));

        assertEquals(0.4, importedRaoResult.getPtdfZonalSum(PREVENTIVE, cnecP, Side.LEFT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(PREVENTIVE, cnecP, Side.RIGHT)));

        assertEquals(importedRaoResult.getFlow(AUTO, cnecP, LEFT, AMPERE), importedRaoResult.getFlow(PREVENTIVE, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(AUTO, cnecP, RIGHT, AMPERE), importedRaoResult.getFlow(PREVENTIVE, cnecP, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(CURATIVE, cnecP, LEFT, AMPERE), importedRaoResult.getFlow(PREVENTIVE, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(CURATIVE, cnecP, RIGHT, AMPERE), importedRaoResult.getFlow(PREVENTIVE, cnecP, RIGHT, AMPERE), DOUBLE_TOLERANCE);

        /*
        cnec1outageId: outage, with loop-flows, optimized
        - contains result in null and in PREVENTIVE. Results in AUTO and CURATIVE are the same as PREVENTIVE because the CNEC is preventive
        - contains result for loop-flows, commercial flows, relative margin and PTDF sum
         */

        FlowCnec cnecO = crac.getFlowCnec("cnec1outageId");
        assertTrue(Double.isNaN(importedRaoResult.getFlow(null, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1120.5, importedRaoResult.getFlow(null, cnecO, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1121., importedRaoResult.getMargin(null, cnecO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1122., importedRaoResult.getRelativeMargin(null, cnecO, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1123.5, importedRaoResult.getLoopFlow(null, cnecO, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1124.5, importedRaoResult.getCommercialFlow(null, cnecO, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getFlow(PREVENTIVE, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1210.5, importedRaoResult.getFlow(PREVENTIVE, cnecO, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1211., importedRaoResult.getMargin(PREVENTIVE, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1212., importedRaoResult.getRelativeMargin(PREVENTIVE, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(PREVENTIVE, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1213.5, importedRaoResult.getLoopFlow(PREVENTIVE, cnecO, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(PREVENTIVE, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1214.5, importedRaoResult.getCommercialFlow(PREVENTIVE, cnecO, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(PREVENTIVE, cnecO, Side.LEFT)));
        assertEquals(0.6, importedRaoResult.getPtdfZonalSum(PREVENTIVE, cnecO, Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(AUTO, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(PREVENTIVE, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(AUTO, cnecO, RIGHT, MEGAWATT), importedRaoResult.getFlow(PREVENTIVE, cnecO, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(CURATIVE, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(PREVENTIVE, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(CURATIVE, cnecO, RIGHT, MEGAWATT), importedRaoResult.getFlow(PREVENTIVE, cnecO, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

        /*
        cnec3autoId: auto, without loop-flows, pureMNEC
        - contains result in null, PREVENTIVE, and AUTO. Results in CURATIVE are the same as AUTO because the CNEC is auto
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecA = crac.getFlowCnec("cnec3autoId");
        assertEquals(3110., importedRaoResult.getFlow(null, cnecA, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3110.5, importedRaoResult.getFlow(null, cnecA, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3111., importedRaoResult.getMargin(null, cnecA, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(null, cnecA, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecA, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecA, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecA, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecA, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(null, cnecA, Side.LEFT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(null, cnecA, Side.RIGHT)));

        assertEquals(3220., importedRaoResult.getFlow(PREVENTIVE, cnecA, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3220.5, importedRaoResult.getFlow(PREVENTIVE, cnecA, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(PREVENTIVE, cnecA, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(AUTO, cnecA, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3310.5, importedRaoResult.getFlow(AUTO, cnecA, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(AUTO, cnecA, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(CURATIVE, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(AUTO, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(CURATIVE, cnecO, RIGHT, MEGAWATT), importedRaoResult.getFlow(AUTO, cnecO, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

         /*
        cnec3curId: curative, without loop-flows, pureMNEC
        - contains result in null, PREVENTIVE, and AUTO and in CURATIVE
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecC = crac.getFlowCnec("cnec3curId");
        assertEquals(3110., importedRaoResult.getFlow(null, cnecC, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3110.5, importedRaoResult.getFlow(null, cnecC, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3111., importedRaoResult.getMargin(null, cnecC, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(null, cnecC, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecC, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecC, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecC, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecC, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(null, cnecC, Side.LEFT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(null, cnecC, Side.RIGHT)));

        assertEquals(3220., importedRaoResult.getFlow(PREVENTIVE, cnecC, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3220.5, importedRaoResult.getFlow(PREVENTIVE, cnecC, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(PREVENTIVE, cnecC, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(AUTO, cnecC, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3310.5, importedRaoResult.getFlow(AUTO, cnecC, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(AUTO, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(3410., importedRaoResult.getFlow(CURATIVE, cnecC, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3410.5, importedRaoResult.getFlow(CURATIVE, cnecC, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3411., importedRaoResult.getMargin(CURATIVE, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        // -----------------------------
        // --- NetworkAction results ---
        // -----------------------------

        State pState = crac.getPreventiveState();
        State oState2 = crac.getState("contingency2Id", Instant.OUTAGE);
        State aState2 = crac.getState("contingency2Id", Instant.AUTO);
        State cState1 = crac.getState("contingency1Id", Instant.CURATIVE);
        State cState2 = crac.getState("contingency2Id", Instant.CURATIVE);

        /*
        complexNetworkActionId, activated in preventive
        */
        NetworkAction naP = crac.getNetworkAction("complexNetworkActionId");
        assertTrue(importedRaoResult.isActivatedDuringState(pState, naP));
        assertTrue(importedRaoResult.isActivated(pState, naP));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naP));
        assertFalse(importedRaoResult.isActivatedDuringState(aState2, naP));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naP));
        assertTrue(importedRaoResult.isActivated(cState1, naP));
        assertTrue(importedRaoResult.isActivated(cState2, naP));

        /*
        injectionSetpointRaId, activated in auto
        */
        NetworkAction naA = crac.getNetworkAction("injectionSetpointRaId");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, naA));
        assertFalse(importedRaoResult.isActivated(pState, naA));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naA));
        assertTrue(importedRaoResult.isActivatedDuringState(aState2, naA));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, naA));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naA));
        assertFalse(importedRaoResult.isActivated(cState1, naA));
        assertTrue(importedRaoResult.isActivated(cState2, naA));

        /*
        pstSetpointRaId, activated curative1
        */
        NetworkAction naC = crac.getNetworkAction("pstSetpointRaId");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, naC));
        assertFalse(importedRaoResult.isActivated(pState, naC));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naC));
        assertFalse(importedRaoResult.isActivatedDuringState(aState2, naC));
        assertTrue(importedRaoResult.isActivatedDuringState(cState1, naC));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naC));
        assertTrue(importedRaoResult.isActivated(cState1, naC));
        assertFalse(importedRaoResult.isActivated(cState2, naC));

        /*
        switchPairRaId, never activated
        */
        NetworkAction naN = crac.getNetworkAction("switchPairRaId");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, naN));
        assertFalse(importedRaoResult.isActivated(pState, naN));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naN));
        assertFalse(importedRaoResult.isActivatedDuringState(aState2, naN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, naN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naN));
        assertFalse(importedRaoResult.isActivated(cState1, naN));
        assertFalse(importedRaoResult.isActivated(cState2, naN));

        // ------------------------------
        // --- PstRangeAction results ---
        // ------------------------------

        /*
        pstRange1Id, activated in preventive
        */
        PstRangeAction pstP = crac.getPstRangeAction("pstRange1Id");
        assertTrue(importedRaoResult.isActivatedDuringState(pState, pstP));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, pstP));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, pstP));
        assertEquals(-3, importedRaoResult.getPreOptimizationTapOnState(pState, pstP));
        assertEquals(0., importedRaoResult.getPreOptimizationSetPointOnState(pState, pstP), DOUBLE_TOLERANCE);
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(pState, pstP));
        assertEquals(3., importedRaoResult.getOptimizedSetPointOnState(pState, pstP), DOUBLE_TOLERANCE);
        assertEquals(3., importedRaoResult.getPreOptimizationSetPointOnState(cState1, pstP), DOUBLE_TOLERANCE);
        assertEquals(3, importedRaoResult.getPreOptimizationTapOnState(cState1, pstP));
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(cState1, pstP));
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(cState2, pstP));

        /*
        pstRange2Id, not activated
        */
        PstRangeAction pstN = crac.getPstRangeAction("pstRange2Id");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, pstN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, pstN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, pstN));
        assertEquals(0, importedRaoResult.getPreOptimizationTapOnState(pState, pstN));
        assertEquals(0, importedRaoResult.getOptimizedTapOnState(pState, pstN));
        assertEquals(0, importedRaoResult.getOptimizedTapOnState(cState1, pstN));
        assertEquals(0, importedRaoResult.getOptimizedTapOnState(cState2, pstN));

        // ---------------------------
        // --- RangeAction results ---
        // ---------------------------

        /*
        hvdcRange2Id, two different activations in the two curative states
        */
        HvdcRangeAction hvdcC = crac.getHvdcRangeAction("hvdcRange2Id");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, hvdcC));
        assertTrue(importedRaoResult.isActivatedDuringState(cState1, hvdcC));
        assertTrue(importedRaoResult.isActivatedDuringState(cState2, hvdcC));
        assertEquals(-100, importedRaoResult.getPreOptimizationSetPointOnState(pState, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(-100, importedRaoResult.getOptimizedSetPointOnState(pState, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(-100, importedRaoResult.getPreOptimizationSetPointOnState(cState1, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(100, importedRaoResult.getOptimizedSetPointOnState(cState1, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(400, importedRaoResult.getOptimizedSetPointOnState(cState2, hvdcC), DOUBLE_TOLERANCE);

        /*
        InjectionRange1Id, one activation in curative
        */
        InjectionRangeAction injectionC = crac.getInjectionRangeAction("injectionRange1Id");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, injectionC));
        assertTrue(importedRaoResult.isActivatedDuringState(cState1, injectionC));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, injectionC));
        assertEquals(100, importedRaoResult.getPreOptimizationSetPointOnState(pState, injectionC), DOUBLE_TOLERANCE);
        assertEquals(100, importedRaoResult.getPreOptimizationSetPointOnState(cState1, injectionC), DOUBLE_TOLERANCE);
        assertEquals(-300, importedRaoResult.getOptimizedSetPointOnState(cState1, injectionC), DOUBLE_TOLERANCE);

        /*
        VoltageCnec
        */
        VoltageCnec voltageCnec = crac.getVoltageCnec("voltageCnecId");
        assertEquals(144.38, importedRaoResult.getVoltage(CURATIVE, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(-236.61, importedRaoResult.getMargin(CURATIVE, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);

        // Test computation status map
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus(crac.getPreventiveState()));
        assertEquals(ComputationStatus.FAILURE, importedRaoResult.getComputationStatus(crac.getState("contingency1Id", CURATIVE)));
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus(crac.getState("contingency2Id", AUTO)));
    }
}

