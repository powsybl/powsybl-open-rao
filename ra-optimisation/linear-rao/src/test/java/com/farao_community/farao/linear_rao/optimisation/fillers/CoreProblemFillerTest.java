/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.linear_rao.optimisation.LinearRaoProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.sensitivity.*;
import com.powsybl.sensitivity.json.SensitivityComputationResultJsonSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class CoreProblemFillerTest extends AbstractFillerTest {

    @Before
    public void setUp() throws IOException {
        init();
        coreProblemFiller = new CoreProblemFiller();
    }

    private void fillProblemWithCoreFiller() throws IOException {
        // arrange some additional data
        linearRaoData.getNetwork().getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);

        // fill the problem
        coreProblemFiller.fill(linearRaoData, linearRaoProblem);
    }

    @Test
    public void fillTest() throws IOException {

        fillProblemWithCoreFiller();

        // some additional data
        final double minAlpha = linearRaoData.getNetwork().getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getStep(MIN_TAP).getAlpha();
        final double maxAlpha = linearRaoData.getNetwork().getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getStep(MAX_TAP).getAlpha();
        final double currentAlpha = linearRaoData.getNetwork().getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();

        // check range action setpoint variable
        MPVariable setPointVariable = linearRaoProblem.getRangeActionSetPointVariable(rangeAction);
        assertNotNull(setPointVariable);
        assertEquals(minAlpha, setPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, setPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable
        MPVariable absoluteVariationVariable = linearRaoProblem.getAbsoluteRangeActionVariationVariable(rangeAction);
        assertNotNull(absoluteVariationVariable);
        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationVariable.ub(), DOUBLE_TOLERANCE);

        // check flow variable for cnec1
        MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec1);
        assertNotNull(flowVariable);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec1
        MPConstraint flowConstraint = linearRaoProblem.getFlowConstraint(cnec1);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT1 - currentAlpha * SENSI_CNEC1_IT1, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT1 - currentAlpha * SENSI_CNEC1_IT1, flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(-SENSI_CNEC1_IT1, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2
        MPVariable flowVariable2 = linearRaoProblem.getFlowVariable(cnec2);
        assertNotNull(flowVariable2);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable2.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable2.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec2
        MPConstraint flowConstraint2 = linearRaoProblem.getFlowConstraint(cnec2);
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT1 - currentAlpha * SENSI_CNEC2_IT1, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT1 - currentAlpha * SENSI_CNEC2_IT1, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC2_IT1, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check absolute variation constraints
        MPConstraint absoluteVariationConstraint1 = linearRaoProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearRaoProblem.AbsExtension.NEGATIVE);
        MPConstraint absoluteVariationConstraint2 = linearRaoProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearRaoProblem.AbsExtension.POSITIVE);
        assertNotNull(absoluteVariationConstraint1);
        assertNotNull(absoluteVariationConstraint2);
        assertEquals(-currentAlpha, absoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationConstraint1.ub(), DOUBLE_TOLERANCE);
        assertEquals(currentAlpha, absoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationConstraint2.ub(), DOUBLE_TOLERANCE);

        // check the number of variables and constraints
        // total number of variables 4 :
        //      - 1 per CNEC (flow)
        //      - 2 per range action (set-point and variation)
        // total number of constraints 4 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (absolute variation constraints)
        assertEquals(4, linearRaoProblem.getSolver().numVariables());
        assertEquals(4, linearRaoProblem.getSolver().numConstraints());
    }

    private void updateProblemWithCoreFiller() throws IOException {
        // arrange some additional data
        linearRaoData.getNetwork().getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_IT2);

        systematicSensitivityAnalysisResult.getCnecFlowMap().put(cnec1, REF_FLOW_CNEC1_IT2);
        systematicSensitivityAnalysisResult.getCnecFlowMap().put(cnec2, REF_FLOW_CNEC2_IT2);
        SensitivityComputationResults sensiResults = SensitivityComputationResultJsonSerializer.read(new InputStreamReader(getClass().getResourceAsStream("/small-sensi-results-2.json")));
        linearRaoData.getCrac().getStates().forEach(state -> systematicSensitivityAnalysisResult.getStateSensiMap().put(state, sensiResults));
        linearRaoData.setSystematicSensitivityAnalysisResult(systematicSensitivityAnalysisResult);

        // fill the problem
        coreProblemFiller.update(linearRaoData, linearRaoProblem);
    }

    @Test
    public void updateTest() throws IOException {

        // fill a first time the linearRaoProblem with some data
        fillProblemWithCoreFiller();

        // update the problem with new data
        updateProblemWithCoreFiller();

        // some additional data
        final double currentAlpha = linearRaoData.getNetwork().getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();

        MPVariable setPointVariable = linearRaoProblem.getRangeActionSetPointVariable(rangeAction);

        // check flow variable for cnec1
        MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec1);
        assertNotNull(flowVariable);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec1
        MPConstraint flowConstraint = linearRaoProblem.getFlowConstraint(cnec1);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(-SENSI_CNEC1_IT2, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2
        MPVariable flowVariable2 = linearRaoProblem.getFlowVariable(cnec2);
        assertNotNull(flowVariable2);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable2.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable2.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec2
        MPConstraint flowConstraint2 = linearRaoProblem.getFlowConstraint(cnec2);
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * SENSI_CNEC2_IT2, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * SENSI_CNEC2_IT2, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC2_IT2, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check the number of variables and constraints
        // total number of variables 4 :
        //      - 1 per CNEC (flow)
        //      - 2 per range action (set-point and variation)
        // total number of constraints 4 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (absolute variation constraints)
        assertEquals(4, linearRaoProblem.getSolver().numVariables());
        assertEquals(4, linearRaoProblem.getSolver().numConstraints());
    }

    @Test
    public void updateWithoutFillingTest() throws IOException {
        try {
            updateProblemWithCoreFiller();
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }
}
