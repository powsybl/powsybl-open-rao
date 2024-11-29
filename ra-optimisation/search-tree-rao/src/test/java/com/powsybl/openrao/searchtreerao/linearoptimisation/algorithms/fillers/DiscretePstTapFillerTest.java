/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters.PstModel;
import com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters.Solver;
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
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.data.cracapi.usagerule.UsageMethod.AVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class DiscretePstTapFillerTest extends AbstractFillerTest {

    private LinearProblem linearProblem;
    private State preventiveState;
    private State curativeState;
    private DiscretePstTapFiller discretePstTapFiller;
    private double initialAlpha;
    private Map<Integer, Double> tapToAngle;
    private PstRangeAction pra;
    private PstRangeAction cra;

    @BeforeEach
    void setUpAndFill() throws IOException {
        // prepare data
        init();
        preventiveState = crac.getPreventiveState();
        curativeState = crac.getCurativeStates().iterator().next();
        tapToAngle = pstRangeAction.getTapToAngleConversionMap();
        pra = crac.getPstRangeAction("PRA_PST_BE");
        cra = crac.newPstRangeAction()
            .withId("cra")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .newOnContingencyStateUsageRule().withUsageMethod(AVAILABLE).withContingency("N-1 NL1-NL3").withInstant("curative").add()
            .withInitialTap(0)
            .withTapToAngleConversionMap(tapToAngle)
            .newTapRange()
            .withMinTap(-10)
            .withMaxTap(7)
            .withRangeType(RangeType.RELATIVE_TO_PREVIOUS_INSTANT)
            .add()
            .add();
        PstRangeAction pstRangeAction = crac.getPstRangeAction(RANGE_ACTION_ID);
        initialAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();
        RangeActionSetpointResult initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(pstRangeAction, initialAlpha, cra, initialAlpha));
        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);

        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        rangeActions.put(preventiveState, Set.of(pstRangeAction));
        rangeActions.put(curativeState, Set.of(cra));
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);
        Mockito.when(optimizationPerimeter.getMainOptimizationState()).thenReturn(preventiveState);

        CoreProblemFiller coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionSetpointResult,
            (new RaoParameters()).getRangeActionsOptimizationParameters(),
            null,
            Unit.MEGAWATT,
            false, PstModel.APPROXIMATED_INTEGERS);

        Map<State, Set<PstRangeAction>> pstRangeActions = new HashMap<>();
        pstRangeActions.put(preventiveState, Set.of(pstRangeAction));
        pstRangeActions.put(curativeState, Set.of(cra));
        discretePstTapFiller = new DiscretePstTapFiller(
            optimizationPerimeter,
            pstRangeActions,
            initialRangeActionSetpointResult);

        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(discretePstTapFiller)
            .withSolver(Solver.SCIP)
            .withInitialRangeActionActivationResult(getInitialRangeActionActivationResult())
            .build();

        // fill linear problem
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void checkContent(PstRangeAction pst, State state, int initialTap, int minAbsoluteTap, int maxAbsoluteTap, boolean firstIteration) {
        double initialSetpoint = tapToAngle.get(initialTap);

        // check that all constraints and variables exists
        OpenRaoMPVariable setpointV = linearProblem.getRangeActionSetpointVariable(pst, state);
        OpenRaoMPVariable variationUpV = linearProblem.getPstTapVariationVariable(pst, state, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable variationDownV = linearProblem.getPstTapVariationVariable(pst, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable binaryUpV = linearProblem.getPstTapVariationBinary(pst, state, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable binaryDownV = linearProblem.getPstTapVariationBinary(pst, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPConstraint tapToAngleConversionC = linearProblem.getTapToAngleConversionConstraint(pst, state);
        OpenRaoMPConstraint upOrDownC = linearProblem.getUpOrDownPstVariationConstraint(pst, state);
        OpenRaoMPConstraint upVariationC = linearProblem.getIsVariationInDirectionConstraint(pst, state, LinearProblem.VariationReferenceExtension.PREVIOUS_ITERATION, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPConstraint downVariationC = linearProblem.getIsVariationInDirectionConstraint(pst, state, LinearProblem.VariationReferenceExtension.PREVIOUS_ITERATION, LinearProblem.VariationDirectionExtension.DOWNWARD);

        assertNotNull(setpointV);
        assertNotNull(variationUpV);
        assertNotNull(variationDownV);
        assertNotNull(binaryUpV);
        assertNotNull(binaryDownV);
        assertNotNull(tapToAngleConversionC);
        assertNotNull(upOrDownC);
        assertNotNull(upVariationC);
        assertNotNull(downVariationC);

        // check variable bounds
        assertEquals(0, variationUpV.lb(), 1e-6);
        assertEquals(maxAbsoluteTap - minAbsoluteTap, variationUpV.ub(), 1e-6);
        assertEquals(0, variationDownV.lb(), 1e-6);
        assertEquals(maxAbsoluteTap - minAbsoluteTap, variationDownV.ub(), 1e-6);
        assertEquals(0, binaryUpV.lb(), 1e-6);
        assertEquals(1, binaryUpV.ub(), 1e-6);
        assertEquals(0, binaryDownV.lb(), 1e-6);
        assertEquals(1, binaryDownV.ub(), 1e-6);

        // check tap to angle conversion constraints
        assertEquals(initialSetpoint, tapToAngleConversionC.lb(), 1e-6);
        assertEquals(initialSetpoint, tapToAngleConversionC.ub(), 1e-6);
        assertEquals(1, tapToAngleConversionC.getCoefficient(setpointV), 1e-6);
        double coeffUp;
        double coeffDown;
        if (firstIteration) {
            coeffUp = -(tapToAngle.get(maxAbsoluteTap) - tapToAngle.get(initialTap)) / (maxAbsoluteTap - initialTap);
            coeffDown = -(tapToAngle.get(minAbsoluteTap) - tapToAngle.get(initialTap)) / (initialTap - minAbsoluteTap);
        } else {
            coeffUp = -(tapToAngle.get(initialTap + 1) - tapToAngle.get(initialTap));
            coeffDown = -(tapToAngle.get(initialTap - 1) - tapToAngle.get(initialTap));
        }
        assertEquals(coeffUp, tapToAngleConversionC.getCoefficient(variationUpV), 1e-6);
        assertEquals(coeffDown, tapToAngleConversionC.getCoefficient(variationDownV), 1e-6);

        // check other constraints
        assertEquals(1, upOrDownC.ub(), 1e-6);
        assertEquals(1, upOrDownC.getCoefficient(binaryUpV), 1e-6);
        assertEquals(1, upOrDownC.getCoefficient(binaryDownV), 1e-6);

        assertEquals(0, upVariationC.ub(), 1e-6);
        assertEquals(1, upVariationC.getCoefficient(variationUpV), 1e-6);
        assertEquals(-(maxAbsoluteTap - initialTap), upVariationC.getCoefficient(binaryUpV), 1e-6);

        assertEquals(0, downVariationC.ub(), 1e-6);
        assertEquals(1, downVariationC.getCoefficient(variationDownV), 1e-6);
        assertEquals(-(initialTap - minAbsoluteTap), downVariationC.getCoefficient(binaryDownV), 1e-6);
    }

    private void checkPstRelativeTapConstraint(double expectedLb, double expectedUb) {
        OpenRaoMPVariable variationUpV = linearProblem.getPstTapVariationVariable(pstRangeAction, preventiveState, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable variationDownV = linearProblem.getPstTapVariationVariable(pstRangeAction, preventiveState, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPVariable craVariationUpV = linearProblem.getPstTapVariationVariable(cra, curativeState, LinearProblem.VariationDirectionExtension.UPWARD);
        OpenRaoMPVariable craVariationDownV = linearProblem.getPstTapVariationVariable(cra, curativeState, LinearProblem.VariationDirectionExtension.DOWNWARD);
        OpenRaoMPConstraint craRelativeTapC = linearProblem.getPstRelativeTapConstraint(cra, curativeState);

        assertNotNull(craRelativeTapC);

        assertEquals(expectedLb, craRelativeTapC.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(expectedUb, craRelativeTapC.ub(), 1e-6);
        assertEquals(1, craRelativeTapC.getCoefficient(craVariationUpV));
        assertEquals(-1, craRelativeTapC.getCoefficient(craVariationDownV));
        assertEquals(-1, craRelativeTapC.getCoefficient(variationUpV));
        assertEquals(1, craRelativeTapC.getCoefficient(variationDownV));
    }

    @Test
    void testFillAndUpdateMethods() {
        checkContent(pstRangeAction, preventiveState, 0, -15, 15, true);
        checkContent(cra, curativeState, 0, -16, 16, true);
        checkPstRelativeTapConstraint(-10, 7);

        // update linear problem, with a new PST tap equal to -4
        double alphaBeforeUpdate = tapToAngle.get(-4);
        RangeActionActivationResultImpl rangeActionActivationResultBeforeUpdate = new RangeActionActivationResultImpl(new RangeActionSetpointResultImpl(Map.of(this.pstRangeAction, alphaBeforeUpdate, cra, alphaBeforeUpdate)));
        // Update between sensi iterations considering the following optimal result of the 1st iteration:
        // pra optimal tap = -4, cra optimal tap = -6
        rangeActionActivationResultBeforeUpdate.putResult(pra, preventiveState, tapToAngle.get(-4));
        rangeActionActivationResultBeforeUpdate.putResult(cra, curativeState, tapToAngle.get(-6));
        linearProblem.updateBetweenSensiIteration(flowResult, sensitivityResult, rangeActionActivationResultBeforeUpdate);

        checkContent(pra, preventiveState, -4, -15, 15, false);
        checkContent(cra, curativeState, -6, -16, 16, false);
        checkPstRelativeTapConstraint(-8, 9);
    }

    @Test
    void testUpdateBetweenMipIteration() {
        RangeActionActivationResultImpl rangeActionActivationResult =
            new RangeActionActivationResultImpl(new RangeActionSetpointResultImpl(Map.of(pra, 0., cra, 0.)));

        // Update between MIP iterations considering the following optimal result of the 1st iteration:
        // pra optimal tap = 10, cra optimal tap = 15
        rangeActionActivationResult.putResult(pra, preventiveState, tapToAngle.get(10));
        rangeActionActivationResult.putResult(cra, curativeState, tapToAngle.get(15));
        linearProblem.updateBetweenMipIteration(rangeActionActivationResult);

        checkContent(pstRangeAction, preventiveState, 10, -15, 15, false);
        checkContent(cra, curativeState, 15, -16, 16, false);
        checkPstRelativeTapConstraint(-15, 2);
    }
}
