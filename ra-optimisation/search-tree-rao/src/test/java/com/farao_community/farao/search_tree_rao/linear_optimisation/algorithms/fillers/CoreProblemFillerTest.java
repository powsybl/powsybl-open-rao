/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.State;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.data.crac_api.range_action.RangeAction;
import com.powsybl.open_rao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.powsybl.open_rao.rao_api.parameters.RaoParameters;
import com.powsybl.open_rao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPConstraint;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPVariable;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblemBuilder;
import com.powsybl.open_rao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.powsybl.open_rao.search_tree_rao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.open_rao.search_tree_rao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
    public void setUp() {
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
            .withSolver(mpSolver)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void initializeForPreventive(double pstSensitivityThreshold, double hvdcSensitivityThreshold, double injectionSensitivityThreshold) {
        initialize(Set.of(cnec1), pstSensitivityThreshold, hvdcSensitivityThreshold, injectionSensitivityThreshold, crac.getPreventiveState(), false);
    }

    private void initializeForGlobal() {
        initialize(Set.of(cnec1, cnec2), 1e-6, 1e-6, 1e-6, crac.getPreventiveState(), false);
    }

    private void initialize(Set<FlowCnec> cnecs, double pstSensitivityThreshold, double hvdcSensitivityThreshold, double injectionSensitivityThreshold, State mainState, boolean raRangeShrinking) {
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
            new RangeActionActivationResultImpl(initialRangeActionSetpointResult),
            rangeActionParameters,
            Unit.MEGAWATT, raRangeShrinking);
        buildLinearProblem();
    }

    @Test
    void fillTestOnPreventive() {
        initializeForPreventive(1e-6, 1e-6, 1e-6);
        State state = cnec1.getState();

        // check range action setpoint variable
        FaraoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);
        assertNotNull(setPointVariable);
        assertEquals(minAlpha, setPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, setPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable
        FaraoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction, state);
        assertNotNull(absoluteVariationVariable);
        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
        assertEquals(LinearProblem.infinity(), absoluteVariationVariable.ub(), DOUBLE_TOLERANCE);

        // check flow variable for cnec1
        FaraoMPVariable flowVariable = linearProblem.getFlowVariable(cnec1, Side.LEFT);
        assertNotNull(flowVariable);
        assertEquals(-LinearProblem.infinity(), flowVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), flowVariable.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec1
        FaraoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1, Side.LEFT);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(-SENSI_CNEC1_IT1, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2 does not exist
        Exception e = assertThrows(FaraoException.class, () -> linearProblem.getFlowVariable(cnec2, Side.RIGHT));
        assertEquals("Variable Tieline BE FR - Defaut - N-1 NL1-NL3_right_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec2 does not exist
        e = assertThrows(FaraoException.class, () -> linearProblem.getFlowConstraint(cnec2, Side.RIGHT));
        assertEquals("Constraint Tieline BE FR - Defaut - N-1 NL1-NL3_right_flow_constraint has not been created yet", e.getMessage());

        // check absolute variation constraints
        FaraoMPConstraint absoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, state, LinearProblem.AbsExtension.NEGATIVE);
        FaraoMPConstraint absoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, state, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(absoluteVariationConstraint1);
        assertNotNull(absoluteVariationConstraint2);
        assertEquals(-initialAlpha, absoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), absoluteVariationConstraint1.ub(), DOUBLE_TOLERANCE);
        assertEquals(initialAlpha, absoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), absoluteVariationConstraint2.ub(), DOUBLE_TOLERANCE);

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
        FaraoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);
        assertNotNull(setPointVariable);
        assertEquals(minAlpha, setPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, setPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable
        FaraoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction, state);
        assertNotNull(absoluteVariationVariable);
        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
        assertEquals(LinearProblem.infinity(), absoluteVariationVariable.ub(), DOUBLE_TOLERANCE);

        // check flow variable for cnec1
        FaraoMPVariable flowVariable = linearProblem.getFlowVariable(cnec1, Side.LEFT);
        assertNotNull(flowVariable);
        assertEquals(-LinearProblem.infinity(), flowVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), flowVariable.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec1
        FaraoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1, Side.LEFT);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * 0, flowConstraint.lb(), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * 0, flowConstraint.ub(), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(0, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)

        // check flow variable for cnec2 does not exist
        Exception e = assertThrows(FaraoException.class, () -> linearProblem.getFlowVariable(cnec2, Side.RIGHT));
        assertEquals("Variable Tieline BE FR - Defaut - N-1 NL1-NL3_right_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec2 does not exist
        e = assertThrows(FaraoException.class, () -> linearProblem.getFlowConstraint(cnec2, Side.RIGHT));
        assertEquals("Constraint Tieline BE FR - Defaut - N-1 NL1-NL3_right_flow_constraint has not been created yet", e.getMessage());

        // check absolute variation constraints
        FaraoMPConstraint absoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, state, LinearProblem.AbsExtension.NEGATIVE);
        FaraoMPConstraint absoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, state, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(absoluteVariationConstraint1);
        assertNotNull(absoluteVariationConstraint2);
        assertEquals(-initialAlpha, absoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), absoluteVariationConstraint1.ub(), DOUBLE_TOLERANCE);
        assertEquals(initialAlpha, absoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), absoluteVariationConstraint2.ub(), DOUBLE_TOLERANCE);

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
        initialize(Set.of(cnec2), 1e-6, 1e-6, 1e-6, cnec2.getState(), false);
        State state = cnec2.getState();

        // check range action setpoint variable
        FaraoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);
        assertNotNull(setPointVariable);
        assertEquals(minAlpha, setPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, setPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable
        FaraoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction, state);
        assertNotNull(absoluteVariationVariable);
        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
        assertEquals(LinearProblem.infinity(), absoluteVariationVariable.ub(), DOUBLE_TOLERANCE);

        // check flow variable for cnec1 does not exist
        Exception e = assertThrows(FaraoException.class, () -> linearProblem.getFlowVariable(cnec1, Side.LEFT));
        assertEquals("Variable Tieline BE FR - N - preventive_left_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec1 does not exist
        e = assertThrows(FaraoException.class, () -> linearProblem.getFlowConstraint(cnec1, Side.LEFT));
        assertEquals("Constraint Tieline BE FR - N - preventive_left_flow_constraint has not been created yet", e.getMessage());

        // check flow variable for cnec2
        FaraoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2, Side.RIGHT);
        assertNotNull(flowVariable2);
        assertEquals(-LinearProblem.infinity(), flowVariable2.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), flowVariable2.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec2
        FaraoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2, Side.RIGHT);
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC2_IT1, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check absolute variation constraints
        FaraoMPConstraint absoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, state, LinearProblem.AbsExtension.NEGATIVE);
        FaraoMPConstraint absoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, state, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(absoluteVariationConstraint1);
        assertNotNull(absoluteVariationConstraint2);
        assertEquals(-initialAlpha, absoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), absoluteVariationConstraint1.ub(), DOUBLE_TOLERANCE);
        assertEquals(initialAlpha, absoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), absoluteVariationConstraint2.ub(), DOUBLE_TOLERANCE);

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
    void fillTestOnGlobal() {
        initializeForGlobal();
        State prevState = cnec1.getState();
        State curState = cnec2.getState();

        // check range action setpoint variable for preventive state
        FaraoMPVariable prevSetPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, prevState);
        assertNotNull(prevSetPointVariable);
        assertEquals(minAlpha, prevSetPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, prevSetPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action setpoint variable for curative state
        FaraoMPVariable curSetPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, curState);
        assertNotNull(curSetPointVariable);
        assertEquals(minAlpha, curSetPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, curSetPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable for preventive state
        FaraoMPVariable prevAbsoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction, prevState);
        assertNotNull(prevAbsoluteVariationVariable);
        assertEquals(0, prevAbsoluteVariationVariable.lb(), 0.01);
        assertEquals(LinearProblem.infinity(), prevAbsoluteVariationVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable for curative state
        FaraoMPVariable curAbsoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction, curState);
        assertNotNull(curAbsoluteVariationVariable);
        assertEquals(0, curAbsoluteVariationVariable.lb(), 0.01);
        assertEquals(LinearProblem.infinity(), curAbsoluteVariationVariable.ub(), DOUBLE_TOLERANCE);

        // check flow variable for cnec1
        FaraoMPVariable flowVariable = linearProblem.getFlowVariable(cnec1, Side.LEFT);
        assertNotNull(flowVariable);
        assertEquals(-LinearProblem.infinity(), flowVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), flowVariable.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec1
        FaraoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1, Side.LEFT);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(-SENSI_CNEC1_IT1, flowConstraint.getCoefficient(prevSetPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2
        FaraoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2, Side.RIGHT);
        assertNotNull(flowVariable2);
        assertEquals(-LinearProblem.infinity(), flowVariable2.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), flowVariable2.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec2
        FaraoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2, Side.RIGHT);
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC2_IT1, flowConstraint2.getCoefficient(curSetPointVariable), DOUBLE_TOLERANCE);

        // check absolute variation constraints for preventive state
        FaraoMPConstraint prevAbsoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, prevState, LinearProblem.AbsExtension.NEGATIVE);
        FaraoMPConstraint prevAbsoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, prevState, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(prevAbsoluteVariationConstraint1);
        assertNotNull(prevAbsoluteVariationConstraint2);
        assertEquals(-initialAlpha, prevAbsoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), prevAbsoluteVariationConstraint1.ub(), DOUBLE_TOLERANCE);
        assertEquals(initialAlpha, prevAbsoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), prevAbsoluteVariationConstraint2.ub(), DOUBLE_TOLERANCE);

        // check absolute variation constraints for curative state
        FaraoMPConstraint curAbsoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, curState, LinearProblem.AbsExtension.NEGATIVE);
        FaraoMPConstraint curAbsoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(pstRangeAction, curState, LinearProblem.AbsExtension.POSITIVE);
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
        // total number of constraints 7 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (absolute variation constraints)
        //      - 1 for curative range action (relative variation constraint)
        assertEquals(6, linearProblem.numVariables());
        assertEquals(7, linearProblem.numConstraints());
    }

    private void updateLinearProblem() {
        // arrange some additional data
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_IT2);
        initialAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();

        when(flowResult.getFlow(cnec1, Side.LEFT, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC1_IT2);
        when(flowResult.getFlow(cnec2, Side.RIGHT, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC2_IT2);
        when(sensitivityResult.getSensitivityValue(cnec1, Side.LEFT, pstRangeAction, Unit.MEGAWATT)).thenReturn(SENSI_CNEC1_IT2);
        when(sensitivityResult.getSensitivityValue(cnec2, Side.RIGHT, pstRangeAction, Unit.MEGAWATT)).thenReturn(SENSI_CNEC2_IT2);

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

        FaraoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);

        // check flow variable for cnec1
        FaraoMPVariable flowVariable = linearProblem.getFlowVariable(cnec1, Side.LEFT);
        assertNotNull(flowVariable);
        assertEquals(-LinearProblem.infinity(), flowVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), flowVariable.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec1
        FaraoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1, Side.LEFT);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(-SENSI_CNEC1_IT2, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2 does not exist
        Exception e = assertThrows(FaraoException.class, () -> linearProblem.getFlowVariable(cnec2, Side.RIGHT));
        assertEquals("Variable Tieline BE FR - Defaut - N-1 NL1-NL3_right_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec2 does not exist
        e = assertThrows(FaraoException.class, () -> linearProblem.getFlowConstraint(cnec2, Side.RIGHT));
        assertEquals("Constraint Tieline BE FR - Defaut - N-1 NL1-NL3_right_flow_constraint has not been created yet", e.getMessage());

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
    void updateTestOnCurativeWithRaRangeShrinking() {
        initialize(Set.of(cnec2), 1e-6, 1e-6, 1e-6, cnec2.getState(), true);
        State state = cnec2.getState();
        // update the problem with new data
        updateLinearProblem();

        // some additional data
        final double currentAlpha = pstRangeAction.convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());

        FaraoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);

        // check flow variable for cnec1 does not exist
        Exception e = assertThrows(FaraoException.class, () -> linearProblem.getFlowVariable(cnec1, Side.LEFT));
        assertEquals("Variable Tieline BE FR - N - preventive_left_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec1 does not exist
        e = assertThrows(FaraoException.class, () -> linearProblem.getFlowConstraint(cnec1, Side.LEFT));
        assertEquals("Constraint Tieline BE FR - N - preventive_left_flow_constraint has not been created yet", e.getMessage());

        // check flow variable for cnec2
        FaraoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2, Side.RIGHT);
        assertNotNull(flowVariable2);
        assertEquals(-LinearProblem.infinity(), flowVariable2.lb(), DOUBLE_TOLERANCE);
        assertEquals(LinearProblem.infinity(), flowVariable2.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec2
        FaraoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2, Side.RIGHT);
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
    void updateWithoutFillingTest() {
        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter.getFlowCnecs()).thenReturn(Set.of(cnec1));

        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        rangeActions.put(cnec1.getState(), Set.of(pstRangeAction));
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);

        RaoParameters raoParameters = new RaoParameters();
        RangeActionsOptimizationParameters rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(raoParameters);
        coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionSetpointResult,
            new RangeActionActivationResultImpl(initialRangeActionSetpointResult),
            rangeActionParameters,
            Unit.MEGAWATT,
            false);
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withSolver(mpSolver)
            .build();
        try {
            updateLinearProblem();
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    void testSensitivityFilter1() {
        FaraoMPConstraint flowConstraint;
        FaraoMPVariable rangeActionSetpoint;
        when(flowResult.getPtdfZonalSum(cnec1, Side.LEFT)).thenReturn(0.5);

        // (sensi = 2) < 2.5 should be filtered
        when(flowResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(-1.0);
        initialize(Set.of(cnec1), 2.5, 2.5, 2.5, crac.getPreventiveState(), false);
        flowConstraint = linearProblem.getFlowConstraint(cnec1, Side.LEFT);
        rangeActionSetpoint = linearProblem.getRangeActionSetpointVariable(pstRangeAction, cnec1.getState());
        assertEquals(0, flowConstraint.getCoefficient(rangeActionSetpoint), DOUBLE_TOLERANCE);
        assertEquals(500., flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(500., flowConstraint.ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void testSensitivityFilter2() {
        FaraoMPConstraint flowConstraint;
        FaraoMPVariable rangeActionSetpoint;
        when(flowResult.getPtdfZonalSum(cnec1, Side.LEFT)).thenReturn(0.5);
        Map<Integer, Double> tapToAngle = pstRangeAction.getTapToAngleConversionMap();

        // (sensi = 2) > 1/.5 should not be filtered
        when(flowResult.getMargin(cnec1, Side.LEFT, Unit.MEGAWATT)).thenReturn(-1.0);
        initialize(Set.of(cnec1), 1.5, 1.5, 1.5, crac.getPreventiveState(), false);
        flowConstraint = linearProblem.getFlowConstraint(cnec1, Side.LEFT);
        rangeActionSetpoint = linearProblem.getRangeActionSetpointVariable(pstRangeAction, cnec1.getState());
        assertEquals(-2, flowConstraint.getCoefficient(rangeActionSetpoint), DOUBLE_TOLERANCE);
        assertEquals(500. - 2 * tapToAngle.get(TAP_INITIAL), flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(500. - 2 * tapToAngle.get(TAP_INITIAL), flowConstraint.ub(), DOUBLE_TOLERANCE);
    }
}
