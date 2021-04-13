/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_util.CracCleaner;
import com.farao_community.farao.rao_commons.RaoInputHelper;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.MnecParameters;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;


    private void setUpWithTwoPsts() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        crac = CommonCracCreation.create();
        RangeAction rangeAction1 = new PstRangeActionImpl("PST_FR_1", "PST_FR_1", "FR", new NetworkElement("FFR1AA1  FFR2AA1  2"));
        rangeAction1.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState()));
        ((SimpleCrac) crac).addRangeAction(rangeAction1);
        RangeAction rangeAction2 = new PstRangeActionImpl("PST_FR_2", "PST_FR_2", "FR", new NetworkElement("BBE1AA1  BBE3AA1  2"));
        rangeAction2.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState()));
        ((SimpleCrac) crac).addRangeAction(rangeAction2);
        crac.synchronize(network);
        BranchCnec cnec1 = crac.getBranchCnec("cnec1basecase");
        BranchCnec cnec2 = crac.getBranchCnec("cnec2basecase");
        SystematicSensitivityResult systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(rangeAction1, cnec1)).thenReturn(-30.0);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(rangeAction2, cnec1)).thenReturn(-25.0);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(rangeAction1, cnec2)).thenReturn(10.0);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(rangeAction2, cnec2)).thenReturn(-40.0);
        RaoData raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, new RaoParameters());
        raoData.getCracResultManager().fillRangeActionResultsWithNetworkValues();
        raoData.setSystematicSensitivityResult(systematicSensitivityResult);
        raoData.getCrac().getExtension(ResultVariantManager.class).setInitialVariantId(raoData.getWorkingVariantId());
        raoData.getCrac().getExtension(ResultVariantManager.class).setPrePerimeterVariantId(raoData.getWorkingVariantId());
        PowerMockito.mockStatic(RaoUtil.class);
        PowerMockito.when(RaoUtil.getMostLimitingElement(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyBoolean())).thenAnswer(invocationOnMock -> cnec2);
    }
    @Test
    public void testCompareAbsoluteSensitivities() {
        setUpWithTwoPsts();
        assertEquals(1, CoreProblemFiller.compareAbsoluteSensitivities(rangeAction1, rangeAction2, cnec1, raoData));
        assertEquals(-1, CoreProblemFiller.compareAbsoluteSensitivities(rangeAction2, rangeAction1, cnec1, raoData));
        assertEquals(0, CoreProblemFiller.compareAbsoluteSensitivities(rangeAction1, rangeAction1, cnec1, raoData));
        assertEquals(0, CoreProblemFiller.compareAbsoluteSensitivities(rangeAction2, rangeAction2, cnec1, raoData));
        assertEquals(-1, CoreProblemFiller.compareAbsoluteSensitivities(rangeAction1, rangeAction2, cnec2, raoData));
        assertEquals(1, CoreProblemFiller.compareAbsoluteSensitivities(rangeAction2, rangeAction1, cnec2, raoData));
        assertEquals(0, CoreProblemFiller.compareAbsoluteSensitivities(rangeAction1, rangeAction1, cnec2, raoData));
        assertEquals(0, CoreProblemFiller.compareAbsoluteSensitivities(rangeAction2, rangeAction2, cnec2, raoData));
    }
    @Test
    public void testFilterTwoPsts() {
        setUpWithTwoPsts();
        // Both PSTs should be filtered out
        coreProblemFiller = new CoreProblemFiller(0, Map.of("FR", 0));
        coreProblemFiller.fill(raoData, linearProblem);
        assertNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction1));
        assertNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction2));
        assertNull(linearProblem.getRangeActionSetPointVariable(rangeAction1));
        assertNull(linearProblem.getRangeActionSetPointVariable(rangeAction2));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.POSITIVE));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.NEGATIVE));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.POSITIVE));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.NEGATIVE));
    }
    @Test
    public void testFilterPst1() {
        setUpWithTwoPsts();
        // One PST can be used, cnec2 is most limiting, rangeAction2 has a larger sensitivity on cnec2
        // Thus rangeAction2 can be used, rangeAction1 should be filtered out
        coreProblemFiller = new CoreProblemFiller(0, Map.of("FR", 1));
        coreProblemFiller.fill(raoData, linearProblem);
        assertNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction1));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction2));
        assertNull(linearProblem.getRangeActionSetPointVariable(rangeAction1));
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction2));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.POSITIVE));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.NEGATIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.POSITIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.NEGATIVE));
    }
    @Test
    public void testFilterPst2() {
        setUpWithTwoPsts();
        PowerMockito.when(RaoUtil.getMostLimitingElement(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyBoolean())).thenAnswer(invocationOnMock -> cnec1);
        // One PST can be used, cnec1 is most limiting, rangeAction1 has a larger sensitivity on cnec1
        // Thus rangeAction1 can be used, rangeAction2 should be filtered out
        coreProblemFiller = new CoreProblemFiller(0, Map.of("FR", 1));
        coreProblemFiller.fill(raoData, linearProblem);
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction1));
        assertNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction2));
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction1));
        assertNull(linearProblem.getRangeActionSetPointVariable(rangeAction2));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.POSITIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.NEGATIVE));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.POSITIVE));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.NEGATIVE));
    }
    @Test
    public void testDontFilterPst1() {
        setUpWithTwoPsts();
        // no need to filter out PSTs
        coreProblemFiller = new CoreProblemFiller(0, Map.of("FR", 2));
        coreProblemFiller.fill(raoData, linearProblem);
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction1));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction2));
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction1));
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction2));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.POSITIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.NEGATIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.POSITIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.NEGATIVE));
    }
    @Test
    public void testDontFilterPst2() {
        setUpWithTwoPsts();
        // no need to filter out PSTs
        coreProblemFiller = new CoreProblemFiller(0, null);
        coreProblemFiller.fill(raoData, linearProblem);
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction1));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction2));
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction1));
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction2));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.POSITIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.NEGATIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.POSITIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.NEGATIVE));
    }
    @Test
    public void testDontFilterPst3() {
        setUpWithTwoPsts();
        // no need to filter out PSTs
        coreProblemFiller = new CoreProblemFiller(0, new HashMap<>());
        coreProblemFiller.fill(raoData, linearProblem);
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction1));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction2));
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction1));
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction2));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.POSITIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.NEGATIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.POSITIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.NEGATIVE));
    }
    private void testFilterWrongRangeActions(int initialTapPosition, boolean shouldBeFiltered) {
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(initialTapPosition);
        initRaoData(crac.getPreventiveState());
        coreProblemFiller.fill(raoData, linearProblem);
        MPVariable variable = linearProblem.getAbsoluteRangeActionVariationVariable(crac.getRangeAction(RANGE_ACTION_ID));
        if (shouldBeFiltered) {
            assertNull(variable);
        } else {
            assertNotNull(variable);
        }
    }
    @Test
    public void testFilterWrongRangeActions1() {
        // PST has tap limits of -15 / +15
        testFilterWrongRangeActions(-15, false);
    }
    @Test
    public void testFilterWrongRangeActions2() {
        // PST has tap limits of -15 / +15
        testFilterWrongRangeActions(15, false);
    }
    @Test
    public void testFilterWrongRangeActions3() {
        // PST has tap limits of -15 / +15
        testFilterWrongRangeActions(-1, false);
    }

    @Test
    public void testFilterWrongRangeActions4() {
        // PST has tap limits of -15 / +15
        testFilterWrongRangeActions(-16, true);
    }
    // run an iterating optimization
    IteratingLinearOptimizerOutput iteratingLinearOptimizerOutput = IteratingLinearOptimizer.optimize(iteratingLinearOptimizerInput, iteratingLinearOptimizerParameters);

    @Test
    public void testFilterWrongRangeActions5() {
        // PST has tap limits of -15 / +15
        testFilterWrongRangeActions(16, true);
        // check results
        assertNotNull(iteratingLinearOptimizerOutput);
        assertEquals(IteratingLinearOptimizerOutput.SolveStatus.INFEASIBLE, iteratingLinearOptimizerOutput.getSolveStatus());
        assertEquals(100., iteratingLinearOptimizerOutput.getCost(), DOUBLE_TOLERANCE);
        assertEquals(0., iteratingLinearOptimizerOutput.getRangeActionSetpoint(crac.getRangeAction("PRA_PST_BE")), DOUBLE_TOLERANCE);
    }
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;


