/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.config;

import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.commons.config.PlatformConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SearchTreeRaoParametersTest {

    private RaoParameters parameters;

    @Before
    public void setUp() {
        PlatformConfig config = Mockito.mock(PlatformConfig.class);
        parameters = RaoParameters.load(config);
    }

    @Test
    public void testExtensionRecognition() {
        assertTrue(parameters.getExtensionByName("SearchTreeRaoParameters") instanceof SearchTreeRaoParameters);
        assertNotNull(parameters.getExtension(SearchTreeRaoParameters.class));
    }

    @Test
    public void testLoopFlowExtensionParameters() {
        parameters.addExtension(SearchTreeRaoParameters.class, new SearchTreeRaoParameters());
        SearchTreeRaoParameters searchTreeRaoParameters = parameters.getExtension(SearchTreeRaoParameters.class);
    }
}
