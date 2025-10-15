/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.parameters;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.openrao.data.crac.api.CracFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class CracCreationParametersTest {

    @Test
    void defaultParametersTest() {
        CracCreationParameters parameters = new CracCreationParameters();
        assertEquals(CracFactory.findDefault().getName(), parameters.getCracFactoryName());
        assertEquals(CracFactory.findDefault().getName(), parameters.getCracFactory().getName());
        assertTrue(parameters.getRaUsageLimitsPerInstant().isEmpty());
        assertEquals(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_BOTH_SIDES, parameters.getDefaultMonitoredLineSide());
        assertTrue(parameters.getExtensions().isEmpty());
    }

    @Test
    void factoryTest() {
        CracCreationParameters parameters = new CracCreationParameters();
        parameters.setCracFactoryName("anotherCracFactory");
        assertEquals("anotherCracFactory", parameters.getCracFactoryName());
    }

    @Test
    void configLoaderTest() {
        PlatformConfig platformConfig = Mockito.mock(PlatformConfig.class);
        ModuleConfig cracCreatorModuleConfig = Mockito.mock(ModuleConfig.class);

        Mockito.when(cracCreatorModuleConfig.getStringProperty(ArgumentMatchers.eq("crac-factory"), ArgumentMatchers.any())).thenReturn("coucouCracFactory");
        Mockito.when(platformConfig.getOptionalModuleConfig("crac-creation-parameters")).thenReturn(Optional.of(cracCreatorModuleConfig));

        CracCreationParameters parameters = CracCreationParameters.load(platformConfig);

        assertEquals("coucouCracFactory", parameters.getCracFactoryName());
    }

}
