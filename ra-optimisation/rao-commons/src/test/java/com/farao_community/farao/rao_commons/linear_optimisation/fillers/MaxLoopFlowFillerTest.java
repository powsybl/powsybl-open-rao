/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.LoopFlowParameters;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;


/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */

@RunWith(PowerMockRunner.class)
public class MaxLoopFlowFillerTest extends AbstractFillerTest {

    @Before
    public void setUp() {
        init();
        double initialAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();
        coreProblemFiller = new CoreProblemFiller(linearProblem, network, Set.of(cnec1), Map.of(rangeAction, initialAlpha));
        CnecLoopFlowExtension cnecLoopFlowExtension = new CnecLoopFlowExtension(100.0, Unit.MEGAWATT);
        cnec1.addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension);
    }

    @Test
    public void testFill1() {
        LoopFlowParameters parameters = new LoopFlowParameters(
            true,
            RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
            13.,
            10.,
            5.);

        MaxLoopFlowFiller maxLoopFlowFiller = new MaxLoopFlowFiller(
            linearProblem,
            Map.of(cnec1, 0.),
            parameters);

        sensitivityAndLoopflowResults = new SensitivityAndLoopflowResults(systematicSensitivityResult, Map.of(cnec1, 49.));

        // build problem
        coreProblemFiller.fill(sensitivityAndLoopflowResults);
        maxLoopFlowFiller.fill(sensitivityAndLoopflowResults);

        // check flow constraint for cnec1
        MPConstraint loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnec1, LinearProblem.BoundExtension.UPPER_BOUND);
        MPConstraint loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnec1, LinearProblem.BoundExtension.LOWER_BOUND);

        assertNotNull(loopFlowConstraintUb);
        assertNotNull(loopFlowConstraintLb);

        assertEquals(-(100 - 5.) + 49.0, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((100 - 5.) + 49.0, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);

        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1);
        assertEquals(1, loopFlowConstraintUb.getCoefficient(flowVariable), 0.1);
        assertEquals(1, loopFlowConstraintLb.getCoefficient(flowVariable), 0.1);
    }

    @Test
    public void testFill2() {
        LoopFlowParameters parameters = new LoopFlowParameters(
            true,
            RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
            30.,
            10.,
            5.);

        MaxLoopFlowFiller maxLoopFlowFiller = new MaxLoopFlowFiller(
            linearProblem,
            Map.of(cnec1, 80.),
            parameters);

        sensitivityAndLoopflowResults = new SensitivityAndLoopflowResults(systematicSensitivityResult, Map.of(cnec1, 49.));

        // build problem
        coreProblemFiller.fill(sensitivityAndLoopflowResults);
        maxLoopFlowFiller.fill(sensitivityAndLoopflowResults);

        // check flow constraint for cnec1
        MPConstraint loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnec1, LinearProblem.BoundExtension.UPPER_BOUND);
        MPConstraint loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnec1, LinearProblem.BoundExtension.LOWER_BOUND);

        assertNotNull(loopFlowConstraintUb);
        assertNotNull(loopFlowConstraintLb);

        assertEquals(-(110 - 5.) + 49.0, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((110 - 5.) + 49.0, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);

        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1);
        assertEquals(1, loopFlowConstraintUb.getCoefficient(flowVariable), 0.1);
        assertEquals(1, loopFlowConstraintLb.getCoefficient(flowVariable), 0.1);
    }

    @Test
    public void testShouldUpdate() {
        LoopFlowParameters parameters = new LoopFlowParameters(
            true,
            RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO_AND_PST,
            0.,
            10.,
            5.);

        MaxLoopFlowFiller maxLoopFlowFiller = new MaxLoopFlowFiller(
            linearProblem,
            Map.of(cnec1, 0.),
            parameters);

        sensitivityAndLoopflowResults = new SensitivityAndLoopflowResults(systematicSensitivityResult, Map.of(cnec1, 49.));

        // build problem
        coreProblemFiller.fill(sensitivityAndLoopflowResults);
        maxLoopFlowFiller.fill(sensitivityAndLoopflowResults);

        // update rao data and filler
        sensitivityAndLoopflowResults = new SensitivityAndLoopflowResults(systematicSensitivityResult, Map.of(cnec1, 67.));
        maxLoopFlowFiller.update(sensitivityAndLoopflowResults);

        // check flow constraint for cnec1
        MPConstraint loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnec1, LinearProblem.BoundExtension.UPPER_BOUND);
        MPConstraint loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnec1, LinearProblem.BoundExtension.LOWER_BOUND);

        assertEquals(-(100 - 5.) + 67.0, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((100 - 5.) + 67.0, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);

        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1);
        assertEquals(1, loopFlowConstraintUb.getCoefficient(flowVariable), 0.1);
        assertEquals(1, loopFlowConstraintLb.getCoefficient(flowVariable), 0.1);
    }

    @Test
    public void testShouldNotUpdate() {
        LoopFlowParameters parameters = new LoopFlowParameters(
            true,
            RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO,
            0.,
            10.,
            5.);

        MaxLoopFlowFiller maxLoopFlowFiller = new MaxLoopFlowFiller(
            linearProblem,
            Map.of(cnec1, 0.),
            parameters);

        sensitivityAndLoopflowResults = new SensitivityAndLoopflowResults(systematicSensitivityResult, Map.of(cnec1, 49.));

        // build problem
        coreProblemFiller.fill(sensitivityAndLoopflowResults);
        maxLoopFlowFiller.fill(sensitivityAndLoopflowResults);

        // update rao data and filler
        sensitivityAndLoopflowResults = new SensitivityAndLoopflowResults(systematicSensitivityResult, Map.of(cnec1, 67.));
        maxLoopFlowFiller.update(sensitivityAndLoopflowResults);

        // check flow constraint for cnec1
        MPConstraint loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnec1, LinearProblem.BoundExtension.UPPER_BOUND);
        MPConstraint loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnec1, LinearProblem.BoundExtension.LOWER_BOUND);

        assertEquals(-(100 - 5.) + 49.0, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((100 - 5.) + 49.0, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);
    }
}
