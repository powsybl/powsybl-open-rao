/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.linear_rao.mocks.MPSolverMock;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.FileSystem;
import java.util.*;

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
        LinearRaoData linearRaoDataMock = Mockito.mock(LinearRaoData.class);
        List<AbstractProblemFiller> fillers = new ArrayList<>();
        List<AbstractPostProcessor> postProcessors = new ArrayList<>();

        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);
        RaoParameters raoParameters = RaoParameters.load(platformConfig);

        linearRaoModeller = new LinearRaoModeller(linearRaoProblemMock, linearRaoDataMock, fillers, postProcessors, raoParameters);
    }

    @Test
    public void testOptimalSolve() {
        Mockito.when(linearRaoProblemMock.solve()).thenReturn(MPSolverMock.ResultStatusMock.OPTIMAL);

        linearRaoModeller.buildProblem();
        RaoComputationResult raoComputationResult = linearRaoModeller.solve();
        assertNotNull(raoComputationResult);
        assertEquals(RaoComputationResult.Status.SUCCESS, raoComputationResult.getStatus());
    }

    @Test
    public void testUnboundedSolve() {
        Mockito.when(linearRaoProblemMock.solve()).thenReturn(MPSolverMock.ResultStatusMock.UNBOUNDED);

        linearRaoModeller.buildProblem();
        RaoComputationResult raoComputationResult = linearRaoModeller.solve();
        assertNotNull(raoComputationResult);
        assertEquals(RaoComputationResult.Status.FAILURE, raoComputationResult.getStatus());
    }
}
