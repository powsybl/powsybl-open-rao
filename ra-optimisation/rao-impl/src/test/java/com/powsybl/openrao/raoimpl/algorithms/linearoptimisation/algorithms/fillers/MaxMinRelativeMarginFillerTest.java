/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.PtdfApproximation;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.cnecfiltering.FlowCnecFilter;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.raoimpl.result.api.FlowResult;
import com.powsybl.openrao.raoimpl.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.raoimpl.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.raoimpl.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.Unit.AMPERE;
import static com.powsybl.openrao.commons.Unit.MEGAWATT;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class MaxMinRelativeMarginFillerTest extends AbstractFillerTest {
    private static final double PRECISE_DOUBLE_TOLERANCE = 1e-9;

    private LinearProblem linearProblem;
    private MarginCoreProblemFiller coreProblemFiller;
    private MaxMinRelativeMarginFiller maxMinRelativeMarginFiller;
    private SearchTreeRaoRelativeMarginsParameters parameters;
    private SearchTreeRaoCostlyMinMarginParameters minMarginsParameters;
    private RangeActionSetpointResult initialRangeActionSetpointResult;

    @BeforeEach
    public void setUp() throws IOException {
        init();
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        double initialAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();
        initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(pstRangeAction, initialAlpha));

        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter.getFlowCnecs()).thenReturn(Set.of(cnec1));

        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        rangeActions.put(cnec1.getState(), Set.of(pstRangeAction));
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);

        RaoParameters raoParameters = new RaoParameters(ReportNode.NO_OP);
        raoParameters.getRangeActionsOptimizationParameters().setPstRAMinImpactThreshold(0.01);
        raoParameters.getRangeActionsOptimizationParameters().setHvdcRAMinImpactThreshold(0.01);
        raoParameters.getRangeActionsOptimizationParameters().setInjectionRAMinImpactThreshold(0.01);
        OpenRaoSearchTreeParameters searchTreeParameters = new OpenRaoSearchTreeParameters(ReportNode.NO_OP);
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, searchTreeParameters);
        parameters = new SearchTreeRaoRelativeMarginsParameters();
        searchTreeParameters.setRelativeMarginsParameters(parameters);
        getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters().setDc(true);
        parameters.setPtdfSumLowerBound(0.01);
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN);

        minMarginsParameters = new SearchTreeRaoCostlyMinMarginParameters();
        searchTreeParameters.setMinMarginsParameters(minMarginsParameters);

        coreProblemFiller = new MarginCoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionSetpointResult,
            raoParameters.getRangeActionsOptimizationParameters(),
            null,
            MEGAWATT,
            false,
            SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS,
            null);
    }

    private void createMaxMinRelativeMarginFiller(Unit unit, double cnecInitialAbsolutePtdfSum) {
        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getPtdfZonalSum(cnec1, TwoSides.ONE)).thenReturn(cnecInitialAbsolutePtdfSum);
        maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(
            Set.of(cnec1),
            initialFlowResult,
            unit,
            minMarginsParameters,
            parameters,
            null);
    }

    private void buildLinearProblem() {
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(maxMinRelativeMarginFiller)
            .withSolver(SearchTreeRaoRangeActionsOptimizationParameters.Solver.SCIP)
            .withInitialRangeActionActivationResult(getInitialRangeActionActivationResult())
            .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    @Test
    void fillWithMaxMinRelativeMarginInMegawatt() {
        createMaxMinRelativeMarginFiller(MEGAWATT, 0.9);
        buildLinearProblem();
        checkFillerContentMw(0.9, 0.01);
    }

    @Test
    void fillWithMaxMinRelativeMarginInAmpere() {
        createMaxMinRelativeMarginFiller(AMPERE, 0.005);
        buildLinearProblem();

        OpenRaoMPVariable flowCnec1 = linearProblem.getFlowVariable(cnec1, TwoSides.ONE, Optional.empty());
        OpenRaoMPVariable upwardVariation = linearProblem.getRangeActionVariationVariable(pstRangeAction, cnec1.getState(), LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable downwardVariation = linearProblem.getRangeActionVariationVariable(pstRangeAction, cnec1.getState(), LinearProblem.VariationDirectionExtension.DOWNWARD);

        // check minimum margin variable
        OpenRaoMPVariable minimumMargin = linearProblem.getMinimumMarginVariable(Optional.empty());
        assertNotNull(minimumMargin);
        assertEquals(0.0, minimumMargin.ub(), PRECISE_DOUBLE_TOLERANCE);
        OpenRaoMPVariable minimumRelativeMargin = linearProblem.getMinimumRelativeMarginVariable(Optional.empty());
        assertNotNull(minimumRelativeMargin);

        // check minimum margin constraints
        OpenRaoMPConstraint cnec1AboveThreshold = linearProblem.getMinimumMarginConstraint(cnec1, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD, Optional.empty());
        OpenRaoMPConstraint cnec1BelowThreshold = linearProblem.getMinimumMarginConstraint(cnec1, TwoSides.ONE, LinearProblem.MarginExtension.BELOW_THRESHOLD, Optional.empty());
        OpenRaoMPConstraint cnec1AboveThresholdRelative = linearProblem.getMinimumRelativeMarginConstraint(cnec1, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD, Optional.empty());
        OpenRaoMPConstraint cnec1BelowThresholdRelative = linearProblem.getMinimumRelativeMarginConstraint(cnec1, TwoSides.ONE, LinearProblem.MarginExtension.BELOW_THRESHOLD, Optional.empty());
        assertNotNull(cnec1AboveThreshold);
        assertNotNull(cnec1BelowThreshold);
        assertEquals(-linearProblem.infinity(), cnec1BelowThreshold.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(-MIN_FLOW_1 / (380. * Math.sqrt(3) / 1000.), cnec1BelowThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-linearProblem.infinity(), cnec1AboveThreshold.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(MAX_FLOW_1 / (380. * Math.sqrt(3) / 1000.), cnec1AboveThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-1, cnec1BelowThreshold.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThreshold.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(-1, cnec1BelowThresholdRelative.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThresholdRelative.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(0.01, cnec1AboveThresholdRelative.getCoefficient(minimumRelativeMargin), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(0.01, cnec1BelowThresholdRelative.getCoefficient(minimumRelativeMargin), PRECISE_DOUBLE_TOLERANCE);

        assertEquals(1, cnec1BelowThreshold.getCoefficient(minimumMargin), DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThreshold.getCoefficient(minimumMargin), DOUBLE_TOLERANCE);

        // check objective
        assertEquals(0.01, linearProblem.getObjective().getCoefficient(upwardVariation), DOUBLE_TOLERANCE); // penalty cost
        assertEquals(0.01, linearProblem.getObjective().getCoefficient(downwardVariation), DOUBLE_TOLERANCE); // penalty cost
        assertEquals(-1.0, linearProblem.getObjective().getCoefficient(minimumMargin), DOUBLE_TOLERANCE);
        assertEquals(-1.0, linearProblem.getObjective().getCoefficient(minimumRelativeMargin), DOUBLE_TOLERANCE);
        assertTrue(linearProblem.minimization());

        // check the number of variables and constraints
        assertEquals(7, linearProblem.numVariables());
        assertEquals(8, linearProblem.numConstraints());
    }

    private void createFilteredRelativeFiller(FlowCnecFilter filter) {
        // the core filler only knows cnec2: cnec1 would fail with a missing flow variable if it was not filtered out
        OptimizationPerimeter cnec2Perimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(cnec2Perimeter.getFlowCnecs()).thenReturn(Set.of(cnec2));
        Mockito.when(cnec2Perimeter.getRangeActionsPerState()).thenReturn(Map.of(cnec2.getState(), Set.of(pstRangeAction)));
        RaoParameters raoParameters = new RaoParameters(ReportNode.NO_OP);
        coreProblemFiller = new MarginCoreProblemFiller(
            cnec2Perimeter,
            initialRangeActionSetpointResult,
            raoParameters.getRangeActionsOptimizationParameters(),
            null,
            MEGAWATT,
            false,
            SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS,
            null);

        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getPtdfZonalSum(cnec2, TwoSides.TWO)).thenReturn(0.9);
        maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(Set.of(cnec1, cnec2), initialFlowResult, MEGAWATT, minMarginsParameters, parameters, null, filter);
        buildLinearProblem();
    }

    private static FlowCnecFilter removing(FlowCnec removed) {
        return (cnecs, flowResult, sensitivityResult, linearizationPoint) -> cnecs.stream().filter(c -> !c.equals(removed)).collect(Collectors.toSet());
    }

    @Test
    void filteredCnecHasNoRelativeMarginConstraint() {
        FlowCnecFilter removeCnec1 = removing(cnec1);
        createFilteredRelativeFiller(removeCnec1);

        assertNotNull(linearProblem.getMinimumRelativeMarginConstraint(cnec2, TwoSides.TWO, LinearProblem.MarginExtension.ABOVE_THRESHOLD, Optional.empty()));
        assertThrows(OpenRaoException.class, () -> linearProblem.getMinimumRelativeMarginConstraint(cnec1, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD, Optional.empty()));
        assertThrows(OpenRaoException.class, () -> linearProblem.getMinimumMarginConstraint(cnec1, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD, Optional.empty()));
        assertSame(removeCnec1, maxMinRelativeMarginFiller.getFlowCnecFilter());
    }

    @Test
    void bigMUsesUnfilteredThresholds() {
        // a CNEC with a threshold much larger than cnec1's, removed by the filter
        FlowCnec hugeThresholdCnec = Mockito.mock(FlowCnec.class);
        when(hugeThresholdCnec.getId()).thenReturn("huge-threshold-cnec");
        when(hugeThresholdCnec.isOptimized()).thenReturn(true);
        when(hugeThresholdCnec.getMonitoredSides()).thenReturn(Set.of(TwoSides.ONE));
        when(hugeThresholdCnec.getUpperBound(TwoSides.ONE, MEGAWATT)).thenReturn(Optional.of(5000.));
        when(hugeThresholdCnec.getLowerBound(TwoSides.ONE, MEGAWATT)).thenReturn(Optional.of(-5000.));
        FlowCnecFilter removeHugeThresholdCnec = removing(hugeThresholdCnec);

        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getPtdfZonalSum(cnec1, TwoSides.ONE)).thenReturn(0.9);
        maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(Set.of(cnec1, hugeThresholdCnec), initialFlowResult, MEGAWATT, minMarginsParameters, parameters, null, removeHugeThresholdCnec);
        buildLinearProblem();

        // maxNegativeRam = 5 * highest threshold, maxPositiveRelativeRam = highest threshold / ptdfSumLowerBound, both from the unfiltered set
        OpenRaoMPConstraint signDefinition = linearProblem.getMinimumRelMarginSignDefinitionConstraint(Optional.empty());
        OpenRaoMPVariable signBinary = linearProblem.getMinimumRelativeMarginSignBinaryVariable(Optional.empty());
        assertEquals(5 * 5000., signDefinition.getCoefficient(signBinary), DOUBLE_TOLERANCE);
        OpenRaoMPConstraint setToZero = linearProblem.getMinimumRelMarginSetToZeroConstraint(Optional.empty());
        assertEquals(-5000. / 0.01, setToZero.getCoefficient(signBinary), 1e-3);
    }

    private FlowResult mockFlowResult(double cnecAbsolutePtdfSum) {
        FlowResult mockedFlowResult = Mockito.mock(FlowResult.class);
        when(mockedFlowResult.getPtdfZonalSum(cnec1, TwoSides.ONE)).thenReturn(cnecAbsolutePtdfSum);
        return mockedFlowResult;
    }

    @Test
    void testMustNotUpdatePtdf() {
        createMaxMinRelativeMarginFiller(MEGAWATT, 0.9);
        buildLinearProblem();
        linearProblem.updateBetweenSensiIteration(mockFlowResult(0.6), sensitivityResult, new RangeActionActivationResultImpl(initialRangeActionSetpointResult));
        checkFillerContentMw(0.9, 0.01);
    }

    @Test
    void testMustUpdatePtdf() {
        parameters.setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO_AND_PST);
        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getPtdfZonalSum(cnec1, TwoSides.ONE)).thenReturn(0.9);
        maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(
            Set.of(cnec1),
            initialFlowResult,
            MEGAWATT,
            minMarginsParameters,
            parameters,
            null);
        buildLinearProblem();
        linearProblem.updateBetweenSensiIteration(mockFlowResult(0.6), sensitivityResult, new RangeActionActivationResultImpl(initialRangeActionSetpointResult));
        checkFillerContentMw(0.6, 0.01);
    }

    private void checkFillerContentMw(double expectedPtdfSum, double ptdfSumLowerBound) {
        OpenRaoMPVariable flowCnec1 = linearProblem.getFlowVariable(cnec1, TwoSides.ONE, Optional.empty());
        OpenRaoMPVariable upwardVariation = linearProblem.getRangeActionVariationVariable(pstRangeAction, cnec1.getState(), LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable downwardVariation = linearProblem.getRangeActionVariationVariable(pstRangeAction, cnec1.getState(), LinearProblem.VariationDirectionExtension.DOWNWARD);

        // check minimum margin variable
        OpenRaoMPVariable minimumMargin = linearProblem.getMinimumMarginVariable(Optional.empty());
        assertEquals(0.0, minimumMargin.ub(), PRECISE_DOUBLE_TOLERANCE);
        OpenRaoMPVariable minimumRelativeMargin = linearProblem.getMinimumRelativeMarginVariable(Optional.empty());

        // check minimum margin constraints
        OpenRaoMPConstraint cnec1AboveThreshold = linearProblem.getMinimumMarginConstraint(cnec1, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD, Optional.empty());
        OpenRaoMPConstraint cnec1BelowThreshold = linearProblem.getMinimumMarginConstraint(cnec1, TwoSides.ONE, LinearProblem.MarginExtension.BELOW_THRESHOLD, Optional.empty());
        OpenRaoMPConstraint cnec1AboveThresholdRelative = linearProblem.getMinimumRelativeMarginConstraint(cnec1, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD, Optional.empty());
        OpenRaoMPConstraint cnec1BelowThresholdRelative = linearProblem.getMinimumRelativeMarginConstraint(cnec1, TwoSides.ONE, LinearProblem.MarginExtension.BELOW_THRESHOLD, Optional.empty());
        assertEquals(-linearProblem.infinity(), cnec1BelowThreshold.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(-MIN_FLOW_1, cnec1BelowThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-linearProblem.infinity(), cnec1AboveThreshold.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(MAX_FLOW_1, cnec1AboveThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-1, cnec1BelowThreshold.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThreshold.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(-1, cnec1BelowThresholdRelative.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThresholdRelative.getCoefficient(flowCnec1), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(1 * expectedPtdfSum, cnec1BelowThresholdRelative.getCoefficient(minimumRelativeMargin), DOUBLE_TOLERANCE);
        assertEquals(1 * expectedPtdfSum, cnec1AboveThresholdRelative.getCoefficient(minimumRelativeMargin), DOUBLE_TOLERANCE);

        // check minimum relative margin variable and sign binary variable
        assertEquals(0.0, minimumRelativeMargin.lb(), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(linearProblem.infinity(), minimumRelativeMargin.ub(), linearProblem.infinity() * 1e-3);
        OpenRaoMPVariable minRelMarginSignBinary = linearProblem.getMinimumRelativeMarginSignBinaryVariable(Optional.empty());
        assertNotNull(minRelMarginSignBinary);

        // check relative margin constraint bounds and sign binary variable coefficients
        // (relMarginCoef = max(ptdfSum, ptdfSumLowerBound), maxNegativeRelativeRam = 5 * highestThreshold / ptdfSumLowerBound)
        double relMarginCoef = Math.max(expectedPtdfSum, ptdfSumLowerBound);
        double maxNegativeRelativeRam = 5 * MAX_FLOW_1 / ptdfSumLowerBound;
        assertEquals(-linearProblem.infinity(), cnec1BelowThresholdRelative.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(-MIN_FLOW_1 + relMarginCoef * maxNegativeRelativeRam, cnec1BelowThresholdRelative.ub(), 1e-3);
        assertEquals(-linearProblem.infinity(), cnec1AboveThresholdRelative.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(MAX_FLOW_1 + relMarginCoef * maxNegativeRelativeRam, cnec1AboveThresholdRelative.ub(), 1e-3);
        assertEquals(relMarginCoef * maxNegativeRelativeRam, cnec1BelowThresholdRelative.getCoefficient(minRelMarginSignBinary), 1e-3);
        assertEquals(relMarginCoef * maxNegativeRelativeRam, cnec1AboveThresholdRelative.getCoefficient(minRelMarginSignBinary), 1e-3);

        // check minimum rel margin sign definition constraint (minRelMarginSignBinary * 5 * highestThreshold - minimumMargin <= 5 * highestThreshold)
        OpenRaoMPConstraint signDefinition = linearProblem.getMinimumRelMarginSignDefinitionConstraint(Optional.empty());
        assertEquals(5 * MAX_FLOW_1, signDefinition.ub(), DOUBLE_TOLERANCE);
        assertEquals(5 * MAX_FLOW_1, signDefinition.getCoefficient(minRelMarginSignBinary), DOUBLE_TOLERANCE);
        assertEquals(-1, signDefinition.getCoefficient(minimumMargin), PRECISE_DOUBLE_TOLERANCE);

        // check minimum rel margin set-to-zero constraint (minimumRelativeMargin - highestThreshold / ptdfSumLowerBound * minRelMarginSignBinary <= 0)
        OpenRaoMPConstraint setToZero = linearProblem.getMinimumRelMarginSetToZeroConstraint(Optional.empty());
        assertEquals(0.0, setToZero.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, setToZero.getCoefficient(minimumRelativeMargin), PRECISE_DOUBLE_TOLERANCE);
        assertEquals(-MAX_FLOW_1 / ptdfSumLowerBound, setToZero.getCoefficient(minRelMarginSignBinary), 1e-3);

        // check objective
        assertEquals(0.01, linearProblem.getObjective().getCoefficient(upwardVariation), DOUBLE_TOLERANCE);
        assertEquals(0.01, linearProblem.getObjective().getCoefficient(downwardVariation), DOUBLE_TOLERANCE);
        assertEquals(-1.0, linearProblem.getObjective().getCoefficient(minimumMargin), DOUBLE_TOLERANCE);
        assertEquals(-1.0, linearProblem.getObjective().getCoefficient(minimumRelativeMargin), DOUBLE_TOLERANCE);
        assertTrue(linearProblem.minimization());

        // check the number of variables and constraints
        assertEquals(7, linearProblem.numVariables());
        assertEquals(8, linearProblem.numConstraints());
    }

    @Test
    void testPtdfIsNaN() {
        parameters.setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO_AND_PST);
        parameters.setPtdfSumLowerBound(0.1234);
        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getPtdfZonalSum(cnec1, TwoSides.ONE)).thenReturn(Double.NaN);
        maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(
            Set.of(cnec1),
            initialFlowResult,
            MEGAWATT,
            minMarginsParameters,
            parameters,
            null);
        buildLinearProblem();
        checkFillerContentMw(0.1234, 0.1234);
        linearProblem.updateBetweenSensiIteration(mockFlowResult(Double.NaN), sensitivityResult, new RangeActionActivationResultImpl(initialRangeActionSetpointResult));
        checkFillerContentMw(0.1234, 0.1234);
    }
}
