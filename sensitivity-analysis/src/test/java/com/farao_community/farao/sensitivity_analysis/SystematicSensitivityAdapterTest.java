/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.sensitivity.*;
import org.junit.Test;

import java.util.Set;

import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static com.farao_community.farao.data.crac_api.cnec.Side.RIGHT;
import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class SystematicSensitivityAdapterTest {

    public static final double DOUBLE_TOLERANCE = 1e-6;
    Network network;

    @Test
    public void testWithoutAppliedRa() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(LEFT, RIGHT));
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(network, factorProvider, new SensitivityAnalysisParameters(), "MockSensi");

        // "standard results" of the MockSensiProvider are expected
        assertEquals(10, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(-15, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(25, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT), DOUBLE_TOLERANCE);
        assertEquals(25, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(25, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(-30, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(205, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT), DOUBLE_TOLERANCE);
        assertEquals(205, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(0.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(-0.55, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), RIGHT), DOUBLE_TOLERANCE);
        assertEquals(5.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT), DOUBLE_TOLERANCE);
        assertEquals(5.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWithoutAppliedRaLeftSideOnly() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(LEFT));
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(network, factorProvider, new SensitivityAnalysisParameters(), "MockSensi");

        // "standard results" of the MockSensiProvider are expected
        assertEquals(10, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(25, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(0.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), RIGHT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWithAppliedRa() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(LEFT, RIGHT));
        crac.newFlowCnec()
            .withId("cnec2stateOutageContingency1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(Instant.OUTAGE)
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMin(-1500.).withMax(1500.).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(RIGHT).withMin(-1500.).withMax(1500.).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(LEFT).withMin(-0.3).withMax(0.3).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(RIGHT).withMin(-0.3).withMax(0.3).add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR3", Instant.CURATIVE), crac.getPstRangeAction("pst"), -3.1);

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(network, factorProvider, appliedRemedialActions, new SensitivityAnalysisParameters(), "MockSensi");

        // after initial state or contingency without CRA, "standard results" of the MockSensiProvider are expected
        assertEquals(10, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(-15, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(25, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(25, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(-30, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(205, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(0.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(-0.55, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(5.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT), DOUBLE_TOLERANCE);

        // after contingency with CRA, "alternative" results of the MockSensiProvider are expected
        assertEquals(-40, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(45, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-90, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(95, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-2.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(3.0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT), DOUBLE_TOLERANCE);

        // for outage CNECs, do NOT take CRAs into account
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec2stateOutageContingency1"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(25, result.getReferenceFlow(crac.getFlowCnec("cnec2stateOutageContingency1"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateOutageContingency1"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(205, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateOutageContingency1"), RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateOutageContingency1"), LEFT), DOUBLE_TOLERANCE);
        assertEquals(5.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateOutageContingency1"), RIGHT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testDisableHvdcAngleDroopControl() {
        network = Network.read("TestCase16NodesWith2Hvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWith2Hvdc.xiidm"));
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

        Crac crac = CracFactory.findDefault().create("test-crac");
        Contingency contingency1 = crac.newContingency()
                .withId("contingency1")
                .withNetworkElement("NNL1AA11 NNL2AA11 1")
                .add();
        crac.newFlowCnec()
                .withId("cnec1")
                .withNetworkElement("cnec-ne")
                .withContingency("contingency1")
                .withInstant(Instant.AUTO)
                .withNominalVoltage(220.)
                .newThreshold().withSide(Side.RIGHT).withMax(1000.).withUnit(Unit.AMPERE).add()
                .add();

        // Add HVDC range actions
        HvdcRangeAction hvdcRa1 = crac.newHvdcRangeAction()
                .withId("hvdc-ra1")
                .withGroupId("hvdcGroup")
                .withNetworkElement("BBE2AA11 FFR3AA11 1")
                .withSpeed(1)
                .newRange().withMax(3000).withMin(-3000).add()
                .newFreeToUseUsageRule().withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
                .add();
        HvdcRangeAction hvdcRa2 = crac.newHvdcRangeAction()
                .withId("hvdc-ra2")
                .withGroupId("hvdcGroup")
                .withNetworkElement("BBE2AA12 FFR3AA12 1")
                .withSpeed(1)
                .newRange().withMax(3000).withMin(-3000).add()
                .newFreeToUseUsageRule().withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
                .add();

        State autoState = crac.getState(contingency1, Instant.AUTO);

        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedRangeAction(autoState, hvdcRa1, 10.1);
        SystematicSensitivityAdapter.findAndDisableHvdcAngleDroopActivePowerControl(network, appliedRemedialActions, autoState);
        // check that angle-droop control was disabled on HVDC
        assertFalse(network.getHvdcLine("BBE2AA11 FFR3AA11 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        // check that other HVDC was not touched
        assertTrue(network.getHvdcLine("BBE2AA12 FFR3AA12 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER, network.getHvdcLine(hvdcRa1.getNetworkElement().getId()).getConvertersMode());
        assertEquals(10.1, network.getHvdcLine(hvdcRa1.getNetworkElement().getId()).getActivePowerSetpoint(), DOUBLE_TOLERANCE);
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER, network.getHvdcLine(hvdcRa2.getNetworkElement().getId()).getConvertersMode());
        assertEquals(0, network.getHvdcLine(hvdcRa2.getNetworkElement().getId()).getActivePowerSetpoint(), DOUBLE_TOLERANCE);

        // With 2 HVDC
        AppliedRemedialActions appliedRemedialActions2 = new AppliedRemedialActions();
        appliedRemedialActions2.addAppliedRangeAction(autoState, hvdcRa1, 100.1);
        appliedRemedialActions2.addAppliedRangeAction(autoState, hvdcRa2, 200.2);
        SystematicSensitivityAdapter.findAndDisableHvdcAngleDroopActivePowerControl(network, appliedRemedialActions2, autoState);
        // check that angle-droop control was disabled on HVDC
        assertFalse(network.getHvdcLine("BBE2AA11 FFR3AA11 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertFalse(network.getHvdcLine("BBE2AA12 FFR3AA12 1").getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled());
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER, network.getHvdcLine(hvdcRa1.getNetworkElement().getId()).getConvertersMode());
        // check that angle-droop control was previously disabled => setpoint should not be updated
        assertEquals(10.1, network.getHvdcLine(hvdcRa1.getNetworkElement().getId()).getActivePowerSetpoint(), DOUBLE_TOLERANCE);
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER, network.getHvdcLine(hvdcRa2.getNetworkElement().getId()).getConvertersMode());
        assertEquals(200.2, network.getHvdcLine(hvdcRa2.getNetworkElement().getId()).getActivePowerSetpoint(), DOUBLE_TOLERANCE);
    }
}
