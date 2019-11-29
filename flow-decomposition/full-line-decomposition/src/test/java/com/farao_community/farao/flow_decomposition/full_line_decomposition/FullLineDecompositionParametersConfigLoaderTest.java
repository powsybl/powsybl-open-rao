/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.farao_community.farao.flow_decomposition.full_line_decomposition.FullLineDecompositionParameters.InjectionStrategy;
import com.farao_community.farao.flow_decomposition.full_line_decomposition.FullLineDecompositionParameters.PstStrategy;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.Objects;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FullLineDecompositionParametersConfigLoaderTest {
    @Test
    public void getExtensionName() throws Exception {
        FullLineDecompositionParametersConfigLoader configLoader = new FullLineDecompositionParametersConfigLoader();
        assertEquals("FullLineDecompositionParameters", configLoader.getExtensionName());
    }

    @Test
    public void getCategoryName() throws Exception {
        FullLineDecompositionParametersConfigLoader configLoader = new FullLineDecompositionParametersConfigLoader();
        assertEquals("flow-decomposition-parameters", configLoader.getCategoryName());
    }

    @Test
    public void getExtensionClass() throws Exception {
        FullLineDecompositionParametersConfigLoader configLoader = new FullLineDecompositionParametersConfigLoader();
        assertEquals(FullLineDecompositionParameters.class, configLoader.getExtensionClass());
    }

    @Test(expected = NullPointerException.class)
    public void checkThrowsWhenNullPlatformConfig() throws Exception {
        FullLineDecompositionParametersConfigLoader configLoader = new FullLineDecompositionParametersConfigLoader();
        configLoader.load(null);
    }

    @Test
    public void load() throws Exception {
        try (FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix())) {
            InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);

            InjectionStrategy injectionStrategy = InjectionStrategy.DECOMPOSE_INJECTIONS;
            double pexMatrixTolerance = 1e-2;
            int threadsNumber = 10;
            PstStrategy pstStrategy = PstStrategy.NEUTRAL_TAP;

            MapModuleConfig mapModuleConfig = platformConfig.createModuleConfig("full-line-decomposition-parameters");
            mapModuleConfig.setStringProperty("injectionStrategy", Objects.toString(injectionStrategy));
            mapModuleConfig.setStringProperty("pexMatrixTolerance", Objects.toString(pexMatrixTolerance));
            mapModuleConfig.setStringProperty("threadsNumber", Objects.toString(threadsNumber));
            mapModuleConfig.setStringProperty("pstStrategy", Objects.toString(pstStrategy));

            FullLineDecompositionParametersConfigLoader configLoader = new FullLineDecompositionParametersConfigLoader();
            FullLineDecompositionParameters parameters = configLoader.load(platformConfig);

            assertEquals(InjectionStrategy.DECOMPOSE_INJECTIONS, parameters.getInjectionStrategy());
            assertEquals(1e-2, parameters.getPexMatrixTolerance(), 1e-4);
            assertEquals(10, parameters.getThreadsNumber());
            assertEquals(PstStrategy.NEUTRAL_TAP, parameters.getPstStrategy());
        }
    }

}
