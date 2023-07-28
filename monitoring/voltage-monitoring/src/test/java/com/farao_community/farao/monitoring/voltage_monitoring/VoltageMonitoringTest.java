/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.voltage_monitoring;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult.Status.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class VoltageMonitoringTest {
    private static final double VOLTAGE_TOLERANCE = 0.5;

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

        naOpenL1 = crac.newNetworkAction()
            .withId("Open L1")
            .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        naCloseL1 = crac.newNetworkAction()
            .withId("Close L1")
            .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.CLOSE).add()
            .newOnInstantUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        naOpenL2 = crac.newNetworkAction()
            .withId("Open L2")
            .newTopologicalAction().withNetworkElement("L2").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        naCloseL2 = crac.newNetworkAction()
            .withId("Close L2")
            .newTopologicalAction().withNetworkElement("L2").withActionType(ActionType.CLOSE).add()
            .newOnInstantUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        pst = crac.newPstRangeAction()
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
        assertEquals(List.of("Network element VL1 at state preventive has a voltage of 400 - 400 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testOneLowVoltagePreventiveCnec() {
        addVoltageCnec("vc", Instant.PREVENTIVE, null, "VL1", 401., 410.);
        runVoltageMonitoring();
        assertEquals(400., voltageMonitoringResult.getMinVoltage("vc"), VOLTAGE_TOLERANCE);
        assertEquals(400., voltageMonitoringResult.getMaxVoltage("vc"), VOLTAGE_TOLERANCE);
        assertEquals(LOW_VOLTAGE_CONSTRAINT, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(crac.getVoltageCnec("vc")), voltageMonitoringResult.getConstrainedElements());
        assertEquals(List.of("Network element VL1 at state preventive has a voltage of 400 - 400 kV."), voltageMonitoringResult.printConstraints());
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
        assertEquals(List.of("Network element VL2 at state preventive has a voltage of 368 - 368 kV."), voltageMonitoringResult.printConstraints());
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
        assertEquals(List.of("Network element VL2 at state preventive has a voltage of 368 - 368 kV.", "Network element VL3 at state preventive has a voltage of 383 - 383 kV."), voltageMonitoringResult.printConstraints());
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
        assertEquals(List.of("Network element VL3 at state preventive has a voltage of 400 - 400 kV."), voltageMonitoringResult.printConstraints());
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
        assertEquals(List.of("Network element VL2 at state preventive has a voltage of 368 - 368 kV.", "Network element VL3 at state preventive has a voltage of 400 - 400 kV."), voltageMonitoringResult.printConstraints());
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
        assertEquals(List.of("Network element VL2 at state preventive has a voltage of 379 - 379 kV."), voltageMonitoringResult.printConstraints());
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
        assertEquals(List.of("Network element VL2 at state coL1 - curative has a voltage of 368 - 368 kV.",
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
        assertEquals(List.of("Network element VL2 at state co3 - curative has a voltage of 368 - 368 kV."), voltageMonitoringResult.printConstraints());
    }

    @Test
    void testMultipleVoltageValuesPerVoltageLevel() {
        network = Network.read("ieee14.xiidm", getClass().getResourceAsStream("/ieee14.xiidm"));
        // VL45 : Min = 144.38, Max = 148.41
        // VL46 : Min = 143.10, Max = 147.66

        VoltageCnec vc1 =  addVoltageCnec("VL45", Instant.PREVENTIVE, null, "VL45", 145., 150.);
        VoltageCnec vc2 = addVoltageCnec("VL46", Instant.PREVENTIVE, null, "VL46", 140., 145.);

        runVoltageMonitoring();

        assertEquals(HIGH_AND_LOW_VOLTAGE_CONSTRAINTS, voltageMonitoringResult.getStatus());
        assertEquals(Set.of(vc1, vc2), voltageMonitoringResult.getConstrainedElements());
        assertEquals(144.4, voltageMonitoringResult.getMinVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(148.4, voltageMonitoringResult.getMaxVoltage(vc1), VOLTAGE_TOLERANCE);
        assertEquals(143.1, voltageMonitoringResult.getMinVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(147.7, voltageMonitoringResult.getMaxVoltage(vc2), VOLTAGE_TOLERANCE);
        assertEquals(List.of(
            "Network element VL45 at state preventive has a voltage of 144 - 148 kV.",
            "Network element VL46 at state preventive has a voltage of 143 - 148 kV."),
            voltageMonitoringResult.printConstraints());
    }

    public void setUpCracFactory(String networkFileName) {
        network = Network.read(networkFileName, getClass().getResourceAsStream("/" + networkFileName));
        crac = CracFactory.findDefault().create("test-crac");
    }

    public void mockPreventiveState() {
        vcPrev =  addVoltageCnec("vcPrev", Instant.PREVENTIVE, null, "VL45", 145., 150.);
        crac.newNetworkAction()
                .withId("Open L1 - 1")
                .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
                .newOnVoltageConstraintUsageRule().withInstant(Instant.PREVENTIVE).withVoltageCnec(vcPrev.getId()).add()
                .add();
    }

    public void mockCurativeState() {
        crac.newContingency().withId("coL1").withNetworkElement("L1").add();
        crac.newContingency().withId("coL2").withNetworkElement("L2").add();
        crac.newContingency().withId("coL1L2").withNetworkElement("L1").withNetworkElement("L2").add();
        addVoltageCnec("vcCur1", Instant.CURATIVE, null, "VL2", 375., 395.);
    }

    @Test
    void testDivergentLoadFlowDuringInitialLoadFlow() {
        setUpCracFactory("networkKO.xiidm");
        mockPreventiveState();

        runVoltageMonitoring();

        assertEquals(DIVERGENT, voltageMonitoringResult.getStatus());
        voltageMonitoringResult.getAppliedCras().forEach((state, networkActions) -> assertTrue(networkActions.isEmpty()));
        assertEquals(Double.NaN, voltageMonitoringResult.getMinVoltage(vcPrev));
        assertEquals(Double.NaN, voltageMonitoringResult.getMaxVoltage(vcPrev));
    }

    /*@Test
    void testDivergentLoadFlowAfterApplicationOfRemedialAction() {
        setUpCracFactory("network.xiidm");
        mockPreventiveState();

        crac.newContingency().withId("coL1").withNetworkElement("L2").add();
        addVoltageCnec("vcCur1", Instant.CURATIVE, "coL1", "VL3_Break L2", 375., 395.);

        runVoltageMonitoring();

        //assertEquals(DIVERGENT, voltageMonitoringResult.getStatus());
        assertEquals(1, voltageMonitoringResult.getAppliedCras().size());
    }*/

    @Test
    void testSecureInitialSituationWithAvailableRemedialActions() {
        setUpCracFactory("network.xiidm");
        mockPreventiveState();
        //Add contingency that doesn't break the loadflow
        crac.newContingency().withId("coVL3").withNetworkElement("VL3_Break L2").add();
        addVoltageCnec("vcCur1", Instant.CURATIVE, "coVL3", "VL3", 375., 395.);

        runVoltageMonitoring();

        assertEquals(SECURE, voltageMonitoringResult.getStatus());
        assertEquals(0, voltageMonitoringResult.getAppliedCras().size());
    }
}
