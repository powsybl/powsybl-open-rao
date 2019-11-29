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
import com.farao_community.farao.flow_decomposition.FlowDecompositionParameters;
import com.farao_community.farao.flow_decomposition.full_line_decomposition.FullLineDecompositionParameters.InjectionStrategy;
import com.farao_community.farao.flow_decomposition.full_line_decomposition.FullLineDecompositionParameters.PstStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.Objects;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FullLineDecompositionParametersTest {

    private InMemoryPlatformConfig platformConfig;
    private FileSystem fileSystem;
    private FullLineDecompositionParametersConfigLoader parametersConfigLoader;

    @Before
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        parametersConfigLoader = new FullLineDecompositionParametersConfigLoader();
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    public void checkValues(FullLineDecompositionParameters parameters, InjectionStrategy injectionStrategy, double pexMatrixTolerance, int threadsNumber, PstStrategy pstStrategy) {
        assertEquals(injectionStrategy, parameters.getInjectionStrategy());
        assertEquals(pexMatrixTolerance, parameters.getPexMatrixTolerance(), 1e-4);
        assertEquals(threadsNumber, parameters.getThreadsNumber());
        assertEquals(pstStrategy, parameters.getPstStrategy());
    }

    @Test
    public void testNoConfig() {
        FullLineDecompositionParameters parameters = parametersConfigLoader.load(platformConfig);
        checkValues(parameters,
                FullLineDecompositionParameters.DEFAULT_INJECTION_STRATEGY,
                FullLineDecompositionParameters.DEFAULT_PEX_MATRIX_TOLERANCE,
                FullLineDecompositionParameters.DEFAULT_THREADS_NUMBER,
                FullLineDecompositionParameters.DEFAULT_PST_STRATEGY);
    }

    @Test
    public void checkConfig() {
        InjectionStrategy injectionStrategy = InjectionStrategy.DECOMPOSE_INJECTIONS;
        double pexMatrixTolerance = 1e-2;
        int threadsNumber = 10;
        PstStrategy pstStrategy = PstStrategy.NEUTRAL_TAP;

        MapModuleConfig mapModuleConfig = platformConfig.createModuleConfig("full-line-decomposition-parameters");
        mapModuleConfig.setStringProperty("injectionStrategy", Objects.toString(injectionStrategy));
        mapModuleConfig.setStringProperty("pexMatrixTolerance", Objects.toString(pexMatrixTolerance));
        mapModuleConfig.setStringProperty("threadsNumber", Objects.toString(threadsNumber));
        mapModuleConfig.setStringProperty("pstStrategy", Objects.toString(pstStrategy));

        FullLineDecompositionParameters parameters = parametersConfigLoader.load(platformConfig);
        checkValues(parameters, injectionStrategy, pexMatrixTolerance, threadsNumber, pstStrategy);
    }

    @Test
    public void checkIncompleteConfig() {
        int threadsNumber = 10;
        PstStrategy pstStrategy = PstStrategy.NEUTRAL_TAP;

        MapModuleConfig mapModuleConfig = platformConfig.createModuleConfig("full-line-decomposition-parameters");
        mapModuleConfig.setStringProperty("threadsNumber", Objects.toString(threadsNumber));
        mapModuleConfig.setStringProperty("pstStrategy", Objects.toString(pstStrategy));

        FullLineDecompositionParameters parameters = parametersConfigLoader.load(platformConfig);
        checkValues(parameters,
                FullLineDecompositionParameters.DEFAULT_INJECTION_STRATEGY,
                FullLineDecompositionParameters.DEFAULT_PEX_MATRIX_TOLERANCE,
                threadsNumber,
                pstStrategy);
    }

    @Test
    public void checkSetters() {
        InjectionStrategy injectionStrategy = InjectionStrategy.DECOMPOSE_INJECTIONS;
        double pexMatrixTolerance = 1e-2;
        int threadsNumber = 10;
        PstStrategy pstStrategy = PstStrategy.NEUTRAL_TAP;

        FullLineDecompositionParameters parameters = new FullLineDecompositionParameters();

        parameters.setInjectionStrategy(injectionStrategy);
        parameters.setPexMatrixTolerance(pexMatrixTolerance);
        parameters.setThreadsNumber(threadsNumber);
        parameters.setPstStrategy(pstStrategy);

        checkValues(parameters, injectionStrategy, pexMatrixTolerance, threadsNumber, pstStrategy);
    }

    @Test
    public void checkClone() {
        InjectionStrategy injectionStrategy = InjectionStrategy.DECOMPOSE_INJECTIONS;
        double pexMatrixTolerance = 1e-2;
        int threadsNumber = 10;
        PstStrategy pstStrategy = PstStrategy.NEUTRAL_TAP;

        MapModuleConfig mapModuleConfig = platformConfig.createModuleConfig("full-line-decomposition-parameters");
        mapModuleConfig.setStringProperty("injectionStrategy", Objects.toString(injectionStrategy));
        mapModuleConfig.setStringProperty("pexMatrixTolerance", Objects.toString(pexMatrixTolerance));
        mapModuleConfig.setStringProperty("threadsNumber", Objects.toString(threadsNumber));
        mapModuleConfig.setStringProperty("pstStrategy", Objects.toString(pstStrategy));

        FullLineDecompositionParameters parameters = parametersConfigLoader.load(platformConfig);
        FullLineDecompositionParameters parametersCloned = new FullLineDecompositionParameters(parameters);

        checkValues(parametersCloned, injectionStrategy, pexMatrixTolerance, threadsNumber, pstStrategy);
    }

    @Test
    public void testExtensions() {
        platformConfig.createModuleConfig("full-line-decomposition-parameters");
        FlowDecompositionParameters flowDecompositionParameters = FlowDecompositionParameters.load(platformConfig);

        assertEquals(1, flowDecompositionParameters.getExtensions().size());
        assertEquals(FullLineDecompositionParameters.class, flowDecompositionParameters.getExtensionByName("FullLineDecompositionParameters").getClass());
        assertNotNull(flowDecompositionParameters.getExtension(FullLineDecompositionParameters.class));
        assertEquals(new FullLineDecompositionParameters().toString().trim(), flowDecompositionParameters.getExtension(FullLineDecompositionParameters.class).toString().trim());
        assertTrue(flowDecompositionParameters.getExtensionByName("FullLineDecompositionParameters") instanceof FullLineDecompositionParameters);
    }

    @Test
    public void testNoExtensions() {
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();

        assertEquals(0, flowDecompositionParameters.getExtensions().size());
        assertFalse(flowDecompositionParameters.getExtensionByName("FullLineDecompositionParameters") instanceof FullLineDecompositionParameters);
        assertNull(flowDecompositionParameters.getExtension(FullLineDecompositionParameters.class));
    }

}
