/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearRaoConfigLoaderTest {

    private static final double DOUBLE_TOLERANCE = 0.1;

    private PlatformConfig platformConfig;
    private LinearRaoConfigLoader configLoader;

    @Before
    public void setUp() {
        platformConfig = Mockito.mock(PlatformConfig.class);
        configLoader = new LinearRaoConfigLoader();
    }

    @Test
    public void testLoad() {
        ModuleConfig linearRaoParametersModule = Mockito.mock(ModuleConfig.class);
        Mockito.when(linearRaoParametersModule.getBooleanProperty(eq("security-analysis-without-rao"), anyBoolean())).thenReturn(false);
        Mockito.when(linearRaoParametersModule.getEnumProperty(eq("objective-function"), eq(LinearRaoParameters.ObjectiveFunction.class), any())).thenReturn(LinearRaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT);
        Mockito.when(linearRaoParametersModule.getDoubleProperty(eq("sensitivity-fallback-overcost"), anyDouble())).thenReturn(100.0);

        Mockito.when(platformConfig.getOptionalModuleConfig("linear-rao-parameters")).thenReturn(Optional.of(linearRaoParametersModule));

        LinearRaoParameters raoParameters = configLoader.load(platformConfig);
        assertFalse(raoParameters.isSecurityAnalysisWithoutRao());
        assertEquals(LinearRaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT, raoParameters.getObjectiveFunction());
        assertEquals(100.0, raoParameters.getFallbackOvercost(), DOUBLE_TOLERANCE);
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
