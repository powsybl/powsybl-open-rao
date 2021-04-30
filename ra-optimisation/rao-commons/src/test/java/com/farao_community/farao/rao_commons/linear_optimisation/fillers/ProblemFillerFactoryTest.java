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
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.SimpleCracFactory;
import com.farao_community.farao.rao_api.parameters.*;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerInput;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ProblemFillerFactoryTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private Crac crac;
    private LinearOptimizerInput input;

    private BranchCnec pureCnec;
    private BranchCnec cnecMnec;
    private BranchCnec pureMnec;
    private BranchCnec loopFlowCnec;

    private Map<BranchCnec, Double> initialAbsolutePtdfSumPerCnec;
    private Map<BranchCnec, Double> initialFlowPerCnec;
    private Map<BranchCnec, Double> initialLoopFlowPerCnec;
    private Map<BranchCnec, Double> prePerimeterCnecMarginsInMW;

    private LinearOptimizerParameters parameters;

    @Before
    public void setUp() {
        crac = CracFactory.findDefault().create("crac");

        pureCnec = crac.newFlowCnec()
            .withId("pure-cnec")
            .withInstant(Instant.PREVENTIVE)
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(2000.).withMin(-500.).withUnit(Unit.MEGAWATT).add()
            .withOptimized(true)
            .withOperator("FR")
            .add();

        cnecMnec = crac.newFlowCnec()
            .withId("cnec-mnec")
            .withInstant(Instant.PREVENTIVE)
            .withNetworkElement("BBE1AA1  BBE3AA1  1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(500.).withMin(-500.).withUnit(Unit.MEGAWATT).add()
            .withOptimized(true)
            .withMonitored(true)
            .withOperator("FR")
            .add();

        pureMnec = crac.newFlowCnec()
            .withId("pure-mnec")
            .withInstant(Instant.PREVENTIVE)
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(500.).withMin(-500.).withUnit(Unit.MEGAWATT).add()
            .withMonitored(true)
            .withOperator("BE")
            .add();

        loopFlowCnec = crac.newFlowCnec()
            .withId("loop-flow-cnec")
            .withInstant(Instant.PREVENTIVE)
            .withNetworkElement("FFR1AA1  FFR2AA1  1")
            .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(500.).withMin(-500.).withUnit(Unit.MEGAWATT).add()
            .withOptimized(true)
            .withOperator("BE")
            .add();

        PstRangeAction ra1 = crac.newPstRangeAction()
            .withId("pst1")
            .withNetworkElement("FFR1AA1  FFR2AA1  2")
            .withInitialTap(0)
            .withTapToAngleConversionMap(Map.of(-1, -20., 0, 0., 1, 20.))
            .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(-5).withMaxTap(5).add()
            .add();

        PstRangeAction ra2 = crac.newPstRangeAction()
            .withId("pst2")
            .withNetworkElement("FFR1AA1  FFR2AA1  2")
            .withInitialTap(0)
            .withTapToAngleConversionMap(Map.of(-1, -20., 0, 0., 1, 20.))
            .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(-5).withMaxTap(5).add()
            .add();

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

        prePerimeterCnecMarginsInMW = new HashMap<>();
        crac.getFlowCnecs().forEach(cnec -> prePerimeterCnecMarginsInMW.put(cnec, 100.));

        input = LinearOptimizerInput.create()
            .withCnecs(crac.getBranchCnecs())
            .withLoopflowCnecs(Set.of(loopFlowCnec))
            .withInitialCnecResults(initialCnecResults)
            .withMostLimitingElements(List.of(cnecMnec, pureCnec, loopFlowCnec))
            .withRangeActions(crac.getRangeActions())
            .withPreperimeterSetpoints(Map.of(ra1, 0., ra2, 0.))
            .withPrePerimeterCnecMarginsInMW(prePerimeterCnecMarginsInMW)
            .build();
    }

    @Test
    public void createCoreProblemFillerWithDefaultParameters() {
        parameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(0.25)
                .build();
        ProblemFiller pf = new ProblemFillerFactory(Mockito.mock(LinearProblem.class), input, parameters).createCoreProblemFiller();

        assertTrue(pf instanceof CoreProblemFiller);
        CoreProblemFiller pfImpl = (CoreProblemFiller) pf;

        // It has all the CNECs
        assertEquals(crac.getFlowCnecs().size(), pfImpl.getCnecs().size());
        // It has all the range actions with their initial set points
        assertEquals(crac.getRangeActions().size(), pfImpl.getPrePerimeterSetPointPerRangeAction().size());
        // Check pst sensitivity threshold value
        assertEquals(0.25, pfImpl.getPstSensitivityThreshold(), DOUBLE_TOLERANCE);
    }

    @Test
    public void createMaxMinMarginFillerInMW() {
        parameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(10))
                .withPstSensitivityThreshold(2.5)
                .build();
        ProblemFiller pf = new ProblemFillerFactory(Mockito.mock(LinearProblem.class), input, parameters).createMaxMinMarginFiller();

        assertTrue(pf instanceof MaxMinMarginFiller);
        MaxMinMarginFiller pfImpl = (MaxMinMarginFiller) pf;

        // It has got only the optimized CNECs
        assertEquals(crac.getBranchCnecs().stream().filter(Cnec::isOptimized).count(), pfImpl.getOptimizedCnecs().size());
        // It has got all the range actions
        assertEquals(crac.getRangeActions().size(), pfImpl.getRangeActions().size());
        // Check parameter values
        assertEquals(Unit.MEGAWATT, pfImpl.getUnit());
        assertEquals(10, pfImpl.getPstPenaltyCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void createMaxMinMarginFillerInA() {
        parameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(2.5)
                .build();
        ProblemFiller pf = new ProblemFillerFactory(Mockito.mock(LinearProblem.class), input, parameters).createMaxMinMarginFiller();

        assertEquals(Unit.AMPERE, ((MaxMinMarginFiller) pf).getUnit());
    }

    @Test
    public void createMaxMinRelativeMarginFiller() {
        parameters = LinearOptimizerParameters.create()
            .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT)
            .withMaxMinRelativeMarginParameters(new MaxMinRelativeMarginParameters(10, 5000, 0.2))
            .withPstSensitivityThreshold(2.5)
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
        // Check parameter values
        assertEquals(Unit.MEGAWATT, pfImpl.getUnit());
        assertEquals(10, pfImpl.getPstPenaltyCost(), DOUBLE_TOLERANCE);
        assertEquals(5000, pfImpl.getNegativeMarginObjectiveCoefficient(), DOUBLE_TOLERANCE);
        assertEquals(0.2, pfImpl.getPtdfSumLowerBound(), DOUBLE_TOLERANCE);
    }

    @Test
    public void createMnecFillerInMW() {
        parameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(2.5)
                .withMnecParameters(new MnecParameters(10, 50, 1))
                .build();

        ProblemFiller pf = new ProblemFillerFactory(Mockito.mock(LinearProblem.class), input, parameters).createMnecFiller();

        assertTrue(pf instanceof MnecFiller);
        MnecFiller pfImpl = (MnecFiller) pf;

        // It has got only the monitored CNECs
        assertEquals(crac.getBranchCnecs().stream().filter(Cnec::isMonitored).count(), pfImpl.getInitialFlowInMWPerMnec().size());
        assertEquals(initialFlowPerCnec.get(cnecMnec), pfImpl.getInitialFlowInMWPerMnec().get(cnecMnec));
        assertEquals(initialFlowPerCnec.get(pureMnec), pfImpl.getInitialFlowInMWPerMnec().get(pureMnec));
        // Default mnec parameter values
        assertEquals(10, pfImpl.getMnecAcceptableMarginDiminution(), DOUBLE_TOLERANCE);
        assertEquals(50, pfImpl.getMnecViolationCost(), DOUBLE_TOLERANCE);
        assertEquals(1, pfImpl.getMnecConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
    }

    @Test
    public void createUnoptimizedCnecFiller() {
        parameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(2.5)
                .withUnoptimizedCnecParameters(new UnoptimizedCnecParameters(Set.of("FR"), 2000))
                .build();
        ProblemFiller pf = new ProblemFillerFactory(Mockito.mock(LinearProblem.class), input, parameters).createUnoptimizedCnecFiller();

        assertTrue(pf instanceof UnoptimizedCnecFiller);
        UnoptimizedCnecFiller pfImpl = (UnoptimizedCnecFiller) pf;

        // It has got only the french optimized CNECs
        assertEquals(crac.getBranchCnecs().stream().filter(cnec -> cnec.getOperator().equals("FR")).count(), pfImpl.getPrePerimeterMarginInMWPerOptimizedCnec().size());
        assertEquals(prePerimeterCnecMarginsInMW.get(pureCnec), pfImpl.getPrePerimeterMarginInMWPerOptimizedCnec().get(pureCnec));
        assertEquals(prePerimeterCnecMarginsInMW.get(pureCnec), pfImpl.getPrePerimeterMarginInMWPerOptimizedCnec().get(cnecMnec));
        // Threshold of the pure-cnec
        assertEquals(2000., pfImpl.getHighestThresholdValue(), DOUBLE_TOLERANCE);
    }

    @Test
    public void createMaxLoopFlowFiller() {
        parameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(2.5)
                .withLoopFlowParameters(new LoopFlowParameters(
                        RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                        50,
                        200,
                        10))
                .build();

        ProblemFiller pf = new ProblemFillerFactory(Mockito.mock(LinearProblem.class), input, parameters).createMaxLoopFlowFiller();

        assertTrue(pf instanceof MaxLoopFlowFiller);
        MaxLoopFlowFiller pfImpl = (MaxLoopFlowFiller) pf;

        // It has got only the french optimized CNECs
        assertEquals(1, pfImpl.getInitialLoopFlowPerLoopFlowCnec().size());
        assertEquals(initialLoopFlowPerCnec.get(loopFlowCnec), pfImpl.getInitialLoopFlowPerLoopFlowCnec().get(loopFlowCnec));
        // Default loop-flow parameter values
        assertEquals(RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF, pfImpl.getLoopFlowApproximationLevel());
        assertEquals(200, pfImpl.getLoopFlowViolationCost(), DOUBLE_TOLERANCE);
        assertEquals(50, pfImpl.getLoopFlowAcceptableAugmentation(), DOUBLE_TOLERANCE);
        assertEquals(10, pfImpl.getLoopFlowConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
    }
}
