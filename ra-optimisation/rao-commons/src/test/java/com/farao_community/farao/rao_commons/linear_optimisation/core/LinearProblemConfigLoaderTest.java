/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.core;

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
public class LinearProblemConfigLoaderTest {

    private static final double DOUBLE_TOLERANCE = 0.1;

    private PlatformConfig platformConfig;
    private LinearProblemConfigLoader configLoader;

    @Before
    public void setUp() {
        platformConfig = Mockito.mock(PlatformConfig.class);
        configLoader = new LinearProblemConfigLoader();
    }

    @Test
    public void testLoad() {
        ModuleConfig linearProblemParametersModule = Mockito.mock(ModuleConfig.class);
        Mockito.when(linearProblemParametersModule.getDoubleProperty(eq("pst-penalty-cost"), anyDouble())).thenReturn(0.5);
        Mockito.when(linearProblemParametersModule.getDoubleProperty(eq("pst-sensitivity-threshold"), anyDouble())).thenReturn(2.0);

        Mockito.when(platformConfig.getOptionalModuleConfig("linear-problem-parameters")).thenReturn(Optional.of(linearProblemParametersModule));

        LinearProblemParameters parameters = configLoader.load(platformConfig);
        assertEquals(2.0, parameters.getPstSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(0.5, parameters.getPstPenaltyCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void getExtensionName() {
        assertEquals("LinearProblemParameters", configLoader.getExtensionName());
    }

    @Test
    public void getCategoryName() {
        assertEquals("rao-parameters", configLoader.getCategoryName());
    }

    @Test
    public void getExtensionClass() {
        assertEquals(LinearProblemParameters.class, configLoader.getExtensionClass());
    }
}
