/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *//*

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.rao_api.RaoParameters.DEFAULT_PST_PENALTY_COST;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

*/
/**
 * @author Joris Mancini{@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 *//*

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
        maxMinMarginFiller = new MaxMinMarginFiller(MEGAWATT, DEFAULT_PST_PENALTY_COST);
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

    @Test(expected = Test.None.class */
/* no exception expected *//*
)
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
                .setInstant(Instant.PREVENTIVE)
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
                .setInstant(Instant.PREVENTIVE)
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
}

*/
