/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

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
public class SearchTreeRaoParametersConfigLoaderTest {

    private static final double DOUBLE_TOLERANCE = 0.1;

    private PlatformConfig platformConfig;
    private SearchTreeRaoParametersConfigLoader configLoader;

    @Before
    public void setUp() {
        platformConfig = Mockito.mock(PlatformConfig.class);
        configLoader = new SearchTreeRaoParametersConfigLoader();
    }

    @Test
    public void testLoad() {
        ModuleConfig searchTreeRaoParametersModule = Mockito.mock(ModuleConfig.class);
        Mockito.when(searchTreeRaoParametersModule.getEnumProperty(eq("preventive-rao-stop-criterion"), eq(SearchTreeRaoParameters.PreventiveRaoStopCriterion.class), any())).thenReturn(SearchTreeRaoParameters.PreventiveRaoStopCriterion.MIN_OBJECTIVE);
        Mockito.when(searchTreeRaoParametersModule.getIntProperty(eq("maximum-search-depth"), anyInt())).thenReturn(2);
        Mockito.when(searchTreeRaoParametersModule.getDoubleProperty(eq("relative-network-action-minimum-impact-threshold"), anyDouble())).thenReturn(0.1);
        Mockito.when(searchTreeRaoParametersModule.getDoubleProperty(eq("absolute-network-action-minimum-impact-threshold"), anyDouble())).thenReturn(20.0);
        Mockito.when(searchTreeRaoParametersModule.getIntProperty(eq("preventive-leaves-in-parallel"), anyInt())).thenReturn(4);
        Mockito.when(searchTreeRaoParametersModule.getIntProperty(eq("curative-leaves-in-parallel"), anyInt())).thenReturn(2);
        Mockito.when(searchTreeRaoParametersModule.getBooleanProperty(eq("skip-network-actions-far-from-most-limiting-element"), anyBoolean())).thenReturn(true);
        Mockito.when(searchTreeRaoParametersModule.getIntProperty(eq("max-number-of-boundaries-for-skipping-network-actions"), anyInt())).thenReturn(1);
        Mockito.when(searchTreeRaoParametersModule.getEnumProperty(eq("curative-rao-stop-criterion"), eq(SearchTreeRaoParameters.CurativeRaoStopCriterion.class), any())).thenReturn(SearchTreeRaoParameters.CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE);
        Mockito.when(searchTreeRaoParametersModule.getDoubleProperty(eq("curative-rao-min-obj-improvement"), anyDouble())).thenReturn(456.0);
        Mockito.when(searchTreeRaoParametersModule.getBooleanProperty(eq("curative-rao-optimize-operators-not-sharing-cras"), anyBoolean())).thenReturn(false);
        Mockito.when(searchTreeRaoParametersModule.getEnumProperty(eq("second-preventive-optimization-condition"), eq(SearchTreeRaoParameters.SecondPreventiveRaoCondition.class), any())).thenReturn(SearchTreeRaoParameters.SecondPreventiveRaoCondition.COST_INCREASE);

        Mockito.when(platformConfig.getOptionalModuleConfig("search-tree-rao-parameters")).thenReturn(Optional.of(searchTreeRaoParametersModule));

        SearchTreeRaoParameters parameters = configLoader.load(platformConfig);
        assertEquals(SearchTreeRaoParameters.PreventiveRaoStopCriterion.MIN_OBJECTIVE, parameters.getPreventiveRaoStopCriterion());
        assertEquals(2, parameters.getMaximumSearchDepth());
        assertEquals(0.1, parameters.getRelativeNetworkActionMinimumImpactThreshold(), DOUBLE_TOLERANCE);
        assertEquals(20.0, parameters.getAbsoluteNetworkActionMinimumImpactThreshold(), DOUBLE_TOLERANCE);
        assertEquals(4, parameters.getPreventiveLeavesInParallel());
        assertEquals(2, parameters.getCurativeLeavesInParallel());
        assertTrue(parameters.getSkipNetworkActionsFarFromMostLimitingElement());
        assertEquals(1, parameters.getMaxNumberOfBoundariesForSkippingNetworkActions());
        assertEquals(SearchTreeRaoParameters.CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE, parameters.getCurativeRaoStopCriterion());
        assertEquals(456.0, parameters.getCurativeRaoMinObjImprovement(), DOUBLE_TOLERANCE);
        assertFalse(parameters.getCurativeRaoOptimizeOperatorsNotSharingCras());
        assertEquals(SearchTreeRaoParameters.SecondPreventiveRaoCondition.COST_INCREASE, parameters.getSecondPreventiveOptimizationCondition());
    }

    @Test
    public void getExtensionName() {
        assertEquals("SearchTreeRaoParameters", configLoader.getExtensionName());
    }

    @Test
    public void getCategoryName() {
        assertEquals("rao-parameters", configLoader.getCategoryName());
    }

    @Test
    public void getExtensionClass() {
        assertEquals(SearchTreeRaoParameters.class, configLoader.getExtensionClass());
    }
}
