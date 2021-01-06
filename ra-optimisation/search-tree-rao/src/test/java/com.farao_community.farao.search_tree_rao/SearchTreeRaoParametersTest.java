/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.commons.config.PlatformConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
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
    public void testRelativeNetworkActionMinimumImpactThresholdBounds() {
        SearchTreeRaoParameters params = new SearchTreeRaoParameters();
        params.setRelativeNetworkActionMinimumImpactThreshold(-0.5);
        assertEquals(0, params.getRelativeNetworkActionMinimumImpactThreshold(), 1e-6);
        params.setRelativeNetworkActionMinimumImpactThreshold(1.1);
        assertEquals(1, params.getRelativeNetworkActionMinimumImpactThreshold(), 1e-6);
    }

    @Test
    public void testMaxNumberOfBoundariesForSkippingNetworkActionsBounds() {
        SearchTreeRaoParameters params = new SearchTreeRaoParameters();
        params.setMaxNumberOfBoundariesForSkippingNetworkActions(300);
        assertEquals(300, params.getMaxNumberOfBoundariesForSkippingNetworkActions());
        params.setMaxNumberOfBoundariesForSkippingNetworkActions(-2);
        assertEquals(0, params.getMaxNumberOfBoundariesForSkippingNetworkActions());
    
    @Test
    public void testNegativeCurativeRaoMinObjImprovement() {
        SearchTreeRaoParameters parameters = new SearchTreeRaoParameters();
        parameters.setCurativeRaoMinObjImprovement(100);
        assertEquals(100, parameters.getCurativeRaoMinObjImprovement(), 1e-6);
        parameters.setCurativeRaoMinObjImprovement(-100);
        assertEquals(100, parameters.getCurativeRaoMinObjImprovement(), 1e-6);
    }
}