/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class MnecFillerTest extends AbstractFillerTest {

    private BranchCnec mnec1;
    private BranchCnec mnec2;

    @Before
    public void setUp() {
        init();
        crac.newBranchCnec().setId("MNEC1 - N - preventive")
                .newNetworkElement().setId("DDE2AA1  NNL3AA1  1").add()
                .newThreshold().setMin(-1000.).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(1000.0).setUnit(Unit.MEGAWATT).add()
                .optimized().monitored()
                .setInstant(Instant.PREVENTIVE)
                .add();
        mnec1 = crac.getBranchCnec("MNEC1 - N - preventive");

        crac.newBranchCnec().setId("MNEC2 - N - preventive")
                .newNetworkElement().setId("NNL2AA1  BBE3AA1  1").add()
                .newThreshold().setMin(-100.).setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(100.0).setUnit(Unit.MEGAWATT).add()
                .optimized().monitored()
                .setInstant(Instant.PREVENTIVE)
                .add();
        mnec2 = crac.getBranchCnec("MNEC2 - N - preventive");

        crac.desynchronize();
        CracCleaner cracCleaner = new CracCleaner();
        cracCleaner.cleanCrac(crac, network);
        RaoInputHelper.synchronize(crac, network);

        // fill the problem : the core filler is required
        coreProblemFiller = new CoreProblemFiller(
            linearProblem,
            network,
            Set.of(mnec1, mnec2),
            Collections.emptyMap()
        );
        coreProblemFiller.fill(sensitivityAndLoopflowResults);
    }

    private void fillProblemWithFiller(Unit unit) {
        MnecFiller mnecFiller = new MnecFiller(
            linearProblem,
            Map.of(mnec1, 900., mnec2, -200.),
            unit,
            new MnecParameters(50, 10, 3.5));
        mnecFiller.fill(sensitivityAndLoopflowResults);
    }

    @Test
    public void testAddMnecViolationVariables() {
        fillProblemWithFiller(Unit.MEGAWATT);
        crac.getBranchCnecs().forEach(cnec -> {
            MPVariable variable = linearProblem.getMnecViolationVariable(cnec);
            if (cnec.isMonitored()) {
                assertNotNull(variable);
                assertEquals(0, variable.lb(), DOUBLE_TOLERANCE);
                assertEquals(Double.POSITIVE_INFINITY, variable.ub(), DOUBLE_TOLERANCE);
            } else {
                assertNull(variable);
            }
        });
    }

    @Test
    public void testAddMnecMinFlowConstraints() {
        fillProblemWithFiller(Unit.MEGAWATT);

        crac.getBranchCnecs().stream().filter(cnec -> !cnec.isMonitored()).forEach(cnec -> {
            assertNull(linearProblem.getMnecFlowConstraint(cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        });

        MPConstraint ct1Max = linearProblem.getMnecFlowConstraint(mnec1, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(ct1Max);
        assertEquals(Double.NEGATIVE_INFINITY, ct1Max.lb(), DOUBLE_TOLERANCE);
        double mnec1MaxFlow = 1000 - 3.5;
        assertEquals(mnec1MaxFlow, ct1Max.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, ct1Max.getCoefficient(linearProblem.getFlowVariable(mnec1)), DOUBLE_TOLERANCE);
        assertEquals(-1, ct1Max.getCoefficient(linearProblem.getMnecViolationVariable(mnec1)), DOUBLE_TOLERANCE);

        MPConstraint ct1Min = linearProblem.getMnecFlowConstraint(mnec1, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(ct1Min);
        double mnec1MinFlow = -1000 + 3.5;
        assertEquals(mnec1MinFlow, ct1Min.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, ct1Min.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, ct1Min.getCoefficient(linearProblem.getFlowVariable(mnec1)), DOUBLE_TOLERANCE);
        assertEquals(1, ct1Min.getCoefficient(linearProblem.getMnecViolationVariable(mnec1)), DOUBLE_TOLERANCE);

        MPConstraint ct2Max = linearProblem.getMnecFlowConstraint(mnec2, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(ct2Max);
        assertEquals(Double.NEGATIVE_INFINITY, ct2Max.lb(), DOUBLE_TOLERANCE);
        double mnec2MaxFlow = 100 - 3.5;
        assertEquals(mnec2MaxFlow, ct2Max.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, ct2Max.getCoefficient(linearProblem.getFlowVariable(mnec2)), DOUBLE_TOLERANCE);
        assertEquals(-1, ct2Max.getCoefficient(linearProblem.getMnecViolationVariable(mnec2)), DOUBLE_TOLERANCE);

        MPConstraint ct2Min = linearProblem.getMnecFlowConstraint(mnec2, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(ct2Min);
        double mnec2MinFlow = -250 + 3.5;
        assertEquals(mnec2MinFlow, ct2Min.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, ct2Min.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, ct2Min.getCoefficient(linearProblem.getFlowVariable(mnec2)), DOUBLE_TOLERANCE);
        assertEquals(1, ct2Min.getCoefficient(linearProblem.getMnecViolationVariable(mnec2)), DOUBLE_TOLERANCE);
    }

    @Test
    public void testAddMnecPenaltyCostMW() {
        fillProblemWithFiller(Unit.MEGAWATT);
        crac.getBranchCnecs().stream().filter(Cnec::isMonitored).forEach(cnec -> {
            MPVariable mnecViolationVariable = linearProblem.getMnecViolationVariable(cnec);
            assertEquals(10.0, linearProblem.getObjective().getCoefficient(mnecViolationVariable), DOUBLE_TOLERANCE);
        });
    }

    @Test
    public void testAddMnecPenaltyCostA() {
        fillProblemWithFiller(Unit.AMPERE);
        crac.getBranchCnecs().stream().filter(Cnec::isMonitored).forEach(cnec -> {
            MPVariable mnecViolationVariable = linearProblem.getMnecViolationVariable(cnec);
            assertEquals(10.0 / 0.658, linearProblem.getObjective().getCoefficient(mnecViolationVariable), DOUBLE_TOLERANCE);
        });
    }
}
