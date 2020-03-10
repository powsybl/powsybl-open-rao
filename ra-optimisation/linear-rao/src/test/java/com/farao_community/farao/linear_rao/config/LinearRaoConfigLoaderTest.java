/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.config;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(SensitivityComputationParameters.class)
public class LinearRaoConfigLoaderTest {

    private PlatformConfig platformConfig;
    private LinearRaoConfigLoader configLoader;

    @Before
    public void setUp() throws Exception {
        platformConfig = Mockito.mock(PlatformConfig.class);
        configLoader = new LinearRaoConfigLoader();
    }

    @Test
    public void testLoad() {
        SensitivityComputationParameters sensitivityComputationParameters = Mockito.mock(SensitivityComputationParameters.class);
        PowerMockito.mockStatic(SensitivityComputationParameters.class);
        BDDMockito.given(SensitivityComputationParameters.load(platformConfig)).willReturn(sensitivityComputationParameters);

        ModuleConfig linearRaoParametersModule = Mockito.mock(ModuleConfig.class);
        Mockito.when(linearRaoParametersModule.getIntProperty(eq("max-number-of-iterations"), anyInt())).thenReturn(25);
        Mockito.when(platformConfig.getOptionalModuleConfig("linear-rao-parameters")).thenReturn(Optional.of(linearRaoParametersModule));

        LinearRaoParameters raoParameters = configLoader.load(platformConfig);
        assertSame(sensitivityComputationParameters, raoParameters.getSensitivityComputationParameters());
        assertEquals(25, raoParameters.getMaxIterations());
    }

    @Test
    public void getExtensionName() {
        assertEquals("LinearRaoParameters", configLoader.getExtensionName());
    }

    @Test
    public void getCategoryName() {
        assertEquals("rao-parameters", configLoader.getCategoryName());
    }

    @Test
    public void getExtensionClass() {
        assertEquals(LinearRaoParameters.class, configLoader.getExtensionClass());
    }
}
