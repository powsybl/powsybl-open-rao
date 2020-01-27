/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.farao_community.farao.linear_rao.mocks.CnecMock;
import com.farao_community.farao.linear_rao.mocks.RangeActionMock;
import com.farao_community.farao.linear_rao.mocks.TwoWindingsTransformerMock;
import com.google.ortools.linearsolver.MPObjective;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class PositiveMinMarginFillerTest extends FillerTest {

    private PositiveMinMarginFiller positiveMinMarginFiller;

    @Before
    public void setUp() {
        init();
        coreProblemFiller = new CoreProblemFiller(linearRaoProblem, linearRaoData);
        positiveMinMarginFiller = new PositiveMinMarginFiller(linearRaoProblem, linearRaoData);
    }

    @Test
    public void fillWithRangeAction() {
        final String rangeActionId = "range-action-id";
        final String networkElementId = "network-element-id";
        final int minTap = -10;
        final int maxTap = 16;
        final int currentTap = 5;
        final double referenceFlow1 = 500.;
        final double referenceFlow2 = 300.;
        final double cnec1toRangeSensitivity = 0.2;
        final double cnec2toRangeSensitivity = 0.5;
        final double cnec1MaxFlow = 400.;
        final double cnec2MaxFlow = 500.;
        final double cnec1MinFlow = 10.;
        final double cnec2MinFlow = 50.;
        Cnec cnec1 = new CnecMock("cnec1-id", cnec1MinFlow, cnec1MaxFlow);
        Cnec cnec2 = new CnecMock("cnec2-id", cnec2MinFlow, cnec2MaxFlow);
        when(linearRaoData.getReferenceFlow(cnec1)).thenReturn(referenceFlow1);
        when(linearRaoData.getReferenceFlow(cnec2)).thenReturn(referenceFlow2);

        cnecs.add(cnec1);
        cnecs.add(cnec2);
        RangeAction rangeAction = new RangeActionMock(rangeActionId, networkElementId, minTap, maxTap);
        when(linearRaoData.getSensitivity(cnec1, rangeAction)).thenReturn(cnec1toRangeSensitivity);
        when(linearRaoData.getSensitivity(cnec2, rangeAction)).thenReturn(cnec2toRangeSensitivity);
        rangeActions.add(rangeAction);
        TwoWindingsTransformer twoWindingsTransformer = new TwoWindingsTransformerMock(minTap, maxTap, currentTap);
        when(network.getIdentifiable(networkElementId)).thenReturn((Identifiable) twoWindingsTransformer);

        coreProblemFiller.fill();
        positiveMinMarginFiller.fill();
        assertEquals(-LinearRaoProblem.infinity(), linearRaoProblem.getMinimumMarginVariable().lb(), 0.1);
        assertEquals(LinearRaoProblem.infinity(), linearRaoProblem.getMinimumMarginVariable().ub(), 0.1);
        assertEquals(cnec1MaxFlow, linearRaoProblem.getMinimumMarginConstraint(cnec1.getId(), "max").ub(), 0.1);
        assertEquals(-cnec1MinFlow, linearRaoProblem.getMinimumMarginConstraint(cnec1.getId(), "min").ub(), 0.1);
        assertEquals(cnec2MaxFlow, linearRaoProblem.getMinimumMarginConstraint(cnec2.getId(), "max").ub(), 0.1);
        assertEquals(-cnec2MinFlow, linearRaoProblem.getMinimumMarginConstraint(cnec2.getId(), "min").ub(), 0.1);

        MPObjective objective = linearRaoProblem.getObjective();
        assertTrue(objective.maximization());
        assertEquals(1, objective.getCoefficient(linearRaoProblem.getMinimumMarginVariable()), 0.1);
        assertEquals(-LinearRaoProblem.PENALTY_COST, objective.getCoefficient(linearRaoProblem.getNegativeRangeActionVariable(rangeActionId, networkElementId)), 0.01);
        assertEquals(-LinearRaoProblem.PENALTY_COST, objective.getCoefficient(linearRaoProblem.getPositiveRangeActionVariable(rangeActionId, networkElementId)), 0.01);
    }
}
