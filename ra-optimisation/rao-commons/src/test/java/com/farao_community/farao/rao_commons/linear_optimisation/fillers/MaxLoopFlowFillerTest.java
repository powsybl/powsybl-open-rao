/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_api.parameters.LoopFlowParameters;
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
    private LoopFlowParameters loopFlowParameters;

    @Before
    public void setUp() {
        init();
        double initialAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();
        coreProblemFiller = new CoreProblemFiller(linearProblem, network, Set.of(cnec1), Map.of(rangeAction, initialAlpha), 0);
        cnec1.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.MEGAWATT)
            .withValue(100.)
            .add();
    }

    @Test
    public void testFill1() {
        loopFlowParameters = new LoopFlowParameters(
                RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                13,
                10,
                5);

        MaxLoopFlowFiller maxLoopFlowFiller = new MaxLoopFlowFiller(
                linearProblem,
                Map.of(cnec1, 0.),
                loopFlowParameters);

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
        loopFlowParameters = new LoopFlowParameters(
                RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                30,
                10,
                5);

        MaxLoopFlowFiller maxLoopFlowFiller = new MaxLoopFlowFiller(
                linearProblem,
                Map.of(cnec1, 80.),
                loopFlowParameters);

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
        loopFlowParameters = new LoopFlowParameters(
                RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO_AND_PST,
                0,
                10,
                5);

        MaxLoopFlowFiller maxLoopFlowFiller = new MaxLoopFlowFiller(
                linearProblem,
                Map.of(cnec1, 0.),
                loopFlowParameters);

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
        loopFlowParameters = new LoopFlowParameters(
                RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO,
                0,
                10,
                5);

        MaxLoopFlowFiller maxLoopFlowFiller = new MaxLoopFlowFiller(
                linearProblem,
                Map.of(cnec1, 0.),
                loopFlowParameters);

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
