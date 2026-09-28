/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.action.Action;
import com.powsybl.action.AreaInterchangeTargetAction;
import com.powsybl.action.GeneratorActionBuilder;
import com.powsybl.action.HvdcActionBuilder;
import com.powsybl.action.LoadActionBuilder;
import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.action.RatioTapChangerTapPositionAction;
import com.powsybl.action.ShuntCompensatorPositionActionBuilder;
import com.powsybl.action.SwitchAction;
import com.powsybl.action.TerminalsConnectionAction;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class SystematicSensitivityAdapterTest {

    public static final double DOUBLE_TOLERANCE = 1e-6;
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String CURATIVE_INSTANT_ID = "curative";

    @Test
    void testWithoutAppliedRa() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(ONE, TWO));
        Instant outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(network, factorProvider, new SensitivityAnalysisParameters(), "MockSensi", outageInstant);

        // "standard results" of the MockSensiProvider are expected
        assertEquals(10, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), ONE), DOUBLE_TOLERANCE);
        assertEquals(-15, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), TWO), DOUBLE_TOLERANCE);

        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), ONE), DOUBLE_TOLERANCE);
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), ONE), DOUBLE_TOLERANCE);
        assertEquals(25, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), TWO), DOUBLE_TOLERANCE);
        assertEquals(25, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), TWO), DOUBLE_TOLERANCE);

        assertEquals(25, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), ONE), DOUBLE_TOLERANCE);
        assertEquals(-30, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), TWO), DOUBLE_TOLERANCE);

        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), ONE), DOUBLE_TOLERANCE);
        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), ONE), DOUBLE_TOLERANCE);
        assertEquals(205, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), TWO), DOUBLE_TOLERANCE);
        assertEquals(205, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), TWO), DOUBLE_TOLERANCE);

        assertEquals(0.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), ONE), DOUBLE_TOLERANCE);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), ONE), DOUBLE_TOLERANCE);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), ONE), DOUBLE_TOLERANCE);
        assertEquals(-0.55, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), TWO), DOUBLE_TOLERANCE);
        assertEquals(5.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), TWO), DOUBLE_TOLERANCE);
        assertEquals(5.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), TWO), DOUBLE_TOLERANCE);
    }

    @Test
    void testWithoutAppliedRaLeftSideOnly() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(ONE));
        Instant outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(network, factorProvider, new SensitivityAnalysisParameters(), "MockSensi", outageInstant);

        // "standard results" of the MockSensiProvider are expected
        assertEquals(10, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), ONE), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), TWO), DOUBLE_TOLERANCE);

        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), ONE), DOUBLE_TOLERANCE);
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), ONE), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), TWO), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), TWO), DOUBLE_TOLERANCE);

        assertEquals(25, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), ONE), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), TWO), DOUBLE_TOLERANCE);

        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), ONE), DOUBLE_TOLERANCE);
        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), ONE), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), TWO), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), TWO), DOUBLE_TOLERANCE);

        assertEquals(0.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), ONE), DOUBLE_TOLERANCE);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), ONE), DOUBLE_TOLERANCE);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), ONE), DOUBLE_TOLERANCE);
        assertEquals(0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), TWO), DOUBLE_TOLERANCE);
        assertEquals(0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), TWO), DOUBLE_TOLERANCE);
        assertEquals(0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), TWO), DOUBLE_TOLERANCE);
    }

    @Test
    void testWithAppliedRa() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(ONE, TWO));
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        crac.newFlowCnec()
            .withId("cnec2stateOutageContingency1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMin(-1500.).withMax(1500.).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(TWO).withMin(-1500.).withMax(1500.).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(ONE).withMin(-0.3).withMax(0.3).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(TWO).withMin(-0.3).withMax(0.3).add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR3", curativeInstant), crac.getPstRangeAction("pst"), -3.1);

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(
            network,
            factorProvider,
            appliedRemedialActions,
            new SensitivityAnalysisParameters(),
            "MockSensi",
            crac.getOutageInstant()
        );

        // after initial state or contingency without CRA, "standard results" of the MockSensiProvider are expected
        assertEquals(10, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), ONE), DOUBLE_TOLERANCE);
        assertEquals(-15, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), TWO), DOUBLE_TOLERANCE);

        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), ONE), DOUBLE_TOLERANCE);
        assertEquals(25, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), TWO), DOUBLE_TOLERANCE);

        assertEquals(25, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), ONE), DOUBLE_TOLERANCE);
        assertEquals(-30, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), TWO), DOUBLE_TOLERANCE);

        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), ONE), DOUBLE_TOLERANCE);
        assertEquals(205, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), TWO), DOUBLE_TOLERANCE);

        assertEquals(0.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), ONE), DOUBLE_TOLERANCE);
        assertEquals(-0.55, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), TWO), DOUBLE_TOLERANCE);

        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), ONE), DOUBLE_TOLERANCE);
        assertEquals(5.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), TWO), DOUBLE_TOLERANCE);

        // after contingency with CRA, "alternative" results of the MockSensiProvider are expected
        assertEquals(-40, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), ONE), DOUBLE_TOLERANCE);
        assertEquals(45, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), TWO), DOUBLE_TOLERANCE);

        assertEquals(-90, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), ONE), DOUBLE_TOLERANCE);
        assertEquals(95, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), TWO), DOUBLE_TOLERANCE);

        assertEquals(-2.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), ONE), DOUBLE_TOLERANCE);
        assertEquals(3.0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), TWO), DOUBLE_TOLERANCE);

        // for outage CNECs, do NOT take CRAs into account
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec2stateOutageContingency1"), ONE), DOUBLE_TOLERANCE);
        assertEquals(25, result.getReferenceFlow(crac.getFlowCnec("cnec2stateOutageContingency1"), TWO), DOUBLE_TOLERANCE);

        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateOutageContingency1"), ONE), DOUBLE_TOLERANCE);
        assertEquals(205, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateOutageContingency1"), TWO), DOUBLE_TOLERANCE);

        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateOutageContingency1"), ONE), DOUBLE_TOLERANCE);
        assertEquals(5.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateOutageContingency1"), TWO), DOUBLE_TOLERANCE);
    }

    @Test
    void testWithAppliedRaNotSupportedByOperatorStrategies() {
        // the applied range action cannot be converted to actions: the fallback path applies it on a network variant
        // and runs one sensitivity analysis per state with RA
        Network network = NetworkImportsUtil.import12NodesNetwork();
        String workingVariantId = network.getVariantManager().getWorkingVariantId();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(ONE, TWO));
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        RangeAction<?> unsupportedRangeAction = Mockito.mock(RangeAction.class);
        when(unsupportedRangeAction.getCurrentSetpoint(any())).thenReturn(0.);
        when(unsupportedRangeAction.toActions(anyDouble(), any())).thenThrow(new UnsupportedOperationException("toActions method not implemented"));
        doAnswer(invocation -> {
            // applying the range action moves the PST, which makes the MockSensiProvider return its "alternative" results
            Network n = invocation.getArgument(0);
            n.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().setTapPosition(-3);
            return null;
        }).when(unsupportedRangeAction).apply(any(), anyDouble());
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR3", curativeInstant), unsupportedRangeAction, -3.1);

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(
            network,
            factorProvider,
            appliedRemedialActions,
            new SensitivityAnalysisParameters(),
            "MockSensi",
            crac.getOutageInstant()
        );

        // after initial state or contingency without CRA, "standard results" of the MockSensiProvider are expected
        assertEquals(10, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), ONE), DOUBLE_TOLERANCE);
        assertEquals(-15, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), TWO), DOUBLE_TOLERANCE);
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), ONE), DOUBLE_TOLERANCE);
        assertEquals(25, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), TWO), DOUBLE_TOLERANCE);

        // after contingency with CRA, "alternative" results of the MockSensiProvider are expected
        assertEquals(-40, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), ONE), DOUBLE_TOLERANCE);
        assertEquals(45, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), TWO), DOUBLE_TOLERANCE);
        assertEquals(-2.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), ONE), DOUBLE_TOLERANCE);
        assertEquals(3.0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), TWO), DOUBLE_TOLERANCE);

        // the network is left untouched: working variant restored, temporary variant removed, PST at its initial tap
        assertEquals(workingVariantId, network.getVariantManager().getWorkingVariantId());
        assertEquals(1, network.getVariantManager().getVariantIds().size());
        assertEquals(0, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
    }

    @Test
    void testIsSupportedByOperatorStrategies() {
        Action pstTap = new PhaseTapChangerTapPositionAction("pst", "transformer", false, 3);
        Action terminals = new TerminalsConnectionAction("line", "line", true);
        Action switchAction = new SwitchAction("switch", "switch", true);
        Action generator = new GeneratorActionBuilder().withId("gen").withGeneratorId("gen").withActivePowerRelativeValue(false).withActivePowerValue(100.).build();
        Action load = new LoadActionBuilder().withId("load").withLoadId("load").withRelativeValue(false).withActivePowerValue(100.).build();
        Action ratioTap = new RatioTapChangerTapPositionAction("rtc", "transformer", false, 3);
        Action shunt = new ShuntCompensatorPositionActionBuilder().withId("shunt").withShuntCompensatorId("shunt").withSectionCount(2).build();
        Action area = new AreaInterchangeTargetAction("area", "area", 100.);
        Action hvdcSetpoint = new HvdcActionBuilder().withId("hvdc1").withHvdcId("hvdc").withActivePowerSetpoint(100.).build();
        Action hvdcDisableAcEmulation = new HvdcActionBuilder().withId("hvdc2").withHvdcId("hvdc").withAcEmulationEnabled(false).build();
        Action hvdcEnableAcEmulation = new HvdcActionBuilder().withId("hvdc3").withHvdcId("hvdc").withAcEmulationEnabled(true).build();
        Action hvdcNoEffect = new HvdcActionBuilder().withId("hvdc4").withHvdcId("hvdc").withDroop(100.).build();

        // supported by the Woodbury engine: DC and AC
        for (Action action : List.of(pstTap, terminals, switchAction, generator, load)) {
            assertTrue(SystematicSensitivityAdapter.isSupportedByOperatorStrategies(action, true), action.getId());
            assertTrue(SystematicSensitivityAdapter.isSupportedByOperatorStrategies(action, false), action.getId());
        }
        // AC only
        for (Action action : List.of(ratioTap, shunt, area, hvdcSetpoint, hvdcDisableAcEmulation)) {
            assertFalse(SystematicSensitivityAdapter.isSupportedByOperatorStrategies(action, true), action.getId());
            assertTrue(SystematicSensitivityAdapter.isSupportedByOperatorStrategies(action, false), action.getId());
        }
        // never supported
        for (Action action : List.of(hvdcEnableAcEmulation, hvdcNoEffect)) {
            assertFalse(SystematicSensitivityAdapter.isSupportedByOperatorStrategies(action, true), action.getId());
            assertFalse(SystematicSensitivityAdapter.isSupportedByOperatorStrategies(action, false), action.getId());
        }
    }

    @Test
    void testConvertToOperatorStrategyActions() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(ONE, TWO));
        State curativeState = crac.getState("Contingency FR1 FR3", crac.getInstant(CURATIVE_INSTANT_ID));

        // PST range action: supported in DC and AC
        AppliedRemedialActions pstApplied = new AppliedRemedialActions();
        pstApplied.addAppliedRangeAction(curativeState, crac.getPstRangeAction("pst"), -3.1);
        Optional<Map<State, List<Action>>> converted = SystematicSensitivityAdapter.convertToOperatorStrategyActions(Set.of(curativeState), pstApplied, network, true);
        assertTrue(converted.isPresent());
        assertEquals(1, converted.get().get(curativeState).size());
        assertTrue(converted.get().get(curativeState).get(0) instanceof PhaseTapChangerTapPositionAction);
        assertTrue(SystematicSensitivityAdapter.convertToOperatorStrategyActions(Set.of(curativeState), pstApplied, network, false).isPresent());

        // network action with an HVDC elementary action: supported in AC only
        NetworkAction hvdcNetworkAction = Mockito.mock(NetworkAction.class);
        when(hvdcNetworkAction.getElementaryActions()).thenReturn(Set.of(new HvdcActionBuilder().withId("hvdc").withHvdcId("hvdc").withAcEmulationEnabled(false).build()));
        AppliedRemedialActions hvdcApplied = new AppliedRemedialActions();
        hvdcApplied.addAppliedNetworkAction(curativeState, hvdcNetworkAction);
        assertTrue(SystematicSensitivityAdapter.convertToOperatorStrategyActions(Set.of(curativeState), hvdcApplied, network, true).isEmpty());
        assertTrue(SystematicSensitivityAdapter.convertToOperatorStrategyActions(Set.of(curativeState), hvdcApplied, network, false).isPresent());

        // range action that cannot be converted: never supported
        RangeAction<?> unsupportedRangeAction = Mockito.mock(RangeAction.class);
        when(unsupportedRangeAction.toActions(anyDouble(), any())).thenThrow(new UnsupportedOperationException("toActions method not implemented"));
        AppliedRemedialActions unsupportedApplied = new AppliedRemedialActions();
        unsupportedApplied.addAppliedRangeAction(curativeState, unsupportedRangeAction, 10.);
        assertTrue(SystematicSensitivityAdapter.convertToOperatorStrategyActions(Set.of(curativeState), unsupportedApplied, network, true).isEmpty());
        assertTrue(SystematicSensitivityAdapter.convertToOperatorStrategyActions(Set.of(curativeState), unsupportedApplied, network, false).isEmpty());
    }

    @Test
    void testCatchInRunSensitivity() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        network.setName("Mock_Exception");
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(ONE, TWO));
        Instant outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));
        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(network, factorProvider, new SensitivityAnalysisParameters(), "MockSensi", outageInstant);
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus());
    }

    @Test
    void testFirstCatchInRunSensitivityWithAppliedRa() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        network.setName("Mock_Exception");
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(ONE, TWO));
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        crac.newFlowCnec()
            .withId("cnec2stateOutageContingency1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMin(-1500.).withMax(1500.).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(TWO).withMin(-1500.).withMax(1500.).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(ONE).withMin(-0.3).withMax(0.3).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(TWO).withMin(-0.3).withMax(0.3).add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR3", curativeInstant), crac.getPstRangeAction("pst"), -3.1);

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(
            network,
            factorProvider,
            appliedRemedialActions,
            new SensitivityAnalysisParameters(),
            "MockSensi",
            crac.getOutageInstant()
        );
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus());
    }

    @Test
    void testSecondCatchInRunSensitivityWithAppliedRa() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        network.setName("Second_Run_Exception");
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(ONE, TWO));
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        crac.newFlowCnec()
            .withId("cnec2stateOutageContingency1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMin(-1500.).withMax(1500.).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(TWO).withMin(-1500.).withMax(1500.).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(ONE).withMin(-0.3).withMax(0.3).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(TWO).withMin(-0.3).withMax(0.3).add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR3", curativeInstant), crac.getPstRangeAction("pst"), -3.1);

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(
            network,
            factorProvider,
            appliedRemedialActions,
            new SensitivityAnalysisParameters(),
            "MockSensi",
            crac.getOutageInstant()
        );
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.PARTIAL_FAILURE, result.getStatus());
    }
}
