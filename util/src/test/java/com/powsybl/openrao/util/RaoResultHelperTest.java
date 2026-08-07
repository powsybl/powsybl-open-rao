package com.powsybl.openrao.util;
/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.powsybl.loadflow.LoadFlowParameters;
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
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.parameters.NotOptimizedCnecsParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static com.powsybl.openrao.util.RaoResultHelper.isSecure;
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
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class RaoResultHelperTest {
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

        RaoParameters raoParameters = mock(RaoParameters.class);
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

    static Stream<Arguments> raoParameters() {
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
                    RaoParameters raoParameters = mock(RaoParameters.class);
                    NotOptimizedCnecsParameters notOptimizedCnecsParameters = mock(NotOptimizedCnecsParameters.class);
                    LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = mock(LoadFlowAndSensitivityParameters.class);
                    SensitivityAnalysisParameters sensitivityAnalysisParameters = mock(SensitivityAnalysisParameters.class);
                    OpenRaoSearchTreeParameters openRaoSearchTreeParameters = mock(OpenRaoSearchTreeParameters.class);
                    LoadFlowParameters loadFlowParameters = mock(LoadFlowParameters.class);

                    when(notOptimizedCnecsParameters.getDoNotOptimizeCurativeCnecsForTsosWithoutCras()).thenReturn(exclude);
                    when(raoParameters.getNotOptimizedCnecsParameters()).thenReturn(notOptimizedCnecsParameters);

                    when(raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)).thenReturn(true);
                    when(raoParameters.getExtension(OpenRaoSearchTreeParameters.class)).thenReturn(openRaoSearchTreeParameters);

                    when(openRaoSearchTreeParameters.getLoadFlowAndSensitivityParameters()).thenReturn(loadFlowAndSensitivityParameters);
                    when(loadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters()).thenReturn(sensitivityAnalysisParameters);
                    when(sensitivityAnalysisParameters.getLoadFlowParameters()).thenReturn(loadFlowParameters);

                    when(loadFlowParameters.isDc()).thenReturn(unit == Unit.MEGAWATT);

                    builder.add(Arguments.of(raoParameters, params));
                }
            }
        }

        return builder.build();
    }

    @ParameterizedTest
    @MethodSource("raoParameters")
    void testIsSecureTrue(RaoParameters raoParameters, PhysicalParameter... parameters) {
        mockMargins(1.0);
        assertTrue(isSecure(raoResult, crac, raoParameters, parameters));
    }

    @ParameterizedTest
    @MethodSource("raoParameters")
    void testIsSecureFalse(RaoParameters raoParameters, PhysicalParameter... parameters) {
        mockMargins(-1.0);
        assertFalse(isSecure(raoResult, crac, raoParameters, parameters));
    }

    @ParameterizedTest
    @MethodSource("raoParameters")
    void testIsSecureException(RaoParameters raoParameters, PhysicalParameter... parameters) {
        mockMargins(Double.NaN);
        assertThrows(OpenRaoException.class, () -> isSecure(raoResult, crac, raoParameters, parameters));
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
    @MethodSource("raoParameters")
    void testIsSecureFailureStatus(RaoParameters raoParameters, PhysicalParameter... parameters) {
        when(raoResult.getComputationStatus()).thenReturn(ComputationStatus.FAILURE);
        assertFalse(isSecure(raoResult, crac, raoParameters, parameters));
    }

    @ParameterizedTest
    @MethodSource("raoParameters")
    void testIsSecureEmptyParams(RaoParameters raoParameters) {
        assertThrows(OpenRaoException.class, () -> isSecure(raoResult, crac, raoParameters));
    }

}
