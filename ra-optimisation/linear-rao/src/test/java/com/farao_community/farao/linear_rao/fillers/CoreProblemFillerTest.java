/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.farao_community.farao.linear_rao.mocks.CnecMock;
import com.farao_community.farao.linear_rao.mocks.RangeActionMock;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.sensitivity.*;
import com.powsybl.sensitivity.json.SensitivityComputationResultJsonSerializer;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class CoreProblemFillerTest extends FillerTest {

    private static final double DOUBLE_TOLERANCE = 0.1;

    private static final double REF_FLOW_CNEC1_IT1 = 500.0;
    private static final double REF_FLOW_CNEC2_IT1 = 300.0;
    private static final double REF_FLOW_CNEC1_IT2 = 400.0;
    private static final double REF_FLOW_CNEC2_IT2 = 350.0;

    private static final int MIN_TAP = -16;
    private static final int MAX_TAP = 16;
    private static final int TAP_INITIAL = 5;
    private static final int TAP_IT1 = -7;

    private static final String RANGE_ACTION_ELEMENT_ID = "BBE2AA1  BBE3AA1  1";

    private Cnec cnec1;
    private Cnec cnec2;
    private RangeAction rangeAction;

    @Before
    public void setUp() {
        init();
        coreProblemFiller = new CoreProblemFiller(linearRaoProblem, linearRaoData);
        cnec1 = crac.getCnecs().stream().filter(c -> c.getId().equals(CNEC_1_ID)).findFirst().orElseThrow(FaraoException::new);
        cnec2 = crac.getCnecs().stream().filter(c -> c.getId().equals(CNEC_2_ID)).findFirst().orElseThrow(FaraoException::new);
        rangeAction = crac.getRangeAction(RANGE_ACTION_ID);
    }

    private void fillProblemWithCoreFiller() throws IOException {
        // arrange some additional data
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        final double currentAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();
        byte[] inputBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/small-sensi-results-1.json"));
        SensitivityComputationResults sensiResults = SensitivityComputationResultJsonSerializer.read(new InputStreamReader(new ByteArrayInputStream(inputBytes)));

        // complete the mock of linearRaoData
        when(linearRaoData.getReferenceFlow(cnec1)).thenReturn(REF_FLOW_CNEC1_IT1);
        when(linearRaoData.getReferenceFlow(cnec2)).thenReturn(REF_FLOW_CNEC2_IT1);
        when(linearRaoData.getCurrentValue(rangeAction)).thenReturn(currentAlpha);
        when(linearRaoData.getSensitivityComputationResults(any())).thenReturn(sensiResults);

        // fill the problem
        coreProblemFiller.fill();
    }

    @Test
    public void fillTest() throws IOException {

        fillProblemWithCoreFiller();

        // some input data of the sensi (see small-sensi-results-1.json)
        final double cnec1toRangeSensitivity = 2;
        final double cnec2toRangeSensitivity = 5;

        // some additional data
        final double minAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getStep(MIN_TAP).getAlpha();
        final double maxAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getStep(MAX_TAP).getAlpha();
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        final double currentAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();


        // check range action setpoint variable
        MPVariable setPointVariable = linearRaoProblem.getRangeActionSetPointVariable(rangeAction);
        assertNotNull(setPointVariable);
        assertEquals(minAlpha, setPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, setPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable
        MPVariable absoluteVariationVariable = linearRaoProblem.getAboluteRangeActionVariationVariable(rangeAction);
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
        assertEquals(REF_FLOW_CNEC1_IT1 - currentAlpha * cnec1toRangeSensitivity , flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT1 - currentAlpha * cnec1toRangeSensitivity , flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(-cnec1toRangeSensitivity, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2
        MPVariable flowVariable2 = linearRaoProblem.getFlowVariable(cnec2);
        assertNotNull(flowVariable2);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable2.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable2.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec2
        MPConstraint flowConstraint2 = linearRaoProblem.getFlowConstraint(cnec2);
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT1 - currentAlpha * cnec2toRangeSensitivity, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT1 - currentAlpha * cnec2toRangeSensitivity, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-cnec2toRangeSensitivity, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check absolute variation constraints
        MPConstraint absoluteVariationConstraint1 = linearRaoProblem.getAboluteRangeActionVariationConstraint(rangeAction, LinearRaoProblem.AbsExtension.NEGATIVE);
        MPConstraint absoluteVariationConstraint2 = linearRaoProblem.getAboluteRangeActionVariationConstraint(rangeAction, LinearRaoProblem.AbsExtension.POSITIVE);
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
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_IT1);
        final double currentAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();
        byte[] inputBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/small-sensi-results-2.json"));
        SensitivityComputationResults sensiResults = SensitivityComputationResultJsonSerializer.read(new InputStreamReader(new ByteArrayInputStream(inputBytes)));

        // complete the mock of linearRaoData
        when(linearRaoData.getReferenceFlow(cnec1)).thenReturn(REF_FLOW_CNEC1_IT2);
        when(linearRaoData.getReferenceFlow(cnec2)).thenReturn(REF_FLOW_CNEC2_IT2);
        when(linearRaoData.getCurrentValue(rangeAction)).thenReturn(currentAlpha);
        when(linearRaoData.getSensitivityComputationResults(any())).thenReturn(sensiResults);

        // fill the problem
        coreProblemFiller.update(new ArrayList<>());
    }

    @Test
    public void updateTest() throws IOException {

        // fill a first time the linearRaoProblem with some data
        fillProblemWithCoreFiller();

        // update the problem with new data
        updateProblemWithCoreFiller();

        // some input data of the sensi (see small-sensi-results-1.json)
        final double cnec1toRangeSensitivity = 3;
        final double cnec2toRangeSensitivity = 7;

        // some additional data
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_IT1);
        final double currentAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();

        MPVariable setPointVariable = linearRaoProblem.getRangeActionSetPointVariable(rangeAction);

        // check flow variable for cnec1
        MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec1);
        assertNotNull(flowVariable);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec1
        MPConstraint flowConstraint = linearRaoProblem.getFlowConstraint(cnec1);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * cnec1toRangeSensitivity , flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * cnec1toRangeSensitivity , flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(-cnec1toRangeSensitivity, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2
        MPVariable flowVariable2 = linearRaoProblem.getFlowVariable(cnec2);
        assertNotNull(flowVariable2);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable2.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable2.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec2
        MPConstraint flowConstraint2 = linearRaoProblem.getFlowConstraint(cnec2);
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * cnec2toRangeSensitivity, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * cnec2toRangeSensitivity, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-cnec2toRangeSensitivity, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

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