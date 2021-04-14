/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.SimpleCracFactory;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.CnecResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerInput;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ProblemFillerFactoryTest {
    private static final double PST_SENSITIVITY_THRESHOLD = 0.0;
    private static final double PST_PENALTY_COST = 0.01;
    private static final double NEGATIVE_MARGIN_OBJECTIVE_COEFFICIENT = 1000;
    private static final double PTDF_SUM_LOWER_BOUND = 0.01;
    private static final double LOOP_FLOW_ACCEPTABLE_AUGMENTATION = 0.0;
    private static final double LOOP_FLOW_CONSTRAINT_ADJUSTMENT_COEFFICIENT = 0.0;
    private static final double LOOP_FLOW_VIOLATION_COST = 0.0;
    private static final double DOUBLE_TOLERANCE = 0.01;

    private Crac crac;
    private LinearOptimizerInput input;
    private LinearOptimizerParameters parameters;

    private BranchCnec pureCnec;
    private BranchCnec cnecMnec;
    private BranchCnec pureMnec;
    private BranchCnec loopFlowCnec;

    private Map<BranchCnec, Double> initialAbsolutePtdfSumPerCnec;
    private Map<BranchCnec, Double> initialFlowPerCnec;
    private Map<BranchCnec, Double> initialLoopFlowPerCnec;

    @Before
    public void setUp() {
        crac = new SimpleCracFactory().create("crac");

        crac.newBranchCnec()
            .setId("pure-cnec")
            .setInstant(Instant.PREVENTIVE)
            .newNetworkElement().setId("BBE1AA1  BBE2AA1  1").add()
            .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(2000.).setMin(-500.).setUnit(Unit.MEGAWATT).add()
            .optimized()
            .setOperator("FR")
            .add();
        pureCnec = crac.getBranchCnec("pure-cnec");

        crac.newBranchCnec()
            .setId("cnec-mnec")
            .setInstant(Instant.PREVENTIVE)
            .newNetworkElement().setId("BBE1AA1  BBE3AA1  1").add()
            .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(500.).setMin(-500.).setUnit(Unit.MEGAWATT).add()
            .optimized()
            .monitored()
            .setOperator("FR")
            .add();
        cnecMnec = crac.getBranchCnec("cnec-mnec");

        crac.newBranchCnec()
            .setId("pure-mnec")
            .setInstant(Instant.PREVENTIVE)
            .newNetworkElement().setId("BBE2AA1  BBE3AA1  1").add()
            .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(500.).setMin(-500.).setUnit(Unit.MEGAWATT).add()
            .monitored()
            .setOperator("BE")
            .add();
        pureMnec = crac.getBranchCnec("pure-mnec");

        crac.newBranchCnec()
            .setId("loop-flow-cnec")
            .setInstant(Instant.PREVENTIVE)
            .newNetworkElement().setId("FFR1AA1  FFR2AA1  1").add()
            .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(500.).setMin(-500.).setUnit(Unit.MEGAWATT).add()
            .optimized()
            .setOperator("BE")
            .add();
        loopFlowCnec = crac.getBranchCnec("loop-flow-cnec");

        crac.newPstRangeAction()
            .setId("pst1")
            .newNetworkElement().setId("FFR1AA1  FFR2AA1  2").add()
            .setUnit(Unit.TAP)
            .setMinValue(-5.)
            .setMaxValue(5.)
            .add();
        RangeAction ra1 = crac.getRangeAction("pst1");

        crac.newPstRangeAction()
            .setId("pst2")
            .newNetworkElement().setId("FFR1AA1  FFR2AA1  2").add()
            .setUnit(Unit.TAP)
            .setMinValue(-5.)
            .setMaxValue(5.)
            .add();
        RangeAction ra2 = crac.getRangeAction("pst2");

        initialFlowPerCnec = Map.of(
            pureCnec, 200.,
            cnecMnec, -400.,
            pureMnec, 50.,
            loopFlowCnec, 500.
        );

        initialLoopFlowPerCnec =  Map.of(
            pureCnec, 20.,
            cnecMnec, -150.,
            pureMnec, 10.,
            loopFlowCnec, 300.
        );

        initialAbsolutePtdfSumPerCnec =  Map.of(
            pureCnec, 0.13,
            cnecMnec, 0.34,
            pureMnec, 0.02,
            loopFlowCnec, 0.56
        );
        CnecResults initialCnecResults = new CnecResults();
        initialCnecResults.setFlowsInMW(initialFlowPerCnec);
        initialCnecResults.setLoopflowsInMW(initialLoopFlowPerCnec);
        initialCnecResults.setAbsolutePtdfSums(initialAbsolutePtdfSumPerCnec);

        input = LinearOptimizerInput.create()
            .withCnecs(crac.getBranchCnecs())
            .withLoopflowCnecs(Set.of(loopFlowCnec))
            .withInitialCnecResults(initialCnecResults)
            .withMostLimitingElements(List.of(cnecMnec, pureCnec, loopFlowCnec))
            .withRangeActions(crac.getRangeActions())
            .withPreperimeterSetpoints(Map.of(ra1, 0., ra2, 0.))
            .build();

        parameters = LinearOptimizerParameters.create().build();
    }

    @Test
    public void createCoreProblemFillerWithDefaultParameters() {
        ProblemFiller pf = new ProblemFillerFactory(Mockito.mock(LinearProblem.class), input, parameters).createCoreProblemFiller();

        assertTrue(pf instanceof CoreProblemFiller);
        CoreProblemFiller pfImpl = (CoreProblemFiller) pf;

        // It has all the CNECs
        assertEquals(crac.getBranchCnecs().size(), pfImpl.getCnecs().size());
        // It has all the range actions with their initial set points
        assertEquals(crac.getRangeActions().size(), pfImpl.getPrePerimeterSetPointPerRangeAction().size());
        // Default pst sensitivity threshold value
        assertEquals(PST_SENSITIVITY_THRESHOLD, pfImpl.getPstSensitivityThreshold(), DOUBLE_TOLERANCE);
    }

    @Test
    public void createCoreProblemFillerWithCustomParameters() {
        double customPstSensitivityThreshold = 0.05;
        parameters = LinearOptimizerParameters.create()
            .withPstSensitivityThreshold(customPstSensitivityThreshold)
            .build();
        ProblemFiller pf = new ProblemFillerFactory(Mockito.mock(LinearProblem.class), input, parameters).createCoreProblemFiller();

        CoreProblemFiller pfImpl = (CoreProblemFiller) pf;
        // Custom pst sensitivity threshold value
        assertEquals(customPstSensitivityThreshold, pfImpl.getPstSensitivityThreshold(), 0.01);
    }

    @Test
    public void createMaxMinMarginFiller() {
        parameters = LinearOptimizerParameters.create()
            .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE)
            .withPstPenaltyCost(PST_PENALTY_COST)
            .build();
        ProblemFiller pf = new ProblemFillerFactory(Mockito.mock(LinearProblem.class), input, parameters).createMaxMinMarginFiller();

        assertTrue(pf instanceof MaxMinMarginFiller);
        MaxMinMarginFiller pfImpl = (MaxMinMarginFiller) pf;

        // It has got only the optimized CNECs
        assertEquals(crac.getBranchCnecs().stream().filter(Cnec::isOptimized).count(), pfImpl.getOptimizedCnecs().size());
        // It has got all the range actions
        assertEquals(crac.getRangeActions().size(), pfImpl.getRangeActions().size());
        // Default max min margin parameter values
        assertEquals(parameters.getUnit(), pfImpl.getParameters().getUnit());
        assertEquals(PST_PENALTY_COST, pfImpl.getParameters().getPstPenaltyCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void createMaxMinRelativeMarginFiller() {
        parameters = LinearOptimizerParameters.create()
            .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE)
            .withPstPenaltyCost(PST_PENALTY_COST)
            .withNegativeMarginObjectiveCoefficient(NEGATIVE_MARGIN_OBJECTIVE_COEFFICIENT)
            .withPtdfSumLowerBound(PTDF_SUM_LOWER_BOUND)
            .build();
        ProblemFiller pf = new ProblemFillerFactory(Mockito.mock(LinearProblem.class), input, parameters).createMaxMinRelativeMarginFiller();

        assertTrue(pf instanceof MaxMinRelativeMarginFiller);
        MaxMinRelativeMarginFiller pfImpl = (MaxMinRelativeMarginFiller) pf;

        // It has got only the optimized CNECs
        assertEquals(crac.getBranchCnecs().stream().filter(Cnec::isOptimized).count(), pfImpl.getOptimizedCnecs().size());
        assertEquals(crac.getBranchCnecs().stream().filter(Cnec::isOptimized).count(), pfImpl.getInitialAbsolutePtdfSumPerOptimizedCnec().size());
        assertEquals(initialAbsolutePtdfSumPerCnec.get(pureCnec), pfImpl.getInitialAbsolutePtdfSumPerOptimizedCnec().get(pureCnec));
        assertEquals(initialAbsolutePtdfSumPerCnec.get(cnecMnec), pfImpl.getInitialAbsolutePtdfSumPerOptimizedCnec().get(cnecMnec));
        assertEquals(initialAbsolutePtdfSumPerCnec.get(loopFlowCnec), pfImpl.getInitialAbsolutePtdfSumPerOptimizedCnec().get(loopFlowCnec));
        // It has got all the range actions
        assertEquals(crac.getRangeActions().size(), pfImpl.getRangeActions().size());
        // Default max min margin parameter values
        assertEquals(parameters.getUnit(), pfImpl.getParameters().getUnit());
        assertEquals(PST_PENALTY_COST,
            pfImpl.getParameters().getPstPenaltyCost(), DOUBLE_TOLERANCE);
        assertEquals(NEGATIVE_MARGIN_OBJECTIVE_COEFFICIENT,
            pfImpl.getRelativeParameters().getNegativeMarginObjectiveCoefficient(), DOUBLE_TOLERANCE);
        assertEquals(PTDF_SUM_LOWER_BOUND,
            pfImpl.getRelativeParameters().getPtdfSumLowerBound(), DOUBLE_TOLERANCE);
    }

    @Test
    public void createMnecFillerInMW() {
        parameters = LinearOptimizerParameters.create()
            .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)
            .withMnecParameters(new MnecParameters(
                RaoParameters.DEFAULT_MNEC_ACCEPTABLE_MARGIN_DIMINUTION,
                RaoParameters.DEFAULT_MNEC_VIOLATION_COST,
                RaoParameters.DEFAULT_MNEC_CONSTRAINT_ADJUSTMENT_COEFFICIENT))
            .build();
        ProblemFiller pf = new ProblemFillerFactory(Mockito.mock(LinearProblem.class), input, parameters).createMnecFiller();

        assertTrue(pf instanceof MnecFiller);
        MnecFiller pfImpl = (MnecFiller) pf;

        // It has got only the monitored CNECs
        assertEquals(crac.getBranchCnecs().stream().filter(Cnec::isMonitored).count(), pfImpl.getInitialFlowPerMnec().size());
        assertEquals(initialFlowPerCnec.get(cnecMnec), pfImpl.getInitialFlowPerMnec().get(cnecMnec));
        assertEquals(initialFlowPerCnec.get(pureMnec), pfImpl.getInitialFlowPerMnec().get(pureMnec));
        // Default mnec parameter values
        assertEquals(RaoParameters.DEFAULT_MNEC_ACCEPTABLE_MARGIN_DIMINUTION,
            pfImpl.getMnecParameters().getMnecAcceptableMarginDiminution(), DOUBLE_TOLERANCE);
        assertEquals(RaoParameters.DEFAULT_MNEC_VIOLATION_COST,
            pfImpl.getMnecParameters().getMnecViolationCost(), DOUBLE_TOLERANCE);
        assertEquals(RaoParameters.DEFAULT_MNEC_CONSTRAINT_ADJUSTMENT_COEFFICIENT,
            pfImpl.getMnecParameters().getMnecConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
    }

    @Test
    public void createUnoptimizedCnecFiller() {
        parameters = LinearOptimizerParameters.create()
            .withOperatorsNotToOptimize(Set.of("FR"))
            .build();
        ProblemFiller pf = new ProblemFillerFactory(Mockito.mock(LinearProblem.class), input, parameters).createUnoptimizedCnecFiller();

        assertTrue(pf instanceof UnoptimizedCnecFiller);
        UnoptimizedCnecFiller pfImpl = (UnoptimizedCnecFiller) pf;

        // It has got only the french optimized CNECs
        assertEquals(crac.getBranchCnecs().stream().filter(cnec -> cnec.getOperator().equals("FR")).count(), pfImpl.getInitialFlowInMWPerUnoptimizedCnec().size());
        assertEquals(initialFlowPerCnec.get(pureCnec), pfImpl.getInitialFlowInMWPerUnoptimizedCnec().get(pureCnec));
        assertEquals(initialFlowPerCnec.get(cnecMnec), pfImpl.getInitialFlowInMWPerUnoptimizedCnec().get(cnecMnec));
        // Threshold of the pure-cnec
        assertEquals(2000., pfImpl.getHighestThresholdValue(), DOUBLE_TOLERANCE);
    }

    @Test
    public void createMaxLoopFlowFiller() {
        parameters = LinearOptimizerParameters.create()
            .withLoopFlowParameters(new LoopFlowParameters(
                RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                LOOP_FLOW_ACCEPTABLE_AUGMENTATION,
                LOOP_FLOW_VIOLATION_COST,
                LOOP_FLOW_CONSTRAINT_ADJUSTMENT_COEFFICIENT
            ))

            .build();
        ProblemFiller pf = new ProblemFillerFactory(Mockito.mock(LinearProblem.class), input, parameters).createMaxLoopFlowFiller();

        assertTrue(pf instanceof MaxLoopFlowFiller);
        MaxLoopFlowFiller pfImpl = (MaxLoopFlowFiller) pf;

        // It has got only the french optimized CNECs
        assertEquals(1, pfImpl.getInitialLoopFlowPerLoopFlowCnec().size());
        assertEquals(initialLoopFlowPerCnec.get(loopFlowCnec), pfImpl.getInitialLoopFlowPerLoopFlowCnec().get(loopFlowCnec));
        // Default loop-flow parameter values
        assertEquals(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
            pfImpl.getLoopFlowParameters().getLoopFlowApproximationLevel());
        assertEquals(LOOP_FLOW_ACCEPTABLE_AUGMENTATION,
            pfImpl.getLoopFlowParameters().getLoopFlowAcceptableAugmentation(), DOUBLE_TOLERANCE);
        assertEquals(LOOP_FLOW_VIOLATION_COST,
            pfImpl.getLoopFlowParameters().getLoopFlowViolationCost(), DOUBLE_TOLERANCE);
        assertEquals(LOOP_FLOW_CONSTRAINT_ADJUSTMENT_COEFFICIENT,
            pfImpl.getLoopFlowParameters().getLoopFlowConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
    }
}
