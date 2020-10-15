/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.iidm.network.Country;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RaoPtdfParametersConfigLoaderTest {

    private PlatformConfig platformConfig;
    private RaoPtdfParametersConfigLoader configLoader;

    @Before
    public void setUp() {
        platformConfig = Mockito.mock(PlatformConfig.class);
        configLoader = new RaoPtdfParametersConfigLoader();
    }

    @Test
    public void testLoad() {
        ModuleConfig ptdfParametersModule = Mockito.mock(ModuleConfig.class);
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("FR-ES", "ES-PT"));
        Mockito.when(ptdfParametersModule.getStringListProperty(eq("boundaries"))).thenReturn(stringBoundaries);
        Mockito.when(platformConfig.getOptionalModuleConfig("rao-ptdf-parameters")).thenReturn(Optional.of(ptdfParametersModule));
        RaoPtdfParameters ptdfParameters = configLoader.load(platformConfig);
        assertEquals(2, ptdfParameters.getBoundaries().size());
        assertTrue(ptdfParameters.getBoundaries().contains(new ImmutablePair<>(Country.FR, Country.ES)));
        assertTrue(ptdfParameters.getBoundaries().contains(new ImmutablePair<>(Country.ES, Country.PT)));
    }

    @Test
    public void getExtensionName() {
        assertEquals("RaoPtdfParameters", configLoader.getExtensionName());
    }

    @Test
    public void getCategoryName() {
        assertEquals("rao-parameters", configLoader.getCategoryName());
    }

    @Test
    public void getExtensionClass() {
        assertEquals(RaoPtdfParameters.class, configLoader.getExtensionClass());
    }

}
