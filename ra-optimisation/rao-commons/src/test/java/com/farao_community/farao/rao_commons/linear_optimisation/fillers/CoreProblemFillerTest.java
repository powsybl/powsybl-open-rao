/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.result.RangeActionResultImpl;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class CoreProblemFillerTest extends AbstractFillerTest {
    private LinearProblem linearProblem;
    private CoreProblemFiller coreProblemFiller;
    private RangeActionResult initialRangeActionResult;
    // some additional data
    private double minAlpha;
    private double maxAlpha;
    private double initialAlpha;

    @Before
    public void setUp() {
        init();
        // arrange some additional data
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        minAlpha = crac.getRangeAction(RANGE_ACTION_ID).getMinAdmissibleSetpoint(0);
        maxAlpha = crac.getRangeAction(RANGE_ACTION_ID).getMaxAdmissibleSetpoint(0);
        initialAlpha = ((PstRangeAction) rangeAction).convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());

        initialRangeActionResult = new RangeActionResultImpl(Map.of(rangeAction, initialAlpha));
    }

    private void buildLinearProblem() {
        linearProblem = new LinearProblem(List.of(coreProblemFiller), mpSolver);
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void initializeForPreventive(double pstSensitivityThreshold, double hvdcSensitivityThreshold) {
        initialize(cnec1, pstSensitivityThreshold, hvdcSensitivityThreshold, false);
    }

    private void initializeForCurative() {
        initialize(cnec2, 0, 0, false);
    }

    private void initialize(FlowCnec cnec, double pstSensitivityThreshold, double hvdcSensitivityThreshold, boolean relativePositiveMargins) {
        coreProblemFiller = new CoreProblemFiller(
                network,
                Set.of(cnec),
                Set.of(rangeAction),
                initialRangeActionResult,
                pstSensitivityThreshold,
                hvdcSensitivityThreshold,
                relativePositiveMargins
        );
        buildLinearProblem();
    }

    @Test
    public void fillTestOnPreventive() {
        initializeForPreventive(0, 0);

        // check range action setpoint variable
        MPVariable setPointVariable = linearProblem.getRangeActionSetPointVariable(rangeAction);
        assertNotNull(setPointVariable);
        assertEquals(minAlpha, setPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, setPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable
        MPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);
        assertNotNull(absoluteVariationVariable);
        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationVariable.ub(), DOUBLE_TOLERANCE);

        // check flow variable for cnec1
        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1);
        assertNotNull(flowVariable);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec1
        MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * SENSI_CNEC1_IT1, flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(-SENSI_CNEC1_IT1, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2 does not exist
        MPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2);
        assertNull(flowVariable2);

        // check flow constraint for cnec2 does not exist
        MPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2);
        assertNull(flowConstraint2);

        // check absolute variation constraints
        MPConstraint absoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.NEGATIVE);
        MPConstraint absoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(absoluteVariationConstraint1);
        assertNotNull(absoluteVariationConstraint2);
        assertEquals(-initialAlpha, absoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationConstraint1.ub(), DOUBLE_TOLERANCE);
        assertEquals(initialAlpha, absoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationConstraint2.ub(), DOUBLE_TOLERANCE);

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
    public void fillTestOnPreventiveFiltered() {
        initializeForPreventive(2.5, 2.5);

        // check range action setpoint variable
        MPVariable setPointVariable = linearProblem.getRangeActionSetPointVariable(rangeAction);
        assertNotNull(setPointVariable);
        assertEquals(minAlpha, setPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, setPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable
        MPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);
        assertNotNull(absoluteVariationVariable);
        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationVariable.ub(), DOUBLE_TOLERANCE);

        // check flow variable for cnec1
        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1);
        assertNotNull(flowVariable);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec1
        MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * 0, flowConstraint.lb(), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)
        assertEquals(REF_FLOW_CNEC1_IT1 - initialAlpha * 0, flowConstraint.ub(), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(0, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)

        // check flow variable for cnec2 does not exist
        MPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2);
        assertNull(flowVariable2);

        // check flow constraint for cnec2 does not exist
        MPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2);
        assertNull(flowConstraint2);

        // check absolute variation constraints
        MPConstraint absoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.NEGATIVE);
        MPConstraint absoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(absoluteVariationConstraint1);
        assertNotNull(absoluteVariationConstraint2);
        assertEquals(-initialAlpha, absoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationConstraint1.ub(), DOUBLE_TOLERANCE);
        assertEquals(initialAlpha, absoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationConstraint2.ub(), DOUBLE_TOLERANCE);

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
    public void fillTestOnCurative() {
        initializeForCurative();

        // check range action setpoint variable
        MPVariable setPointVariable = linearProblem.getRangeActionSetPointVariable(rangeAction);
        assertNotNull(setPointVariable);
        assertEquals(minAlpha, setPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, setPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable
        MPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);
        assertNotNull(absoluteVariationVariable);
        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationVariable.ub(), DOUBLE_TOLERANCE);

        // check flow variable for cnec1 does not exist
        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1);
        assertNull(flowVariable);

        // check flow constraint for cnec1 does not exist
        MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1);
        assertNull(flowConstraint);

        // check flow variable for cnec2
        MPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2);
        assertNotNull(flowVariable2);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable2.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable2.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec2
        MPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2);
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT1 - initialAlpha * SENSI_CNEC2_IT1, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC2_IT1, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check absolute variation constraints
        MPConstraint absoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.NEGATIVE);
        MPConstraint absoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(absoluteVariationConstraint1);
        assertNotNull(absoluteVariationConstraint2);
        assertEquals(-initialAlpha, absoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationConstraint1.ub(), DOUBLE_TOLERANCE);
        assertEquals(initialAlpha, absoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationConstraint2.ub(), DOUBLE_TOLERANCE);

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

    private void updateLinearProblem() {
        // arrange some additional data
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_IT2);
        initialAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();

        when(flowResult.getFlow(cnec1, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC1_IT2);
        when(flowResult.getFlow(cnec2, Unit.MEGAWATT)).thenReturn(REF_FLOW_CNEC2_IT2);
        when(sensitivityResult.getSensitivityValue(cnec1, rangeAction, Unit.MEGAWATT)).thenReturn(SENSI_CNEC1_IT2);
        when(sensitivityResult.getSensitivityValue(cnec2, rangeAction, Unit.MEGAWATT)).thenReturn(SENSI_CNEC2_IT2);

        // update the problem
        linearProblem.update(flowResult, sensitivityResult);
    }

    @Test
    public void updateTestOnPreventive() {
        initializeForPreventive(0, 0);
        // update the problem with new data
        updateLinearProblem();

        // some additional data
        final double currentAlpha = ((PstRangeAction) rangeAction).convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());

        MPVariable setPointVariable = linearProblem.getRangeActionSetPointVariable(rangeAction);

        // check flow variable for cnec1
        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1);
        assertNotNull(flowVariable);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec1
        MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(-SENSI_CNEC1_IT2, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2 does not exist
        MPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2);
        assertNull(flowVariable2);

        // check flow constraint for cnec2 does not exist
        MPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2);
        assertNull(flowConstraint2);

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
    public void updateTestOnCurative() {
        initializeForCurative();
        // update the problem with new data
        updateLinearProblem();

        // some additional data
        final double currentAlpha = ((PstRangeAction) rangeAction).convertTapToAngle(network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getTapPosition());

        MPVariable setPointVariable = linearProblem.getRangeActionSetPointVariable(rangeAction);

        // check flow variable for cnec1 does not exist
        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1);
        assertNull(flowVariable);

        // check flow constraint for cnec1 does not exist
        MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1);
        assertNull(flowConstraint);

        // check flow variable for cnec2
        MPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2);
        assertNotNull(flowVariable2);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable2.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable2.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec2
        MPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2);
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * SENSI_CNEC2_IT2, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * SENSI_CNEC2_IT2, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC2_IT2, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

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
    public void testFillerWithRangeActionGroup() {
        crac.newPstRangeAction()
                .withId("pst1-group1")
                .withGroupId("group1")
                .withNetworkElement("BBE2AA1  BBE3AA1  1")
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -20., 0, 0., 1, 20.))
                .newTapRange()
                    .withRangeType(RangeType.ABSOLUTE)
                    .withMinTap(-2)
                    .withMaxTap(5)
                    .add()
                .withOperator("RTE")
                .add();
        crac.newPstRangeAction()
                .withId("pst2-group1")
                .withGroupId("group1")
                .withNetworkElement("BBE1AA1  BBE3AA1  1")
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -20., 0, 0., 1, 20.))
                .newTapRange()
                    .withRangeType(RangeType.ABSOLUTE)
                    .withMinTap(-5)
                    .withMaxTap(10)
                    .add()
                .withOperator("RTE")
                .add();

        network = NetworkImportsUtil.import12NodesWith2PstsNetwork();

        RangeAction ra1 = crac.getRangeAction("pst1-group1");
        RangeAction ra2 = crac.getRangeAction("pst2-group1");

        coreProblemFiller = new CoreProblemFiller(
                network,
                Set.of(cnec1),
                Set.of(rangeAction, ra1, ra2),
                new RangeActionResultImpl(Map.of(rangeAction, initialAlpha, ra1, 0., ra2, 0.)),
                0.,
                0.,
                false
        );
        buildLinearProblem();

        // check the number of variables and constraints
        // total number of variables 8 :
        //      - 1 per CNEC (flow)
        //      - 2 per range action (set-point and variation) x 3
        //      - 1 per group
        // total number of constraints 9 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (absolute variation constraints) x 3
        //      - 1 per range action in a group (group constraint) x 2
        assertEquals(8, linearProblem.numVariables());
        assertEquals(9, linearProblem.numConstraints());
    }

    @Test
    public void updateWithoutFillingTest() {
        coreProblemFiller = new CoreProblemFiller(
                network,
                Set.of(cnec1),
                Set.of(rangeAction),
                initialRangeActionResult,
                0.,
                0.,
                false
        );
        linearProblem = new LinearProblem(List.of(coreProblemFiller), mpSolver);
        try {
            updateLinearProblem();
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    public void testRelativeFilter1() {
        MPConstraint flowConstraint;
        MPVariable rangeActionSetpoint;
        when(flowResult.getPtdfZonalSum(cnec1)).thenReturn(0.5);

        // Case 1: margin on cnec1 is negative
        // (sensi = 2) < 2.5 should be filtered
        when(flowResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(-1.0);
        initialize(cnec1, 2.5, 2.5, true);
        flowConstraint = linearProblem.getFlowConstraint(cnec1);
        rangeActionSetpoint = linearProblem.getRangeActionSetPointVariable(rangeAction);
        assertEquals(0, flowConstraint.getCoefficient(rangeActionSetpoint), DOUBLE_TOLERANCE);
        assertEquals(500., flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(500., flowConstraint.ub(), DOUBLE_TOLERANCE);

        // Case 2: margin on cnec1 is positive
        // (relative sensi = 2 / 0.5 = 4) > 2.5 should not be filtered
        when(flowResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(1.0);
        linearProblem.update(flowResult, sensitivityResult);
        assertEquals(-2, flowConstraint.getCoefficient(rangeActionSetpoint), DOUBLE_TOLERANCE);
        assertEquals(500. - 2 * 4, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(500. - 2 * 4, flowConstraint.ub(), DOUBLE_TOLERANCE);

        // Case 3: margin on cnec1 is 0
        // should be filtered like in case 1
        when(flowResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(.0);
        linearProblem.update(flowResult, sensitivityResult);
        assertEquals(0, flowConstraint.getCoefficient(rangeActionSetpoint), DOUBLE_TOLERANCE);
        assertEquals(500., flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(500., flowConstraint.ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testRelativeFilter2() {
        MPConstraint flowConstraint;
        MPVariable rangeActionSetpoint;
        when(flowResult.getPtdfZonalSum(cnec1)).thenReturn(0.5);

        // Case 1: margin on cnec1 is positive
        // (relative sensi = 2 / 0.5 = 4) > 2.5 should not be filtered
        when(flowResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(1.0);
        initialize(cnec1, 2.5, 2.5, true);
        flowConstraint = linearProblem.getFlowConstraint(cnec1);
        rangeActionSetpoint = linearProblem.getRangeActionSetPointVariable(rangeAction);
        assertEquals(-2, flowConstraint.getCoefficient(rangeActionSetpoint), DOUBLE_TOLERANCE);
        assertEquals(500. - 2 * 4, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(500. - 2 * 4, flowConstraint.ub(), DOUBLE_TOLERANCE);

        // Case 2: margin on cnec1 is negative
        // (sensi = 2) < 2.5 should be filtered
        when(flowResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(-1.0);
        linearProblem.update(flowResult, sensitivityResult);
        assertEquals(0, flowConstraint.getCoefficient(rangeActionSetpoint), DOUBLE_TOLERANCE);
        assertEquals(500., flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(500., flowConstraint.ub(), DOUBLE_TOLERANCE);

        // Case 3: margin on cnec1 is 0
        // should be filtered like in case 2
        when(flowResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(.0);
        linearProblem.update(flowResult, sensitivityResult);
        assertEquals(0, flowConstraint.getCoefficient(rangeActionSetpoint), DOUBLE_TOLERANCE);
        assertEquals(500., flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(500., flowConstraint.ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testRelativeFilter3() {
        MPConstraint flowConstraint;
        MPVariable rangeActionSetpoint;
        when(flowResult.getPtdfZonalSum(cnec1)).thenReturn(0.5);

        // Case 1: margin on cnec1 is positive, but relativePositiveMargins is false
        // RA should be filtered
        when(flowResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(1.0);
        initialize(cnec1, 2.5, 2.5, false);
        flowConstraint = linearProblem.getFlowConstraint(cnec1);
        rangeActionSetpoint = linearProblem.getRangeActionSetPointVariable(rangeAction);
        assertEquals(0, flowConstraint.getCoefficient(rangeActionSetpoint), DOUBLE_TOLERANCE);
    }
}
