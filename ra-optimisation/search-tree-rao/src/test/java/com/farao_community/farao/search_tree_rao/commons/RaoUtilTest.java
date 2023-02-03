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
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraintInCountry;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static com.farao_community.farao.rao_api.parameters.RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE;
import static com.farao_community.farao.rao_api.parameters.RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

public class RaoUtilTest {
    private static final double DOUBLE_TOLERANCE = 0.1;
    private RaoParameters raoParameters;
    private RaoInput raoInput;
    private Network network;
    private Crac crac;
    private String variantId;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
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

    @Test(expected = FaraoException.class)
    public void testExceptionForGlskOnRelativeMargin() {
        raoParameters.setRelativeMarginPtdfBoundariesFromString(new ArrayList<>(Arrays.asList("{FR}-{ES}", "{ES}-{PT}")));
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForNoPtdfParametersOnRelativeMargin() {
        addGlskProvider();
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForNullBoundariesOnRelativeMargin() {
        addGlskProvider();
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForEmptyBoundariesOnRelativeMargin() {
        addGlskProvider();
        raoParameters.setRelativeMarginPtdfBoundariesFromString(new ArrayList<>());
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test(expected = FaraoException.class)
    public void testAmpereWithDc() {
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        raoParameters.getDefaultSensitivityAnalysisParameters().getLoadFlowParameters().setDc(true);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test
    public void testGetBranchFlowUnitMultiplier() {
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

        assertThrows(FaraoException.class, () -> RaoUtil.getFlowUnitMultiplier(cnec, Side.LEFT, Unit.MEGAWATT, Unit.PERCENT_IMAX));
        assertThrows(FaraoException.class, () -> RaoUtil.getFlowUnitMultiplier(cnec, Side.LEFT, Unit.KILOVOLT, Unit.MEGAWATT));
        assertThrows(FaraoException.class, () -> RaoUtil.getFlowUnitMultiplier(cnec, Side.RIGHT, Unit.AMPERE, Unit.TAP));
        assertThrows(FaraoException.class, () -> RaoUtil.getFlowUnitMultiplier(cnec, Side.RIGHT, Unit.DEGREE, Unit.AMPERE));
    }

    @Test
    public void testRounding() {
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
    public void testGetLargestCnecThreshold() {
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
    public void testIsOnFlowConstraintAvailable() {
        State optimizedState = crac.getState("Contingency FR1 FR3", Instant.CURATIVE);

        FlowCnec flowCnec = crac.getFlowCnec("cnec1stateCurativeContingency1");
        FlowResult flowResult = mock(FlowResult.class);

        NetworkAction na1 = crac.newNetworkAction().withId("na1")
            .newTopologicalAction().withNetworkElement("ne1").withActionType(ActionType.OPEN).add()
            .newFreeToUseUsageRule().withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        assertTrue(RaoUtil.isRemedialActionAvailable(na1, optimizedState, flowResult, crac.getFlowCnecs(), network, raoParameters.getObjectiveFunction().getUnit()));

        NetworkAction na2 = crac.newNetworkAction().withId("na2")
            .newTopologicalAction().withNetworkElement("ne2").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintUsageRule().withInstant(Instant.CURATIVE).withFlowCnec(flowCnec.getId()).add()
            .add();
        OnFlowConstraint onFlowConstraint = (OnFlowConstraint) na2.getUsageRules().get(0);

        when(flowResult.getMargin(eq(flowCnec), any())).thenReturn(10.);
        assertFalse(RaoUtil.isOnFlowConstraintAvailable(onFlowConstraint, optimizedState, flowResult, raoParameters.getObjectiveFunction().getUnit()));
        assertFalse(RaoUtil.isRemedialActionAvailable(na2, optimizedState, flowResult, crac.getFlowCnecs(), network, raoParameters.getObjectiveFunction().getUnit()));

        when(flowResult.getMargin(eq(flowCnec), any())).thenReturn(-10.);
        assertTrue(RaoUtil.isOnFlowConstraintAvailable(onFlowConstraint, optimizedState, flowResult, raoParameters.getObjectiveFunction().getUnit()));
        assertTrue(RaoUtil.isRemedialActionAvailable(na2, optimizedState, flowResult, crac.getFlowCnecs(), network, raoParameters.getObjectiveFunction().getUnit()));

        when(flowResult.getMargin(eq(flowCnec), any())).thenReturn(0.);
        assertTrue(RaoUtil.isOnFlowConstraintAvailable(onFlowConstraint, optimizedState, flowResult, raoParameters.getObjectiveFunction().getUnit()));
        assertTrue(RaoUtil.isRemedialActionAvailable(na2, optimizedState, flowResult, crac.getFlowCnecs(), network, raoParameters.getObjectiveFunction().getUnit()));

        optimizedState = crac.getPreventiveState();
        assertFalse(RaoUtil.isRemedialActionAvailable(na1, optimizedState, flowResult, crac.getFlowCnecs(), network, raoParameters.getObjectiveFunction().getUnit()));
        assertFalse(RaoUtil.isOnFlowConstraintAvailable(onFlowConstraint, optimizedState, flowResult, raoParameters.getObjectiveFunction().getUnit()));
        assertFalse(RaoUtil.isRemedialActionAvailable(na2, optimizedState, flowResult, crac.getFlowCnecs(), network, raoParameters.getObjectiveFunction().getUnit()));
    }

    @Test
    public void testIsOnFlowConstraintInCountryAvailable() {
        State optimizedState = Mockito.mock(State.class);
        when(optimizedState.getInstant()).thenReturn(Instant.CURATIVE);

        FlowCnec cnecFrBe = crac.getFlowCnec("cnec1stateCurativeContingency1");
        FlowCnec cnecFrDe = crac.getFlowCnec("cnec2stateCurativeContingency2");
        FlowResult flowResult = mock(FlowResult.class);

        NetworkAction na1 = crac.newNetworkAction().withId("na1")
            .newTopologicalAction().withNetworkElement("ne1").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withInstant(Instant.CURATIVE).withCountry(Country.FR).add()
            .add();

        NetworkAction na2 = crac.newNetworkAction().withId("na2")
            .newTopologicalAction().withNetworkElement("ne2").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withInstant(Instant.CURATIVE).withCountry(Country.BE).add()
            .add();

        NetworkAction na3 = crac.newNetworkAction().withId("na3")
            .newTopologicalAction().withNetworkElement("ne3").withActionType(ActionType.OPEN).add()
            .newOnFlowConstraintInCountryUsageRule().withInstant(Instant.CURATIVE).withCountry(Country.DE).add()
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
        when(optimizedState.getInstant()).thenReturn(Instant.PREVENTIVE);
        assertIsOnFlowInCountryAvailable(na1, optimizedState, flowResult, false);
        assertIsOnFlowInCountryAvailable(na2, optimizedState, flowResult, false);
        assertIsOnFlowInCountryAvailable(na3, optimizedState, flowResult, false);
    }

    private void assertIsOnFlowInCountryAvailable(RemedialAction<?> ra, State optimizedState, FlowResult flowResult, boolean available) {
        assertEquals(available, RaoUtil.isOnFlowConstraintInCountryAvailable((OnFlowConstraintInCountry) ra.getUsageRules().get(0), optimizedState, flowResult, crac.getFlowCnecs(), network, raoParameters.getObjectiveFunction().getUnit()));
        assertEquals(available, RaoUtil.isRemedialActionAvailable(ra, optimizedState, flowResult, crac.getFlowCnecs(), network, raoParameters.getObjectiveFunction().getUnit()));
    }
}
