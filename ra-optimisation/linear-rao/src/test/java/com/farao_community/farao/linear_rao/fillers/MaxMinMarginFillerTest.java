/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.google.ortools.linearsolver.MPConstraint;

import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.json.SensitivityComputationResultJsonSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.InputStreamReader;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini{@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class MaxMinMarginFillerTest extends AbstractFillerTest {

    private MaxMinMarginFiller maxMinMarginFiller;

    @Before
    public void setUp() {
        init();
        coreProblemFiller = new CoreProblemFiller(linearRaoProblem, linearRaoData, linearRaoParameters);
        maxMinMarginFiller = new MaxMinMarginFiller(linearRaoProblem, linearRaoData, linearRaoParameters);
    }

    private void fillProblemWithFiller() throws IOException {
        // arrange some additional data
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        SensitivityComputationResults sensiResults = SensitivityComputationResultJsonSerializer.read(new InputStreamReader(getClass().getResourceAsStream("/small-sensi-results-1.json")));

        // complete the mock of linearRaoData
        when(linearRaoData.getReferenceFlow(cnec1)).thenReturn(REF_FLOW_CNEC1_IT1);
        when(linearRaoData.getReferenceFlow(cnec2)).thenReturn(REF_FLOW_CNEC2_IT1);
        when(linearRaoData.getSensitivityComputationResults(any())).thenReturn(sensiResults);

        // fill the problem : the core filler is required
        coreProblemFiller.fill();
        maxMinMarginFiller.fill();
    }

    @Test
    public void fillWithRangeAction() throws IOException  {
        fillProblemWithFiller();

        MPVariable flowCnec1 = linearRaoProblem.getFlowVariable(cnec1);
        MPVariable absoluteVariation = linearRaoProblem.getAbsoluteRangeActionVariationVariable(rangeAction);

        // check minimum margin variable
        MPVariable minimumMargin = linearRaoProblem.getMinimumMarginVariable();
        assertNotNull(minimumMargin);

        // check minimum margin constraints
        MPConstraint cnec1AboveThreshold = linearRaoProblem.getMinimumMarginConstraint(cnec1, LinearRaoProblem.MarginExtension.ABOVE_THRESHOLD);
        MPConstraint cnec1BelowThreshold = linearRaoProblem.getMinimumMarginConstraint(cnec1, LinearRaoProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(cnec1AboveThreshold);
        assertNotNull(cnec1BelowThreshold);
        assertEquals(-Double.POSITIVE_INFINITY, cnec1BelowThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(-MIN_FLOW_1, cnec1BelowThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-Double.POSITIVE_INFINITY, cnec1AboveThreshold.lb(), DOUBLE_TOLERANCE);
        assertEquals(MAX_FLOW_1, cnec1AboveThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-1, cnec1BelowThreshold.getCoefficient(flowCnec1), DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThreshold.getCoefficient(flowCnec1), DOUBLE_TOLERANCE);

        // check objective
        assertNotEquals(0, linearRaoProblem.getObjective().getCoefficient(absoluteVariation)); // penalty cost
        assertEquals(-1, linearRaoProblem.getObjective().getCoefficient(minimumMargin), DOUBLE_TOLERANCE); // penalty cost
        assertTrue(linearRaoProblem.getObjective().minimization());

        // check the number of variables and constraints
        // total number of variables 5 :
        //      - 4 due to CoreFiller
        //      - minimum margin variable
        // total number of constraints 8 :
        //      - 4 due to CoreFiller
        //      - 2 per CNEC (min margin constraints)
        assertEquals(5, linearRaoProblem.getSolver().numVariables());
        assertEquals(8, linearRaoProblem.getSolver().numConstraints());
    }

    @Test
    public void fillWithMissingFlowVariables() {
        try {
            // AbsoluteRangeActionVariables present, but no the FlowVariables
            linearRaoProblem.addAbsoluteRangeActionVariationVariable(0.0, 0.0, rangeAction);
            maxMinMarginFiller.fill();
            fail();
        } catch (FaraoException e) {
            assertTrue(e.getMessage().contains("Flow variable"));
        }
    }

    @Test
    public void fillWithMissingRangeActionVariables() {
        try {
            // FlowVariables present , but not the absoluteRangeActionVariables present,
            linearRaoProblem.addFlowVariable(0.0, 0.0, cnec1);
            linearRaoProblem.addFlowVariable(0.0, 0.0, cnec2);
            maxMinMarginFiller.fill();
            fail();
        } catch (FaraoException e) {
            assertTrue(e.getMessage().contains("Range action variable"));
        }
    }
}

