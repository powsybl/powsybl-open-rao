/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class HybridPstProblemFillerTest extends AbstractFillerTest {

    @Test
    void continuousAndDiscretePstModelsCoexistAcrossMipIterations() throws IOException {
        init();
        addPstGroupInCrac();
        State state = crac.getPreventiveState();
        PstRangeAction pstRa1 = crac.getPstRangeAction("pst1-group1");
        PstRangeAction pstRa2 = crac.getPstRangeAction("pst2-group1");
        RangeAction<?> nonPstRangeAction = crac.newInjectionRangeAction()
            .withId("non-pst")
            .withGroupId("non-pst-group")
            .withNetworkElementAndKey(1.0, "BBE1AA1 _generator")
            .withNetworkElementAndKey(-1.0, "FFR1AA1 _load")
            .newRange().withMin(-100.0).withMax(100.0).add()
            .add();
        Map<State, Set<PstRangeAction>> pstRangeActions = Map.of(state, Set.of(pstRa1, pstRa2));
        Map<State, Set<RangeAction<?>>> continuousPstRangeActions = Map.of(state, Set.of(pstRa1, pstRa2));
        Map<State, Set<RangeAction<?>>> nonPstRangeActions = Map.of(state, Set.of(nonPstRangeAction));
        OptimizationPerimeter optimizationPerimeter = mock(OptimizationPerimeter.class);
        when(optimizationPerimeter.getMainOptimizationState()).thenReturn(state);
        when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(Map.of(state, Set.of(pstRa1, pstRa2, nonPstRangeAction)));
        RangeActionSetpointResult initialSetpoints = new RangeActionSetpointResultImpl(Map.of(
            pstRa1, pstRa1.convertTapToAngle(0), pstRa2, pstRa2.convertTapToAngle(0), nonPstRangeAction, 0.0));
        RangeActionActivationResultImpl activationResult = new RangeActionActivationResultImpl(initialSetpoints);
        RangeActionsOptimizationParameters parameters = new RaoParameters(ReportNode.NO_OP).getRangeActionsOptimizationParameters();

        // Create filler
        MarginCoreProblemFiller coreFiller = new MarginCoreProblemFiller(
            optimizationPerimeter, initialSetpoints, parameters, null, Unit.MEGAWATT, false,
            SearchTreeRaoRangeActionsOptimizationParameters.PstModel.HYBRID, null);
        ContinuousRangeActionGroupFiller continuousFiller = new ContinuousRangeActionGroupFiller(continuousPstRangeActions);
        DiscretePstTapFiller discreteFiller = new DiscretePstTapFiller(
            optimizationPerimeter, pstRangeActions, initialSetpoints, parameters, false, false);
        DiscretePstGroupFiller discreteGroupFiller = new DiscretePstGroupFiller(state, pstRangeActions);
        HybridPstProblemFiller hybridFiller = new HybridPstProblemFiller(continuousFiller, discreteFiller, discreteGroupFiller);

        // create linear problem
        LinearProblem linearProblem = LinearProblem.create()
            .withSolver(SearchTreeRaoRangeActionsOptimizationParameters.Solver.SCIP)
            .withInitialRangeActionActivationResult(activationResult)
            .withProblemFiller(coreFiller)
            .withProblemFiller(hybridFiller)
            .withProblemFiller(new ContinuousRangeActionGroupFiller(nonPstRangeActions))
            .build();

        linearProblem.fill(flowResult, sensitivityResult);

        // The initial 11 variables and 7 constraints cover the core model and continuous range-action
        // range action setpoint variable * 3 (on per range action), group virtual setpoint * 2, variation variable * 2 (UP and DOWN) * 3 (one per range action) = 11 variables
        // constraints:
        // - injection balance * 1
        // - group constraint  * 3
        // - range action variation constraint * 3
        // group setpoints; the discrete PST tap variables have not been added yet.
        assertEquals(11, linearProblem.numVariables());
        assertEquals(7, linearProblem.numConstraints());

        // Keep references to continuous setpoints to verify they coexist with the discrete PST model.
        OpenRaoMPVariable groupSetpoint = linearProblem.getRangeActionGroupSetpointVariable("group1", state);
        OpenRaoMPVariable nonPstSetpoint = linearProblem.getRangeActionSetpointVariable(nonPstRangeAction, state);

        // Before the first MIP update, neither an individual PST tap nor a PST group tap is present.
        assertThrows(OpenRaoException.class, () -> linearProblem.getTapVariable(pstRa1, state));
        assertThrows(OpenRaoException.class, () -> linearProblem.getPstGroupTapVariable("group1", state));

        // Each update fixes the activation result to a new tap. The first update adds the discrete model;
        // the second updates its tap constraints without adding duplicate variables or constraints.
        for (int tap = 1; tap <= 2; tap++) {
            activationResult.putResult(pstRa1, state, pstRa1.convertTapToAngle(tap));
            activationResult.putResult(pstRa2, state, pstRa2.convertTapToAngle(tap));
            linearProblem.updateBetweenMipIteration(activationResult);

            // The discrete PST fillers add 11 variables and 12 constraints while preserving the original
            // continuous model. Unchanged totals on the second iteration verify that none are duplicated.
            // variable added :
            // - group tap virtual setpoint * 1
            // - tap variation * 2 (up and down) * 2 (pst1 and pst2)
            // - is variation variable * 2 (up and down) * 2 (pst1 and pst2)
            // - tap variable * 2 (pst1 and pst2)
            // constraint added :
            // - virtual tap constraint * 2 (pst1 and pst2)
            // - tap to angle constraint * 2 (pst1 and pst2)
            // - is variation previous iteration * 2 (up and down) * 2 (pst1 and pst2)
            // - up or down variation constraint * 2 (pst1 and pst2)
            // - tap constraint * 2 (pst1 and pst2)
            // variable & constraint removed: PST group setpoint variable (1) and constraint (2)
            assertEquals(21, linearProblem.numVariables());
            assertEquals(17, linearProblem.numConstraints());
            OpenRaoMPVariable groupTap = linearProblem.getPstGroupTapVariable("group1", state);
            for (PstRangeAction pst : pstRangeActions.get(state)) {
                // setpoint variable remains but not group setpoint variable and constraint
                OpenRaoMPVariable setpoint = linearProblem.getRangeActionSetpointVariable(pst, state);
                assertThrows(OpenRaoException.class, () -> linearProblem.getRangeActionGroupSetpointConstraint(pst, state));
                assertThrows(OpenRaoException.class, () -> linearProblem.getRangeActionGroupSetpointVariable("group1", state));

                // Each PST retains both directional variation binaries and the constraint linking their choices.
                assertNotNull(linearProblem.getPstTapVariationBinary(pst, state, LinearProblem.VariationDirectionExtension.UPWARD));
                assertNotNull(linearProblem.getPstTapVariationBinary(pst, state, LinearProblem.VariationDirectionExtension.DOWNWARD));
                assertNotNull(linearProblem.getUpOrDownPstVariationConstraint(pst, state));

                // The PST group-tap constraint includes the shared group-tap variable with coefficient +1.
                assertEquals(1, linearProblem.getPstGroupTapConstraint(pst, state).getCoefficient(groupTap), DOUBLE_TOLERANCE);

                // The tap constraint bound is updated
                assertEquals(tap, linearProblem.getTapConstraint(pst, state).lb(), DOUBLE_TOLERANCE);
                assertEquals(tap, linearProblem.getTapConstraint(pst, state).ub(), DOUBLE_TOLERANCE);

                // The tap to angle constraint bound is updated
                OpenRaoMPConstraint tapToAngleConversionConstraint = linearProblem.getTapToAngleConversionConstraint(pst, state);
                assertEquals(pst.convertTapToAngle(tap), tapToAngleConversionConstraint.lb(), DOUBLE_TOLERANCE);
                assertEquals(pst.convertTapToAngle(tap), tapToAngleConversionConstraint.ub(), DOUBLE_TOLERANCE);
                OpenRaoMPVariable pstTapUpwardVariationVariable = linearProblem.getPstTapVariationVariable(pst, state, LinearProblem.VariationDirectionExtension.UPWARD);
                OpenRaoMPVariable pstTapDownwardVariationVariable = linearProblem.getPstTapVariationVariable(pst, state, LinearProblem.VariationDirectionExtension.DOWNWARD);
                assertEquals(-pst.convertTapToAngle(tap + 1) + pst.convertTapToAngle(tap), tapToAngleConversionConstraint.getCoefficient(pstTapUpwardVariationVariable), DOUBLE_TOLERANCE);
                assertEquals(pst.convertTapToAngle(tap) - pst.convertTapToAngle(tap - 1), tapToAngleConversionConstraint.getCoefficient(pstTapDownwardVariationVariable), DOUBLE_TOLERANCE);

            }
            // Non-PST group modeling and the core injection-balance constraint remain after the PST transition.
            assertEquals(1, linearProblem.getRangeActionGroupSetpointConstraint(nonPstRangeAction, state).getCoefficient(nonPstSetpoint), DOUBLE_TOLERANCE);
            assertNotNull(linearProblem.getRangeActionGroupSetpointVariable("non-pst-group", state));
            assertNotNull(linearProblem.getInjectionBalanceConstraint(state));
        }
    }

    // Verify that the first MIP update builds the discrete model using the latest flow and sensitivity
    // inputs and the continuous solution, and that subsequent updates reuse it without rebuilding.
    @Test
    void fillsDiscreteModelOnceWithLatestInputsAndUpdatesItBetweenMipIterations() {
        // create hybrid filler
        LinearProblem linearProblem = mock(LinearProblem.class);
        ContinuousRangeActionGroupFiller continuousFiller = mock(ContinuousRangeActionGroupFiller.class);
        DiscretePstTapFiller discreteFiller = mock(DiscretePstTapFiller.class);
        DiscretePstGroupFiller discreteGroupFiller = mock(DiscretePstGroupFiller.class);
        HybridPstProblemFiller hybridFiller = new HybridPstProblemFiller(continuousFiller, discreteFiller, discreteGroupFiller);

        // First call to hybridFiller.fill -> fill the problem using continuousGroupFiller, no interaction with discrete fillers
        FlowResult firstFlowResult = mock(FlowResult.class);
        SensitivityResult firstSensitivityResult = mock(SensitivityResult.class);
        RangeActionActivationResult initialActivation = mock(RangeActionActivationResult.class);
        hybridFiller.fill(linearProblem, firstFlowResult, firstSensitivityResult, initialActivation);
        // The discrete and discrete group fillers are not called.
        verifyNoInteractions(discreteFiller, discreteGroupFiller);
        // check that hybridFiller.fill(...) forwarded both calls to continuousFiller.fill(...)
        verify(continuousFiller).fill(linearProblem, firstFlowResult, firstSensitivityResult, initialActivation);

        // First call to updateBetweenMipIteration, fill discrete fillers + update the bounds with continuous solution
        RangeActionActivationResult firstContinuousSolution = mock(RangeActionActivationResult.class);
        hybridFiller.updateBetweenMipIteration(linearProblem, firstContinuousSolution);
        verify(discreteFiller).fill(linearProblem, firstFlowResult, firstSensitivityResult, firstContinuousSolution);
        verify(discreteGroupFiller).fill(linearProblem, firstFlowResult, firstSensitivityResult, firstContinuousSolution);
        verify(discreteFiller).updateBetweenMipIteration(linearProblem, firstContinuousSolution);
        verify(discreteGroupFiller).updateBetweenMipIteration(linearProblem, firstContinuousSolution);
        verify(continuousFiller).remove(linearProblem);
        verifyNoMoreInteractions(continuousFiller, discreteFiller, discreteGroupFiller);

        // Second call to fill, forward the call to discrete fillers
        FlowResult updatedFlowResult = mock(FlowResult.class);
        SensitivityResult updatedSensitivityResult = mock(SensitivityResult.class);
        RangeActionActivationResult updatedActivation = mock(RangeActionActivationResult.class);
        hybridFiller.fill(linearProblem, updatedFlowResult, updatedSensitivityResult, updatedActivation);
        // fill using discrete filler and not continuous filler
        verify(discreteFiller).fill(linearProblem, updatedFlowResult, updatedSensitivityResult, updatedActivation);
        verify(discreteGroupFiller).fill(linearProblem, updatedFlowResult, updatedSensitivityResult, updatedActivation);
        // Initial fill and removal were verified above; no further continuous calls are allowed.
        verifyNoMoreInteractions(continuousFiller);

        // Second call to updateBetweenMipIteration, forward the call to discrete fillers
        RangeActionActivationResult secondContinuousSolution = mock(RangeActionActivationResult.class);
        hybridFiller.updateBetweenMipIteration(linearProblem, secondContinuousSolution);
        verify(discreteFiller).updateBetweenMipIteration(linearProblem, secondContinuousSolution);
        verify(discreteGroupFiller).updateBetweenMipIteration(linearProblem, secondContinuousSolution);
        verifyNoMoreInteractions(continuousFiller, discreteFiller, discreteGroupFiller);
    }
}
