/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystem;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SystematicSensitivityAnalysisService.class})
public class LinearRaoModellerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinearRaoTest.class);

    private LinearRaoModeller linearRaoModeller;

    @Before
    public void setUp() {
        LinearRaoProblem linearRaoProblemMock = Mockito.mock(LinearRaoProblem.class);
        LinearRaoData linearRaoDataMock = Mockito.mock(LinearRaoData.class);
        List<AbstractProblemFiller> fillers = new ArrayList<>();
        List<AbstractPostProcessor> postProcessors = new ArrayList<>();

        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);
        RaoParameters raoParameters = RaoParameters.load(platformConfig);

        linearRaoModeller = new LinearRaoModeller(linearRaoProblemMock, linearRaoDataMock, fillers, postProcessors, raoParameters);
    }

    @Test
    public void testSolve() {
        assertNotNull(linearRaoModeller.solve());
    }

    @Test
    public void testBuild() {
        linearRaoModeller.buildProblem();
    }
}
