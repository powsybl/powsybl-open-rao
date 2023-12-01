/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.ObjectiveFunctionParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.parameters.extensions.RelativeMarginsParametersExtension;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
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
        raoParameters.addExtension(RelativeMarginsParametersExtension.class, new RelativeMarginsParametersExtension());
        raoParameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfBoundariesFromString(new ArrayList<>(Arrays.asList("{FR}-{ES}", "{ES}-{PT}")));
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        FaraoException exception = assertThrows(FaraoException.class, () -> RaoUtil.checkParameters(raoParameters, raoInput));
        assertEquals("Objective function MAX_MIN_RELATIVE_MARGIN_IN_AMPERE requires glsks", exception.getMessage());
    }

    @Test
    void testExceptionForNoPtdfParametersOnRelativeMargin() {
        addGlskProvider();
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        FaraoException exception = assertThrows(FaraoException.class, () -> RaoUtil.checkParameters(raoParameters, raoInput));
        assertEquals("Objective function MAX_MIN_RELATIVE_MARGIN_IN_AMPERE requires a config with a non empty boundary set", exception.getMessage());
    }

    @Test
    void testExceptionForNullBoundariesOnRelativeMargin() {
        addGlskProvider();
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        FaraoException exception = assertThrows(FaraoException.class, () -> RaoUtil.checkParameters(raoParameters, raoInput));
        assertEquals("Objective function MAX_MIN_RELATIVE_MARGIN_IN_AMPERE requires a config with a non empty boundary set", exception.getMessage());
    }

    @Test
    void testExceptionForEmptyBoundariesOnRelativeMargin() {
        addGlskProvider();
        raoParameters.addExtension(RelativeMarginsParametersExtension.class, new RelativeMarginsParametersExtension());
        raoParameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfBoundariesFromString(new ArrayList<>());
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        FaraoException exception = assertThrows(FaraoException.class, () -> RaoUtil.checkParameters(raoParameters, raoInput));
        assertEquals("Objective function MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT requires a config with a non empty boundary set", exception.getMessage());
    }

    @Test
    void testAmpereWithDc() {
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters().setDc(true);
        FaraoException exception = assertThrows(FaraoException.class, () -> RaoUtil.checkParameters(raoParameters, raoInput));
        assertEquals("Objective function MAX_MIN_RELATIVE_MARGIN_IN_AMPERE cannot be calculated with a DC default sensitivity engine", exception.getMessage());
    }

    @Test
    void testGetBranchFlowUnitMultiplier() {
        FlowCnec cnec = Mockito.mock(FlowCnec.class);
        Mockito.when(cnec.getNominalVoltage(Side.LEFT)).thenReturn(400.);
        Mockito.when(cnec.getNominalVoltage(Side.RIGHT)).thenReturn(200.);

        assertEquals(1., RaoUtil.getFlowUnitMultiplier(cnec, Side.LEFT, Unit.MEGAWATT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1., RaoUtil.getFlowUnitMultiplier(cnec, Side.RIGHT, Unit.MEGAWATT, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1., RaoUtil.getFlowUnitMultiplier(cnec, Side.LEFT, Unit.AMPERE, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1., RaoUtil.getFlowUnitMultiplier(cnec, Side.RIGHT, Unit.AMPERE, Unit.AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1000 / 400. / Math.sqrt(3), RaoUtil.getFlowUnitMultiplier(cnec, Side.LEFT, Unit.MEGAWATT, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(400 * Math.sqrt(3) / 1000., RaoUtil.getFlowUnitMultiplier(cnec, Side.LEFT, Unit.AMPERE, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(1000 / 200. / Math.sqrt(3), RaoUtil.getFlowUnitMultiplier(cnec, Side.RIGHT, Unit.MEGAWATT, Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(200 * Math.sqrt(3) / 1000., RaoUtil.getFlowUnitMultiplier(cnec, Side.RIGHT, Unit.AMPERE, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        FaraoException exception = assertThrows(FaraoException.class, () -> RaoUtil.getFlowUnitMultiplier(cnec, Side.LEFT, Unit.MEGAWATT, Unit.PERCENT_IMAX));
        assertEquals("Only conversions between MW and A are supported.", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> RaoUtil.getFlowUnitMultiplier(cnec, Side.LEFT, Unit.KILOVOLT, Unit.MEGAWATT));
        assertEquals("Only conversions between MW and A are supported.", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> RaoUtil.getFlowUnitMultiplier(cnec, Side.RIGHT, Unit.AMPERE, Unit.TAP));
        assertEquals("Only conversions between MW and A are supported.", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> RaoUtil.getFlowUnitMultiplier(cnec, Side.RIGHT, Unit.DEGREE, Unit.AMPERE));
        assertEquals("Only conversions between MW and A are supported.", exception.getMessage());
    }

    @Test
    void testRounding() {
        double d1 = 1.;

        // big enough deltas are not rounded out by the rounding method
        double eps = 1e-6;
        double d2 = d1 + eps;
        for (int i = 0; i <= 30; i++) {
            assertNotEquals(RaoUtil.roundDouble(d1, i), RaoUtil.roundDouble(d2, i), 1e-20);
        }

        // small deltas are rounded out as long as we round enough bits
        eps = 1e-15;
        d2 = d1 + eps;
        for (int i = 20; i <= 30; i++) {
            assertEquals(RaoUtil.roundDouble(d1, i), RaoUtil.roundDouble(d2, i), 1e-20);
        }
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
        Mockito.when(cnecA.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(1000.));
        Mockito.when(cnecA.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(cnecB.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(cnecB.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(-1500.));
        Mockito.when(cnecC.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(cnecC.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.empty());
        Mockito.when(cnecD.getUpperBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(-16000.));
        Mockito.when(cnecD.getLowerBound(Side.LEFT, Unit.MEGAWATT)).thenReturn(Optional.of(-16000.));
        Set.of(cnecA, cnecB, cnecC, cnecD).forEach(cnec -> when(cnec.getMonitoredSides()).thenReturn(Set.of(Side.LEFT)));

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

        RemedialAction<?> na1 = crac.newNetworkAction().withId("na1")
            .newTopologicalAction().withNetworkElement("ne1").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(curativeInstant).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        assertTrue(na1.isRemedialActionAvailable(optimizedState, RaoUtil.isAnyMarginNegative(flowResult, na1.getFlowCnecsConstrainingUsageRules(crac.getFlowCnecs(), network, optimizedState), raoParameters.getObjectiveFunctionParameters().getType().getUnit())));

        RemedialAction<?> na2 = crac.newNetworkAction().withId("na2")
            .newTopologicalAction().withNetworkElement("ne2").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintUsageRule().withInstant(curativeInstant).withFlowCnec(flowCnec.getId()).add()
            .add();

        when(flowResult.getMargin(eq(flowCnec), any())).thenReturn(10.);
        assertFalse(na2.isRemedialActionAvailable(optimizedState, RaoUtil.isAnyMarginNegative(flowResult, na2.getFlowCnecsConstrainingUsageRules(crac.getFlowCnecs(), network, optimizedState), raoParameters.getObjectiveFunctionParameters().getType().getUnit())));

        when(flowResult.getMargin(eq(flowCnec), any())).thenReturn(-10.);
        assertTrue(na2.isRemedialActionAvailable(optimizedState, RaoUtil.isAnyMarginNegative(flowResult, na2.getFlowCnecsConstrainingUsageRules(crac.getFlowCnecs(), network, optimizedState), raoParameters.getObjectiveFunctionParameters().getType().getUnit())));

        when(flowResult.getMargin(eq(flowCnec), any())).thenReturn(0.);
        assertTrue(na2.isRemedialActionAvailable(optimizedState, RaoUtil.isAnyMarginNegative(flowResult, na2.getFlowCnecsConstrainingUsageRules(crac.getFlowCnecs(), network, optimizedState), raoParameters.getObjectiveFunctionParameters().getType().getUnit())));

        optimizedState = crac.getPreventiveState();
        assertFalse(na1.isRemedialActionAvailable(optimizedState, RaoUtil.isAnyMarginNegative(flowResult, na1.getFlowCnecsConstrainingUsageRules(crac.getFlowCnecs(), network, optimizedState), raoParameters.getObjectiveFunctionParameters().getType().getUnit())));
        assertFalse(na2.isRemedialActionAvailable(optimizedState, RaoUtil.isAnyMarginNegative(flowResult, na2.getFlowCnecsConstrainingUsageRules(crac.getFlowCnecs(), network, optimizedState), raoParameters.getObjectiveFunctionParameters().getType().getUnit())));
    }

    @Test
    void testIsOnFlowConstraintInCountryAvailable() {
        Instant preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        State optimizedState = Mockito.mock(State.class);
        when(optimizedState.getInstant()).thenReturn(curativeInstant);

        FlowCnec cnecFrBe = crac.getFlowCnec("cnec1stateCurativeContingency1");
        FlowCnec cnecFrDe = crac.getFlowCnec("cnec2stateCurativeContingency2");
        FlowResult flowResult = mock(FlowResult.class);

        RemedialAction<?> na1 = crac.newNetworkAction().withId("na1")
            .newTopologicalAction().withNetworkElement("ne1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withInstant(curativeInstant).withCountry(Country.FR).add()
            .add();

        RemedialAction<?> na2 = crac.newNetworkAction().withId("na2")
            .newTopologicalAction().withNetworkElement("ne2").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withInstant(curativeInstant).withCountry(Country.BE).add()
            .add();

        RemedialAction<?> na3 = crac.newNetworkAction().withId("na3")
            .newTopologicalAction().withNetworkElement("ne3").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withInstant(curativeInstant).withCountry(Country.DE).add()
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

    private void assertIsOnFlowInCountryAvailable(RemedialAction<?> ra, State optimizedState, FlowResult flowResult, boolean available) {
        assertEquals(available, ra.isRemedialActionAvailable(optimizedState, RaoUtil.isAnyMarginNegative(flowResult, ra.getFlowCnecsConstrainingUsageRules(crac.getFlowCnecs(), network, optimizedState), raoParameters.getObjectiveFunctionParameters().getType().getUnit())));
    }

    @Test
    void testCnecShouldBeOptimizedBasic() {
        FlowCnec cnec = crac.getFlowCnec("cnec1basecase");
        PstRangeAction pst = crac.getPstRangeAction("pst");
        FlowResult flowResult = mock(FlowResult.class);
        RangeActionSetpointResult prePerimeterRangeActionSetpointResult = mock(PrePerimeterResult.class);
        SensitivityResult sensitivityResult = mock(SensitivityResult.class);

        // Cnec not in map
        assertTrue(RaoUtil.cnecShouldBeOptimized(Map.of(), flowResult, cnec, Side.LEFT, Map.of(), prePerimeterRangeActionSetpointResult, sensitivityResult, Unit.MEGAWATT));

        // Margins > 0
        when(flowResult.getFlow(cnec, Side.LEFT, Unit.MEGAWATT)).thenReturn(0.);
        assertFalse(RaoUtil.cnecShouldBeOptimized(Map.of(cnec, pst), flowResult, cnec, Side.LEFT, Map.of(), prePerimeterRangeActionSetpointResult, sensitivityResult, Unit.MEGAWATT));
    }

    @Test
    void testCnecShouldBeOptimizedUpper() {
        FlowCnec cnec = crac.getFlowCnec("cnec1basecase");
        PstRangeAction pst = crac.getPstRangeAction("pst");
        FlowResult flowResult = mock(FlowResult.class);
        RangeActionSetpointResult prePerimeterRangeActionSetpointResult = mock(PrePerimeterResult.class);
        SensitivityResult sensitivityResult = mock(SensitivityResult.class);
        Map<FlowCnec, RangeAction<?>> map = Map.of(cnec, pst);

        // Upper margin < 0 (max threshold is 2279 A)
        when(flowResult.getFlow(cnec, Side.LEFT, Unit.AMPERE)).thenReturn(2379.);

        // Sensi > 0
        when(sensitivityResult.getSensitivityValue(cnec, Side.LEFT, pst, Unit.MEGAWATT)).thenReturn(33.); // = 50 A
        // Some taps left (PST at set-point -4.22, can go down to -6.2)
        assertFalse(RaoUtil.cnecShouldBeOptimized(map, flowResult, cnec, Side.LEFT, Map.of(pst, -4.22), prePerimeterRangeActionSetpointResult, sensitivityResult, Unit.AMPERE));
        // Not enough taps left
        assertTrue(RaoUtil.cnecShouldBeOptimized(map, flowResult, cnec, Side.LEFT, Map.of(pst, -5.22), prePerimeterRangeActionSetpointResult, sensitivityResult, Unit.AMPERE));
        // Sensi < 0
        when(sensitivityResult.getSensitivityValue(cnec, Side.LEFT, pst, Unit.MEGAWATT)).thenReturn(-33.); // = -50 A
        // Some taps left (PST at set-point 4.22, can go up to 6.2)
        assertFalse(RaoUtil.cnecShouldBeOptimized(map, flowResult, cnec, Side.LEFT, Map.of(pst, 4.22), prePerimeterRangeActionSetpointResult, sensitivityResult, Unit.AMPERE));
        // Not enough taps left
        assertTrue(RaoUtil.cnecShouldBeOptimized(map, flowResult, cnec, Side.LEFT, Map.of(pst, 5.22), prePerimeterRangeActionSetpointResult, sensitivityResult, Unit.AMPERE));
    }

    @Test
    void testCnecShouldBeOptimizedLower() {
        FlowCnec cnec = crac.getFlowCnec("cnec1basecase");
        PstRangeAction pst = crac.getPstRangeAction("pst");
        FlowResult flowResult = mock(FlowResult.class);
        RangeActionSetpointResult prePerimeterRangeActionSetpointResult = mock(PrePerimeterResult.class);
        SensitivityResult sensitivityResult = mock(SensitivityResult.class);
        Map<FlowCnec, RangeAction<?>> map = Map.of(cnec, pst);

        // Lower margin < 0 (min threshold is -1500 MW)
        when(flowResult.getFlow(cnec, Side.LEFT, Unit.MEGAWATT)).thenReturn(-1700.);

        // Sensi > 0
        when(sensitivityResult.getSensitivityValue(cnec, Side.LEFT, pst, Unit.MEGAWATT)).thenReturn(50.);
        // Some taps left (PST at set-point 2.22, can go up to 6.2)
        when(prePerimeterRangeActionSetpointResult.getSetpoint(pst)).thenReturn(2.22);
        assertFalse(RaoUtil.cnecShouldBeOptimized(map, flowResult, cnec, Side.LEFT, Map.of(), prePerimeterRangeActionSetpointResult, sensitivityResult, Unit.MEGAWATT));
        // Not enough taps left
        when(prePerimeterRangeActionSetpointResult.getSetpoint(pst)).thenReturn(3.22);
        assertTrue(RaoUtil.cnecShouldBeOptimized(map, flowResult, cnec, Side.LEFT, Map.of(), prePerimeterRangeActionSetpointResult, sensitivityResult, Unit.MEGAWATT));
        // Sensi < 0
        when(sensitivityResult.getSensitivityValue(cnec, Side.LEFT, pst, Unit.MEGAWATT)).thenReturn(-50.);
        // Some taps left (PST at set-point -2.22, can go down to -6.2)
        when(prePerimeterRangeActionSetpointResult.getSetpoint(pst)).thenReturn(-2.22);
        assertFalse(RaoUtil.cnecShouldBeOptimized(map, flowResult, cnec, Side.LEFT, Map.of(), prePerimeterRangeActionSetpointResult, sensitivityResult, Unit.MEGAWATT));
        // Not enough taps left
        when(prePerimeterRangeActionSetpointResult.getSetpoint(pst)).thenReturn(-3.22);
        assertTrue(RaoUtil.cnecShouldBeOptimized(map, flowResult, cnec, Side.LEFT, Map.of(), prePerimeterRangeActionSetpointResult, sensitivityResult, Unit.MEGAWATT));
    }
}
