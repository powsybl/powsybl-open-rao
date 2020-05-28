/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.systematic_sensitivity;

import com.farao_community.farao.rao_commons.systematic_sensitivity.parameters.SystematicSensitivityComputationConfigLoader;
import com.farao_community.farao.rao_commons.systematic_sensitivity.parameters.SystematicSensitivityComputationParameters;
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

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(SensitivityComputationParameters.class)
public class SystematicSensitivityComputationConfigLoaderTest {

    private PlatformConfig platformConfig;
    private SystematicSensitivityComputationConfigLoader configLoader;

    @Before
    public void setUp() {
        platformConfig = Mockito.mock(PlatformConfig.class);
        configLoader = new SystematicSensitivityComputationConfigLoader();
    }

    @Test
    public void testLoad() {
        SensitivityComputationParameters sensitivityComputationParameters = Mockito.mock(SensitivityComputationParameters.class);
        PowerMockito.mockStatic(SensitivityComputationParameters.class);
        BDDMockito.given(SensitivityComputationParameters.load(platformConfig)).willReturn(sensitivityComputationParameters);
        SystematicSensitivityComputationParameters raoParameters = configLoader.load(platformConfig);
        assertSame(sensitivityComputationParameters, raoParameters.getDefaultParameters());
    }

    @Test
    public void getExtensionName() {
        assertEquals("SystematicSensitivityComputationParameters", configLoader.getExtensionName());
    }

    @Test
    public void getCategoryName() {
        assertEquals("rao-parameters", configLoader.getCategoryName());
    }

    @Test
    public void getExtensionClass() {
        assertEquals(SystematicSensitivityComputationParameters.class, configLoader.getExtensionClass());
    }
}
