/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
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
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class CostCoreProblemFillerTest extends AbstractFillerTest {
    private LinearProblem linearProblem;
    private CostCoreProblemFiller coreProblemFiller;
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
            .withSolver(SearchTreeRaoRangeActionsOptimizationParameters.Solver.SCIP)
            .withInitialRangeActionActivationResult(getInitialRangeActionActivationResult())
            .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void initializeForPreventive(double pstSensitivityThreshold, double hvdcSensitivityThreshold, double injectionSensitivityThreshold) {
        initialize(
            Set.of(cnec1),
            pstSensitivityThreshold,
            hvdcSensitivityThreshold,
            injectionSensitivityThreshold,
            crac.getPreventiveState(),
            false,
            SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS
        );
    }

    private void initializeForGlobal(SearchTreeRaoRangeActionsOptimizationParameters.PstModel pstModel) {
        initialize(Set.of(cnec1, cnec2), 1e-6, 1e-6, 1e-6, crac.getPreventiveState(), false, pstModel);
    }

    private void initialize(Set<FlowCnec> cnecs,
                            double pstSensitivityThreshold,
                            double hvdcSensitivityThreshold,
                            double injectionSensitivityThreshold,
                            State mainState,
                            boolean raRangeShrinking,
                            SearchTreeRaoRangeActionsOptimizationParameters.PstModel pstModel) {
        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter.getFlowCnecs()).thenReturn(cnecs);
        Mockito.when(optimizationPerimeter.getMainOptimizationState()).thenReturn(mainState);

        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        cnecs.forEach(cnec -> rangeActions.put(cnec.getState(), Set.of(pstRangeAction)));
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);

        RaoParameters raoParameters = new RaoParameters();
        OpenRaoSearchTreeParameters searchTreeParameters = new OpenRaoSearchTreeParameters();
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, searchTreeParameters);
        searchTreeParameters.getRangeActionsOptimizationParameters().setPstSensitivityThreshold(pstSensitivityThreshold);
        searchTreeParameters.getRangeActionsOptimizationParameters().setHvdcSensitivityThreshold(hvdcSensitivityThreshold);
        searchTreeParameters.getRangeActionsOptimizationParameters().setInjectionRaSensitivityThreshold(injectionSensitivityThreshold);
        RangeActionsOptimizationParameters rangeActionParameters = raoParameters.getRangeActionsOptimizationParameters();

        coreProblemFiller = new CostCoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionSetpointResult,
            rangeActionParameters,
            searchTreeParameters.getRangeActionsOptimizationParameters(),
            Unit.MEGAWATT,
            raRangeShrinking,
            pstModel,
            null);
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

        // check upward variation variable
        OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(pstRangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);
        assertNotNull(upwardVariationVariable);
        assertEquals(0, upwardVariationVariable.lb(), 0.01);
        assertEquals(linearProblem.infinity(), upwardVariationVariable.ub(), linearProblem.infinity() * 1e-3);

        // check downward variation variable
        OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(pstRangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        assertNotNull(downwardVariationVariable);
        assertEquals(0, downwardVariationVariable.lb(), 0.01);
        assertEquals(linearProblem.infinity(), downwardVariationVariable.ub(), linearProblem.infinity() * 1e-3);

        // check binary activation variable
        OpenRaoMPVariable activationVariable = linearProblem.getRangeActionVariationBinary(pstRangeAction, state);
        assertNotNull(activationVariable);
        assertEquals(0, activationVariable.lb(), 0.01);
        assertEquals(1, activationVariable.ub(), 0.01);

        // check flow variable for cnec1
        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec1, TwoSides.ONE, Optional.empty());
        assertNotNull(flowVariable);
        assertEquals(-linearProblem.infinity(), flowVariable.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec1
        OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1, TwoSides.ONE, Optional.empty());
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(-SENSI_CNEC1_IT1, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2 does not exist
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec2, TwoSides.TWO, Optional.empty()));
        assertEquals("Variable Tieline BE FR - Defaut - N-1 NL1-NL3_two_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec2 does not exist
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec2, TwoSides.TWO, Optional.empty()));
        assertEquals("Constraint Tieline BE FR - Defaut - N-1 NL1-NL3_two_flow_constraint has not been created yet", e.getMessage());

        // check set-point variation constraint
        OpenRaoMPConstraint setPointVariationConstraint = linearProblem.getRangeActionSetPointVariationConstraint(pstRangeAction, state);
        assertNotNull(setPointVariationConstraint);
        assertEquals(1.9479, setPointVariationConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(1.9479, setPointVariationConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, setPointVariationConstraint.getCoefficient(setPointVariable));
        assertEquals(-1, setPointVariationConstraint.getCoefficient(upwardVariationVariable));
        assertEquals(1, setPointVariationConstraint.getCoefficient(downwardVariationVariable));

        // check activation constraint
        OpenRaoMPConstraint activationConstraint = linearProblem.getIsVariationConstraint(pstRangeAction, state);
        assertNotNull(activationConstraint);
        assertEquals(0, activationConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), activationConstraint.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(-1, activationConstraint.getCoefficient(upwardVariationVariable));
        assertEquals(-1, activationConstraint.getCoefficient(downwardVariationVariable));
        assertEquals(11.6782, activationConstraint.getCoefficient(activationVariable), DOUBLE_TOLERANCE);

        // check the number of variables and constraints
        // total number of variables 4 :
        //      - 1 per CNEC (flow)
        //      - 4 per range action (set-point, activation and variation up/down)
        // total number of constraints 4 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (activation and set-point)
        assertEquals(5, linearProblem.numVariables());
        assertEquals(3, linearProblem.numConstraints());

        // check objective
        assertEquals(15.0, linearProblem.getObjective().getCoefficient(activationVariable));
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

        // check upward variation variable
        OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(pstRangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);
        assertNotNull(upwardVariationVariable);
        assertEquals(0, upwardVariationVariable.lb(), 0.01);
        assertEquals(linearProblem.infinity(), upwardVariationVariable.ub(), linearProblem.infinity() * 1e-3);

        // check downward variation variable
        OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(pstRangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        assertNotNull(downwardVariationVariable);
        assertEquals(0, downwardVariationVariable.lb(), 0.01);
        assertEquals(linearProblem.infinity(), downwardVariationVariable.ub(), linearProblem.infinity() * 1e-3);

        // check binary activation variable
        OpenRaoMPVariable activationVariable = linearProblem.getRangeActionVariationBinary(pstRangeAction, state);
        assertNotNull(activationVariable);
        assertEquals(0, activationVariable.lb(), 0.01);
        assertEquals(1, activationVariable.ub(), 0.01);

        // check flow variable for cnec1
        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec1, TwoSides.ONE, Optional.empty());
        assertNotNull(flowVariable);
        assertEquals(-linearProblem.infinity(), flowVariable.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec1
        OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1, TwoSides.ONE, Optional.empty());
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * 0, flowConstraint.lb(), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * 0, flowConstraint.ub(), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(0, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)

        // check flow variable for cnec2 does not exist
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec2, TwoSides.TWO, Optional.empty()));
        assertEquals("Variable Tieline BE FR - Defaut - N-1 NL1-NL3_two_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec2 does not exist
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec2, TwoSides.TWO, Optional.empty()));
        assertEquals("Constraint Tieline BE FR - Defaut - N-1 NL1-NL3_two_flow_constraint has not been created yet", e.getMessage());

        // check set-point variation constraint
        OpenRaoMPConstraint setPointVariationConstraint = linearProblem.getRangeActionSetPointVariationConstraint(pstRangeAction, state);
        assertNotNull(setPointVariationConstraint);
        assertEquals(1.9479, setPointVariationConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(1.9479, setPointVariationConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, setPointVariationConstraint.getCoefficient(setPointVariable));
        assertEquals(-1, setPointVariationConstraint.getCoefficient(upwardVariationVariable));
        assertEquals(1, setPointVariationConstraint.getCoefficient(downwardVariationVariable));

        // check activation constraint
        OpenRaoMPConstraint activationConstraint = linearProblem.getIsVariationConstraint(pstRangeAction, state);
        assertNotNull(activationConstraint);
        assertEquals(0, activationConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), activationConstraint.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(-1, activationConstraint.getCoefficient(upwardVariationVariable));
        assertEquals(-1, activationConstraint.getCoefficient(downwardVariationVariable));
        assertEquals(11.6782, activationConstraint.getCoefficient(activationVariable), DOUBLE_TOLERANCE);

        // check the number of variables and constraints
        // total number of variables 5 :
        //      - 1 per CNEC (flow)
        //      - 4 per range action (set-point, activation and variation up/down)
        // total number of constraints 3 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (activation and set-point)
        assertEquals(5, linearProblem.numVariables());
        assertEquals(3, linearProblem.numConstraints());

        // check objective
        assertEquals(15.0, linearProblem.getObjective().getCoefficient(activationVariable));
    }

    @Test
    void fillTestOnCurative() {
        initialize(Set.of(cnec2), 1e-6, 1e-6, 1e-6, cnec2.getState(), false, SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);
        State state = cnec2.getState();

        // check range action setpoint variable
        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);
        assertNotNull(setPointVariable);
        assertEquals(minAlpha, setPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, setPointVariable.ub(), DOUBLE_TOLERANCE);

        // check upward variation variable
        OpenRaoMPVariable upwardVariationVariable = linearProblem.getRangeActionVariationVariable(pstRangeAction, state, LinearProblem.VariationDirectionExtension.UPWARD);
        assertNotNull(upwardVariationVariable);
        assertEquals(0, upwardVariationVariable.lb(), 0.01);
        assertEquals(linearProblem.infinity(), upwardVariationVariable.ub(), linearProblem.infinity() * 1e-3);

        // check downward variation variable
        OpenRaoMPVariable downwardVariationVariable = linearProblem.getRangeActionVariationVariable(pstRangeAction, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        assertNotNull(downwardVariationVariable);
        assertEquals(0, downwardVariationVariable.lb(), 0.01);
        assertEquals(linearProblem.infinity(), downwardVariationVariable.ub(), linearProblem.infinity() * 1e-3);

        // check binary activation variable
        OpenRaoMPVariable activationVariable = linearProblem.getRangeActionVariationBinary(pstRangeAction, state);
        assertNotNull(activationVariable);
        assertEquals(0, activationVariable.lb(), 0.01);
        assertEquals(1, activationVariable.ub(), 0.01);

        // check flow variable for cnec1 does not exist
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec1, TwoSides.ONE, Optional.empty()));
        assertEquals("Variable Tieline BE FR - N - preventive_one_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec1 does not exist
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec1, TwoSides.ONE, Optional.empty()));
        assertEquals("Constraint Tieline BE FR - N - preventive_one_flow_constraint has not been created yet", e.getMessage());

        // check flow variable for cnec2
        OpenRaoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2, TwoSides.TWO, Optional.empty());
        assertNotNull(flowVariable2);
        assertEquals(-linearProblem.infinity(), flowVariable2.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable2.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec2
        OpenRaoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2, TwoSides.TWO, Optional.empty());
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC2_IT1, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check set-point variation constraint
        OpenRaoMPConstraint setPointVariationConstraint = linearProblem.getRangeActionSetPointVariationConstraint(pstRangeAction, state);
        assertNotNull(setPointVariationConstraint);
        assertEquals(1.9479, setPointVariationConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(1.9479, setPointVariationConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, setPointVariationConstraint.getCoefficient(setPointVariable));
        assertEquals(-1, setPointVariationConstraint.getCoefficient(upwardVariationVariable));
        assertEquals(1, setPointVariationConstraint.getCoefficient(downwardVariationVariable));

        // check activation constraint
        OpenRaoMPConstraint activationConstraint = linearProblem.getIsVariationConstraint(pstRangeAction, state);
        assertNotNull(activationConstraint);
        assertEquals(0, activationConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), activationConstraint.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(-1, activationConstraint.getCoefficient(upwardVariationVariable));
        assertEquals(-1, activationConstraint.getCoefficient(downwardVariationVariable));
        assertEquals(11.6782, activationConstraint.getCoefficient(activationVariable), DOUBLE_TOLERANCE);

        // check the number of variables and constraints
        // total number of variables 5 :
        //      - 1 per CNEC (flow)
        //      - 4 per range action (set-point, activation and variation up/down)
        // total number of constraints 3 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (activation, set-point variation and iterative relative variation constraint: created before 2nd iteration)
        assertEquals(5, linearProblem.numVariables());
        assertEquals(3, linearProblem.numConstraints());

        // check objective
        assertEquals(15.0, linearProblem.getObjective().getCoefficient(activationVariable));
    }

    @Test
    void testContinuousPstMode() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> initializeForGlobal(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS));
        assertEquals("Costly remedial action optimization is only available for the APPROXIMATED_INTEGERS mode of PST range actions.", exception.getMessage());
    }

    @Test
    void fillTestOnGlobal() {
        initializeForGlobal(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);
        State prevState = cnec1.getState();
        State curState = cnec2.getState();

        // check relative setpoint constraint for PRA_PST_BE has not been created in curative.
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getRangeActionRelativeSetpointConstraint(pstRangeAction, curState, LinearProblem.RaRangeShrinking.FALSE));
        assertEquals("Constraint PRA_PST_BE_N-1 NL1-NL3 - curative_relative_setpoint_constraint has not been created yet", e.getMessage());

        // check range action setpoint variable for preventive state
        OpenRaoMPVariable prevSetPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, prevState);
        assertNotNull(prevSetPointVariable);
        assertEquals(minAlpha, prevSetPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, prevSetPointVariable.ub(), DOUBLE_TOLERANCE);

        // check upward variation variable for preventive state
        OpenRaoMPVariable prevUpwardVariationVariable = linearProblem.getRangeActionVariationVariable(pstRangeAction, prevState, LinearProblem.VariationDirectionExtension.UPWARD);
        assertNotNull(prevUpwardVariationVariable);
        assertEquals(0, prevUpwardVariationVariable.lb(), 0.01);
        assertEquals(linearProblem.infinity(), prevUpwardVariationVariable.ub(), linearProblem.infinity() * 1e-3);

        // check downward variation variable for preventive state
        OpenRaoMPVariable prevDownwardVariationVariable = linearProblem.getRangeActionVariationVariable(pstRangeAction, prevState, LinearProblem.VariationDirectionExtension.DOWNWARD);
        assertNotNull(prevDownwardVariationVariable);
        assertEquals(0, prevDownwardVariationVariable.lb(), 0.01);
        assertEquals(linearProblem.infinity(), prevDownwardVariationVariable.ub(), linearProblem.infinity() * 1e-3);

        // check binary activation variable for preventive state
        OpenRaoMPVariable prevActivationVariable = linearProblem.getRangeActionVariationBinary(pstRangeAction, prevState);
        assertNotNull(prevActivationVariable);
        assertEquals(0, prevActivationVariable.lb(), 0.01);
        assertEquals(1, prevActivationVariable.ub(), 0.01);

        // check range action setpoint variable for curative state
        OpenRaoMPVariable curSetPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, curState);
        assertNotNull(curSetPointVariable);
        assertEquals(minAlpha, curSetPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, curSetPointVariable.ub(), DOUBLE_TOLERANCE);

        // check upward variation variable for curative state
        OpenRaoMPVariable curUpwardVariationVariable = linearProblem.getRangeActionVariationVariable(pstRangeAction, curState, LinearProblem.VariationDirectionExtension.UPWARD);
        assertNotNull(curUpwardVariationVariable);
        assertEquals(0, curUpwardVariationVariable.lb(), 0.01);
        assertEquals(linearProblem.infinity(), curUpwardVariationVariable.ub(), linearProblem.infinity() * 1e-3);

        // check downward variation variable for curative state
        OpenRaoMPVariable curDownwardVariationVariable = linearProblem.getRangeActionVariationVariable(pstRangeAction, curState, LinearProblem.VariationDirectionExtension.DOWNWARD);
        assertNotNull(curDownwardVariationVariable);
        assertEquals(0, curDownwardVariationVariable.lb(), 0.01);
        assertEquals(linearProblem.infinity(), curDownwardVariationVariable.ub(), linearProblem.infinity() * 1e-3);

        // check binary activation variable for curative state
        OpenRaoMPVariable curActivationVariable = linearProblem.getRangeActionVariationBinary(pstRangeAction, curState);
        assertNotNull(curActivationVariable);
        assertEquals(0, curActivationVariable.lb(), 0.01);
        assertEquals(1, curActivationVariable.ub(), 0.01);

        // check flow variable for cnec1
        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec1, TwoSides.ONE, Optional.empty());
        assertNotNull(flowVariable);
        assertEquals(-linearProblem.infinity(), flowVariable.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec1
        OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1, TwoSides.ONE, Optional.empty());
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(-SENSI_CNEC1_IT1, flowConstraint.getCoefficient(prevSetPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2
        OpenRaoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2, TwoSides.TWO, Optional.empty());
        assertNotNull(flowVariable2);
        assertEquals(-linearProblem.infinity(), flowVariable2.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable2.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec2
        OpenRaoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2, TwoSides.TWO, Optional.empty());
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC2_IT1, flowConstraint2.getCoefficient(curSetPointVariable), DOUBLE_TOLERANCE);

        // check set-point variation constraint for preventive state
        OpenRaoMPConstraint prevSetPointVariationConstraint = linearProblem.getRangeActionSetPointVariationConstraint(pstRangeAction, prevState);
        assertNotNull(prevSetPointVariationConstraint);
        assertEquals(1.9479, prevSetPointVariationConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(1.9479, prevSetPointVariationConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, prevSetPointVariationConstraint.getCoefficient(prevSetPointVariable));
        assertEquals(-1, prevSetPointVariationConstraint.getCoefficient(prevUpwardVariationVariable));
        assertEquals(1, prevSetPointVariationConstraint.getCoefficient(prevDownwardVariationVariable));

        // check activation constraint for preventive state
        OpenRaoMPConstraint prevActivationConstraint = linearProblem.getIsVariationConstraint(pstRangeAction, prevState);
        assertNotNull(prevActivationConstraint);
        assertEquals(0, prevActivationConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), prevActivationConstraint.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(-1, prevActivationConstraint.getCoefficient(prevUpwardVariationVariable));
        assertEquals(-1, prevActivationConstraint.getCoefficient(prevDownwardVariationVariable));
        assertEquals(11.6782, prevActivationConstraint.getCoefficient(prevActivationVariable), DOUBLE_TOLERANCE);

        // check set-point variation constraint for curative state
        OpenRaoMPConstraint curSetPointVariationConstraint = linearProblem.getRangeActionSetPointVariationConstraint(pstRangeAction, curState);
        assertNotNull(curSetPointVariationConstraint);
        assertEquals(0.0, curSetPointVariationConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(0.0, curSetPointVariationConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, curSetPointVariationConstraint.getCoefficient(curSetPointVariable));
        assertEquals(-1, curSetPointVariationConstraint.getCoefficient(curUpwardVariationVariable));
        assertEquals(1, curSetPointVariationConstraint.getCoefficient(curDownwardVariationVariable));
        assertEquals(-1, curSetPointVariationConstraint.getCoefficient(prevSetPointVariable));

        // check activation constraint for curative state
        OpenRaoMPConstraint curActivationConstraint = linearProblem.getIsVariationConstraint(pstRangeAction, curState);
        assertNotNull(curActivationConstraint);
        assertEquals(0, curActivationConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), curActivationConstraint.ub(), linearProblem.infinity() * 1e-3);
        assertEquals(-1, curActivationConstraint.getCoefficient(curUpwardVariationVariable));
        assertEquals(-1, curActivationConstraint.getCoefficient(curDownwardVariationVariable));
        assertEquals(11.6782, curActivationConstraint.getCoefficient(curActivationVariable), DOUBLE_TOLERANCE);

        // check the number of variables and constraints
        // total number of variables 10 :
        //      - 1 per CNEC (flow)
        //      - 5 per range action (set-point, activation and variation up/down)
        // total number of constraints 6 or 7:
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (activation and set-point)
        //      - 0 or 1 for curative range action (relative variation constraint)
        assertEquals(10, linearProblem.numVariables());
        assertEquals(6, linearProblem.numConstraints());
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
        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec1, TwoSides.ONE, Optional.empty());
        assertNotNull(flowVariable);
        assertEquals(-linearProblem.infinity(), flowVariable.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec1
        OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1, TwoSides.ONE, Optional.empty());
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC1_IT2, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2 does not exist
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec2, TwoSides.TWO, Optional.empty()));
        assertEquals("Variable Tieline BE FR - Defaut - N-1 NL1-NL3_two_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec2 does not exist
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec2, TwoSides.TWO, Optional.empty()));
        assertEquals("Constraint Tieline BE FR - Defaut - N-1 NL1-NL3_two_flow_constraint has not been created yet", e.getMessage());

        // check the number of variables and constraints
        // total number of variables 5 :
        //      - 1 per CNEC (flow)
        //      - 4 per range action (set-point, activation and variation up/down)
        // total number of constraints 3 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (activation, set-point variation and iterative relative variation constraint: created before 2nd iteration)

        assertEquals(5, linearProblem.numVariables());
        assertEquals(3, linearProblem.numConstraints());
    }

    @Test
    void updateTestOnPreventiveWithRaRangeShrinking() {
        initialize(Set.of(cnec1), 1e-6, 1e-6, 1e-6, crac.getPreventiveState(), true, SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);
        State state = cnec1.getState();

        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getRangeActionRelativeSetpointConstraint(pstRangeAction, state, LinearProblem.RaRangeShrinking.TRUE));
        assertEquals("Constraint PRA_PST_BE_preventive_relative_setpoint_iterative-shrink_constraint has not been created yet", e.getMessage());

        // 1st update
        updateLinearProblem();

        assertEquals(5, linearProblem.numVariables());
        assertEquals(4, linearProblem.numConstraints());

        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);
        OpenRaoMPConstraint shrinkingConstraint = linearProblem.getRangeActionRelativeSetpointConstraint(pstRangeAction, state, LinearProblem.RaRangeShrinking.TRUE);
        assertNotNull(shrinkingConstraint);
        assertEquals(1, shrinkingConstraint.getCoefficient(setPointVariable));
        assertEquals(-10.5161, shrinkingConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(5.0626, shrinkingConstraint.ub(), DOUBLE_TOLERANCE);

        // 2nd update
        updateLinearProblem();

        assertEquals(5, linearProblem.numVariables());
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
        initialize(Set.of(cnec2), 1e-6, 1e-6, 1e-6, cnec2.getState(), true, SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);
        State state = cnec2.getState();
        // update the problem with new data
        updateLinearProblem();

        // some additional data
        final double currentAlpha = pstRangeAction.convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());

        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, state);

        // check flow variable for cnec1 does not exist
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec1, TwoSides.ONE, Optional.empty()));
        assertEquals("Variable Tieline BE FR - N - preventive_one_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec1 does not exist
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec1, TwoSides.ONE, Optional.empty()));
        assertEquals("Constraint Tieline BE FR - N - preventive_one_flow_constraint has not been created yet", e.getMessage());

        // check flow variable for cnec2
        OpenRaoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2, TwoSides.TWO, Optional.empty());
        assertNotNull(flowVariable2);
        assertEquals(-linearProblem.infinity(), flowVariable2.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable2.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec2
        OpenRaoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2, TwoSides.TWO, Optional.empty());
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * SENSI_CNEC2_IT2, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * SENSI_CNEC2_IT2, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC2_IT2, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check the number of variables and constraints
        // total number of variables 5 :
        //      - 1 per CNEC (flow)
        //      - 4 per range action (set-point, activation and variation up/down)
        // total number of constraints 3 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (activation, set-point variation and iterative relative variation constraint: created before 2nd iteration)
        assertEquals(5, linearProblem.numVariables());
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
        initialize(Set.of(cnec1), 2.5, 2.5, 2.5, crac.getPreventiveState(), false, SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);
        flowConstraint = linearProblem.getFlowConstraint(cnec1, TwoSides.ONE, Optional.empty());
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
        initialize(Set.of(cnec1), 1.5, 1.5, 1.5, crac.getPreventiveState(), false, SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);
        flowConstraint = linearProblem.getFlowConstraint(cnec1, TwoSides.ONE, Optional.empty());
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
        initialize(Set.of(cnec1, cnec2), 1e-6, 1e-6, 1e-6, cnec1.getState(), false, SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);

        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, cnec2.getState());

        // check flow variable for cnec1 does not exist
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec1, TwoSides.ONE, Optional.empty()));
        assertEquals("Variable Tieline BE FR - N - preventive_one_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec1 does not exist
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec1, TwoSides.ONE, Optional.empty()));
        assertEquals("Constraint Tieline BE FR - N - preventive_one_flow_constraint has not been created yet", e.getMessage());

        // check flow variable for cnec2
        OpenRaoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2, TwoSides.TWO, Optional.empty());
        assertNotNull(flowVariable2);
        assertEquals(-linearProblem.infinity(), flowVariable2.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable2.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec2
        OpenRaoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2, TwoSides.TWO, Optional.empty());
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
        initialize(Set.of(cnec1, cnec2), 1e-6, 1e-6, 1e-6, cnec1.getState(), false, SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);

        updateLinearProblem();

        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, cnec2.getState());

        // check flow variable for cnec1 does not exist
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec1, TwoSides.ONE, Optional.empty()));
        assertEquals("Variable Tieline BE FR - N - preventive_one_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec1 does not exist
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec1, TwoSides.ONE, Optional.empty()));
        assertEquals("Constraint Tieline BE FR - N - preventive_one_flow_constraint has not been created yet", e.getMessage());

        // check flow variable for cnec2
        OpenRaoMPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2, TwoSides.TWO, Optional.empty());
        assertNotNull(flowVariable2);
        assertEquals(-linearProblem.infinity(), flowVariable2.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable2.ub(), linearProblem.infinity() * 1e-3);

        // check flow constraint for cnec2
        final double currentAlpha = pstRangeAction.convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());
        OpenRaoMPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2, TwoSides.TWO, Optional.empty());
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
        initialize(Set.of(cnec1, cnec2), 1e-6, 1e-6, 1e-6, cnec1.getState(), false, SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);

        // invert sensitivity failure statuses & update
        // only cnec1's flow variables & constraints must be added to MIP
        when(sensitivityResult.getSensitivityStatus(cnec1.getState())).thenReturn(ComputationStatus.DEFAULT);
        when(sensitivityResult.getSensitivityStatus(cnec2.getState())).thenReturn(ComputationStatus.FAILURE);
        updateLinearProblem();

        // check flow variable for cnec2 does not exist
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec2, TwoSides.TWO, Optional.empty()));
        assertEquals("Variable Tieline BE FR - Defaut - N-1 NL1-NL3_two_flow_variable has not been created yet", e.getMessage());

        // check flow constraint for cnec2 does not exist
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec2, TwoSides.TWO, Optional.empty()));
        assertEquals("Constraint Tieline BE FR - Defaut - N-1 NL1-NL3_two_flow_constraint has not been created yet", e.getMessage());

        // check flow variable for cnec1
        OpenRaoMPVariable flowVariable1 = linearProblem.getFlowVariable(cnec1, TwoSides.ONE, Optional.empty());
        assertNotNull(flowVariable1);
        assertEquals(-linearProblem.infinity(), flowVariable1.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(linearProblem.infinity(), flowVariable1.ub(), linearProblem.infinity() * 1e-3);

        final double currentAlpha = pstRangeAction.convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());
        OpenRaoMPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(pstRangeAction, cnec1.getState());

        // check flow constraint for cnec1
        OpenRaoMPConstraint flowConstraint1 = linearProblem.getFlowConstraint(cnec1, TwoSides.ONE, Optional.empty());
        assertNotNull(flowConstraint1);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint1.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint1.getCoefficient(flowVariable1), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC1_IT2, flowConstraint1.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);
    }
}
