/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.sensitivity.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

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
        coreProblemFiller = new CoreProblemFiller();
        initRaoData(crac.getPreventiveState());
    }

    @Test
    public void testFill() {
        CnecLoopFlowExtension cnecLoopFlowExtension = new CnecLoopFlowExtension(0.0, Unit.PERCENT_IMAX);
        cnecLoopFlowExtension.setLoopFlowConstraintInMW(100.0);
        cnec1.addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension);
        initRaoData(crac.getPreventiveState());
        cnec1.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId()).setCommercialFlowInMW(49.0);

        double loopFlowConstraintAdjustmentCoefficient = 5.;
        double loopFlowViolationCost = 10.;

        MaxLoopFlowFiller maxLoopFlowFiller = new MaxLoopFlowFiller(loopFlowConstraintAdjustmentCoefficient, loopFlowViolationCost, RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF);

        // build problem
        coreProblemFiller.fill(raoData, linearProblem);
        maxLoopFlowFiller.fill(raoData, linearProblem);

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
    public void testShouldUpdate() {
        CnecLoopFlowExtension cnecLoopFlowExtension = new CnecLoopFlowExtension(0.0, Unit.PERCENT_IMAX);
        cnecLoopFlowExtension.setLoopFlowConstraintInMW(100.0);
        cnec1.addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension);
        initRaoData(crac.getPreventiveState());
        cnec1.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId()).setCommercialFlowInMW(49.0);

        double loopFlowConstraintAdjustmentCoefficient = 5.;
        double loopFlowViolationCost = 10.;

        MaxLoopFlowFiller maxLoopFlowFiller = new MaxLoopFlowFiller(loopFlowConstraintAdjustmentCoefficient, loopFlowViolationCost, RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO_AND_PST);

        // build problem
        coreProblemFiller.fill(raoData, linearProblem);
        maxLoopFlowFiller.fill(raoData, linearProblem);

        // update rao data and filler
        cnecLoopFlowExtension.setLoopFlowConstraintInMW(350.0);
        cnec1.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId()).setCommercialFlowInMW(67.0);
        maxLoopFlowFiller.update(raoData, linearProblem);

        // check flow constraint for cnec1
        MPConstraint loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnec1, LinearProblem.BoundExtension.UPPER_BOUND);
        MPConstraint loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnec1, LinearProblem.BoundExtension.LOWER_BOUND);

        assertEquals(-(350 - 5.) + 67.0, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((350 - 5.) + 67.0, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);

        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1);
        assertEquals(1, loopFlowConstraintUb.getCoefficient(flowVariable), 0.1);
        assertEquals(1, loopFlowConstraintLb.getCoefficient(flowVariable), 0.1);
    }

    @Test
    public void testShouldNotUpdate() {
        CnecLoopFlowExtension cnecLoopFlowExtension = new CnecLoopFlowExtension(0.0, Unit.PERCENT_IMAX);
        cnecLoopFlowExtension.setLoopFlowConstraintInMW(100.0);
        cnec1.addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension);
        initRaoData(crac.getPreventiveState());
        cnec1.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId()).setCommercialFlowInMW(49.0);

        double loopFlowConstraintAdjustmentCoefficient = 5.;
        double loopFlowViolationCost = 10.;

        MaxLoopFlowFiller maxLoopFlowFiller = new MaxLoopFlowFiller(loopFlowConstraintAdjustmentCoefficient, loopFlowViolationCost, RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO);

        // build problem
        coreProblemFiller.fill(raoData, linearProblem);
        maxLoopFlowFiller.fill(raoData, linearProblem);

        // update rao data and filler
        cnecLoopFlowExtension.setLoopFlowConstraintInMW(350.0);
        cnec1.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId()).setCommercialFlowInMW(67.0);
        maxLoopFlowFiller.update(raoData, linearProblem);

        // check flow constraint for cnec1
        MPConstraint loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnec1, LinearProblem.BoundExtension.UPPER_BOUND);
        MPConstraint loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnec1, LinearProblem.BoundExtension.LOWER_BOUND);

        assertEquals(-(100 - 5.) + 49.0, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((100 - 5.) + 49.0, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);
    }
}
