/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.parameters.IteratingLinearOptimizerConfigLoader;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.parameters.IteratingLinearOptimizerParameters;
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
public class IteratingLinearOptimizerConfigLoaderTest {

    private PlatformConfig platformConfig;
    private IteratingLinearOptimizerConfigLoader configLoader;

    @Before
    public void setUp() {
        platformConfig = Mockito.mock(PlatformConfig.class);
        configLoader = new IteratingLinearOptimizerConfigLoader();
    }

    @Test
    public void testLoad() {
        ModuleConfig iteratingLinearOptimizerModule = Mockito.mock(ModuleConfig.class);
        Mockito.when(iteratingLinearOptimizerModule.getIntProperty(eq("max-number-of-iterations"), anyInt())).thenReturn(25);

        Mockito.when(platformConfig.getOptionalModuleConfig("iterating-linear-optimizer-parameters")).thenReturn(Optional.of(iteratingLinearOptimizerModule));

        IteratingLinearOptimizerParameters parameters = configLoader.load(platformConfig);
        assertEquals(25, parameters.getMaxIterations());
    }

    @Test
    public void getExtensionName() {
        assertEquals("IteratingLinearOptimizerParameters", configLoader.getExtensionName());
    }

    @Test
    public void getCategoryName() {
        assertEquals("rao-parameters", configLoader.getCategoryName());
    }

    @Test
    public void getExtensionClass() {
        assertEquals(IteratingLinearOptimizerParameters.class, configLoader.getExtensionClass());
    }
}
