/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

class RaoUtilTest {
    private static final double DOUBLE_TOLERANCE = 0.1;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String CURATIVE_INSTANT_ID = "curative";
    private static final String AUTO_INSTANT_ID = "auto";

    private RaoParameters raoParameters;
    private RaoInput raoInput;
    private Network network;
    private Crac crac;
    private String variantId;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.createWithPreventivePstRange();
        variantId = network.getVariantManager().getWorkingVariantId();
        raoInput = RaoInput.buildWithPreventiveState(network, crac)
            .withNetworkVariantId(variantId)
            .build();
        raoParameters = new RaoParameters();
    }

    private void addGlskProvider() {
        ZonalData<SensitivityVariableSet> glskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/glsk/GlskCountry.xml"))
            .getZonalGlsks(network);
        raoInput = RaoInput.buildWithPreventiveState(network, crac)
            .withNetworkVariantId(variantId)
            .withGlskProvider(glskProvider)
            .build();
    }

    @Test
    void testExceptionForGlskOnRelativeMargin() {
        RelativeMarginsParameters relativeMarginsParameters = new RelativeMarginsParameters();
        raoParameters.setRelativeMarginsParameters(relativeMarginsParameters);
        relativeMarginsParameters.setPtdfBoundariesFromString(new ArrayList<>(Arrays.asList("{FR}-{ES}", "{ES}-{PT}")));
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> RaoUtil.checkParameters(raoParameters, raoInput));
        assertEquals("Objective function MAX_MIN_RELATIVE_MARGIN requires glsks", exception.getMessage());
    }

    @Test
    void testExceptionForNoRelativeMarginParametersOnRelativeMargin() {
        addGlskProvider();
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> RaoUtil.checkParameters(raoParameters, raoInput));
        assertEquals("Objective function MAX_MIN_RELATIVE_MARGIN requires a config with a non empty boundary set", exception.getMessage());
    }

    @Test
    void testExceptionForNullBoundariesOnRelativeMargin() {
        addGlskProvider();
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> RaoUtil.checkParameters(raoParameters, raoInput));
        assertEquals("Objective function MAX_MIN_RELATIVE_MARGIN requires a config with a non empty boundary set", exception.getMessage());
    }

    @Test
    void testExceptionForEmptyBoundariesOnRelativeMargin() {
        addGlskProvider();
        RelativeMarginsParameters relativeMarginsParameters = new RelativeMarginsParameters();
        raoParameters.setRelativeMarginsParameters(relativeMarginsParameters);
        relativeMarginsParameters.setPtdfBoundariesFromString(new ArrayList<>());
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> RaoUtil.checkParameters(raoParameters, raoInput));
        assertEquals("Objective function MAX_MIN_RELATIVE_MARGIN requires a config with a non empty boundary set", exception.getMessage());
    }

    @Test
    void testAmpereWithDc() {
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        OpenRaoSearchTreeParameters searchTreeParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class);
        searchTreeParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters().setDc(true);
        raoParameters.getObjectiveFunctionParameters().setUnit(Unit.AMPERE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> RaoUtil.checkParameters(raoParameters, raoInput));
        assertEquals("Objective function unit A cannot be calculated with a DC default sensitivity engine", exception.getMessage());
    }

    @Test
    void testGetBranchFlowUnitMultiplier() {
        FlowCnec cnec = Mockito.mock(FlowCnec.class);
        Mockito.when(cnec.getNominalVoltage(TwoSides.ONE)).thenReturn(400.);
        Mockito.when(cnec.getNominalVoltage(TwoSides.TWO)).thenReturn(200.);

        assertEquals(1., RaoUtil.getFlowUnitMultiplier(cnec, TwoSides.ONE, Unit.MEGAWATT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1., RaoUtil.getFlowUnitMultiplier(cnec, TwoSides.TWO, Unit.MEGAWATT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1., RaoUtil.getFlowUnitMultiplier(cnec, TwoSides.ONE, Unit.AMPERE, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1., RaoUtil.getFlowUnitMultiplier(cnec, TwoSides.TWO, Unit.AMPERE, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000 / 400. / Math.sqrt(3), RaoUtil.getFlowUnitMultiplier(cnec, TwoSides.ONE, Unit.MEGAWATT, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(400 * Math.sqrt(3) / 1000., RaoUtil.getFlowUnitMultiplier(cnec, TwoSides.ONE, Unit.AMPERE, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(1000 / 200. / Math.sqrt(3), RaoUtil.getFlowUnitMultiplier(cnec, TwoSides.TWO, Unit.MEGAWATT, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(200 * Math.sqrt(3) / 1000., RaoUtil.getFlowUnitMultiplier(cnec, TwoSides.TWO, Unit.AMPERE, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> RaoUtil.getFlowUnitMultiplier(cnec, TwoSides.ONE, Unit.MEGAWATT, Unit.PERCENT_IMAX));
        assertEquals("Only conversions between MW and A are supported.", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> RaoUtil.getFlowUnitMultiplier(cnec, TwoSides.ONE, Unit.KILOVOLT, Unit.MEGAWATT));
        assertEquals("Only conversions between MW and A are supported.", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> RaoUtil.getFlowUnitMultiplier(cnec, TwoSides.TWO, Unit.AMPERE, Unit.TAP));
        assertEquals("Only conversions between MW and A are supported.", exception.getMessage());
        exception = assertThrows(OpenRaoException.class, () -> RaoUtil.getFlowUnitMultiplier(cnec, TwoSides.TWO, Unit.DEGREE, Unit.AMPERE));
        assertEquals("Only conversions between MW and A are supported.", exception.getMessage());
    }

    @Test
    void testGetLargestCnecThreshold() {
        FlowCnec cnecA = Mockito.mock(FlowCnec.class);
        FlowCnec cnecB = Mockito.mock(FlowCnec.class);
        FlowCnec cnecC = Mockito.mock(FlowCnec.class);
        FlowCnec cnecD = Mockito.mock(FlowCnec.class);
        Mockito.when(cnecA.isOptimized()).thenReturn(true);
        Mockito.when(cnecB.isOptimized()).thenReturn(true);
        Mockito.when(cnecC.isOptimized()).thenReturn(true);
        Mockito.when(cnecD.isOptimized()).thenReturn(false);
        Mockito.when(cnecA.getUpperBound(TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Optional.of(1000.));
        Mockito.when(cnecA.getLowerBound(TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(cnecB.getUpperBound(TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(cnecB.getLowerBound(TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Optional.of(-1500.));
        Mockito.when(cnecC.getUpperBound(TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(cnecC.getLowerBound(TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(cnecD.getUpperBound(TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Optional.of(-16000.));
        Mockito.when(cnecD.getLowerBound(TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Optional.of(-16000.));
        Set.of(cnecA, cnecB, cnecC, cnecD).forEach(cnec -> when(cnec.getMonitoredSides()).thenReturn(Set.of(TwoSides.ONE)));

        assertEquals(1000., RaoUtil.getLargestCnecThreshold(Set.of(cnecA), Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1500., RaoUtil.getLargestCnecThreshold(Set.of(cnecB), Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1500., RaoUtil.getLargestCnecThreshold(Set.of(cnecA, cnecB), Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1500., RaoUtil.getLargestCnecThreshold(Set.of(cnecA, cnecB, cnecC), Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., RaoUtil.getLargestCnecThreshold(Set.of(cnecA, cnecC), Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1500., RaoUtil.getLargestCnecThreshold(Set.of(cnecA, cnecB, cnecD), Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testIsOnFlowConstraintAvailable() {
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        State optimizedState = crac.getState("Contingency FR1 FR3", curativeInstant);

        FlowCnec flowCnec = crac.getFlowCnec("cnec1stateCurativeContingency1");
        FlowResult flowResult = mock(FlowResult.class);
        PrePerimeterResult prePerimeterResult = mock(PrePerimeterResult.class);

        RemedialAction<?> na1 = crac.newNetworkAction().withId("na1")
            .newSwitchAction().withNetworkElement("ne1").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        // Asserts that the method returns True when given an empty set
        assertTrue(RaoUtil.isRemedialActionAvailable(na1, optimizedState, prePerimeterResult, crac.getFlowCnecs(), network, raoParameters));

        RemedialAction<?> na2 = crac.newNetworkAction().withId("na2")
            .newSwitchAction().withNetworkElement("ne2").withActionType(ActionType.OPEN).add()
            .newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec(flowCnec.getId()).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        when(flowResult.getMargin(eq(flowCnec), any())).thenReturn(10.);
        when(prePerimeterResult.getMargin(eq(flowCnec), any())).thenReturn(10.);
        assertFalse(RaoUtil.isRemedialActionAvailable(na2, optimizedState, prePerimeterResult, crac.getFlowCnecs(), network, raoParameters));

        when(flowResult.getMargin(eq(flowCnec), any())).thenReturn(-10.);
        when(prePerimeterResult.getMargin(eq(flowCnec), any())).thenReturn(-10.);
        assertTrue(RaoUtil.isRemedialActionAvailable(na2, optimizedState, prePerimeterResult, crac.getFlowCnecs(), network, raoParameters));

        when(flowResult.getMargin(eq(flowCnec), any())).thenReturn(0.);
        when(prePerimeterResult.getMargin(eq(flowCnec), any())).thenReturn(0.);
        assertTrue(RaoUtil.isRemedialActionAvailable(na2, optimizedState, prePerimeterResult, crac.getFlowCnecs(), network, raoParameters));

        optimizedState = crac.getPreventiveState();
        assertFalse(RaoUtil.isRemedialActionAvailable(na1, optimizedState, prePerimeterResult, crac.getFlowCnecs(), network, raoParameters));
        assertFalse(RaoUtil.isRemedialActionAvailable(na2, optimizedState, prePerimeterResult, crac.getFlowCnecs(), network, raoParameters));

        // asserts that a preventive remedial action with forced usage rule cannot be available
        RemedialAction<?> na3 = crac.newNetworkAction().withId("na3")
            .newTerminalsConnectionAction().withNetworkElement("ne2").withActionType(ActionType.CLOSE).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .add();
        assertFalse(RaoUtil.isRemedialActionAvailable(na3, optimizedState, prePerimeterResult, crac.getFlowCnecs(), network, raoParameters));

        // asserts that a remedial action with no usage rule cannot be available
        NetworkAction networkActionWhithoutUsageRule = Mockito.mock(NetworkAction.class);
        when(networkActionWhithoutUsageRule.getName()).thenReturn("ra without usage rule");
        when(networkActionWhithoutUsageRule.getUsageRules()).thenReturn(Set.of());
        assertFalse(RaoUtil.isRemedialActionAvailable(networkActionWhithoutUsageRule, optimizedState, prePerimeterResult, crac.getFlowCnecs(), network, raoParameters));

        // mock AUTO state for the next assertions
        NetworkAction automatonRa = Mockito.mock(NetworkAction.class);
        when(automatonRa.getName()).thenReturn("fake automaton");
        OnInstant onInstant = Mockito.mock(OnInstant.class);
        OnConstraint<FlowCnec> onFlowConstraint = Mockito.mock(OnConstraint.class);
        State automatonState = Mockito.mock(State.class);
        when(automatonState.getInstant()).thenReturn(crac.getInstant(AUTO_INSTANT_ID));
        when(automatonState.getId()).thenReturn("fake automaton state");

        // remedial action with OnInstant Usage Rule
        when(automatonRa.getUsageRules()).thenReturn(Set.of(onInstant));
        when(onInstant.getUsageMethod(automatonState)).thenReturn(UsageMethod.FORCED);
        assertTrue(RaoUtil.isRemedialActionForced(automatonRa, automatonState, prePerimeterResult, crac.getFlowCnecs(), network, raoParameters));
        when(onInstant.getUsageMethod(automatonState)).thenReturn(UsageMethod.AVAILABLE);
        assertTrue(RaoUtil.isRemedialActionAvailable(automatonRa, automatonState, prePerimeterResult, crac.getFlowCnecs(), network, raoParameters));

        // remedial action with OnFlowConstraint Usage Rule
        when(automatonRa.getUsageRules()).thenReturn(Set.of(onFlowConstraint));
        when(onFlowConstraint.getUsageMethod(automatonState)).thenReturn(UsageMethod.AVAILABLE);
        assertFalse(RaoUtil.isRemedialActionAvailable(automatonRa, automatonState, prePerimeterResult, crac.getFlowCnecs(), network, raoParameters));
        when(onFlowConstraint.getUsageMethod(automatonState)).thenReturn(UsageMethod.FORCED);
        assertFalse(RaoUtil.isRemedialActionAvailable(automatonRa, automatonState, prePerimeterResult, crac.getFlowCnecs(), network, raoParameters));
    }

    @Test
    void testIsOnFlowConstraintInCountryAvailable() {
        Instant preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        State optimizedState = Mockito.mock(State.class);
        when(optimizedState.getInstant()).thenReturn(curativeInstant);

        FlowCnec cnecFrBe = crac.getFlowCnec("cnec1stateCurativeContingency1");
        FlowCnec cnecFrDe = crac.getFlowCnec("cnec2stateCurativeContingency2");
        PrePerimeterResult flowResult = mock(PrePerimeterResult.class);

        RemedialAction<?> na1 = crac.newNetworkAction().withId("na1")
            .newTerminalsConnectionAction().withNetworkElement("ne1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withInstant(CURATIVE_INSTANT_ID).withCountry(Country.FR).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        RemedialAction<?> na2 = crac.newNetworkAction().withId("na2")
            .newSwitchAction().withNetworkElement("ne2").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withInstant(CURATIVE_INSTANT_ID).withCountry(Country.BE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        RemedialAction<?> na3 = crac.newNetworkAction().withId("na3")
            .newSwitchAction().withNetworkElement("ne3").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withInstant(CURATIVE_INSTANT_ID).withCountry(Country.DE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        when(flowResult.getMargin(any(), any())).thenReturn(100.);

        when(flowResult.getMargin(eq(cnecFrBe), any())).thenReturn(10.);
        assertIsOnFlowInCountryAvailable(na1, optimizedState, flowResult, false);
        assertIsOnFlowInCountryAvailable(na2, optimizedState, flowResult, false);
        assertIsOnFlowInCountryAvailable(na3, optimizedState, flowResult, false);

        when(flowResult.getMargin(eq(cnecFrBe), any())).thenReturn(-10.);
        assertIsOnFlowInCountryAvailable(na1, optimizedState, flowResult, true);
        assertIsOnFlowInCountryAvailable(na2, optimizedState, flowResult, true);
        assertIsOnFlowInCountryAvailable(na3, optimizedState, flowResult, false);

        when(flowResult.getMargin(eq(cnecFrBe), any())).thenReturn(0.);
        assertIsOnFlowInCountryAvailable(na1, optimizedState, flowResult, true);
        assertIsOnFlowInCountryAvailable(na2, optimizedState, flowResult, true);
        assertIsOnFlowInCountryAvailable(na3, optimizedState, flowResult, false);

        when(flowResult.getMargin(eq(cnecFrBe), any())).thenReturn(150.);
        when(flowResult.getMargin(eq(cnecFrDe), any())).thenReturn(0.);
        assertIsOnFlowInCountryAvailable(na1, optimizedState, flowResult, true);
        assertIsOnFlowInCountryAvailable(na2, optimizedState, flowResult, false);
        assertIsOnFlowInCountryAvailable(na3, optimizedState, flowResult, true);

        when(flowResult.getMargin(eq(cnecFrBe), any())).thenReturn(-150.);
        when(optimizedState.getInstant()).thenReturn(preventiveInstant);
        assertIsOnFlowInCountryAvailable(na1, optimizedState, flowResult, false);
        assertIsOnFlowInCountryAvailable(na2, optimizedState, flowResult, false);
        assertIsOnFlowInCountryAvailable(na3, optimizedState, flowResult, false);
    }

    @Test
    void testIsOnFlowConstraintInCountryAvailableWithContingency() {
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        State optimizedState = Mockito.mock(State.class);
        when(optimizedState.getInstant()).thenReturn(curativeInstant);
        when(optimizedState.getContingency()).thenReturn(Optional.of(crac.getContingency("Contingency FR1 FR3")));

        FlowCnec cnecCont1 = crac.getFlowCnec("cnec1stateCurativeContingency1");
        FlowCnec cnecCont2 = crac.getFlowCnec("cnec2stateCurativeContingency2");
        PrePerimeterResult flowResult = mock(PrePerimeterResult.class);

        RemedialAction<?> na = crac.newNetworkAction().withId("na1")
            .newSwitchAction().withNetworkElement("ne1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withInstant(CURATIVE_INSTANT_ID).withContingency("Contingency FR1 FR3").withCountry(Country.FR).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        // cnecCont1 is after same contingency as usage rule, not cnecCont2
        // So the RA should only be available when cnecCont1 has a negative margin
        when(flowResult.getMargin(any(), any())).thenReturn(100.);

        when(flowResult.getMargin(eq(cnecCont1), any())).thenReturn(-10.);
        when(flowResult.getMargin(eq(cnecCont2), any())).thenReturn(10.);
        assertIsOnFlowInCountryAvailable(na, optimizedState, flowResult, true);

        when(flowResult.getMargin(eq(cnecCont1), any())).thenReturn(10.);
        when(flowResult.getMargin(eq(cnecCont2), any())).thenReturn(-10.);
        assertIsOnFlowInCountryAvailable(na, optimizedState, flowResult, false);
    }

    private void assertIsOnFlowInCountryAvailable(RemedialAction<?> ra, State optimizedState, FlowResult flowResult, boolean available) {
        assertEquals(available, RaoUtil.isRemedialActionAvailable(ra, optimizedState, flowResult, ra.getFlowCnecsConstrainingUsageRules(crac.getFlowCnecs(), network, optimizedState), network, raoParameters));
    }

    @Test
    void testElementaryActionsLimitWithNonDiscretePsts() {
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().setPstModel(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        raoInput.getCrac().newRaUsageLimits(PREVENTIVE_INSTANT_ID).withMaxElementaryActionPerTso(Map.of("TSO", 2)).add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> RaoUtil.checkParameters(raoParameters, raoInput));
        assertEquals("The PSTs must be approximated as integers to use the limitations of elementary actions as a constraint in the RAO.", exception.getMessage());
    }

    @Test
    void testGetLastAvailableRangeActionOnSameNetworkElementWithOutageState() {
        OptimizationPerimeter optimizationContext = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationContext.getMainOptimizationState()).thenReturn(crac.getPreventiveState());
        State outageState = Mockito.mock(State.class);
        Mockito.when(outageState.getContingency()).thenReturn(Optional.of(crac.getContingency("Contingency FR1 FR3")));
        Mockito.when(outageState.getInstant()).thenReturn(crac.getInstant(InstantKind.OUTAGE));
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> RaoUtil.getLastAvailableRangeActionOnSameNetworkElement(optimizationContext, crac.getRangeActions().iterator().next(), outageState));
        assertEquals("Linear optimization does not handle range actions which are neither PREVENTIVE nor CURATIVE.", exception.getMessage());
    }

    @Test
    void testGetLastAvailableRangeActionOnSameNetworkElementMultiCurative() {
        Contingency contingency = crac.getContingency("Contingency FR1 FR3");

        Instant curative1Instant = Mockito.mock(Instant.class);
        Mockito.when(curative1Instant.getKind()).thenReturn(InstantKind.CURATIVE);

        State curativeState1 = Mockito.mock(State.class);
        Mockito.when(curativeState1.getInstant()).thenReturn(curative1Instant);
        Mockito.when(curativeState1.getContingency()).thenReturn(Optional.of(contingency));

        Instant curative3Instant = Mockito.mock(Instant.class);
        Mockito.when(curative3Instant.getKind()).thenReturn(InstantKind.CURATIVE);
        Mockito.when(curative3Instant.isCurative()).thenReturn(true);

        State curativeState3 = Mockito.mock(State.class);
        Mockito.when(curativeState3.getInstant()).thenReturn(curative3Instant);
        Mockito.when(curativeState3.getContingency()).thenReturn(Optional.of(contingency));

        Mockito.when(curative1Instant.comesBefore(curative3Instant)).thenReturn(true);

        NetworkElement pst = Mockito.mock(NetworkElement.class);
        Mockito.when(pst.getId()).thenReturn("pst");

        RangeAction<?> rangeAction1 = Mockito.mock(RangeAction.class);
        Mockito.when(rangeAction1.getNetworkElements()).thenReturn(Set.of(pst));
        Mockito.when(rangeAction1.getId()).thenReturn("range-action-1");

        RangeAction<?> rangeAction2 = Mockito.mock(RangeAction.class);
        Mockito.when(rangeAction2.getNetworkElements()).thenReturn(Set.of(pst));
        Mockito.when(rangeAction2.getId()).thenReturn("range-action-2");

        OptimizationPerimeter optimizationContext = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationContext.getMainOptimizationState()).thenReturn(curativeState1);
        Mockito.when(optimizationContext.getRangeActionsPerState()).thenReturn(Map.of(curativeState1, Set.of(rangeAction1)));

        assertEquals(Pair.of(rangeAction1, curativeState1), RaoUtil.getLastAvailableRangeActionOnSameNetworkElement(optimizationContext, rangeAction2, curativeState3));
    }
}
