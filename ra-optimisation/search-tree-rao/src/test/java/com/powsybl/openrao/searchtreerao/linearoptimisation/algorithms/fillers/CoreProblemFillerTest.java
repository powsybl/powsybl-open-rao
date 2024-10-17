/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class CoreProblemFillerTest extends AbstractFillerTest {
    private LinearProblem linearProblem;
    private CoreProblemFiller coreProblemFiller;
    private RangeActionSetpointResult initialRangeActionSetpointResult;
    // some additional data
    private double minAlpha;
    private double maxAlpha;
    private double initialAlpha;

    @BeforeEach
    public void setUp() throws IOException {
        init();
        // arrange some additional data
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        minAlpha = crac.getRangeAction(RANGE_ACTION_ID).getMinAdmissibleSetpoint(0);
        maxAlpha = crac.getRangeAction(RANGE_ACTION_ID).getMaxAdmissibleSetpoint(0);
        initialAlpha = pstRangeAction.convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());

        initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(pstRangeAction, initialAlpha));
    }

    private void buildLinearProblem() {
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .withInitialRangeActionActivationResult(getInitialRangeActionActivationResult())
            .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void initializeForPreventive(double pstSensitivityThreshold, double hvdcSensitivityThreshold, double injectionSensitivityThreshold) {
        initialize(Set.of(cnec1), pstSensitivityThreshold, hvdcSensitivityThreshold, injectionSensitivityThreshold, crac.getPreventiveState(), false, RangeActionsOptimizationParameters.PstModel.CONTINUOUS);
    }

    private void initializeForGlobal(RangeActionsOptimizationParameters.PstModel pstModel) {
        initialize(Set.of(cnec1, cnec2), 1e-6, 1e-6, 1e-6, crac.getPreventiveState(), false, pstModel);
    }

    private void initialize(Set<FlowCnec> cnecs, double pstSensitivityThreshold, double hvdcSensitivityThreshold, double injectionSensitivityThreshold, State mainState, boolean raRangeShrinking, RangeActionsOptimizationParameters.PstModel pstModel) {
        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter.getFlowCnecs()).thenReturn(cnecs);
        Mockito.when(optimizationPerimeter.getMainOptimizationState()).thenReturn(mainState);

        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        cnecs.forEach(cnec -> rangeActions.put(cnec.getState(), Set.of(pstRangeAction)));
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);

        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getRangeActionsOptimizationParameters().setPstSensitivityThreshold(pstSensitivityThreshold);
        raoParameters.getRangeActionsOptimizationParameters().setHvdcSensitivityThreshold(hvdcSensitivityThreshold);
        raoParameters.getRangeActionsOptimizationParameters().setInjectionRaSensitivityThreshold(injectionSensitivityThreshold);
        RangeActionsOptimizationParameters rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(raoParameters);

        coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionSetpointResult,
            rangeActionParameters,
            Unit.MEGAWATT, raRangeShrinking, pstModel);
        buildLinearProblem();
    }

    private void initializeForInjection(Set<FlowCnec> cnecs, double pstSensitivityThreshold, double hvdcSensitivityThreshold, double injectionSensitivityThreshold, State mainState, boolean raRangeShrinking, RangeActionsOptimizationParameters.PstModel pstModel) {
        RangeAction<?> injGen = crac.getRangeAction(INJECTION_RANGE_ACTION_ID_0);
        RangeAction<?> injLoad = crac.getRangeAction(INJECTION_RANGE_ACTION_ID_1);

        initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(injGen, PRE_RESULT_SET_POINT_INJ_0, injLoad, PRE_RESULT_SET_POINT_INJ_1));
        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter.getFlowCnecs()).thenReturn(cnecs);
        Mockito.when(optimizationPerimeter.getMainOptimizationState()).thenReturn(mainState);

        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        cnecs.forEach(cnec -> rangeActions.put(cnec.getState(), Set.of(injGen, injLoad)));
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);

        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getRangeActionsOptimizationParameters().setPstSensitivityThreshold(pstSensitivityThreshold);
        raoParameters.getRangeActionsOptimizationParameters().setHvdcSensitivityThreshold(hvdcSensitivityThreshold);
        raoParameters.getRangeActionsOptimizationParameters().setInjectionRaSensitivityThreshold(injectionSensitivityThreshold);
        RangeActionsOptimizationParameters rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(raoParameters);

        coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionSetpointResult,
            rangeActionParameters,
            Unit.MEGAWATT, raRangeShrinking, pstModel);
        buildLinearProblem();
    }

    @Test
    void fillTestOnPreventive() {
        initializeForPreventive(1e-6, 1e-6, 1e-6);
        State state = cnec1.getState();

        // check range action setpoint variable
        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);
        assertNotNull(setPointVariable);
        assertEquals(minAlpha, setPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, setPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable
        OpenRaoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction, state);
        assertNotNull(absoluteVariationVariable);
        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
        assertEquals(linearProblem.infinity(), absoluteVariationVariable.ub(), linearProblem.infinity() * 1e-3);

        // check flow variable for cnec1
        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec1, TwoSides.ONE);
        assertNotNull(flowVariable);
        assertEquals(-linearProblem.infinity(), flowVariable.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec1
        OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1, TwoSides.ONE);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(-SENSI_CNEC1_IT1, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2 does not exist
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec2, TwoSides.TWO));
        assertEquals("Variable Tieline BE FR - Defaut - N-1 NL1-NL3_two_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec2 does not exist
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec2, TwoSides.TWO));
        assertEquals("Constraint Tieline BE FR - Defaut - N-1 NL1-NL3_two_flow_constraint has not been created yet", e.getMessage());

        // check absolute variation constraints
        OpenRaoMPConstraint absoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, state, LinearProblem.AbsExtension.NEGATIVE);
        OpenRaoMPConstraint absoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, state, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(absoluteVariationConstraint1);
        assertNotNull(absoluteVariationConstraint2);
        assertEquals(-initialAlpha, absoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), absoluteVariationConstraint1.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(initialAlpha, absoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), absoluteVariationConstraint2.ub(), linearProblem.infinity() * 1e-3);

        // check the number of variables and constraints
        // total number of variables 4 :
        //      - 1 per CNEC (flow)
        //      - 2 per range action (set-point and variation)
        // total number of constraints 4 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (absolute variation constraints)
        assertEquals(3, linearProblem.numVariables());
        assertEquals(3, linearProblem.numConstraints());
    }

    @Test
    void fillTestOnPreventiveFiltered() {
        initializeForPreventive(2.5, 2.5, 2.5);
        State state = cnec1.getState();

        // check range action setpoint variable
        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);
        assertNotNull(setPointVariable);
        assertEquals(minAlpha, setPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, setPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable
        OpenRaoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction, state);
        assertNotNull(absoluteVariationVariable);
        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
        assertEquals(linearProblem.infinity(), absoluteVariationVariable.ub(), linearProblem.infinity() * 1e-3);

        // check flow variable for cnec1
        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec1, TwoSides.ONE);
        assertNotNull(flowVariable);
        assertEquals(-linearProblem.infinity(), flowVariable.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec1
        OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1, TwoSides.ONE);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * 0, flowConstraint.lb(), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * 0, flowConstraint.ub(), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(0, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)

        // check flow variable for cnec2 does not exist
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec2, TwoSides.TWO));
        assertEquals("Variable Tieline BE FR - Defaut - N-1 NL1-NL3_two_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec2 does not exist
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec2, TwoSides.TWO));
        assertEquals("Constraint Tieline BE FR - Defaut - N-1 NL1-NL3_two_flow_constraint has not been created yet", e.getMessage());

        // check absolute variation constraints
        OpenRaoMPConstraint absoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, state, LinearProblem.AbsExtension.NEGATIVE);
        OpenRaoMPConstraint absoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, state, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(absoluteVariationConstraint1);
        assertNotNull(absoluteVariationConstraint2);
        assertEquals(-initialAlpha, absoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), absoluteVariationConstraint1.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(initialAlpha, absoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), absoluteVariationConstraint2.ub(), linearProblem.infinity() * 1e-3);

        // check the number of variables and constraints
        // total number of variables 4 :
        //      - 1 per CNEC (flow)
        //      - 2 per range action (set-point and variation)
        // total number of constraints 4 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (absolute variation constraints)
        assertEquals(3, linearProblem.numVariables());
        assertEquals(3, linearProblem.numConstraints());
    }

    @Test
    void fillTestOnCurative() {
        initialize(Set.of(cnec2), 1e-6, 1e-6, 1e-6, cnec2.getState(), false, RangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        State state = cnec2.getState();

        // check range action setpoint variable
        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);
        assertNotNull(setPointVariable);
        assertEquals(minAlpha, setPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, setPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable
        OpenRaoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction, state);
        assertNotNull(absoluteVariationVariable);
        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
        assertEquals(linearProblem.infinity(), absoluteVariationVariable.ub(), linearProblem.infinity() * 1e-3);

        // check flow variable for cnec1 does not exist
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec1, TwoSides.ONE));
        assertEquals("Variable Tieline BE FR - N - preventive_one_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec1 does not exist
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec1, TwoSides.ONE));
        assertEquals("Constraint Tieline BE FR - N - preventive_one_flow_constraint has not been created yet", e.getMessage());

        // check flow variable for cnec2
        OpenRaoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2, TwoSides.TWO);
        assertNotNull(flowVariable2);
        assertEquals(-linearProblem.infinity(), flowVariable2.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable2.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec2
        OpenRaoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2, TwoSides.TWO);
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC2_IT1, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check absolute variation constraints
        OpenRaoMPConstraint absoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, state, LinearProblem.AbsExtension.NEGATIVE);
        OpenRaoMPConstraint absoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, state, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(absoluteVariationConstraint1);
        assertNotNull(absoluteVariationConstraint2);
        assertEquals(-initialAlpha, absoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), absoluteVariationConstraint1.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(initialAlpha, absoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), absoluteVariationConstraint2.ub(), linearProblem.infinity() * 1e-3);

        // check the number of variables and constraints
        // total number of variables 4 :
        //      - 1 per CNEC (flow)
        //      - 2 per range action (set-point and variation)
        // total number of constraints 4 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (absolute variation constraints)
        assertEquals(3, linearProblem.numVariables());
        assertEquals(3, linearProblem.numConstraints());
    }

    @ParameterizedTest
    @EnumSource(value = RangeActionsOptimizationParameters.PstModel.class)
    void fillTestOnGlobal(RangeActionsOptimizationParameters.PstModel pstModel) {
        initializeForGlobal(pstModel);
        State prevState = cnec1.getState();
        State curState = cnec2.getState();

        if (pstModel.equals(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS)) {
            // check relative setpoint constraint for PRA_PST_BE has not been created in curative.
            Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getRangeActionRelativeSetpointConstraint(pstRangeAction, curState, LinearProblem.RaRangeShrinking.FALSE));
            assertEquals("Constraint PRA_PST_BE_N-1 NL1-NL3 - curative_relative_setpoint_constraint has not been created yet", e.getMessage());
        }

        // check range action setpoint variable for preventive state
        OpenRaoMPVariable prevSetPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, prevState);
        assertNotNull(prevSetPointVariable);
        assertEquals(minAlpha, prevSetPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, prevSetPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action setpoint variable for curative state
        OpenRaoMPVariable curSetPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, curState);
        assertNotNull(curSetPointVariable);
        assertEquals(minAlpha, curSetPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, curSetPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable for preventive state
        OpenRaoMPVariable prevAbsoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction, prevState);
        assertNotNull(prevAbsoluteVariationVariable);
        assertEquals(0, prevAbsoluteVariationVariable.lb(), 0.01);
        assertEquals(linearProblem.infinity(), prevAbsoluteVariationVariable.ub(), linearProblem.infinity() * 1e-3);

        // check range action absolute variation variable for curative state
        OpenRaoMPVariable curAbsoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction, curState);
        assertNotNull(curAbsoluteVariationVariable);
        assertEquals(0, curAbsoluteVariationVariable.lb(), 0.01);
        assertEquals(linearProblem.infinity(), curAbsoluteVariationVariable.ub(), linearProblem.infinity() * 1e-3);

        // check flow variable for cnec1
        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec1, TwoSides.ONE);
        assertNotNull(flowVariable);
        assertEquals(-linearProblem.infinity(), flowVariable.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec1
        OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1, TwoSides.ONE);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(-SENSI_CNEC1_IT1, flowConstraint.getCoefficient(prevSetPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2
        OpenRaoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2, TwoSides.TWO);
        assertNotNull(flowVariable2);
        assertEquals(-linearProblem.infinity(), flowVariable2.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable2.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec2
        OpenRaoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2, TwoSides.TWO);
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC2_IT1, flowConstraint2.getCoefficient(curSetPointVariable), DOUBLE_TOLERANCE);

        // check absolute variation constraints for preventive state
        OpenRaoMPConstraint prevAbsoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, prevState, LinearProblem.AbsExtension.NEGATIVE);
        OpenRaoMPConstraint prevAbsoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, prevState, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(prevAbsoluteVariationConstraint1);
        assertNotNull(prevAbsoluteVariationConstraint2);
        assertEquals(-initialAlpha, prevAbsoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), prevAbsoluteVariationConstraint1.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(initialAlpha, prevAbsoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), prevAbsoluteVariationConstraint2.ub(), linearProblem.infinity() * 1e-3);

        // check absolute variation constraints for curative state
        OpenRaoMPConstraint curAbsoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, curState, LinearProblem.AbsExtension.NEGATIVE);
        OpenRaoMPConstraint curAbsoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, curState, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(curAbsoluteVariationConstraint1);
        assertNotNull(curAbsoluteVariationConstraint2);
        assertEquals(0, curAbsoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(1., curAbsoluteVariationConstraint1.getCoefficient(prevSetPointVariable), DOUBLE_TOLERANCE);
        assertEquals(-1., curAbsoluteVariationConstraint1.getCoefficient(curSetPointVariable), DOUBLE_TOLERANCE);
        assertEquals(1., curAbsoluteVariationConstraint1.getCoefficient(curAbsoluteVariationVariable), DOUBLE_TOLERANCE);
        assertEquals(0, curAbsoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1., curAbsoluteVariationConstraint2.getCoefficient(prevSetPointVariable), DOUBLE_TOLERANCE);
        assertEquals(1., curAbsoluteVariationConstraint2.getCoefficient(curSetPointVariable), DOUBLE_TOLERANCE);
        assertEquals(1., curAbsoluteVariationConstraint2.getCoefficient(curAbsoluteVariationVariable), DOUBLE_TOLERANCE);

        // check the number of variables and constraints
        // total number of variables 6 :
        //      - 1 per CNEC (flow)
        //      - 2 per range action (set-point and variation)
        // total number of constraints 6 or 7:
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (absolute variation constraints)
        //      - 0 or 1 for curative range action (relative variation constraint)
        assertEquals(6, linearProblem.numVariables());
        if (pstModel.equals(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS)) {
            assertEquals(6, linearProblem.numConstraints());
        } else {
            assertEquals(7, linearProblem.numConstraints());
        }
    }

    private void updateLinearProblem() {
        // arrange some additional data
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_IT2);
        initialAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();

        when(flowResult.getFlow(cnec1, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC1_IT2);
        when(flowResult.getFlow(cnec2, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC2_IT2);
        when(sensitivityResult.getSensitivityValue(cnec1, TwoSides.ONE, pstRangeAction, Unit.MEGAWATT)).thenReturn(SENSI_CNEC1_IT2);
        when(sensitivityResult.getSensitivityValue(cnec2, TwoSides.TWO, pstRangeAction, Unit.MEGAWATT)).thenReturn(SENSI_CNEC2_IT2);

        // update the problem
        RangeActionSetpointResult rangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(pstRangeAction, initialAlpha));
        linearProblem.updateBetweenSensiIteration(flowResult, sensitivityResult, new RangeActionActivationResultImpl(rangeActionSetpointResult));
    }

    @Test
    void updateTestOnPreventive() {
        initializeForPreventive(1e-6, 1e-6, 1e-6);
        State state = cnec1.getState();
        // update the problem with new data
        updateLinearProblem();

        // some additional data
        final double currentAlpha = pstRangeAction.convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());

        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);

        // check flow variable for cnec1
        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec1, TwoSides.ONE);
        assertNotNull(flowVariable);
        assertEquals(-linearProblem.infinity(), flowVariable.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec1
        OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1, TwoSides.ONE);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC1_IT2, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2 does not exist
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec2, TwoSides.TWO));
        assertEquals("Variable Tieline BE FR - Defaut - N-1 NL1-NL3_two_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec2 does not exist
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec2, TwoSides.TWO));
        assertEquals("Constraint Tieline BE FR - Defaut - N-1 NL1-NL3_two_flow_constraint has not been created yet", e.getMessage());

        // check the number of variables and constraints
        // No iterative relative variation constraint should be created since CoreProblemFiller.raRangeShrinking = false
        // total number of variables 3 :
        //      - 1 per CNEC (flow)
        //      - 2 per range action (set-point)
        // total number of constraints 3 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (absolute variation constraints)

        assertEquals(3, linearProblem.numVariables());
        assertEquals(3, linearProblem.numConstraints());
    }

    @Test
    void updateTestOnPreventiveWithRaRangeShrinking() {
        initialize(Set.of(cnec1), 1e-6, 1e-6, 1e-6, crac.getPreventiveState(), true, RangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        State state = cnec1.getState();

        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getRangeActionRelativeSetpointConstraint(pstRangeAction, state, LinearProblem.RaRangeShrinking.TRUE));
        assertEquals("Constraint PRA_PST_BE_preventive_relative_setpoint_iterative-shrinkconstraint has not been created yet", e.getMessage());

        // 1st update
        updateLinearProblem();

        assertEquals(3, linearProblem.numVariables());
        assertEquals(4, linearProblem.numConstraints());

        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);
        OpenRaoMPConstraint shrinkingConstraint = linearProblem.getRangeActionRelativeSetpointConstraint(pstRangeAction, state, LinearProblem.RaRangeShrinking.TRUE);
        assertNotNull(shrinkingConstraint);
        assertEquals(1, shrinkingConstraint.getCoefficient(setPointVariable));
        assertEquals(-10.5161, shrinkingConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(5.0626, shrinkingConstraint.ub(), DOUBLE_TOLERANCE);

        // 2nd update
        updateLinearProblem();

        assertEquals(3, linearProblem.numVariables());
        assertEquals(4, linearProblem.numConstraints());

        setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);
        shrinkingConstraint = linearProblem.getRangeActionRelativeSetpointConstraint(pstRangeAction, state, LinearProblem.RaRangeShrinking.TRUE);
        assertNotNull(shrinkingConstraint);
        assertEquals(1, shrinkingConstraint.getCoefficient(setPointVariable));
        assertEquals(-7.9222, shrinkingConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(2.4687, shrinkingConstraint.ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void updateTestOnCurativeWithRaRangeShrinking() {
        initialize(Set.of(cnec2), 1e-6, 1e-6, 1e-6, cnec2.getState(), true, RangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        State state = cnec2.getState();
        // update the problem with new data
        updateLinearProblem();

        // some additional data
        final double currentAlpha = pstRangeAction.convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());

        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);

        // check flow variable for cnec1 does not exist
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec1, TwoSides.ONE));
        assertEquals("Variable Tieline BE FR - N - preventive_one_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec1 does not exist
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec1, TwoSides.ONE));
        assertEquals("Constraint Tieline BE FR - N - preventive_one_flow_constraint has not been created yet", e.getMessage());

        // check flow variable for cnec2
        OpenRaoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2, TwoSides.TWO);
        assertNotNull(flowVariable2);
        assertEquals(-linearProblem.infinity(), flowVariable2.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable2.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec2
        OpenRaoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2, TwoSides.TWO);
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * SENSI_CNEC2_IT2, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * SENSI_CNEC2_IT2, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC2_IT2, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check the number of variables and constraints
        // total number of variables 3 :
        //      - 1 per CNEC (flow)
        //      - 2 per range action (set-point and variation)
        // total number of constraints 4 :
        //      - 1 per CNEC (flow constraint)
        //      - 3 per range action (absolute variation constraints and iterative relative variation constraint: created before 2nd iteration)
        assertEquals(3, linearProblem.numVariables());
        assertEquals(4, linearProblem.numConstraints());

        // assert that no other constraint is created after 2nd iteration
        updateLinearProblem();
        assertEquals(4, linearProblem.numConstraints());
    }

    @Test
    void testSensitivityFilter1() {
        OpenRaoMPConstraint flowConstraint;
        OpenRaoMPVariable rangeActionSetpoint;
        when(flowResult.getPtdfZonalSum(cnec1, TwoSides.ONE)).thenReturn(0.5);

        // (sensi = 2) < 2.5 should be filtered
        when(flowResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(-1.0);
        initialize(Set.of(cnec1), 2.5, 2.5, 2.5, crac.getPreventiveState(), false, RangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        flowConstraint = linearProblem.getFlowConstraint(cnec1, TwoSides.ONE);
        rangeActionSetpoint = linearProblem.getRangeActionSetpointVariable(pstRangeAction, cnec1.getState());
        assertEquals(0, flowConstraint.getCoefficient(rangeActionSetpoint), DOUBLE_TOLERANCE);
        assertEquals(500., flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(500., flowConstraint.ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void testSensitivityFilter2() {
        OpenRaoMPConstraint flowConstraint;
        OpenRaoMPVariable rangeActionSetpoint;
        when(flowResult.getPtdfZonalSum(cnec1, TwoSides.ONE)).thenReturn(0.5);
        Map<Integer, Double> tapToAngle = pstRangeAction.getTapToAngleConversionMap();

        // (sensi = 2) > 1/.5 should not be filtered
        when(flowResult.getMargin(cnec1, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(-1.0);
        initialize(Set.of(cnec1), 1.5, 1.5, 1.5, crac.getPreventiveState(), false, RangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        flowConstraint = linearProblem.getFlowConstraint(cnec1, TwoSides.ONE);
        rangeActionSetpoint = linearProblem.getRangeActionSetpointVariable(pstRangeAction, cnec1.getState());
        assertEquals(-2, flowConstraint.getCoefficient(rangeActionSetpoint), DOUBLE_TOLERANCE);
        assertEquals(500. - 2 * tapToAngle.get(TAP_INITIAL), flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(500. - 2 * tapToAngle.get(TAP_INITIAL), flowConstraint.ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void testFilterCnecWithSensiFailure() {
        // cnec1 has a failed state, cnec2 has a succeeded state
        // only cnec2's flow variables & constraints must be added to MIP
        when(sensitivityResult.getSensitivityStatus(cnec1.getState())).thenReturn(ComputationStatus.FAILURE);
        when(sensitivityResult.getSensitivityStatus(cnec2.getState())).thenReturn(ComputationStatus.DEFAULT);
        initialize(Set.of(cnec1, cnec2), 1e-6, 1e-6, 1e-6, cnec1.getState(), false, RangeActionsOptimizationParameters.PstModel.CONTINUOUS);

        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, cnec2.getState());

        // check flow variable for cnec1 does not exist
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec1, TwoSides.ONE));
        assertEquals("Variable Tieline BE FR - N - preventive_one_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec1 does not exist
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec1, TwoSides.ONE));
        assertEquals("Constraint Tieline BE FR - N - preventive_one_flow_constraint has not been created yet", e.getMessage());

        // check flow variable for cnec2
        OpenRaoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2, TwoSides.TWO);
        assertNotNull(flowVariable2);
        assertEquals(-linearProblem.infinity(), flowVariable2.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable2.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec2
        OpenRaoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2, TwoSides.TWO);
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC2_IT1, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);
    }

    @Test
    void testFilterCnecWithSensiFailureAndUpdateWithoutChange() {
        // cnec1 has a failed state, cnec2 has a succeeded state
        // only cnec2's flow variables & constraints must be added to MIP
        when(sensitivityResult.getSensitivityStatus(cnec1.getState())).thenReturn(ComputationStatus.FAILURE);
        when(sensitivityResult.getSensitivityStatus(cnec2.getState())).thenReturn(ComputationStatus.DEFAULT);
        initialize(Set.of(cnec1, cnec2), 1e-6, 1e-6, 1e-6, cnec1.getState(), false, RangeActionsOptimizationParameters.PstModel.CONTINUOUS);

        updateLinearProblem();

        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, cnec2.getState());

        // check flow variable for cnec1 does not exist
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec1, TwoSides.ONE));
        assertEquals("Variable Tieline BE FR - N - preventive_one_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec1 does not exist
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec1, TwoSides.ONE));
        assertEquals("Constraint Tieline BE FR - N - preventive_one_flow_constraint has not been created yet", e.getMessage());

        // check flow variable for cnec2
        OpenRaoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2, TwoSides.TWO);
        assertNotNull(flowVariable2);
        assertEquals(-linearProblem.infinity(), flowVariable2.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable2.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec2
        final double currentAlpha = pstRangeAction.convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());
        OpenRaoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2, TwoSides.TWO);
        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * SENSI_CNEC2_IT2, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * SENSI_CNEC2_IT2, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC2_IT2, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);
    }

    @Test
    void testFilterCnecWithSensiFailureAndUpdateWithChange() {
        // cnec1 has a failed state, cnec2 has a succeeded state
        // only cnec2's flow variables & constraints must be added to MIP
        when(sensitivityResult.getSensitivityStatus(cnec1.getState())).thenReturn(ComputationStatus.FAILURE);
        when(sensitivityResult.getSensitivityStatus(cnec2.getState())).thenReturn(ComputationStatus.DEFAULT);
        initialize(Set.of(cnec1, cnec2), 1e-6, 1e-6, 1e-6, cnec1.getState(), false, RangeActionsOptimizationParameters.PstModel.CONTINUOUS);

        // invert sensitivity failure statuses & update
        // only cnec1's flow variables & constraints must be added to MIP
        when(sensitivityResult.getSensitivityStatus(cnec1.getState())).thenReturn(ComputationStatus.DEFAULT);
        when(sensitivityResult.getSensitivityStatus(cnec2.getState())).thenReturn(ComputationStatus.FAILURE);
        updateLinearProblem();

        // check flow variable for cnec2 does not exist
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec2, TwoSides.TWO));
        assertEquals("Variable Tieline BE FR - Defaut - N-1 NL1-NL3_two_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec2 does not exist
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec2, TwoSides.TWO));
        assertEquals("Constraint Tieline BE FR - Defaut - N-1 NL1-NL3_two_flow_constraint has not been created yet", e.getMessage());

        // check flow variable for cnec1
        OpenRaoMPVariable flowVariable1 = linearProblem.getFlowVariable(cnec1, TwoSides.ONE);
        assertNotNull(flowVariable1);
        assertEquals(-linearProblem.infinity(), flowVariable1.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable1.ub(), linearProblem.infinity() * 1e-3);

        final double currentAlpha = pstRangeAction.convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());
        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, cnec1.getState());

        // check flow constraint for cnec1
        OpenRaoMPConstraint flowConstraint1 = linearProblem.getFlowConstraint(cnec1, TwoSides.ONE);
        assertNotNull(flowConstraint1);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint1.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint1.getCoefficient(flowVariable1), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC1_IT2, flowConstraint1.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);
    }

    @Test
    void testInjectionBalance() {
        addInjectionsInCrac();
        initializeForInjection(Set.of(cnec1), 1e-6, 1e-6, 1e-6, crac.getPreventiveState(), false, RangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        State state = cnec1.getState();

        // test the global balance constraint
        InjectionRangeAction injectionRangeAction0 = crac.getInjectionRangeAction(INJECTION_RANGE_ACTION_ID_0);
        InjectionRangeAction injectionRangeAction1 = crac.getInjectionRangeAction(INJECTION_RANGE_ACTION_ID_1);
        InjectionRangeAction injectionRangeAction2 = crac.getInjectionRangeAction(INJECTION_RANGE_ACTION_ID_2);
        OpenRaoMPConstraint balanceConstraint = linearProblem.getInjectionBalanceVariationConstraint(state);
        OpenRaoMPVariable signedVariationVariableInj0 = linearProblem.getInjectionVariationVariable(injectionRangeAction0, state);
        OpenRaoMPVariable signedVariationVariableInj1 = linearProblem.getInjectionVariationVariable(injectionRangeAction1, state);
        assertNotNull(balanceConstraint);
        assertThrows(OpenRaoException.class, () -> linearProblem.getInjectionVariationVariable(injectionRangeAction2, state));
        assertEquals(1, balanceConstraint.getCoefficient(signedVariationVariableInj0), DOUBLE_TOLERANCE);
        assertEquals(1, balanceConstraint.getCoefficient(signedVariationVariableInj1), DOUBLE_TOLERANCE);
        // test the injection variation constraints for each injection
        OpenRaoMPConstraint singedVariationConstraint0 = linearProblem.getInjectionVariationConstraint(injectionRangeAction0, state);
        OpenRaoMPConstraint singedVariationConstraint1 = linearProblem.getInjectionVariationConstraint(injectionRangeAction1, state);
        OpenRaoMPVariable setPointVariableInj0 = linearProblem.getRangeActionSetpointVariable(injectionRangeAction0, state);
        OpenRaoMPVariable setPointVariableInj1 = linearProblem.getRangeActionSetpointVariable(injectionRangeAction1, state);
        double sumDistributionKeys0 = injectionRangeAction0.getInjectionDistributionKeys().values().stream().mapToDouble(d -> d).sum();
        double sumDistributionKeys1 = injectionRangeAction1.getInjectionDistributionKeys().values().stream().mapToDouble(d -> d).sum();
        assertNotNull(singedVariationConstraint0);
        assertNotNull(singedVariationConstraint1);
        assertThrows(OpenRaoException.class, () -> linearProblem.getInjectionVariationConstraint(injectionRangeAction2, state));
        assertEquals(1, singedVariationConstraint0.getCoefficient(signedVariationVariableInj0), DOUBLE_TOLERANCE);
        assertEquals(1, singedVariationConstraint1.getCoefficient(signedVariationVariableInj1), DOUBLE_TOLERANCE);
        assertEquals(-sumDistributionKeys0, singedVariationConstraint0.getCoefficient(setPointVariableInj0), DOUBLE_TOLERANCE);
        assertEquals(-sumDistributionKeys1, singedVariationConstraint1.getCoefficient(setPointVariableInj1), DOUBLE_TOLERANCE);
        assertEquals(-sumDistributionKeys0 * PRE_RESULT_SET_POINT_INJ_0, singedVariationConstraint0.lb(), DOUBLE_TOLERANCE);
        assertEquals(-sumDistributionKeys0 * PRE_RESULT_SET_POINT_INJ_0, singedVariationConstraint0.ub(), DOUBLE_TOLERANCE);
        assertEquals(-sumDistributionKeys1 * PRE_RESULT_SET_POINT_INJ_1, singedVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(-sumDistributionKeys1 * PRE_RESULT_SET_POINT_INJ_1, singedVariationConstraint1.ub(), DOUBLE_TOLERANCE);
    }
}
