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
import com.farao_community.farao.rao_api.parameters.*;
import com.farao_community.farao.rao_commons.CnecResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerInput;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.*;
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

        prePerimeterCnecMarginsInMW = new HashMap<>();
        crac.getBranchCnecs().forEach(cnec -> prePerimeterCnecMarginsInMW.put(cnec, 100.));

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
        assertEquals(crac.getBranchCnecs().size(), pfImpl.getCnecs().size());
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
