/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.api.parameters;

import com.farao_community.farao.data.crac_api.CracFactory;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracCreationParametersTest {

    @Test
    public void defaultParametersTest() {
        CracCreationParameters parameters = new CracCreationParameters();
        assertEquals(CracFactory.findDefault().getName(), parameters.getCracFactoryName());
        assertEquals(CracFactory.findDefault().getName(), parameters.getCracFactory().getName());
    }

    @Test
    public void factoryTest() {
        CracCreationParameters parameters = new CracCreationParameters();
        parameters.setCracFactoryName("anotherCracFactory");
        assertEquals("anotherCracFactory", parameters.getCracFactoryName());
    }

    @Test
    public void configLoaderTest() {
        PlatformConfig platformConfig = Mockito.mock(PlatformConfig.class);
        ModuleConfig cracCreatorModuleConfig = Mockito.mock(ModuleConfig.class);

        Mockito.when(cracCreatorModuleConfig.getStringProperty(eq("crac-factory"), any())).thenReturn("coucouCracFactory");
        Mockito.when(platformConfig.getOptionalModuleConfig("crac-creation-parameters")).thenReturn(Optional.of(cracCreatorModuleConfig));

        CracCreationParameters parameters = CracCreationParameters.load(platformConfig);

        assertEquals("coucouCracFactory", parameters.getCracFactoryName());
    }

}
