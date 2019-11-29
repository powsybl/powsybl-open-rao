/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtension;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
public class RaoComputationParametersTest {

    protected PlatformConfig config;

    @Before
    public void setUp() {
        config = Mockito.mock(PlatformConfig.class);
    }

    @Test
    public void testExtensions() {
        RaoComputationParameters parameters = new RaoComputationParameters();
        DummyExtension dummyExtension = new DummyExtension();
        parameters.addExtension(DummyExtension.class, dummyExtension);

        assertEquals(1, parameters.getExtensions().size());
        assertEquals(true, parameters.getExtensions().contains(dummyExtension));
        assertEquals(true, parameters.getExtensionByName("dummyExtension") instanceof DummyExtension);
        assertEquals(true, parameters.getExtension(DummyExtension.class) instanceof DummyExtension);
    }

    @Test
    public void testNoExtensions() {
        RaoComputationParameters parameters = new RaoComputationParameters();

        assertEquals(0, parameters.getExtensions().size());
        assertEquals(false, parameters.getExtensions().contains(new DummyExtension()));
        assertEquals(false, parameters.getExtensionByName("dummyExtension") instanceof DummyExtension);
        assertEquals(false, parameters.getExtension(DummyExtension.class) instanceof DummyExtension);
    }

    @Test
    public void testExtensionFromConfig() {
        RaoComputationParameters parameters = RaoComputationParameters.load(config);

/*        assertEquals(1, parameters.getExtensions().size());
        assertEquals(true, parameters.getExtensionByName("dummyExtension") instanceof DummyExtension);
        assertNotNull(parameters.getExtension(DummyExtension.class));*/
    }

    private static class DummyExtension extends AbstractExtension<RaoComputationParameters> {

        @Override
        public String getName() {
            return "dummyExtension";
        }
    }

    @AutoService(RaoComputationParameters.ConfigLoader.class)
    public static class DummyLoader implements RaoComputationParameters.ConfigLoader<DummyExtension> {

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
            return "ra_optimisation-computation-parameters";
        }

        @Override
        public Class<? super DummyExtension> getExtensionClass() {
            return DummyExtension.class;
        }
    }
}
