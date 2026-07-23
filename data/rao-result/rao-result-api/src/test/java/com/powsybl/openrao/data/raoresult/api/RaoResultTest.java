/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class RaoResultTest {

    private Crac crac;
    private Instant preventiveInstant;
    private Instant curativeInstant;
    private FlowCnec preventiveFlowCnecFr;
    private AngleCnec preventiveAngleCnecFr;
    private VoltageCnec preventiveVoltageCnecFr;
    private FlowCnec curativeFlowCnecFr;
    private AngleCnec curativeAngleCnecFr;
    private VoltageCnec curativeVoltageCnecFr;
    private PstRangeAction pstRangeActionBe;
    private FlowCnec curativeFlowCnecBe;
    private RaoResult raoResult;

    @BeforeEach
    void setUp() {
        crac = mock(Crac.class);
        raoResult = mock(RaoResult.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        when(raoResult.getComputationStatus()).thenReturn(ComputationStatus.DEFAULT);

        preventiveInstant = mockInstant("preventive", InstantKind.PREVENTIVE);
        curativeInstant = mockInstant("curative", InstantKind.CURATIVE);

        State preventiveState = mockState(preventiveInstant);
        State curativeState = mockState(curativeInstant);

        preventiveFlowCnecFr = mockFlowCnec("preventiveFlowFr", preventiveState, "FR");
        preventiveAngleCnecFr = mockAngleCnec("preventiveAngleFr", preventiveState, "FR");
        preventiveVoltageCnecFr = mockVoltageCnec("preventiveVoltageFr", preventiveState, "FR");

        curativeFlowCnecFr = mockFlowCnec("curativeFlowFr", curativeState, "FR");
        curativeAngleCnecFr = mockAngleCnec("curativeAngleFr", curativeState, "FR");
        curativeVoltageCnecFr = mockVoltageCnec("curativeVoltageFr", curativeState, "FR");

        pstRangeActionBe = mock(PstRangeAction.class);
        when(pstRangeActionBe.getOperator()).thenReturn("BE");
        UsageRule beUsageRule = mock(UsageRule.class);
        when(beUsageRule.getInstant()).thenReturn(preventiveInstant); // Not curative
        when(pstRangeActionBe.getUsageRules()).thenReturn(Collections.singleton(beUsageRule));

        curativeFlowCnecBe = mockFlowCnec("curativeFlowBe", curativeState, "BE");
        // For curative RA of BE, we need a RemedialAction with a curative usage rule
        RemedialAction<?> curativeRaBe = mock(RemedialAction.class);
        when(curativeRaBe.getOperator()).thenReturn("BE");
        UsageRule curativeUsageRuleBe = mock(UsageRule.class);
        when(curativeUsageRuleBe.getInstant()).thenReturn(curativeInstant);
        when(curativeRaBe.getUsageRules()).thenReturn(Collections.singleton(curativeUsageRuleBe));

        when(crac.getFlowCnecs()).thenReturn(new HashSet<>(Arrays.asList(preventiveFlowCnecFr, curativeFlowCnecFr, curativeFlowCnecBe)));
        when(crac.getAngleCnecs()).thenReturn(new HashSet<>(Arrays.asList(preventiveAngleCnecFr, curativeAngleCnecFr)));
        when(crac.getVoltageCnecs()).thenReturn(new HashSet<>(Arrays.asList(preventiveVoltageCnecFr, curativeVoltageCnecFr)));

        Set<RemedialAction<?>> remedialActions = new HashSet<>();
        remedialActions.add(pstRangeActionBe);
        remedialActions.add(curativeRaBe);
        when(crac.getRemedialActions()).thenReturn(remedialActions);
    }

    private Instant mockInstant(String id, InstantKind kind) {
        Instant instant = mock(Instant.class);
        when(instant.getId()).thenReturn(id);
        when(instant.getKind()).thenReturn(kind);
        when(instant.isCurative()).thenReturn(kind == InstantKind.CURATIVE);
        when(instant.isPreventive()).thenReturn(kind == InstantKind.PREVENTIVE);
        return instant;
    }

    private State mockState(Instant instant) {
        State state = mock(State.class);
        when(state.getInstant()).thenReturn(instant);
        return state;
    }

    private FlowCnec mockFlowCnec(String id, State state, String operator) {
        FlowCnec cnec = mock(FlowCnec.class);
        when(cnec.getId()).thenReturn(id);
        when(cnec.getState()).thenReturn(state);
        when(cnec.getOperator()).thenReturn(operator);
        when(cnec.isOptimized()).thenReturn(true);
        return cnec;
    }

    private AngleCnec mockAngleCnec(String id, State state, String operator) {
        AngleCnec cnec = mock(AngleCnec.class);
        when(cnec.getId()).thenReturn(id);
        when(cnec.getState()).thenReturn(state);
        when(cnec.getOperator()).thenReturn(operator);
        return cnec;
    }

    private VoltageCnec mockVoltageCnec(String id, State state, String operator) {
        VoltageCnec cnec = mock(VoltageCnec.class);
        when(cnec.getId()).thenReturn(id);
        when(cnec.getState()).thenReturn(state);
        when(cnec.getOperator()).thenReturn(operator);
        return cnec;
    }

    static Stream<Arguments> isSecureParams() {
        List<Unit> flowUnits = Arrays.asList(Unit.AMPERE, Unit.MEGAWATT);
        List<Boolean> excludeCnecsOptions = Arrays.asList(true, false);
        List<PhysicalParameter[]> parameterCombinations = new ArrayList<>();
        PhysicalParameter[] all = {PhysicalParameter.FLOW, PhysicalParameter.ANGLE, PhysicalParameter.VOLTAGE};

        // Non-empty combinations
        for (int i = 1; i < (1 << all.length); i++) {
            List<PhysicalParameter> combo = new ArrayList<>();
            for (int j = 0; j < all.length; j++) {
                if (((i >> j) & 1) == 1) {
                    combo.add(all[j]);
                }
            }
            parameterCombinations.add(combo.toArray(new PhysicalParameter[0]));
        }

        Stream.Builder<Arguments> builder = Stream.builder();
        for (Unit unit : flowUnits) {
            for (Boolean exclude : excludeCnecsOptions) {
                for (PhysicalParameter[] params : parameterCombinations) {
                    builder.add(Arguments.of(unit, exclude, params));
                }
            }
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource("isSecureParams")
    void testIsSecureTrue(Unit flowUnit, boolean excludeCnecsForTsosWithoutCras, PhysicalParameter... parameters) {
        mockMargins(1.0);
        assertTrue(raoResult.isSecure(crac, flowUnit, excludeCnecsForTsosWithoutCras, parameters));
    }

    @ParameterizedTest
    @MethodSource("isSecureParams")
    void testIsSecureFalse(Unit flowUnit, boolean excludeCnecsForTsosWithoutCras, PhysicalParameter... parameters) {
        mockMargins(-1.0);
        assertFalse(raoResult.isSecure(crac, flowUnit, excludeCnecsForTsosWithoutCras, parameters));
    }

    @ParameterizedTest
    @MethodSource("isSecureParams")
    void testIsSecureException(Unit flowUnit, boolean excludeCnecsForTsosWithoutCras, PhysicalParameter... parameters) {
        mockMargins(Double.NaN);
        assertThrows(OpenRaoException.class, () -> raoResult.isSecure(crac, flowUnit, excludeCnecsForTsosWithoutCras, parameters));
    }

    private void mockMargins(double margin) {
        // Flow
        doReturn(margin).when(raoResult).getMargin(any(), any(FlowCnec.class), any());
        // Angle
        doReturn(margin).when(raoResult).getMargin(any(), any(AngleCnec.class), eq(Unit.DEGREE));
        // Voltage
        doReturn(margin).when(raoResult).getMargin(any(), any(VoltageCnec.class), eq(Unit.KILOVOLT));
    }

    @ParameterizedTest
    @MethodSource("isSecureParams")
    void testIsSecureFailureStatus(Unit flowUnit, boolean excludeCnecsForTsosWithoutCras, PhysicalParameter... parameters) {
        when(raoResult.getComputationStatus()).thenReturn(ComputationStatus.FAILURE);
        assertFalse(raoResult.isSecure(crac, flowUnit, excludeCnecsForTsosWithoutCras, parameters));
    }

    @ParameterizedTest
    @MethodSource("isSecureParams")
    void testIsSecureEmptyParams(Unit flowUnit, boolean excludeCnecsForTsosWithoutCras) {
        assertThrows(OpenRaoException.class, () -> raoResult.isSecure(crac, flowUnit, excludeCnecsForTsosWithoutCras));
    }
}
