/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.farao_community.farao.search_tree_rao.commons.ToolProvider;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.search_tree_rao.result.impl.AutomatonPerimeterResultImpl;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class AutomatonSimulatorTest {
    private AutomatonSimulator automatonSimulator;

    private Crac crac;
    private Network network;
    private State autoState;
    private RangeAction<?> ra2;
    private RangeAction<?> ra3;
    private RangeAction<?> ra4;
    private PstRangeAction ara1;
    private RangeAction<?> ara2;
    private RangeAction<?> ara3;
    private RangeAction<?> ara4;
    private RangeAction<?> ara5;
    private RangeAction<?> ara6;
    private NetworkAction na;
    private HvdcRangeAction hvdcRa1;
    private HvdcRangeAction hvdcRa2;
    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private PrePerimeterSensitivityAnalysis mockedPreAutoPerimeterSensitivityAnalysis;
    private PrePerimeterResult mockedPrePerimeterResult;

    private static final double DOUBLE_TOLERANCE = 0.01;

    @Before
    public void setup() {
        network = Network.read("TestCase16NodesWith2Hvdc.xiidm", getClass().getResourceAsStream("/network/TestCase16NodesWith2Hvdc.xiidm"));
        // Add some lines otherwise HVDC2 is connected to nothing and load-flow produces NaN angles
        network.newLine()
            .setId("newline1")
            .setR(0.01).setX(0.01)
            .setBus1("BBE2AA12").setVoltageLevel1("BBE2AA1").setG1(0.01).setB1(0.01)
            .setBus2("DDE3AA11").setVoltageLevel2("DDE3AA1").setG2(0.01).setB2(0.01)
            .add();
        network.newLine()
            .setId("newline2")
            .setR(0.01).setX(0.01)
            .setBus1("FFR3AA12").setVoltageLevel1("FFR3AA1").setG1(0.01).setB1(0.01)
            .setBus2("DDE2AA11").setVoltageLevel2("DDE2AA1").setG2(0.01).setB2(0.01)
            .add();

        crac = CracFactory.findDefault().create("test-crac");
        Contingency contingency1 = crac.newContingency()
            .withId("contingency1")
            .withNetworkElement("NNL1AA11 NNL2AA11 1")
            .add();
        crac.newFlowCnec()
            .withId("cnec-prev")
            .withNetworkElement("cnec-ne")
            .withInstant(Instant.PREVENTIVE)
            .withNominalVoltage(220.)
            .newThreshold().withSide(Side.RIGHT).withMax(1000.).withUnit(Unit.AMPERE).add()
            .add();
        cnec1 = crac.newFlowCnec()
            .withId("cnec1")
            .withNetworkElement("cnec-ne")
            .withContingency("contingency1")
            .withInstant(Instant.AUTO)
            .withNominalVoltage(220.)
            .newThreshold().withSide(Side.RIGHT).withMax(1000.).withUnit(Unit.AMPERE).add()
            .add();
        cnec2 = crac.newFlowCnec()
            .withId("cnec2")
            .withNetworkElement("cnec-ne")
            .withContingency("contingency1")
            .withInstant(Instant.AUTO)
            .withNominalVoltage(220.)
            .newThreshold().withSide(Side.RIGHT).withMax(1000.).withUnit(Unit.AMPERE).add()
            .add();
        autoState = crac.getState(contingency1, Instant.AUTO);
        ra2 = crac.newPstRangeAction()
            .withId("ra2")
            .withNetworkElement("ra2-ne")
            .withSpeed(2)
            .newFreeToUseUsageRule().withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        ra3 = crac.newPstRangeAction()
            .withId("ra3")
            .withNetworkElement("ra3-ne")
            .withSpeed(4)
            .newOnFlowConstraintUsageRule().withInstant(Instant.AUTO).withFlowCnec("cnec1").add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        ra4 = crac.newPstRangeAction()
            .withId("ra4")
            .withNetworkElement("ra4-ne")
            .withSpeed(4)
            .newOnFlowConstraintUsageRule().withInstant(Instant.PREVENTIVE).withFlowCnec("cnec-prev").add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();

        // Add 2 aligned range actions
        ara1 = crac.newPstRangeAction()
            .withId("ara1")
            .withGroupId("group1")
            .withNetworkElement("BBE2AA11 BBE3AA11 1")
            .withSpeed(3)
            .newOnFlowConstraintUsageRule().withInstant(Instant.AUTO).withFlowCnec("cnec1").add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, 0.1, 1, 1.1, 2, 2.1, 3, 3.1, -1, -1.1, -2, -2.1, -3, -3.1))
            .add();
        ara2 = crac.newPstRangeAction()
            .withId("ara2")
            .withGroupId("group1")
            .withNetworkElement("FFR2AA11 FFR4AA11 1")
            .withSpeed(3)
            .newOnFlowConstraintUsageRule().withInstant(Instant.AUTO).withFlowCnec("cnec1").add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, 0.1, 1, 1.1, 2, 2.1, 3, 3.1, -1, -1.1, -2, -2.1, -3, -3.1))
            .add();

        // Add 2 aligned range actions of different types
        ara3 = crac.newPstRangeAction()
            .withId("ara3")
            .withGroupId("group2")
            .withNetworkElement("ra2-ne")
            .withSpeed(5)
            .newFreeToUseUsageRule().withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        ara4 = crac.newHvdcRangeAction()
            .withId("ara4")
            .withNetworkElement("ra1-ne")
            .withGroupId("group2")
            .withSpeed(5)
            .newRange().withMax(1).withMin(-1).add()
            .add();

        // Add 2 aligned range actions with different usage methods
        ara5 = crac.newPstRangeAction()
            .withId("ara5")
            .withGroupId("group3")
            .withNetworkElement("ra2-ne")
            .withSpeed(6)
            .newFreeToUseUsageRule().withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        ara6 = crac.newPstRangeAction()
            .withId("ara6")
            .withGroupId("group3")
            .withNetworkElement("ra3-ne")
            .withSpeed(6)
            .newOnFlowConstraintUsageRule().withInstant(Instant.AUTO).withFlowCnec("cnec1").add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();

        // Add a network action
        na = crac.newNetworkAction()
            .withId("na")
            .newTopologicalAction().withActionType(ActionType.CLOSE).withNetworkElement("DDE3AA11 DDE4AA11 1").add()
            .newOnFlowConstraintUsageRule().withInstant(Instant.AUTO).withFlowCnec("cnec2").add()
            .add();

        // Add HVDC range actions
        hvdcRa1 = crac.newHvdcRangeAction()
            .withId("hvdc-ra1")
            .withGroupId("hvdcGroup")
            .withNetworkElement("BBE2AA11 FFR3AA11 1")
            .withSpeed(1)
            .newRange().withMax(3000).withMin(-3000).add()
            .newFreeToUseUsageRule().withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
            .add();
        hvdcRa2 = crac.newHvdcRangeAction()
            .withId("hvdc-ra2")
            .withGroupId("hvdcGroup")
            .withNetworkElement("BBE2AA12 FFR3AA12 1")
            .withSpeed(1)
            .newRange().withMax(3000).withMin(-3000).add()
            .newFreeToUseUsageRule().withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
            .add();

        autoState = crac.getState(contingency1, Instant.AUTO);

        RaoParameters raoParameters = new RaoParameters();
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        raoParameters.setSensitivityProvider("OpenLoadFlow");
        raoParameters.addExtension(SearchTreeRaoParameters.class, new SearchTreeRaoParameters());

        mockedPreAutoPerimeterSensitivityAnalysis = mock(PrePerimeterSensitivityAnalysis.class);
        mockedPrePerimeterResult = mock(PrePerimeterResult.class);
        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        ToolProvider toolProvider = Mockito.mock(ToolProvider.class);
        when(toolProvider.getLoopFlowCnecs(any())).thenReturn(Collections.emptySet());
        automatonSimulator = new AutomatonSimulator(crac, raoParameters, toolProvider, null, null, mockedPrePerimeterResult, null, 0);
    }

    @Test
    public void testGatherCnecs() {
        assertEquals(2, automatonSimulator.gatherFlowCnecsForAutoRangeAction(ra2, autoState, network).size());
        assertEquals(1, automatonSimulator.gatherFlowCnecsForAutoRangeAction(ra3, autoState, network).size());
    }

    @Test
    public void testGatherCnecsError() {
        RangeAction<?> ra = Mockito.mock(RangeAction.class);
        Mockito.when(ra.getUsageMethod(autoState)).thenReturn(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, () -> automatonSimulator.gatherFlowCnecsForAutoRangeAction(ra, autoState, network));
    }

    @Test
    public void testCheckAlignedRangeActions1() {
        // OK
        assertTrue(AutomatonSimulator.checkAlignedRangeActions(autoState, List.of(ara1, ara2), List.of(ara1, ara2)));
        assertTrue(AutomatonSimulator.checkAlignedRangeActions(autoState, List.of(ara2, ara1), List.of(ara1, ara2)));
        // different types
        assertFalse(AutomatonSimulator.checkAlignedRangeActions(autoState, List.of(ara3, ara4), List.of(ara3, ara4)));
        assertFalse(AutomatonSimulator.checkAlignedRangeActions(autoState, List.of(ara4, ara3), List.of(ara3, ara4)));
        // different usage method
        assertFalse(AutomatonSimulator.checkAlignedRangeActions(autoState, List.of(ara5, ara6), List.of(ara5, ara6)));
        assertFalse(AutomatonSimulator.checkAlignedRangeActions(autoState, List.of(ara5, ra4), List.of(ara5, ra3, ra4)));
        // one unavailable RA
        assertFalse(AutomatonSimulator.checkAlignedRangeActions(autoState, List.of(ara1, ara2), List.of(ara1)));
    }

    @Test
    public void testBuildRangeActionsGroupsOrderedBySpeed() {
        PrePerimeterResult rangeActionSensitivity = Mockito.mock(PrePerimeterResult.class);
        List<List<RangeAction<?>>> result = automatonSimulator.buildRangeActionsGroupsOrderedBySpeed(rangeActionSensitivity, autoState, network);
        assertEquals(List.of(List.of(hvdcRa1, hvdcRa2), List.of(ra2), List.of(ara1, ara2), List.of(ra3)), result);
    }

    @Test
    public void testDisableHvdcAngleDroopControl1() {
        PrePerimeterResult prePerimeterResult = mock(PrePerimeterResult.class);
        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        Pair<PrePerimeterResult, Map<HvdcRangeAction, Double>> result = automatonSimulator.disableHvdcAngleDroopActivePowerControl(List.of(hvdcRa1), network, mockedPreAutoPerimeterSensitivityAnalysis, prePerimeterResult, autoState);
        // check that angle-droop control was disabled on HVDC
        assertFalse(network.getHvdcLine("BBE2AA11 FFR3AA11 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        // check that other HVDC was not touched
        assertTrue(network.getHvdcLine("BBE2AA12 FFR3AA12 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        // check that sensitivity computation has been run
        assertEquals(mockedPrePerimeterResult, result.getLeft());
        assertEquals(1, result.getRight().size());
        assertEquals(2450.87, result.getRight().get(hvdcRa1), DOUBLE_TOLERANCE);
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER, network.getHvdcLine(hvdcRa1.getNetworkElement().getId()).getConvertersMode());
        assertEquals(2450.87, network.getHvdcLine(hvdcRa1.getNetworkElement().getId()).getActivePowerSetpoint(), DOUBLE_TOLERANCE);
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER, network.getHvdcLine(hvdcRa2.getNetworkElement().getId()).getConvertersMode());
        assertEquals(0, network.getHvdcLine(hvdcRa2.getNetworkElement().getId()).getActivePowerSetpoint(), DOUBLE_TOLERANCE);

        // run a second time => no influence + sensitivity not run
        result = automatonSimulator.disableHvdcAngleDroopActivePowerControl(List.of(hvdcRa1), network, mockedPreAutoPerimeterSensitivityAnalysis, prePerimeterResult, autoState);
        assertFalse(network.getHvdcLine("BBE2AA11 FFR3AA11 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertTrue(network.getHvdcLine("BBE2AA12 FFR3AA12 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertEquals(prePerimeterResult, result.getLeft());
        assertEquals(Map.of(), result.getRight());
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER, network.getHvdcLine(hvdcRa1.getNetworkElement().getId()).getConvertersMode());
        assertEquals(2450.87, network.getHvdcLine(hvdcRa1.getNetworkElement().getId()).getActivePowerSetpoint(), DOUBLE_TOLERANCE);
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER, network.getHvdcLine(hvdcRa2.getNetworkElement().getId()).getConvertersMode());
        assertEquals(0, network.getHvdcLine(hvdcRa2.getNetworkElement().getId()).getActivePowerSetpoint(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testDisableHvdcAngleDroopControl2() {
        PrePerimeterResult prePerimeterResult = mock(PrePerimeterResult.class);
        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        // Test on 2 aligned HVDC RAs
        Pair<PrePerimeterResult, Map<HvdcRangeAction, Double>> result = automatonSimulator.disableHvdcAngleDroopActivePowerControl(List.of(hvdcRa1, hvdcRa2), network, mockedPreAutoPerimeterSensitivityAnalysis, prePerimeterResult, autoState);
        assertFalse(network.getHvdcLine("BBE2AA11 FFR3AA11 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertFalse(network.getHvdcLine("BBE2AA12 FFR3AA12 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertEquals(mockedPrePerimeterResult, result.getLeft());
        assertEquals(2, result.getRight().size());
        assertEquals(2450.87, result.getRight().get(hvdcRa1), DOUBLE_TOLERANCE);
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER, network.getHvdcLine(hvdcRa1.getNetworkElement().getId()).getConvertersMode());
        assertEquals(2450.87, network.getHvdcLine(hvdcRa1.getNetworkElement().getId()).getActivePowerSetpoint(), DOUBLE_TOLERANCE);
        assertEquals(-46.61, result.getRight().get(hvdcRa2), DOUBLE_TOLERANCE);
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER, network.getHvdcLine(hvdcRa2.getNetworkElement().getId()).getConvertersMode());
        assertEquals(46.61, network.getHvdcLine(hvdcRa2.getNetworkElement().getId()).getActivePowerSetpoint(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testDisableHvdcAngleDroopControl3() {
        PrePerimeterResult prePerimeterResult = mock(PrePerimeterResult.class);
        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        // Test on an HVDC with no HvdcAngleDroopActivePowerControl
        network.getHvdcLine("BBE2AA11 FFR3AA11 1").removeExtension(HvdcAngleDroopActivePowerControl.class);
        Pair<PrePerimeterResult, Map<HvdcRangeAction, Double>> result = automatonSimulator.disableHvdcAngleDroopActivePowerControl(List.of(hvdcRa1), network, mockedPreAutoPerimeterSensitivityAnalysis, prePerimeterResult, autoState);
        assertEquals(prePerimeterResult, result.getLeft());
        assertEquals(Map.of(), result.getRight());
    }

    @Test
    public void testDisableHvdcAngleDroopControl4() {
        PrePerimeterResult prePerimeterResult = mock(PrePerimeterResult.class);
        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        // Test on non-HVDC : nothing should happen
        Pair<PrePerimeterResult, Map<HvdcRangeAction, Double>> result = automatonSimulator.disableHvdcAngleDroopActivePowerControl(List.of(ra2), network, mockedPreAutoPerimeterSensitivityAnalysis, prePerimeterResult, autoState);
        assertEquals(prePerimeterResult, result.getLeft());
        assertEquals(Map.of(), result.getRight());
    }

    @Test
    public void testDisableHvdcAngleDroopControl5() {
        // Test with phi1 < phi2
        PrePerimeterResult prePerimeterResult = mock(PrePerimeterResult.class);
        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        HvdcLine hvdcLine = network.getHvdcLine("BBE2AA11 FFR3AA11 1");
        hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class).setP0(100f);
        network.getGenerator("BBE2AA11_generator").setTargetP(0);
        network.getLoad("BBE2AA11_load").setP0(3000);
        network.getGenerator("FFR3AA11_generator").setTargetP(6460);
        network.getLoad("FFR3AA11_load").setP0(0);
        network.getGenerator("FFR5AA11_generator").setTargetP(6460);
        network.getLoad("FFR5AA11_load").setP0(0);
        hvdcLine.setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);

        Pair<PrePerimeterResult, Map<HvdcRangeAction, Double>> result = automatonSimulator.disableHvdcAngleDroopActivePowerControl(List.of(hvdcRa1), network, mockedPreAutoPerimeterSensitivityAnalysis, prePerimeterResult, autoState);
        assertFalse(network.getHvdcLine("BBE2AA11 FFR3AA11 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertEquals(mockedPrePerimeterResult, result.getLeft());
        assertEquals(1, result.getRight().size());
        assertEquals(-813.97, result.getRight().get(hvdcRa1), DOUBLE_TOLERANCE);
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER, network.getHvdcLine(hvdcRa1.getNetworkElement().getId()).getConvertersMode());
        assertEquals(813.97, network.getHvdcLine(hvdcRa1.getNetworkElement().getId()).getActivePowerSetpoint(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testDisableHvdcAngleDroopControl6() {
        // Initial setpoint is out of RA's allowed range. Do not disable HvdcAngleDroopControl
        hvdcRa1 = crac.newHvdcRangeAction()
            .withId("hvdc-ra3")
            .withGroupId("hvdcGroup")
            .withNetworkElement("BBE2AA11 FFR3AA11 1")
            .withSpeed(1)
            .newRange().withMax(1000).withMin(-1000).add()
            .newFreeToUseUsageRule().withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
            .add();

        PrePerimeterResult prePerimeterResult = mock(PrePerimeterResult.class);
        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        Pair<PrePerimeterResult, Map<HvdcRangeAction, Double>> result = automatonSimulator.disableHvdcAngleDroopActivePowerControl(List.of(hvdcRa1), network, mockedPreAutoPerimeterSensitivityAnalysis, prePerimeterResult, autoState);
        assertTrue(network.getHvdcLine("BBE2AA11 FFR3AA11 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertTrue(result.getRight().isEmpty());
    }

    @Test
    public void testRoundUpAngleToTapWrtInitialSetpoint() {
        assertEquals(2.1, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint(ara1, 1.2, 0.1), DOUBLE_TOLERANCE);
        assertEquals(1.1, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint(ara1, 1.1, 0.1), DOUBLE_TOLERANCE);
        assertEquals(1.1, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint(ara1, 0.2, 0.1), DOUBLE_TOLERANCE);
        assertEquals(0.1, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint(ara1, 0.2, 1.1), DOUBLE_TOLERANCE);
        assertEquals(2.1, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint(ara1, 1.2, 1.1), DOUBLE_TOLERANCE);
        assertEquals(2.1, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint(ara1, 2.1, 2.1), DOUBLE_TOLERANCE);
        assertEquals(3.1, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint(ara1, 3.1, 2.1), DOUBLE_TOLERANCE);
        assertEquals(-3.1, AutomatonSimulator.roundUpAngleToTapWrtInitialSetpoint(ara1, -3.1, 2.1), DOUBLE_TOLERANCE);
    }

    @Test
    public void testComputeOptimalSetpoint() {
        double optimalSetpoint;
        // limit by min
        optimalSetpoint = automatonSimulator.computeOptimalSetpoint(0.1, 1100., -100., 1, ara1, -3.1, 3.1);
        assertEquals(-3.1, optimalSetpoint, DOUBLE_TOLERANCE);
        // limit by max
        optimalSetpoint = automatonSimulator.computeOptimalSetpoint(0.1, 1100., -100., -1, ara1, -3.1, 3.1);
        assertEquals(3.1, optimalSetpoint, DOUBLE_TOLERANCE);
        // Reduce flow from 1100 to 1000 with one setpoint change, sensi > 0
        optimalSetpoint = automatonSimulator.computeOptimalSetpoint(0.1, 1100., -100., 100, ara1, -3.1, 3.1);
        assertEquals(-1.1, optimalSetpoint, DOUBLE_TOLERANCE);
        // Reduce flow from 1100 to 1000 with two setpoint changes, sensi > 0
        optimalSetpoint = automatonSimulator.computeOptimalSetpoint(0.1, 1100., -100., 50, ara1, -3.1, 3.1);
        assertEquals(-2.1, optimalSetpoint, DOUBLE_TOLERANCE);
        // Increase flow from -1100 to -1000 with two setpoint changes, sensi > 0
        optimalSetpoint = automatonSimulator.computeOptimalSetpoint(0.1, -1100., -100., 50, ara1, -3.1, 3.1);
        assertEquals(2.1, optimalSetpoint, DOUBLE_TOLERANCE);
        // Reduce flow from 1100 to 1000 with two setpoint changes, sensi < 0
        optimalSetpoint = automatonSimulator.computeOptimalSetpoint(0.1, 1100., -100., -50, ara1, -3.1, 3.1);
        assertEquals(2.1, optimalSetpoint, DOUBLE_TOLERANCE);
        // Increase flow from -1100 to -1000 with two setpoint changes, sensi < 0
        optimalSetpoint = automatonSimulator.computeOptimalSetpoint(0.1, -1100., -100., -50, ara1, -3.1, 3.1);
        assertEquals(-2.1, optimalSetpoint, DOUBLE_TOLERANCE);
    }

    @Test
    public void testShiftRangeActionsUntilFlowCnecsSecureCase1() {
        FlowCnec cnec = mock(FlowCnec.class);
        when(cnec.getMonitoredSides()).thenReturn(Set.of(Side.RIGHT));

        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        // suppose threshold is -1000, flow is -1100 then -1010 then -1000
        // getFlow is called once in every iteration
        when(mockedPrePerimeterResult.getFlow(cnec, Side.RIGHT, Unit.MEGAWATT)).thenReturn(-1100., -1010., -1000.);
        // getMargin is called once before loop, once in 1st iteration, once in second iteration
        when(mockedPrePerimeterResult.getMargin(cnec, Unit.MEGAWATT)).thenReturn(-100., -10., 0.);
        // getMargin with side is called once before loop, once in 1st iteration, once in second iteration
        when(mockedPrePerimeterResult.getMargin(cnec, Side.RIGHT, Unit.MEGAWATT)).thenReturn(-100., -100., -10., -10., 0.);
        // suppose approx sensi is +50 on both RAs first, then +5 (so +100 then +10 total)
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, Side.RIGHT, ara1, Unit.MEGAWATT)).thenReturn(50., 5.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, Side.RIGHT, ara2, Unit.MEGAWATT)).thenReturn(50., 5.);
        // so PSTs should be shifted to setpoint +1.1 on first iteration, then +3.1 on second because of under-estimator

        Pair<PrePerimeterResult, Map<RangeAction<?>, Double>> shiftResult =
            automatonSimulator.shiftRangeActionsUntilFlowCnecsSecure(List.of(ara1, ara2), Set.of(cnec), network, mockedPreAutoPerimeterSensitivityAnalysis, mockedPrePerimeterResult, autoState);
        assertEquals(3.1, shiftResult.getRight().get(ara1), DOUBLE_TOLERANCE);
        assertEquals(3.1, shiftResult.getRight().get(ara2), DOUBLE_TOLERANCE);
    }

    @Test
    public void testShiftRangeActionsUntilFlowCnecsSecureCase2() {
        FlowCnec cnec = mock(FlowCnec.class);
        when(cnec.getMonitoredSides()).thenReturn(Set.of(Side.LEFT, Side.RIGHT));

        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        // same as case 1 but flows & sensi are inverted -> setpoints should be the same
        // getFlow is called once in every iteration
        when(mockedPrePerimeterResult.getFlow(cnec, Side.LEFT, Unit.MEGAWATT)).thenReturn(1100., 1010., 1000.);
        when(mockedPrePerimeterResult.getFlow(cnec, Side.RIGHT, Unit.MEGAWATT)).thenReturn(0., 0., 0.);
        // getMargin is called once before loop, once in 1st iteration, once in second iteration
        when(mockedPrePerimeterResult.getMargin(cnec, Unit.MEGAWATT)).thenReturn(-100., -10., 0.);
        // getMargin with side is called once before loop, once in 1st iteration, once in second iteration
        when(mockedPrePerimeterResult.getMargin(cnec, Side.LEFT, Unit.MEGAWATT)).thenReturn(-100., -100., -10., -10., 0.);
        when(mockedPrePerimeterResult.getMargin(cnec, Side.RIGHT, Unit.MEGAWATT)).thenReturn(100.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, Side.LEFT, ara1, Unit.MEGAWATT)).thenReturn(-50., -5.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, Side.LEFT, ara2, Unit.MEGAWATT)).thenReturn(-50., -5.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, Side.RIGHT, ara1, Unit.MEGAWATT)).thenReturn(-50., -5.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, Side.RIGHT, ara2, Unit.MEGAWATT)).thenReturn(-50., -5.);

        Pair<PrePerimeterResult, Map<RangeAction<?>, Double>> shiftResult =
            automatonSimulator.shiftRangeActionsUntilFlowCnecsSecure(List.of(ara1, ara2), Set.of(cnec), network, mockedPreAutoPerimeterSensitivityAnalysis, mockedPrePerimeterResult, autoState);
        assertEquals(3.1, shiftResult.getRight().get(ara1), DOUBLE_TOLERANCE);
        assertEquals(3.1, shiftResult.getRight().get(ara2), DOUBLE_TOLERANCE);
    }

    @Test
    public void testShiftRangeActionsUntilFlowCnecsSecureCase3() {
        FlowCnec cnec = mock(FlowCnec.class);
        when(cnec.getMonitoredSides()).thenReturn(Set.of(Side.LEFT));

        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        // same as case 1 but flows are inverted -> setpoints should be inverted
        // getFlow is called once in every iteration
        when(mockedPrePerimeterResult.getFlow(cnec, Side.LEFT, Unit.MEGAWATT)).thenReturn(1100., 1010., 1000.);
        // getMargin is called once before loop, once in second iteration
        when(mockedPrePerimeterResult.getMargin(cnec, Unit.MEGAWATT)).thenReturn(-100., -10., 0.);
        // getMargin with side is called once before loop, once in 1st iteration, once in second iteration
        when(mockedPrePerimeterResult.getMargin(cnec, Side.LEFT, Unit.MEGAWATT)).thenReturn(-100., -100., -10., -10., 0.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, Side.LEFT, ara1, Unit.MEGAWATT)).thenReturn(50., 5.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, Side.LEFT, ara2, Unit.MEGAWATT)).thenReturn(50., 5.);

        Pair<PrePerimeterResult, Map<RangeAction<?>, Double>> shiftResult =
            automatonSimulator.shiftRangeActionsUntilFlowCnecsSecure(List.of(ara1, ara2), Set.of(cnec), network, mockedPreAutoPerimeterSensitivityAnalysis, mockedPrePerimeterResult, autoState);
        assertEquals(-3.1, shiftResult.getRight().get(ara1), DOUBLE_TOLERANCE);
        assertEquals(-3.1, shiftResult.getRight().get(ara2), DOUBLE_TOLERANCE);
    }

    @Test
    public void testShiftRangeActionsUntilFlowCnecsSecureCase4() {
        FlowCnec cnec = mock(FlowCnec.class);
        when(cnec.getMonitoredSides()).thenReturn(Set.of(Side.RIGHT));

        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        // same as case 1 but sensi are inverted -> setpoints should be inverted
        // + added a cnec with sensi = 0
        // getFlow is called once in every iteration
        when(mockedPrePerimeterResult.getFlow(cnec, Side.RIGHT, Unit.MEGAWATT)).thenReturn(-1100., -1010., -1000.);
        // getMargin is called once before loop, once in 1st iteration when most limiting cnec is different, once in second iteration, once in third iteration
        when(mockedPrePerimeterResult.getMargin(cnec, Unit.MEGAWATT)).thenReturn(-100., -100., -100., -10., 0.);
        // getMargin with side is called once before loop, once in 1st iteration when most limiting cnec is different, once in second iteration, once in third iteration
        when(mockedPrePerimeterResult.getMargin(cnec, Side.RIGHT, Unit.MEGAWATT)).thenReturn(-100., -100., -100., -100., -10., -10., 0.);
        // computeMargin is called once in 1st iteration, once in second iteration, once in third iteration
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, Side.RIGHT, ara1, Unit.MEGAWATT)).thenReturn(-50., -5.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec, Side.RIGHT, ara2, Unit.MEGAWATT)).thenReturn(-50., -5.);

        FlowCnec cnec2 = mock(FlowCnec.class);
        when(cnec2.getMonitoredSides()).thenReturn(Set.of(Side.RIGHT));
        when(mockedPrePerimeterResult.getFlow(cnec2, Side.RIGHT, Unit.MEGAWATT)).thenReturn(2200.);
        when(mockedPrePerimeterResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(-200.);
        when(mockedPrePerimeterResult.getMargin(cnec2, Side.RIGHT, Unit.MEGAWATT)).thenReturn(-200.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec2, Side.RIGHT, ara1, Unit.MEGAWATT)).thenReturn(0.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec2, Side.RIGHT, ara2, Unit.MEGAWATT)).thenReturn(0.);

        Pair<PrePerimeterResult, Map<RangeAction<?>, Double>> shiftResult =
            automatonSimulator.shiftRangeActionsUntilFlowCnecsSecure(List.of(ara1, ara2), Set.of(cnec, cnec2), network, mockedPreAutoPerimeterSensitivityAnalysis, mockedPrePerimeterResult, autoState);
        assertEquals(-3.1, shiftResult.getRight().get(ara1), DOUBLE_TOLERANCE);
        assertEquals(-3.1, shiftResult.getRight().get(ara2), DOUBLE_TOLERANCE);
    }

    @Test
    public void testSimulateRangeAutomatons() {
        State curativeState = mock(State.class);
        when(curativeState.getInstant()).thenReturn(Instant.CURATIVE);
        when(curativeState.getContingency()).thenReturn(Optional.of(crac.getContingency("contingency1")));

        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        // only keep ara1 & ara2
        Set<String> toRemove = crac.getRangeActions().stream().map(Identifiable::getId).collect(Collectors.toSet());
        toRemove.remove("ara1");
        toRemove.remove("ara2");
        toRemove.forEach(ra -> crac.removeRemedialAction(ra));

        when(mockedPrePerimeterResult.getFlow(cnec1, Side.RIGHT, Unit.MEGAWATT)).thenReturn(1100.);
        when(mockedPrePerimeterResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(-100.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec1, Side.RIGHT, ara1, Unit.MEGAWATT)).thenReturn(0.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec1, Side.RIGHT, ara2, Unit.MEGAWATT)).thenReturn(0.);

        AutomatonSimulator.RangeAutomatonSimulationResult result = automatonSimulator.simulateRangeAutomatons(autoState, curativeState, network, mockedPreAutoPerimeterSensitivityAnalysis, mockedPrePerimeterResult);

        assertNotNull(result);
        assertNotNull(result.getPerimeterResult());
        assertNotNull(result.getActivatedRangeActions());
        assertTrue(result.getActivatedRangeActions().isEmpty());
        assertEquals(Map.of(ara1, 0.1, ara2, 0.1), result.getRangeActionsWithSetpoint());
    }

    @Test
    public void testSimulateTopologicalAutomatons() {
        // margin < 0 => activate NA
        when(mockedPrePerimeterResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(-100.);
        AutomatonSimulator.TopoAutomatonSimulationResult result = automatonSimulator.simulateTopologicalAutomatons(autoState, network, mockedPreAutoPerimeterSensitivityAnalysis);
        assertNotNull(result);
        assertNotNull(result.getPerimeterResult());
        assertEquals(Set.of(na), result.getActivatedNetworkActions());

        // margin = 0 => activate NA
        when(mockedPrePerimeterResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(0.);
        result = automatonSimulator.simulateTopologicalAutomatons(autoState, network, mockedPreAutoPerimeterSensitivityAnalysis);
        assertNotNull(result);
        assertNotNull(result.getPerimeterResult());
        assertEquals(Set.of(na), result.getActivatedNetworkActions());

        // margin > 0 => do not activate NA
        when(mockedPrePerimeterResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(1.);
        result = automatonSimulator.simulateTopologicalAutomatons(autoState, network, mockedPreAutoPerimeterSensitivityAnalysis);
        assertNotNull(result);
        assertNotNull(result.getPerimeterResult());
        assertEquals(Set.of(), result.getActivatedNetworkActions());
    }

    @Test
    public void testSimulateAutomatonState() {
        State curativeState = mock(State.class);
        when(curativeState.getInstant()).thenReturn(Instant.CURATIVE);
        when(curativeState.getContingency()).thenReturn(Optional.of(crac.getContingency("contingency1")));

        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        // only keep ara1, ara2 & na
        Set<String> toRemove = crac.getRemedialActions().stream().map(Identifiable::getId).collect(Collectors.toSet());
        toRemove.remove("ara1");
        toRemove.remove("ara2");
        toRemove.remove("na");
        toRemove.forEach(ra -> crac.removeRemedialAction(ra));

        when(mockedPrePerimeterResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(100.);
        when(mockedPrePerimeterResult.getFlow(cnec1, Side.RIGHT, Unit.MEGAWATT)).thenReturn(1100.);
        when(mockedPrePerimeterResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(-100.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec1, Side.RIGHT, ara1, Unit.MEGAWATT)).thenReturn(0.);
        when(mockedPrePerimeterResult.getSensitivityValue(cnec1, Side.RIGHT, ara2, Unit.MEGAWATT)).thenReturn(0.);

        ObjectiveFunction objectiveFunction = Mockito.mock(ObjectiveFunction.class);
        when(mockedPrePerimeterResult.getObjectiveFunction()).thenReturn(objectiveFunction);
        when(objectiveFunction.getFlowCnecs()).thenReturn(Set.of(cnec1, cnec2));
        RangeActionSetpointResult rangeActionSetpointResult = Mockito.mock(RangeActionSetpointResult.class);
        when(mockedPrePerimeterResult.getRangeActionSetpointResult()).thenReturn(rangeActionSetpointResult);
        when(rangeActionSetpointResult.getRangeActions()).thenReturn(Collections.emptySet());
        when(mockedPrePerimeterResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);

        AutomatonPerimeterResultImpl result = automatonSimulator.simulateAutomatonState(autoState, curativeState, network);
        assertNotNull(result);
        assertEquals(Set.of(), result.getActivatedNetworkActions());
        assertEquals(Set.of(), result.getActivatedRangeActions(autoState));
        assertEquals(Map.of(ara1, 0.1, ara2, 0.1), result.getOptimizedSetpointsOnState(autoState));
    }

    @Test
    public void testDisableHvdcAngleDroopControlBeforeShifting() {
        PrePerimeterResult prePerimeterResult = mock(PrePerimeterResult.class);
        when(mockedPreAutoPerimeterSensitivityAnalysis.runBasedOnInitialResults(any(), any(), any(), any(), any(), any())).thenReturn(mockedPrePerimeterResult);

        // check that angle-droop control was not disabled when margins are positive
        when(prePerimeterResult.getMargin(cnec1, Side.RIGHT, Unit.MEGAWATT)).thenReturn(0.);
        when(prePerimeterResult.getMargin(cnec2, Side.RIGHT, Unit.MEGAWATT)).thenReturn(100.);
        automatonSimulator.shiftRangeActionsUntilFlowCnecsSecure(List.of(hvdcRa1, hvdcRa2), Set.of(cnec1, cnec2), network, mockedPreAutoPerimeterSensitivityAnalysis, prePerimeterResult, autoState);
        assertTrue(network.getHvdcLine("BBE2AA11 FFR3AA11 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertTrue(network.getHvdcLine("BBE2AA12 FFR3AA12 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());

        // check that angle-droop control is disabled when one margin is negative
        when(prePerimeterResult.getMargin(cnec1, Side.RIGHT, Unit.MEGAWATT)).thenReturn(-1.);
        when(prePerimeterResult.getMargin(cnec2, Side.RIGHT, Unit.MEGAWATT)).thenReturn(100.);
        automatonSimulator.shiftRangeActionsUntilFlowCnecsSecure(List.of(hvdcRa1, hvdcRa2), Set.of(cnec1, cnec2), network, mockedPreAutoPerimeterSensitivityAnalysis, prePerimeterResult, autoState);
        assertFalse(network.getHvdcLine("BBE2AA11 FFR3AA11 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertFalse(network.getHvdcLine("BBE2AA12 FFR3AA12 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertEquals(2450.87, network.getHvdcLine("BBE2AA11 FFR3AA11 1").getActivePowerSetpoint(), DOUBLE_TOLERANCE);
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER, network.getHvdcLine("BBE2AA11 FFR3AA11 1").getConvertersMode());
        assertEquals(46.61, network.getHvdcLine("BBE2AA12 FFR3AA12 1").getActivePowerSetpoint(), DOUBLE_TOLERANCE);
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER, network.getHvdcLine("BBE2AA12 FFR3AA12 1").getConvertersMode());
    }

}
