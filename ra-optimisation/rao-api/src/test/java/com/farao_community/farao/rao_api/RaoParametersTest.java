/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api;

import com.google.auto.service.AutoService;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtension;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.FileSystem;
import java.util.Objects;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoParametersTest {

    private PlatformConfig config;
    private InMemoryPlatformConfig platformCfg;
    private FileSystem fileSystem;

    @Before
    public void setUp() {
        config = Mockito.mock(PlatformConfig.class);
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformCfg = new InMemoryPlatformConfig(fileSystem);
    }

    @Test
    public void testExtensions() {
        RaoParameters parameters = new RaoParameters();
        DummyExtension dummyExtension = new DummyExtension();
        parameters.addExtension(DummyExtension.class, dummyExtension);

        assertEquals(1, parameters.getExtensions().size());
        assertTrue(parameters.getExtensions().contains(dummyExtension));
        assertTrue(parameters.getExtensionByName("dummyExtension") instanceof DummyExtension);
        assertNotNull(parameters.getExtension(DummyExtension.class));
    }

    @Test
    public void testNoExtensions() {
        RaoParameters parameters = new RaoParameters();

        assertEquals(0, parameters.getExtensions().size());
        assertFalse(parameters.getExtensions().contains(new DummyExtension()));
        assertFalse(parameters.getExtensionByName("dummyExtension") instanceof DummyExtension);
        assertNull(parameters.getExtension(DummyExtension.class));
    }

    @Test
    public void checkConfig() {

        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("rao-with-loop-flow-limitation", Boolean.toString(false));
        moduleConfig.setStringProperty("loopflow-approximation", Boolean.toString(false));
        moduleConfig.setStringProperty("loopflow-constraint-adjustment-coefficient", Objects.toString(0.0));
        moduleConfig.setStringProperty("loopflow-violation-cost", Objects.toString(0.0));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertEquals(false, parameters.isRaoWithLoopFlowLimitation());
    }

    @Test
    public void testExtensionFromConfig() {
        RaoParameters parameters = RaoParameters.load(config);

        assertEquals(1, parameters.getExtensions().size());
        assertTrue(parameters.getExtensionByName("dummyExtension") instanceof DummyExtension);
        assertNotNull(parameters.getExtension(DummyExtension.class));
    }

    private static class DummyExtension extends AbstractExtension<RaoParameters> {

        @Override
        public String getName() {
            return "dummyExtension";
        }
    }

    @AutoService(RaoParameters.ConfigLoader.class)
    public static class DummyLoader implements RaoParameters.ConfigLoader<DummyExtension> {

        @Override
        public DummyExtension load(PlatformConfig platformConfig) {
            return new DummyExtension();
        }

        @Override
        public String getExtensionName() {
            return "dummyExtension";
        }

        @Override
        public String getCategoryName() {
            return "rao-parameters";
        }

        @Override
        public Class<? super DummyExtension> getExtensionClass() {
            return DummyExtension.class;
        }
    }
}
