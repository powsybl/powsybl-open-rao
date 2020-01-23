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

import java.util.HashSet;
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
    protected Set<RangeAction> rangeActions;
    protected Set<Cnec> cnecs;

    protected void init() {
        MPSolverMock solver = new MPSolverMock();
        linearRaoProblem = spy(new LinearRaoProblem(solver));
        linearRaoData = mock(LinearRaoData.class);
        crac = mock(Crac.class);
        network = mock(Network.class);
        preventiveState = mock(State.class);
        rangeActions = new HashSet<>();
        cnecs = new HashSet<>();

        when(linearRaoData.getCrac()).thenReturn(crac);
        when(linearRaoData.getNetwork()).thenReturn(network);
        when(crac.getPreventiveState()).thenReturn(preventiveState);
        when(crac.getRangeActions(network, preventiveState, UsageMethod.AVAILABLE)).thenReturn(rangeActions);
        when(crac.getCnecs()).thenReturn(cnecs);
    }
}
