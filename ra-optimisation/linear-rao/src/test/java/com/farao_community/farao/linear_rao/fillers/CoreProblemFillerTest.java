/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.farao_community.farao.linear_rao.mocks.MPSolverMock;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.Identifiable;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CoreProblemFillerTest {

    private CoreProblemFiller coreProblemFiller;
    private LinearRaoProblem linearRaoProblem;
    private LinearRaoData linearRaoData;
    private Crac crac;
    private Network network;
    private State preventiveState;

    private final double currentAlpha = 1.;
    private final double minAlpha = -3.;
    private final double maxAlpha = 5.;

    private RangeAction rangeAction = mock(RangeAction.class);
    private NetworkElement networkElement = mock(NetworkElement.class);

    private final double referenceFlow1 = 500.;
    private final double referenceFlow2 = 300.;
    private Cnec cnec1 = mock(Cnec.class);
    private Cnec cnec2 = mock(Cnec.class);

    @Before
    public void setUp() {
        coreProblemFiller = new CoreProblemFiller();
        MPSolverMock solver = new MPSolverMock();
        linearRaoProblem = spy(new LinearRaoProblem(solver));
        linearRaoData = mock(LinearRaoData.class);
        crac = mock(Crac.class);
        network = mock(Network.class);
        preventiveState = mock(State.class);

        when(linearRaoData.getCrac()).thenReturn(crac);
        when(linearRaoData.getNetwork()).thenReturn(network);
        when(crac.getPreventiveState()).thenReturn(preventiveState);
    }

    private void initRangeAction() {
        final String rangeActionId = "range-action-id";
        final String networkElementId = "network-element-id";
        final int minTap = -10;
        final int maxTap = 16;

        ApplicableRangeAction applicableRangeAction = mock(ApplicableRangeAction.class);
        TwoWindingsTransformer twoWindingsTransformer = mock(TwoWindingsTransformer.class);
        PhaseTapChanger phaseTapChanger = mock(PhaseTapChanger.class);
        PhaseTapChangerStep currentTapChanger = mock(PhaseTapChangerStep.class);
        PhaseTapChangerStep minTapChanger = mock(PhaseTapChangerStep.class);
        PhaseTapChangerStep maxTapChanger = mock(PhaseTapChangerStep.class);

        when(crac.getRangeActions(network, preventiveState, UsageMethod.AVAILABLE)).thenReturn(Collections.singleton(rangeAction));
        when(rangeAction.getMinValue(network)).thenReturn((double) minTap);
        when(rangeAction.getMaxValue(network)).thenReturn((double) maxTap);
        when(rangeAction.getApplicableRangeActions()).thenReturn(Collections.singleton(applicableRangeAction));
        when(rangeAction.getId()).thenReturn(rangeActionId);
        when(applicableRangeAction.getNetworkElements()).thenReturn(Collections.singleton(networkElement));
        when(networkElement.getId()).thenReturn(networkElementId);
        when(network.getIdentifiable(networkElementId)).thenReturn((Identifiable) twoWindingsTransformer);
        when(twoWindingsTransformer.getPhaseTapChanger()).thenReturn(phaseTapChanger);
        when(phaseTapChanger.getCurrentStep()).thenReturn(currentTapChanger);
        when(phaseTapChanger.getStep(maxTap)).thenReturn(maxTapChanger);
        when(phaseTapChanger.getStep(minTap)).thenReturn(minTapChanger);
        when(currentTapChanger.getAlpha()).thenReturn(currentAlpha);
        when(maxTapChanger.getAlpha()).thenReturn(maxAlpha);
        when(minTapChanger.getAlpha()).thenReturn(minAlpha);
    }

    private void initCnec() {
        when(cnec1.getId()).thenReturn("cnec1-id");
        when(linearRaoData.getReferenceFlow(cnec1)).thenReturn(referenceFlow1);
        when(cnec2.getId()).thenReturn("cnec2-id");
        when(linearRaoData.getReferenceFlow(cnec2)).thenReturn(referenceFlow2);
        Set<Cnec> cnecs = new HashSet<>();
        cnecs.add(cnec1);
        cnecs.add(cnec2);
        when(crac.getCnecs()).thenReturn(cnecs);
    }

    private void initBoth() {
        initRangeAction();
        initCnec();
    }

    @Test
    public void fillWithRangeAction() {
        initRangeAction();
        coreProblemFiller.fill(linearRaoProblem, linearRaoData);

        MPVariable variableNegative = linearRaoProblem.getNegativePstShiftVariable(rangeAction.getId(), networkElement.getId());
        assertNotNull(variableNegative);
        assertEquals(0, variableNegative.lb(), 0.01);
        assertEquals(Math.abs(currentAlpha - minAlpha), variableNegative.ub(), 0.01);

        MPVariable variablePositive = linearRaoProblem.getPositivePstShiftVariable(rangeAction.getId(), networkElement.getId());
        assertNotNull(variablePositive);
        assertEquals(0, variablePositive.lb(), 0.01);
        assertEquals(Math.abs(currentAlpha - maxAlpha), variablePositive.ub(), 0.01);
    }

    @Test
    public void fillWithCnec() {
        initCnec();
        coreProblemFiller.fill(linearRaoProblem, linearRaoData);

        MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec1.getId());
        assertNotNull(flowVariable);
        assertEquals(-Double.MAX_VALUE, flowVariable.lb(), 0.01);
        assertEquals(Double.MAX_VALUE, flowVariable.ub(), 0.01);

        MPConstraint flowConstraint = linearRaoProblem.getFlowConstraint(cnec1.getId());
        assertNotNull(flowConstraint);
        assertEquals(referenceFlow1, flowConstraint.lb(), 0.1);
        assertEquals(referenceFlow1, flowConstraint.ub(), 0.1);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);

        MPVariable flowVariable2 = linearRaoProblem.getFlowVariable(cnec2.getId());
        assertNotNull(flowVariable2);
        assertEquals(-Double.MAX_VALUE, flowVariable2.lb(), 0.01);
        assertEquals(Double.MAX_VALUE, flowVariable2.ub(), 0.01);

        MPConstraint flowConstraint2 = linearRaoProblem.getFlowConstraint(cnec2.getId());
        assertNotNull(flowConstraint2);
        assertEquals(referenceFlow2, flowConstraint2.lb(), 0.1);
        assertEquals(referenceFlow2, flowConstraint2.ub(), 0.1);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), 0.1);
    }

    @Test
    public void fillRaAndCnec() {
        initBoth();
        coreProblemFiller.fill(linearRaoProblem, linearRaoData);

        MPVariable variableNegative = linearRaoProblem.getNegativePstShiftVariable(rangeAction.getId(), networkElement.getId());
        assertNotNull(variableNegative);
        assertEquals(0, variableNegative.lb(), 0.01);
        assertEquals(Math.abs(currentAlpha - minAlpha), variableNegative.ub(), 0.01);

        MPVariable variablePositive = linearRaoProblem.getPositivePstShiftVariable(rangeAction.getId(), networkElement.getId());
        assertNotNull(variablePositive);
        assertEquals(0, variablePositive.lb(), 0.01);
        assertEquals(Math.abs(currentAlpha - maxAlpha), variablePositive.ub(), 0.01);

        MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec1.getId());
        assertNotNull(flowVariable);
        assertEquals(-Double.MAX_VALUE, flowVariable.lb(), 0.01);
        assertEquals(Double.MAX_VALUE, flowVariable.ub(), 0.01);

        MPConstraint flowConstraint = linearRaoProblem.getFlowConstraint(cnec1.getId());
        assertNotNull(flowConstraint);
        assertEquals(referenceFlow1, flowConstraint.lb(), 0.1);
        assertEquals(referenceFlow1, flowConstraint.ub(), 0.1);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);

        MPVariable flowVariable2 = linearRaoProblem.getFlowVariable(cnec2.getId());
        assertNotNull(flowVariable2);
        assertEquals(-Double.MAX_VALUE, flowVariable2.lb(), 0.01);
        assertEquals(Double.MAX_VALUE, flowVariable2.ub(), 0.01);

        MPConstraint flowConstraint2 = linearRaoProblem.getFlowConstraint(cnec2.getId());
        assertNotNull(flowConstraint2);
        assertEquals(referenceFlow2, flowConstraint2.lb(), 0.1);
        assertEquals(referenceFlow2, flowConstraint2.ub(), 0.1);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), 0.1);
    }
}
