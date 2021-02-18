/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.rao_api.RaoParameters.DEFAULT_PST_PENALTY_COST;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

/**
 * @author Joris Mancini{@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class MaxMinMarginFillerTest extends AbstractFillerTest {

    private MaxMinMarginFiller maxMinMarginFiller;
    static final double PRECISE_DOUBLE_TOLERANCE = 1e-10;
    BranchCnec cnecNl;
    BranchCnec cnecFr;

    @Before
    public void setUp() {
        init();
        coreProblemFiller = new CoreProblemFiller();
        maxMinMarginFiller = new MaxMinMarginFiller(MEGAWATT, DEFAULT_PST_PENALTY_COST, null);
    }

    private void fillProblemWithCoreFiller() {
        // arrange some additional data
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        raoData.getCracResultManager().applyRangeActionResultsOnNetwork();

        // fill the problem : the core filler is required
        coreProblemFiller.fill(raoData, linearProblem);
    }

    @Test
    public void fillWithMaxMinMarginInMegawatt() {
        initRaoData(crac.getPreventiveState());
        fillProblemWithCoreFiller();
        maxMinMarginFiller.fill(raoData, linearProblem);

        MPVariable flowCnec1 = linearProblem.getFlowVariable(cnec1);
        MPVariable absoluteVariation = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);

        // check minimum margin variable
        MPVariable minimumMargin = linearProblem.getMinimumMarginVariable();
        assertNotNull(minimumMargin);

        // check minimum margin constraints
        MPConstraint cnec1AboveThreshold = linearProblem.getMinimumMarginConstraint(cnec1, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        MPConstraint cnec1BelowThreshold = linearProblem.getMinimumMarginConstraint(cnec1, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(cnec1AboveThreshold);
        assertNotNull(cnec1BelowThreshold);
        assertEquals(-Double.POSITIVE_INFINITY, cnec1BelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-MIN_FLOW_1, cnec1BelowThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-Double.POSITIVE_INFINITY, cnec1AboveThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(MAX_FLOW_1, cnec1AboveThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-1, cnec1BelowThreshold.getCoefficient(flowCnec1), DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThreshold.getCoefficient(flowCnec1), DOUBLE_TOLERANCE);
        assertEquals(1, cnec1BelowThreshold.getCoefficient(minimumMargin), DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThreshold.getCoefficient(minimumMargin), DOUBLE_TOLERANCE);

        // check objective
        assertEquals(0.01, linearProblem.getObjective().getCoefficient(absoluteVariation), DOUBLE_TOLERANCE); // penalty cost
        assertEquals(-1.0, linearProblem.getObjective().getCoefficient(minimumMargin), DOUBLE_TOLERANCE); // penalty cost
        assertTrue(linearProblem.getObjective().minimization());

        // check the number of variables and constraints
        // total number of variables 4 :
        //      - 3 due to CoreFiller
        //      - minimum margin variable
        // total number of constraints 5 :
        //      - 3 due to CoreFiller
        //      - 2 per CNEC (min margin constraints)
        assertEquals(4, linearProblem.getSolver().numVariables());
        assertEquals(5, linearProblem.getSolver().numConstraints());
    }

    @Test
    public void fillWithMaxMinMarginInAmpere() {
        initRaoData(crac.getPreventiveState());
        maxMinMarginFiller.setUnit(Unit.AMPERE);
        fillProblemWithCoreFiller();
        maxMinMarginFiller.fill(raoData, linearProblem);

        MPVariable flowCnec1 = linearProblem.getFlowVariable(cnec1);
        MPVariable absoluteVariation = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);

        // check minimum margin variable
        MPVariable minimumMargin = linearProblem.getMinimumMarginVariable();
        assertNotNull(minimumMargin);

        // check minimum margin constraints
        MPConstraint cnec1AboveThreshold = linearProblem.getMinimumMarginConstraint(cnec1, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        MPConstraint cnec1BelowThreshold = linearProblem.getMinimumMarginConstraint(cnec1, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(cnec1AboveThreshold);
        assertNotNull(cnec1BelowThreshold);
        assertEquals(-Double.POSITIVE_INFINITY, cnec1BelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-MIN_FLOW_1, cnec1BelowThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-Double.POSITIVE_INFINITY, cnec1AboveThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(MAX_FLOW_1, cnec1AboveThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-1, cnec1BelowThreshold.getCoefficient(flowCnec1), DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThreshold.getCoefficient(flowCnec1), DOUBLE_TOLERANCE);

        assertEquals(380.0 * Math.sqrt(3) / 1000, cnec1BelowThreshold.getCoefficient(minimumMargin), DOUBLE_TOLERANCE);
        assertEquals(380.0 * Math.sqrt(3) / 1000, cnec1AboveThreshold.getCoefficient(minimumMargin), DOUBLE_TOLERANCE);

        // check objective
        assertEquals(0.01, linearProblem.getObjective().getCoefficient(absoluteVariation), DOUBLE_TOLERANCE); // penalty cost
        assertEquals(-1.0, linearProblem.getObjective().getCoefficient(minimumMargin), DOUBLE_TOLERANCE); // penalty cost
        assertTrue(linearProblem.getObjective().minimization());

        // check the number of variables and constraints
        assertEquals(4, linearProblem.getSolver().numVariables());
        assertEquals(5, linearProblem.getSolver().numConstraints());
    }

    @Test
    public void fillWithMissingFlowVariables() {
        initRaoData(crac.getPreventiveState());
        // AbsoluteRangeActionVariables present, but no the FlowVariables
        linearProblem.addAbsoluteRangeActionVariationVariable(0.0, 0.0, rangeAction);
        try {
            maxMinMarginFiller.fill(raoData, linearProblem);
            fail();
        } catch (FaraoException e) {
            assertTrue(e.getMessage().contains("Flow variable"));
        }
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void fillWithMissingRangeActionVariables() {
        initRaoData(crac.getPreventiveState());
        // FlowVariables present , but not the absoluteRangeActionVariables present,
        // This should work since range actions can be filtered out by the CoreProblemFiller if their number
        // exceeds the max-pst-per-tso parameter
        linearProblem.addFlowVariable(0.0, 0.0, cnec1);
        linearProblem.addFlowVariable(0.0, 0.0, cnec2);
        maxMinMarginFiller.fill(raoData, linearProblem);
    }

    @Test
    public void skipPureMnecsInMinMarginDef() {
        crac.newBranchCnec().setId("MNEC - N - preventive")
                .newNetworkElement().setId("DDE2AA1  NNL3AA1  1").add()
                .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(1000.0).setMin(-1000.).setUnit(Unit.MEGAWATT).add()
                .newThreshold().setRule(BranchThresholdRule.ON_RIGHT_SIDE).setMax(1000.0).setMin(-1000.).setUnit(Unit.MEGAWATT).add()
                .monitored()
                .setInstant(crac.getInstant("N"))
                .add();
        BranchCnec mnec = crac.getBranchCnec("MNEC - N - preventive");
        initRaoData(crac.getPreventiveState());
        fillProblemWithCoreFiller();
        maxMinMarginFiller.fill(raoData, linearProblem);
        MPConstraint mnecAboveThreshold = linearProblem.getMinimumMarginConstraint(mnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        MPConstraint mnecBelowThreshold = linearProblem.getMinimumMarginConstraint(mnec, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNull(mnecAboveThreshold);
        assertNull(mnecBelowThreshold);
    }

    @Test
    public void dontSkipCnecsMnecsInMinMarginDef() {
        crac.newBranchCnec().setId("MNEC - N - preventive")
                .newNetworkElement().setId("DDE2AA1  NNL3AA1  1").add()
                .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(1000.0).setMin(-1000.).setUnit(Unit.MEGAWATT).add()
                .newThreshold().setRule(BranchThresholdRule.ON_RIGHT_SIDE).setMax(1000.0).setMin(-1000.).setUnit(Unit.MEGAWATT).add()
                .monitored()
                .optimized()
                .setInstant(crac.getInstant("N"))
                .add();
        BranchCnec mnec = crac.getBranchCnec("MNEC - N - preventive");
        initRaoData(crac.getPreventiveState());
        fillProblemWithCoreFiller();
        maxMinMarginFiller.fill(raoData, linearProblem);
        MPConstraint mnecAboveThreshold = linearProblem.getMinimumMarginConstraint(mnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        MPConstraint mnecBelowThreshold = linearProblem.getMinimumMarginConstraint(mnec, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(mnecAboveThreshold);
        assertNotNull(mnecBelowThreshold);
    }

    private void setupOperatorsNotSharingRas() {
        // Add a cnec
        crac.newBranchCnec().setId("Line NL - N - preventive")
                .newNetworkElement().setId("NNL1AA1  NNL2AA1  1").add()
                .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(800.0).setMin(-1000.).setUnit(Unit.MEGAWATT).add()
                .optimized()
                .setInstant(crac.getInstant("N"))
                .setOperator("NL")
                .add();
        // Set initial margins on both preventive CNECs
        cnecNl = crac.getBranchCnec("Line NL - N - preventive");
        cnecFr = crac.getBranchCnec("Tieline BE FR - N - preventive");

        // Create filler with new operatorsNotSharingRas and fill
        maxMinMarginFiller = new MaxMinMarginFiller(MEGAWATT, DEFAULT_PST_PENALTY_COST, Collections.singleton("NL"));
    }

    @Test
    public void testGetCnecsForOperatorsNotSharingRas1() {
        initRaoData(crac.getPreventiveState());
        assertEquals(0, maxMinMarginFiller.getCnecsForOperatorsNotSharingRas(raoData).count());
    }

    @Test
    public void testGetCnecsForOperatorsNotSharingRas2() {
        setupOperatorsNotSharingRas();
        initRaoData(crac.getPreventiveState());
        assertEquals(1, maxMinMarginFiller.getCnecsForOperatorsNotSharingRas(raoData).count());
        assertSame(cnecNl, maxMinMarginFiller.getCnecsForOperatorsNotSharingRas(raoData).collect(Collectors.toList()).get(0));
    }

    @Test
    public void testGetMinPossibleMarginOnPerimeter() {
        setupOperatorsNotSharingRas();
        initRaoData(crac.getPreventiveState());
        cnecFr.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(600); // wit a threshold of 750
        cnecNl.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(500); // wit a threshold of 800
        assertEquals(150.0, maxMinMarginFiller.getMinPossibleMarginOnPerimeter(raoData), DOUBLE_TOLERANCE);

        String newVariant = raoData.getCracVariantManager().cloneWorkingVariant();
        raoData.getCrac().getExtension(ResultVariantManager.class).setPrePerimeterVariantId(newVariant);
        cnecFr.getExtension(CnecResultExtension.class).getVariant(newVariant).setFlowInMW(650); // wit a threshold of 750
        cnecNl.getExtension(CnecResultExtension.class).getVariant(newVariant).setFlowInMW(-950); // wit a threshold of -1000
        assertEquals(50.0, maxMinMarginFiller.getMinPossibleMarginOnPerimeter(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetLargestCnecThreshold1() {
        setupOperatorsNotSharingRas();
        initRaoData(crac.getPreventiveState());
        assertEquals(1000, maxMinMarginFiller.getLargestCnecThreshold(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetLargestCnecThreshold2() {
        setupOperatorsNotSharingRas();
        crac.newBranchCnec().setId("Pure MNEC")
                .newNetworkElement().setId("DDE2AA1  NNL3AA1  1").add()
                .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(3000.0).setMin(-3000.).setUnit(Unit.MEGAWATT).add()
                .monitored()
                .setInstant(crac.getInstant("N"))
                .add();
        crac.newBranchCnec().setId("CNEC MNEC")
                .newNetworkElement().setId("DDE2AA1  NNL3AA1  1").add()
                .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(2000.0).setMin(-2500.).setUnit(Unit.MEGAWATT).add()
                .monitored()
                .optimized()
                .setInstant(crac.getInstant("N"))
                .add();
        initRaoData(crac.getPreventiveState());
        assertEquals(2500, maxMinMarginFiller.getLargestCnecThreshold(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testExcludeCnecsNotSharingRasBinaryVar() {
        setupOperatorsNotSharingRas();
        initRaoData(crac.getPreventiveState());
        cnecFr.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(600); // wit a threshold of +750/-750
        cnecNl.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(400); // wit a threshold of +800/-1000
        double worstMarginDecreaseOnCnecNl = (800 - 400) - (750 - 600);

        fillProblemWithCoreFiller();
        maxMinMarginFiller.fill(raoData, linearProblem);

        // Verify existence of margin_decrease binary variable
        assertNull(linearProblem.getMarginDecreaseBinaryVariable(cnecFr));
        MPVariable binaryVar = linearProblem.getMarginDecreaseBinaryVariable(cnecNl);
        assertNotNull(binaryVar);

        // Get flow variable
        MPVariable flowVar = linearProblem.getFlowVariable(cnecNl);

        // Verify existence of margin_decrease definition constraints
        assertNull(linearProblem.getMarginDecreaseConstraint(cnecFr, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertNull(linearProblem.getMarginDecreaseConstraint(cnecFr, LinearProblem.MarginExtension.ABOVE_THRESHOLD));

        MPConstraint marginDecreaseConstraintMin = linearProblem.getMarginDecreaseConstraint(cnecNl, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(marginDecreaseConstraintMin);
        assertEquals(linearProblem.infinity(), marginDecreaseConstraintMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(-1000 + (800 - 400), marginDecreaseConstraintMin.lb(), DOUBLE_TOLERANCE);
        assertEquals(1.0, marginDecreaseConstraintMin.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(worstMarginDecreaseOnCnecNl, marginDecreaseConstraintMin.getCoefficient(binaryVar), DOUBLE_TOLERANCE);

        MPConstraint marginDecreaseConstraintMax = linearProblem.getMarginDecreaseConstraint(cnecNl, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertNotNull(marginDecreaseConstraintMax);
        assertEquals(linearProblem.infinity(), marginDecreaseConstraintMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(-800 + (800 - 400), marginDecreaseConstraintMax.lb(), DOUBLE_TOLERANCE);
        assertEquals(-1.0, marginDecreaseConstraintMax.getCoefficient(flowVar), DOUBLE_TOLERANCE);
        assertEquals(worstMarginDecreaseOnCnecNl, marginDecreaseConstraintMax.getCoefficient(binaryVar), DOUBLE_TOLERANCE);
    }

    @Test
    public void testExcludeCnecsNotSharingInMinMargin() {
        setupOperatorsNotSharingRas();
        initRaoData(crac.getPreventiveState());
        cnecFr.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(600); // wit a threshold of +750/-750
        cnecNl.getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setFlowInMW(400); // wit a threshold of +800/-1000
        double maxAbsThreshold = 1000;
        fillProblemWithCoreFiller();
        maxMinMarginFiller.fill(raoData, linearProblem);

        // Test that cnecFr's constraint does not have a bigM
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(cnecFr, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
        assertEquals(750.0, linearProblem.getMinimumMarginConstraint(cnecFr, LinearProblem.MarginExtension.ABOVE_THRESHOLD).ub(), DOUBLE_TOLERANCE);

        // Test that cnecNl's constraint does have a bigM
        MPVariable marginDecreaseVariable = linearProblem.getMarginDecreaseBinaryVariable(cnecNl);
        MPConstraint minMarginDefMin = linearProblem.getMinimumMarginConstraint(cnecNl, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertEquals(1000 + 2 * maxAbsThreshold, minMarginDefMin.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * maxAbsThreshold, minMarginDefMin.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
        MPConstraint minMarginDefMax = linearProblem.getMinimumMarginConstraint(cnecNl, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        assertEquals(800 + 2 * maxAbsThreshold, minMarginDefMax.ub(), DOUBLE_TOLERANCE);
        assertEquals(2 * maxAbsThreshold, minMarginDefMax.getCoefficient(marginDecreaseVariable), DOUBLE_TOLERANCE);
    }
}

