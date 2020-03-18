/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.linear_rao.mocks.MPSolverMock;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class LinearRaoModellerTest {
    private LinearRaoModeller linearRaoModeller;
    private LinearRaoProblem linearRaoProblemMock;

    @Before
    public void setUp() {
        linearRaoProblemMock = Mockito.mock(LinearRaoProblem.class);

        Crac crac = new SimpleCrac("id");
        crac.addState(new SimpleState(Optional.empty(), new Instant("preventive", 0)));
        Network networkMock = Mockito.mock(Network.class);
        SystematicSensitivityAnalysisResult sensitivityResultMock = Mockito.mock(SystematicSensitivityAnalysisResult.class);

        linearRaoModeller = new LinearRaoModeller(crac, networkMock, sensitivityResultMock, linearRaoProblemMock);
    }

    @Test
    public void testOptimalSolve() {
        Mockito.when(linearRaoProblemMock.solve()).thenReturn(MPSolverMock.ResultStatusMock.OPTIMAL);

        RaoResult raoResult = linearRaoModeller.solve("");
        assertNotNull(raoResult);
        assertEquals(RaoResult.Status.SUCCESS, raoResult.getStatus());
    }

    @Test
    public void testUnboundedSolve() {
        Mockito.when(linearRaoProblemMock.solve()).thenReturn(MPSolverMock.ResultStatusMock.UNBOUNDED);

        RaoResult raoResult = linearRaoModeller.solve("");
        assertNotNull(raoResult);
        assertEquals(RaoResult.Status.FAILURE, raoResult.getStatus());
    }
}
