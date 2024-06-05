/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.commons.Unit.MEGAWATT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class UnoptimizedCnecFillerPstLimitationRuleTest extends AbstractFillerTest {
    private static final double MAX_ABS_THRESHOLD = 1000;

    private LinearProblem linearProblem;
    private CoreProblemFiller coreProblemFiller;
    private UnoptimizedCnecFiller unoptimizedCnecFiller;
    private FlowCnec cnecInSeries;
    private FlowCnec classicCnec;
    private PstRangeAction pstRangeActionInSeries;
    private OptimizationPerimeter optimizationPerimeter;
    private Map<FlowCnec, RangeAction<?>> flowCnecRangeActionMap = new HashMap<>();
    private double constraintCoeff;
    private RangeActionsOptimizationParameters rangeActionParameters;

    @BeforeEach
    public void setUp() {
        init();

        // Add a cnec
        crac.newFlowCnec()
            .withId("cnecId")
            .withNetworkElement("neId")
            .newThreshold().withSide(Side.LEFT).withMax(800.0).withMin(-1000.).withUnit(Unit.MEGAWATT).add()
            .withOptimized(true)
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withOperator("NL")
            .add();
        crac.newPstRangeAction().withId("pstRangeActionInSeries")
                .withName("pstRange1Name")
                .withOperator("RTE")
                .withNetworkElement("pst")
                .withInitialTap(1)
                .withTapToAngleConversionMap(Map.of(-3, 0., -2, .5, -1, 1., 0, 1.5, 1, 2., 2, 2.5, 3, 3.))
                .newTapRange().withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK).withMinTap(-3).withMaxTap(3).add()
                .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(PREVENTIVE_INSTANT_ID).add()
                .add();

        // Set initial margins on both preventive CNECs
        cnecInSeries = crac.getFlowCnec("cnecId");
        classicCnec = crac.getFlowCnec("Tieline BE FR - N - preventive");
        pstRangeActionInSeries = crac.getPstRangeAction("pstRangeActionInSeries");

        flowCnecRangeActionMap.put(cnecInSeries, pstRangeActionInSeries);

        RangeActionSetpointResult initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(pstRangeActionInSeries, 0.5));

        optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter.getFlowCnecs()).thenReturn(Set.of(classicCnec, cnecInSeries));
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(Map.of(crac.getPreventiveState(), Set.of(pstRangeActionInSeries)));

        RaoParameters raoParameters = new RaoParameters(ReportNode.NO_OP);
        raoParameters.getRangeActionsOptimizationParameters().setPstPenaltyCost(0.01);
        raoParameters.getRangeActionsOptimizationParameters().setHvdcPenaltyCost(0.01);
        raoParameters.getRangeActionsOptimizationParameters().setInjectionRaPenaltyCost(0.01);
        rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(raoParameters);

        coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionSetpointResult,
            new RangeActionActivationResultImpl(initialRangeActionSetpointResult),
            rangeActionParameters,
            MEGAWATT,
            false);
    }

    private void buildLinearProblemWithMaxMinMarginAndPositiveSensitivityValue(double sensiValue) {
        UnoptimizedCnecParameters unoptimizedCnecParameters = new UnoptimizedCnecParameters(null, flowCnecRangeActionMap);
        MaxMinMarginFiller maxMinMarginFiller = new MaxMinMarginFiller(Set.of(classicCnec, cnecInSeries), Unit.MEGAWATT);
        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getMargin(classicCnec, Unit.MEGAWATT)).thenReturn(600.);
        when(initialFlowResult.getMargin(cnecInSeries, Unit.MEGAWATT)).thenReturn(400.);
        when(sensitivityResult.getSensitivityValue(cnecInSeries, Side.LEFT, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(sensiValue);
        unoptimizedCnecFiller = new UnoptimizedCnecFiller(
                optimizationPerimeter,
                Set.of(classicCnec, cnecInSeries),
                initialFlowResult,
                unoptimizedCnecParameters,
                rangeActionParameters
        );
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(maxMinMarginFiller)
            .withProblemFiller(unoptimizedCnecFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void buildLinearProblemWithMaxMinMarginAndNegativeSensitivityValue() {
        UnoptimizedCnecParameters unoptimizedCnecParameters = new UnoptimizedCnecParameters(null, flowCnecRangeActionMap);
        MaxMinMarginFiller maxMinMarginFiller = new MaxMinMarginFiller(Set.of(classicCnec, cnecInSeries), Unit.MEGAWATT);
        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getMargin(classicCnec, Unit.MEGAWATT)).thenReturn(400.);
        when(initialFlowResult.getMargin(cnecInSeries, Unit.MEGAWATT)).thenReturn(600.);
        when(sensitivityResult.getSensitivityValue(cnecInSeries, Side.LEFT, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(-4.);
        unoptimizedCnecFiller = new UnoptimizedCnecFiller(
                optimizationPerimeter,
                Set.of(classicCnec, cnecInSeries),
                initialFlowResult,
                unoptimizedCnecParameters,
                rangeActionParameters
        );
        linearProblem = new LinearProblemBuilder()
                .withProblemFiller(coreProblemFiller)
                .withProblemFiller(maxMinMarginFiller)
                .withProblemFiller(unoptimizedCnecFiller)
                .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
                .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void buildLinearProblemWithMaxMinRelativeMarginAndPositiveSensi() {
        RelativeMarginsParametersExtension maxMinRelativeMarginParameters = new RelativeMarginsParametersExtension();
        UnoptimizedCnecParameters unoptimizedCnecParameters = new UnoptimizedCnecParameters(null, flowCnecRangeActionMap);
        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getMargin(classicCnec, Unit.MEGAWATT)).thenReturn(400.);
        when(initialFlowResult.getMargin(cnecInSeries, Unit.MEGAWATT)).thenReturn(600.);
        when(initialFlowResult.getPtdfZonalSum(cnecInSeries, Side.LEFT)).thenReturn(0.5);
        when(initialFlowResult.getPtdfZonalSum(classicCnec, Side.LEFT)).thenReturn(2.6);
        when(sensitivityResult.getSensitivityValue(cnecInSeries, Side.LEFT, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(5.);
        MaxMinRelativeMarginFiller maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(
                Set.of(classicCnec, cnecInSeries),
                initialFlowResult,
                Unit.MEGAWATT,
                maxMinRelativeMarginParameters
        );
        double relMarginCoef = Math.max(initialFlowResult.getPtdfZonalSum(classicCnec, Side.LEFT), maxMinRelativeMarginParameters.getPtdfSumLowerBound());
        double unitConversionCoefficient = RaoUtil.getFlowUnitMultiplier(classicCnec, Side.LEFT, MEGAWATT, MEGAWATT);
        constraintCoeff = 5 * RaoUtil.getLargestCnecThreshold(Set.of(cnecInSeries, classicCnec), MEGAWATT) / maxMinRelativeMarginParameters.getPtdfSumLowerBound() * unitConversionCoefficient * relMarginCoef;

        unoptimizedCnecFiller = new UnoptimizedCnecFiller(
                optimizationPerimeter,
                Set.of(cnecInSeries, classicCnec),
                initialFlowResult,
                unoptimizedCnecParameters,
                rangeActionParameters
        );
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(maxMinRelativeMarginFiller)
            .withProblemFiller(unoptimizedCnecFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void buildLinearProblemWithMaxMinRelativeMarginAndNegativeSensi() {
        RelativeMarginsParametersExtension maxMinRelativeMarginParameters = new RelativeMarginsParametersExtension();
        UnoptimizedCnecParameters unoptimizedCnecParameters = new UnoptimizedCnecParameters(null, flowCnecRangeActionMap);
        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getMargin(classicCnec, Unit.MEGAWATT)).thenReturn(400.);
        when(initialFlowResult.getMargin(cnecInSeries, Unit.MEGAWATT)).thenReturn(600.);
        when(initialFlowResult.getPtdfZonalSum(cnecInSeries, Side.LEFT)).thenReturn(0.5);
        when(initialFlowResult.getPtdfZonalSum(classicCnec, Side.LEFT)).thenReturn(2.6);
        when(sensitivityResult.getSensitivityValue(cnecInSeries, Side.LEFT, pstRangeActionInSeries, Unit.MEGAWATT)).thenReturn(-4.);
        MaxMinRelativeMarginFiller maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(
                Set.of(classicCnec, cnecInSeries),
                initialFlowResult,
                Unit.MEGAWATT,
                maxMinRelativeMarginParameters
        );
        double relMarginCoef = Math.max(initialFlowResult.getPtdfZonalSum(classicCnec, Side.LEFT), maxMinRelativeMarginParameters.getPtdfSumLowerBound());
        double unitConversionCoefficient = RaoUtil.getFlowUnitMultiplier(classicCnec, Side.LEFT, MEGAWATT, MEGAWATT);
        constraintCoeff = 5 * RaoUtil.getLargestCnecThreshold(Set.of(cnecInSeries, classicCnec), MEGAWATT) / maxMinRelativeMarginParameters.getPtdfSumLowerBound() * unitConversionCoefficient * relMarginCoef;

        unoptimizedCnecFiller = new UnoptimizedCnecFiller(
                optimizationPerimeter,
                Set.of(cnecInSeries, classicCnec),
                initialFlowResult,
                unoptimizedCnecParameters,
                rangeActionParameters
        );
        linearProblem = new LinearProblemBuilder()
                .withProblemFiller(coreProblemFiller)
                .withProblemFiller(maxMinRelativeMarginFiller)
                .withProblemFiller(unoptimizedCnecFiller)
                .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
                .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    @Test
    void testCnecsNotToOptimizePositiveSensiBinaryVar() {
        buildLinearProblemWithMaxMinMarginAndPositiveSensitivityValue(5.);

        // Verify existence of optimize_cnec binary variable
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getOptimizeCnecBinaryVariable(classicCnec, Side.LEFT));
        assertEquals("Variable Tieline BE FR - N - preventive_left_optimizecnec_variable has not been created yet", e.getMessage());
        OpenRaoMPVariable binaryVar = linearProblem.getOptimizeCnecBinaryVariable(cnecInSeries, Side.LEFT);
        assertNotNull(binaryVar);

        // Get variables
        OpenRaoMPVariable flowVar = linearProblem.getFlowVariable(cnecInSeries, Side.LEFT);
        OpenRaoMPVariable setPointVar = linearProblem.getRangeActionSetpointVariable(pstRangeActionInSeries, crac.getPreventiveState());

        // Verify existence of optimize_cnec definition constraints
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getDontOptimizeCnecConstraint(classicCnec, Side.LEFT, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertEquals("Constraint Tieline BE FR - N - preventive_left_optimizecnecbelow_threshold_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getDontOptimizeCnecConstraint(classicCnec, Side.LEFT, LinearProblem.MarginExtension.ABOVE_THRESHOLD));
        assertEquals("Constraint Tieline BE FR - N - preventive_left_optimizecnecabove_threshold_constraint has not been created yet", e.getMessage());

        OpenRaoMPConstraint optimizeCnecConstraintBelowThreshold = linearProblem.getDontOptimizeCnecConstraint(cnecInSeries, Side.LEFT, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(optimizeCnecConstraintBelowThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintBelowThreshold.ub(), INFINITY_TOLERANCE);
        assertEquals(-5 * 3 - 1000, optimizeCnecConstraintBelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(1., optimizeCnecConstraintBelowThreshold.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(-5., optimizeCnecConstraintBelowThreshold.getCoefficient(setPointVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000., optimizeCnecConstraintBelowThreshold.getCoefficient(binaryVar), DOUBLE_TOLERANCE);

        OpenRaoMPConstraint optimizeCnecConstraintAboveThreshold = linearProblem.getDontOptimizeCnecConstraint(cnecInSeries, Side.LEFT, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(optimizeCnecConstraintAboveThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintAboveThreshold.ub(), INFINITY_TOLERANCE);
        assertEquals(-5 * -0.5 - 800, optimizeCnecConstraintAboveThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1., optimizeCnecConstraintAboveThreshold.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(5., optimizeCnecConstraintAboveThreshold.getCoefficient(setPointVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000., optimizeCnecConstraintAboveThreshold.getCoefficient(binaryVar), DOUBLE_TOLERANCE);
    }

    @Test
    void testCnecsNotToOptimizeNegativeSensiBinaryVar() {
        buildLinearProblemWithMaxMinMarginAndNegativeSensitivityValue();

        // Verify existence of optimize_cnec binary variable
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getOptimizeCnecBinaryVariable(classicCnec, Side.LEFT));
        assertEquals("Variable Tieline BE FR - N - preventive_left_optimizecnec_variable has not been created yet", e.getMessage());
        OpenRaoMPVariable binaryVar = linearProblem.getOptimizeCnecBinaryVariable(cnecInSeries, Side.LEFT);
        assertNotNull(binaryVar);

        // Get variables
        OpenRaoMPVariable flowVar = linearProblem.getFlowVariable(cnecInSeries, Side.LEFT);
        OpenRaoMPVariable setPointVar = linearProblem.getRangeActionSetpointVariable(pstRangeActionInSeries, crac.getPreventiveState());

        // Verify existence of optimize_cnec definition constraints
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getDontOptimizeCnecConstraint(classicCnec, Side.LEFT, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertEquals("Constraint Tieline BE FR - N - preventive_left_optimizecnecbelow_threshold_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getDontOptimizeCnecConstraint(classicCnec, Side.LEFT, LinearProblem.MarginExtension.ABOVE_THRESHOLD));
        assertEquals("Constraint Tieline BE FR - N - preventive_left_optimizecnecabove_threshold_constraint has not been created yet", e.getMessage());

        OpenRaoMPConstraint optimizeCnecConstraintBelowThreshold = linearProblem.getDontOptimizeCnecConstraint(cnecInSeries, Side.LEFT, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(optimizeCnecConstraintBelowThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintBelowThreshold.ub(), INFINITY_TOLERANCE);
        assertEquals(-4 * -0.5 - 1000, optimizeCnecConstraintBelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(1., optimizeCnecConstraintBelowThreshold.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(4., optimizeCnecConstraintBelowThreshold.getCoefficient(setPointVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000., optimizeCnecConstraintBelowThreshold.getCoefficient(binaryVar), DOUBLE_TOLERANCE);

        OpenRaoMPConstraint optimizeCnecConstraintAboveThreshold = linearProblem.getDontOptimizeCnecConstraint(cnecInSeries, Side.LEFT, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(optimizeCnecConstraintAboveThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintAboveThreshold.ub(), INFINITY_TOLERANCE);
        assertEquals(-4 * 3 - 800, optimizeCnecConstraintAboveThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1., optimizeCnecConstraintAboveThreshold.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(-4., optimizeCnecConstraintAboveThreshold.getCoefficient(setPointVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000., optimizeCnecConstraintAboveThreshold.getCoefficient(binaryVar), DOUBLE_TOLERANCE);
    }

    @Test
    void testCnecsNotToOptimizePositiveSensiBinaryVarRelative() {
        buildLinearProblemWithMaxMinRelativeMarginAndPositiveSensi();

        // Verify existence of margin_decrease binary variable
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getOptimizeCnecBinaryVariable(classicCnec, Side.LEFT));
        assertEquals("Variable Tieline BE FR - N - preventive_left_optimizecnec_variable has not been created yet", e.getMessage());
        OpenRaoMPVariable binaryVar = linearProblem.getOptimizeCnecBinaryVariable(cnecInSeries, Side.LEFT);
        assertNotNull(binaryVar);

        // Get variables
        OpenRaoMPVariable flowVar = linearProblem.getFlowVariable(cnecInSeries, Side.LEFT);
        OpenRaoMPVariable setPointVar = linearProblem.getRangeActionSetpointVariable(pstRangeActionInSeries, crac.getPreventiveState());

        // Verify existence of margin_decrease definition constraints
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getDontOptimizeCnecConstraint(classicCnec, Side.LEFT, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertEquals("Constraint Tieline BE FR - N - preventive_left_optimizecnecbelow_threshold_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getDontOptimizeCnecConstraint(classicCnec, Side.LEFT, LinearProblem.MarginExtension.ABOVE_THRESHOLD));
        assertEquals("Constraint Tieline BE FR - N - preventive_left_optimizecnecabove_threshold_constraint has not been created yet", e.getMessage());

        OpenRaoMPConstraint optimizeCnecConstraintBelowThreshold = linearProblem.getDontOptimizeCnecConstraint(cnecInSeries, Side.LEFT, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(optimizeCnecConstraintBelowThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintBelowThreshold.ub(), INFINITY_TOLERANCE);
        assertEquals(5 * -3 - 1000, optimizeCnecConstraintBelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(1., optimizeCnecConstraintBelowThreshold.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(-5., optimizeCnecConstraintBelowThreshold.getCoefficient(setPointVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000., optimizeCnecConstraintBelowThreshold.getCoefficient(binaryVar), DOUBLE_TOLERANCE);

        OpenRaoMPConstraint optimizeCnecConstraintAboveThreshold = linearProblem.getDontOptimizeCnecConstraint(cnecInSeries, Side.LEFT, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(optimizeCnecConstraintAboveThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintAboveThreshold.ub(), INFINITY_TOLERANCE);
        assertEquals(-5 * -0.5 - 800, optimizeCnecConstraintAboveThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1., optimizeCnecConstraintAboveThreshold.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(5., optimizeCnecConstraintAboveThreshold.getCoefficient(setPointVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000., optimizeCnecConstraintAboveThreshold.getCoefficient(binaryVar), DOUBLE_TOLERANCE);
    }

    @Test
    void testCnecsNotToOptimizeNegativeSensiBinaryVarRelative() {
        buildLinearProblemWithMaxMinRelativeMarginAndNegativeSensi();

        // Verify existence of margin_decrease binary variable
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getOptimizeCnecBinaryVariable(classicCnec, Side.LEFT));
        assertEquals("Variable Tieline BE FR - N - preventive_left_optimizecnec_variable has not been created yet", e.getMessage());
        OpenRaoMPVariable binaryVar = linearProblem.getOptimizeCnecBinaryVariable(cnecInSeries, Side.LEFT);
        assertNotNull(binaryVar);

        // Get variables
        OpenRaoMPVariable flowVar = linearProblem.getFlowVariable(cnecInSeries, Side.LEFT);
        OpenRaoMPVariable setPointVar = linearProblem.getRangeActionSetpointVariable(pstRangeActionInSeries, crac.getPreventiveState());

        // Verify existence of margin_decrease definition constraints
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getDontOptimizeCnecConstraint(classicCnec, Side.LEFT, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertEquals("Constraint Tieline BE FR - N - preventive_left_optimizecnecbelow_threshold_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getDontOptimizeCnecConstraint(classicCnec, Side.LEFT, LinearProblem.MarginExtension.ABOVE_THRESHOLD));
        assertEquals("Constraint Tieline BE FR - N - preventive_left_optimizecnecabove_threshold_constraint has not been created yet", e.getMessage());

        OpenRaoMPConstraint optimizeCnecConstraintBelowThreshold = linearProblem.getDontOptimizeCnecConstraint(cnecInSeries, Side.LEFT, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(optimizeCnecConstraintBelowThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintBelowThreshold.ub(), INFINITY_TOLERANCE);
        assertEquals(-4 * -0.5 - 1000, optimizeCnecConstraintBelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(1., optimizeCnecConstraintBelowThreshold.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(4., optimizeCnecConstraintBelowThreshold.getCoefficient(setPointVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000., optimizeCnecConstraintBelowThreshold.getCoefficient(binaryVar), DOUBLE_TOLERANCE);

        OpenRaoMPConstraint optimizeCnecConstraintAboveThreshold = linearProblem.getDontOptimizeCnecConstraint(cnecInSeries, Side.LEFT, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(optimizeCnecConstraintAboveThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintAboveThreshold.ub(), INFINITY_TOLERANCE);
        assertEquals(-4 * 3 - 800, optimizeCnecConstraintAboveThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1., optimizeCnecConstraintAboveThreshold.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(-4., optimizeCnecConstraintAboveThreshold.getCoefficient(setPointVar), DOUBLE_TOLERANCE);
        assertEquals(20 * 1000., optimizeCnecConstraintAboveThreshold.getCoefficient(binaryVar), DOUBLE_TOLERANCE);
    }

    @Test
    void testExcludeCnecsNotToOptimizeInMinMargin() {
        buildLinearProblemWithMaxMinMarginAndPositiveSensitivityValue(5.);

        // Test that classicCnec's constraint does not have a bigM
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(classicCnec, Side.LEFT, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(classicCnec, Side.LEFT, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);

        // Test that cnecInSeries's constraint does have a bigM
        OpenRaoMPVariable optimizeCnecBinaryVariable = linearProblem.getOptimizeCnecBinaryVariable(cnecInSeries, Side.LEFT);
        OpenRaoMPConstraint minMarginDefMin = linearProblem.getMinimumMarginConstraint(cnecInSeries, Side.LEFT, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertEquals(1000 + 2 * MAX_ABS_THRESHOLD, minMarginDefMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * MAX_ABS_THRESHOLD, minMarginDefMin.getCoefficient(optimizeCnecBinaryVariable), DOUBLE_TOLERANCE);
        OpenRaoMPConstraint minMarginDefMax = linearProblem.getMinimumMarginConstraint(cnecInSeries, Side.LEFT, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertEquals(800 + 2 * MAX_ABS_THRESHOLD, minMarginDefMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * MAX_ABS_THRESHOLD, minMarginDefMax.getCoefficient(optimizeCnecBinaryVariable), DOUBLE_TOLERANCE);
    }

    @Test
    void testExcludeCnecsNotToOptimizeInMinMarginRelative() {
        buildLinearProblemWithMaxMinRelativeMarginAndPositiveSensi();

        // Test that classicCnec's constraint does not have a bigM
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(classicCnec, Side.LEFT, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(classicCnec, Side.LEFT, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        // All cnecs now have fmax + maxNegativeRelativeRam * unitConversionCoefficient * relMarginCoef OR - fmin + maxNegativeRelativeRam * unitConversionCoefficient * relMarginCoef as mMRM ub
        assertEquals(750.0 + constraintCoeff, linearProblem.getMinimumRelativeMarginConstraint(classicCnec, Side.LEFT, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0 + constraintCoeff, linearProblem.getMinimumRelativeMarginConstraint(classicCnec, Side.LEFT, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);

        // Test that cnecInSeries's constraint does have a bigM
        OpenRaoMPVariable marginDecreaseVariable = linearProblem.getOptimizeCnecBinaryVariable(cnecInSeries, Side.LEFT);
        OpenRaoMPConstraint minMarginDefMin = linearProblem.getMinimumMarginConstraint(cnecInSeries, Side.LEFT, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertEquals(1000 + 2 * MAX_ABS_THRESHOLD, minMarginDefMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * MAX_ABS_THRESHOLD, minMarginDefMin.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
        OpenRaoMPConstraint minMarginDefMax = linearProblem.getMinimumMarginConstraint(cnecInSeries, Side.LEFT, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertEquals(800 + 2 * MAX_ABS_THRESHOLD, minMarginDefMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * MAX_ABS_THRESHOLD, minMarginDefMax.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
    }

    @Test
    void testCnecsNotToOptimizeSmallSensiBinaryVar() {
        rangeActionParameters.setPstSensitivityThreshold(1e-4);
        buildLinearProblemWithMaxMinMarginAndPositiveSensitivityValue(1e-6);

        // Verify existence of optimize_cnec binary variable
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getOptimizeCnecBinaryVariable(classicCnec, Side.LEFT));
        assertEquals("Variable Tieline BE FR - N - preventive_left_optimizecnec_variable has not been created yet", e.getMessage());
        OpenRaoMPVariable binaryVar = linearProblem.getOptimizeCnecBinaryVariable(cnecInSeries, Side.LEFT);
        assertNotNull(binaryVar);

        // Get variables
        OpenRaoMPVariable flowVar = linearProblem.getFlowVariable(cnecInSeries, Side.LEFT);
        OpenRaoMPVariable setPointVar = linearProblem.getRangeActionSetpointVariable(pstRangeActionInSeries, crac.getPreventiveState());

        // Verify existence of optimize_cnec definition constraints
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getDontOptimizeCnecConstraint(classicCnec, Side.LEFT, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertEquals("Constraint Tieline BE FR - N - preventive_left_optimizecnecbelow_threshold_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getDontOptimizeCnecConstraint(classicCnec, Side.LEFT, LinearProblem.MarginExtension.ABOVE_THRESHOLD));
        assertEquals("Constraint Tieline BE FR - N - preventive_left_optimizecnecabove_threshold_constraint has not been created yet", e.getMessage());

        OpenRaoMPConstraint optimizeCnecConstraintBelowThreshold = linearProblem.getDontOptimizeCnecConstraint(cnecInSeries, Side.LEFT, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(optimizeCnecConstraintBelowThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintBelowThreshold.ub(), INFINITY_TOLERANCE);
        assertEquals(-1000., optimizeCnecConstraintBelowThreshold.lb(), 1e-6);
        assertEquals(1., optimizeCnecConstraintBelowThreshold.getCoefficient(flowVar), 1e-6);
        assertEquals(0., optimizeCnecConstraintBelowThreshold.getCoefficient(setPointVar), 1e-6);
        assertEquals(20 * 1000., optimizeCnecConstraintBelowThreshold.getCoefficient(binaryVar), 1e-6);

        OpenRaoMPConstraint optimizeCnecConstraintAboveThreshold = linearProblem.getDontOptimizeCnecConstraint(cnecInSeries, Side.LEFT, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(optimizeCnecConstraintAboveThreshold);
        assertEquals(LinearProblem.infinity(), optimizeCnecConstraintAboveThreshold.ub(), INFINITY_TOLERANCE);
        assertEquals(-800., optimizeCnecConstraintAboveThreshold.lb(), 1e-6);
        assertEquals(-1., optimizeCnecConstraintAboveThreshold.getCoefficient(flowVar), 1e-6);
        assertEquals(0., optimizeCnecConstraintAboveThreshold.getCoefficient(setPointVar), 1e-6);
        assertEquals(20 * 1000., optimizeCnecConstraintAboveThreshold.getCoefficient(binaryVar), 1e-6);
    }
}
