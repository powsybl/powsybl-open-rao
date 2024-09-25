/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;

import static com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem.VariationDirectionExtension.DOWNWARD;
import static com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem.VariationDirectionExtension.UPWARD;
import static com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem.VariationReferenceExtension.PREVIOUS_ITERATION;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class LinearProblemTest {

    private static final double LB = -11.1;
    private static final double UB = 22.2;
    private static final double DOUBLE_TOLERANCE = 0.1;

    private static final String CNEC_ID = "cnec_id";
    private static final String RANGE_ACTION_ID = "rangeaction_id";
    private static final String GROUP_ID = "group_id";

    private LinearProblem linearProblem;
    private FlowCnec cnec;
    private State state;
    private PstRangeAction rangeAction;

    @BeforeEach
    public void setUp() {
        linearProblem = new LinearProblemBuilder().withSolver(RangeActionsOptimizationParameters.Solver.SCIP).build();

        rangeAction = Mockito.mock(PstRangeAction.class);
        cnec = Mockito.mock(FlowCnec.class);
        Mockito.when(cnec.getMonitoredSides()).thenReturn(Collections.singleton(TwoSides.ONE));
        state = Mockito.mock(State.class);

        Mockito.when(rangeAction.getId()).thenReturn(RANGE_ACTION_ID);
        Mockito.when(rangeAction.getGroupId()).thenReturn(Optional.of(GROUP_ID));
        Mockito.when(cnec.getId()).thenReturn(CNEC_ID);
    }

    @Test
    void flowVariableTest() {
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowVariable(cnec, TwoSides.ONE));
        assertEquals("Variable cnec_id_one_flow_variable has not been created yet", e.getMessage());
        linearProblem.addFlowVariable(LB, UB, cnec, TwoSides.ONE);
        assertNotNull(linearProblem.getFlowVariable(cnec, TwoSides.ONE));
        assertEquals(LB, linearProblem.getFlowVariable(cnec, TwoSides.ONE).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getFlowVariable(cnec, TwoSides.ONE).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void flowConstraintTest() {
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getFlowConstraint(cnec, TwoSides.ONE));
        assertEquals("Constraint cnec_id_one_flow_constraint has not been created yet", e.getMessage());
        linearProblem.addFlowConstraint(LB, UB, cnec, TwoSides.ONE);
        assertNotNull(linearProblem.getFlowConstraint(cnec, TwoSides.ONE));
        assertEquals(LB, linearProblem.getFlowConstraint(cnec, TwoSides.ONE).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getFlowConstraint(cnec, TwoSides.ONE).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void rangeActionSetPointVariableTest() {
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getRangeActionSetpointVariable(rangeAction, state));
        assertEquals("Variable rangeaction_id_null_setpoint_variable has not been created yet", e.getMessage());
        linearProblem.addRangeActionSetpointVariable(LB, UB, rangeAction, state);
        assertNotNull(linearProblem.getRangeActionSetpointVariable(rangeAction, state));
        assertEquals(LB, linearProblem.getRangeActionSetpointVariable(rangeAction, state).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getRangeActionSetpointVariable(rangeAction, state).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void rangeActionAbsoluteVariationVariableTest() {
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, state));
        assertEquals("Variable rangeaction_id_null_absolutevariation_variable has not been created yet", e.getMessage());
        linearProblem.addAbsoluteRangeActionVariationVariable(LB, UB, rangeAction, state);
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, state));
        assertEquals(LB, linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, state).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, state).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void rangeActionAbsoluteVariationConstraintTest() {
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, state, LinearProblem.AbsExtension.NEGATIVE));
        assertEquals("Constraint rangeaction_id_null_absolutevariationnegative_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, state, LinearProblem.AbsExtension.POSITIVE));
        assertEquals("Constraint rangeaction_id_null_absolutevariationpositive_constraint has not been created yet", e.getMessage());
        linearProblem.addAbsoluteRangeActionVariationConstraint(LB, UB, rangeAction, state, LinearProblem.AbsExtension.NEGATIVE);
        linearProblem.addAbsoluteRangeActionVariationConstraint(LB, UB, rangeAction, state, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, state, LinearProblem.AbsExtension.NEGATIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, state, LinearProblem.AbsExtension.POSITIVE));
        assertEquals(LB, linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, state, LinearProblem.AbsExtension.NEGATIVE).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, state, LinearProblem.AbsExtension.POSITIVE).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void pstTapVariationIntegerAndBinaryVariablesTest() {
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getPstTapVariationVariable(rangeAction, state, UPWARD));
        assertEquals("Variable rangeaction_id_null_tapvariationupward_variable has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getPstTapVariationVariable(rangeAction, state, DOWNWARD));
        assertEquals("Variable rangeaction_id_null_tapvariationdownward_variable has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getPstTapVariationBinary(rangeAction, state, UPWARD));
        assertEquals("Variable rangeaction_id_null_isvariationupward_variable has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getPstTapVariationBinary(rangeAction, state, DOWNWARD));
        assertEquals("Variable rangeaction_id_null_isvariationdownward_variable has not been created yet", e.getMessage());

        linearProblem.addPstTapVariationVariable(LB, UB, rangeAction, state, UPWARD);
        linearProblem.addPstTapVariationVariable(LB, UB, rangeAction, state, DOWNWARD);
        linearProblem.addPstTapVariationBinary(rangeAction, state, UPWARD);
        linearProblem.addPstTapVariationBinary(rangeAction, state, DOWNWARD);

        assertNotNull(linearProblem.getPstTapVariationVariable(rangeAction, state, UPWARD));
        assertNotNull(linearProblem.getPstTapVariationVariable(rangeAction, state, DOWNWARD));
        assertNotNull(linearProblem.getPstTapVariationBinary(rangeAction, state, UPWARD));
        assertNotNull(linearProblem.getPstTapVariationBinary(rangeAction, state, DOWNWARD));
        assertEquals(LB, linearProblem.getPstTapVariationVariable(rangeAction, state, UPWARD).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getPstTapVariationVariable(rangeAction, state, DOWNWARD).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void pstTapConstraintsTest() {
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getIsVariationInDirectionConstraint(rangeAction, state, PREVIOUS_ITERATION, UPWARD));
        assertEquals("Constraint rangeaction_id_null_isvariation_previous_iteration_upward_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getIsVariationInDirectionConstraint(rangeAction, state, PREVIOUS_ITERATION, DOWNWARD));
        assertEquals("Constraint rangeaction_id_null_isvariation_previous_iteration_downward_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getUpOrDownPstVariationConstraint(rangeAction, state));
        assertEquals("Constraint rangeaction_id_null_upordownvariation_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getTapToAngleConversionConstraint(rangeAction, state));
        assertEquals("Constraint rangeaction_id_null_taptoangleconversion_constraint has not been created yet", e.getMessage());

        linearProblem.addIsVariationInDirectionConstraint(-linearProblem.infinity(), 0, rangeAction, state, PREVIOUS_ITERATION, UPWARD);
        linearProblem.addIsVariationInDirectionConstraint(-linearProblem.infinity(), 0, rangeAction, state, PREVIOUS_ITERATION, DOWNWARD);
        linearProblem.addUpOrDownPstVariationConstraint(rangeAction, state);
        linearProblem.addTapToAngleConversionConstraint(LB, UB, rangeAction, state);

        assertNotNull(linearProblem.getIsVariationInDirectionConstraint(rangeAction, state, PREVIOUS_ITERATION, UPWARD));
        assertNotNull(linearProblem.getIsVariationInDirectionConstraint(rangeAction, state, PREVIOUS_ITERATION, DOWNWARD));
        assertNotNull(linearProblem.getUpOrDownPstVariationConstraint(rangeAction, state));
        assertNotNull(linearProblem.getTapToAngleConversionConstraint(rangeAction, state));
    }

    @Test
    void pstGroupVariablesAndConstraintsTest() {
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getPstGroupTapVariable(GROUP_ID, state));
        assertEquals("Variable group_id_null_virtualtap_variable has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getPstGroupTapConstraint(rangeAction, state));
        assertEquals("Constraint rangeaction_id_null_group_id_virtualtap_constraint has not been created yet", e.getMessage());

        linearProblem.addPstGroupTapVariable(LB, UB, GROUP_ID, state);
        linearProblem.addPstGroupTapConstraint(LB, UB, rangeAction, state);

        assertNotNull(linearProblem.getPstGroupTapVariable(GROUP_ID, state));
        assertNotNull(linearProblem.getPstGroupTapConstraint(rangeAction, state));
        assertEquals(LB, linearProblem.getPstGroupTapVariable(GROUP_ID, state).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getPstGroupTapConstraint(rangeAction, state).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void minimumMarginConstraintTest() {
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getMinimumMarginConstraint(cnec, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD));
        assertEquals("Constraint cnec_id_one_minmargin_above_threshold_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getMinimumMarginConstraint(cnec, TwoSides.ONE, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertEquals("Constraint cnec_id_one_minmargin_below_threshold_constraint has not been created yet", e.getMessage());
        linearProblem.addMinimumMarginConstraint(LB, UB, cnec, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        linearProblem.addMinimumMarginConstraint(LB, UB, cnec, TwoSides.ONE, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(linearProblem.getMinimumMarginConstraint(cnec, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD));
        assertNotNull(linearProblem.getMinimumMarginConstraint(cnec, TwoSides.ONE, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertEquals(LB, linearProblem.getMinimumMarginConstraint(cnec, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getMinimumMarginConstraint(cnec, TwoSides.ONE, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void minimumMarginVariableTest() {
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getMinimumMarginVariable());
        assertEquals("Variable minmargin_variable has not been created yet", e.getMessage());
        linearProblem.addMinimumMarginVariable(LB, UB);
        assertNotNull(linearProblem.getMinimumMarginVariable());
        assertEquals(LB, linearProblem.getMinimumMarginVariable().lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getMinimumMarginVariable().ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void minimumRelativeMarginSignBinaryVariableTest() {
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getMinimumRelativeMarginSignBinaryVariable());
        assertEquals("Variable minrelmarginispositive_variable has not been created yet", e.getMessage());
        linearProblem.addMinimumRelativeMarginSignBinaryVariable();
        assertNotNull(linearProblem.getMinimumRelativeMarginSignBinaryVariable());
    }

    @Test
    void minimumRelMarginSignDefinitionConstraintTest() {
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getMinimumRelMarginSignDefinitionConstraint());
        assertEquals("Constraint minrelmarginispositive_constraint has not been created yet", e.getMessage());
        linearProblem.addMinimumRelMarginSignDefinitionConstraint(LB, UB);
        assertNotNull(linearProblem.getMinimumRelMarginSignDefinitionConstraint());
    }

    @Test
    void minimumRelMarginSetToZeroConstraintTest() {
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getMinimumRelMarginSetToZeroConstraint());
        assertEquals("Constraint minrelmargin_constraint has not been created yet", e.getMessage());
        linearProblem.addMinimumRelMarginSetToZeroConstraint(LB, UB);
        assertNotNull(linearProblem.getMinimumRelMarginSetToZeroConstraint());
    }

    @Test
    void maxLoopFlowConstraintTest() {
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxLoopFlowConstraint(cnec, TwoSides.ONE, LinearProblem.BoundExtension.UPPER_BOUND));
        assertEquals("Constraint cnec_id_one_maxloopflow_upper_bound_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxLoopFlowConstraint(cnec, TwoSides.ONE, LinearProblem.BoundExtension.LOWER_BOUND));
        assertEquals("Constraint cnec_id_one_maxloopflow_lower_bound_constraint has not been created yet", e.getMessage());

        linearProblem.addMaxLoopFlowConstraint(LB, UB, cnec, TwoSides.ONE, LinearProblem.BoundExtension.UPPER_BOUND);
        linearProblem.addMaxLoopFlowConstraint(LB, UB, cnec, TwoSides.ONE, LinearProblem.BoundExtension.LOWER_BOUND);

        assertEquals(LB, linearProblem.getMaxLoopFlowConstraint(cnec, TwoSides.ONE, LinearProblem.BoundExtension.UPPER_BOUND).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getMaxLoopFlowConstraint(cnec, TwoSides.ONE, LinearProblem.BoundExtension.UPPER_BOUND).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void objectiveTest() {
        assertNotNull(linearProblem.getObjective());
    }
}
