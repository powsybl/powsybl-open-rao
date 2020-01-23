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
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.Identifiable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class FillerTest {

    protected CoreProblemFiller coreProblemFiller;
    protected LinearRaoProblem linearRaoProblem;
    protected LinearRaoData linearRaoData;
    protected Crac crac;
    protected Network network;
    protected State preventiveState;

    protected final double currentAlpha = 1.;
    protected final double minAlpha = -3.;
    protected final double maxAlpha = 5.;

    protected RangeAction rangeAction = mock(RangeAction.class);
    protected NetworkElement networkElement = mock(NetworkElement.class);

    protected final double referenceFlow1 = 500.;
    protected final double referenceFlow2 = 300.;
    protected Cnec cnec1 = mock(Cnec.class);
    protected Cnec cnec2 = mock(Cnec.class);

    protected void init() {
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

    protected void initRangeAction() {
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

    protected void initCnec() throws SynchronizationException {
        final Optional<Double> minThreshold = Optional.of(1d);
        final Optional<Double> maxThreshold = Optional.of(100d);

        Threshold threshold = mock(Threshold.class);

        when(cnec1.getId()).thenReturn("cnec1-id");
        when(cnec1.getThreshold()).thenReturn(threshold);
        when(cnec1.getThreshold().getMinThreshold()).thenReturn(minThreshold);
        when(cnec1.getThreshold().getMaxThreshold()).thenReturn(maxThreshold);
        when(linearRaoData.getReferenceFlow(cnec1)).thenReturn(referenceFlow1);
        when(cnec2.getId()).thenReturn("cnec2-id");
        when(cnec2.getThreshold()).thenReturn(threshold);
        when(cnec2.getThreshold().getMinThreshold()).thenReturn(minThreshold);
        when(cnec2.getThreshold().getMaxThreshold()).thenReturn(maxThreshold);
        when(linearRaoData.getReferenceFlow(cnec2)).thenReturn(referenceFlow2);
        Set<Cnec> cnecs = new HashSet<>();
        cnecs.add(cnec1);
        cnecs.add(cnec2);
        when(crac.getCnecs()).thenReturn(cnecs);
    }

    protected void initBoth() throws SynchronizationException {
        initRangeAction();
        initCnec();
    }
}
