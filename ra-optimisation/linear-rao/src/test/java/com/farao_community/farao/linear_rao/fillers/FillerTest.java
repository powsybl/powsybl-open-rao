/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.farao_community.farao.linear_rao.mocks.MPSolverMock;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@PrepareForTest(MPSolver.class)
public class FillerTest {

    protected CoreProblemFiller coreProblemFiller;
    protected LinearRaoProblem linearRaoProblem;
    protected LinearRaoData linearRaoData;
    protected Crac crac;
    protected Network network;

    protected static final String RANGE_ACTION_ID = "PRA_PST_BE";
    protected static final String CNEC_1_ID = "Tieline BE FR - N - preventive";
    protected static final String CNEC_2_ID = "Tieline BE FR - Défaut - N-1 NL1-NL3";


    protected void init() {

        // arrange some data for all fillers test
        // crac and network
        crac = CracImporters.importCrac("small-crac.json", getClass().getResourceAsStream("/small-crac.json"));
        network = Importers.loadNetwork("TestCase12Nodes.uct", getClass().getResourceAsStream("/TestCase12Nodes.uct"));
        crac.setReferenceValues(network);
        crac.synchronize(network);

        // MPSolver and linearRaoProblem
        MPSolverMock solver = new MPSolverMock();
        PowerMockito.mockStatic(MPSolver.class);
        when(MPSolver.infinity()).thenReturn(Double.POSITIVE_INFINITY);
        linearRaoProblem = new LinearRaoProblem(solver);

        // LinearRaoData
        linearRaoData = mock(LinearRaoData.class);
        when(linearRaoData.getCrac()).thenReturn(crac);
        when(linearRaoData.getNetwork()).thenReturn(network);
    }
}
