/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.powsybl.commons.AbstractConverterTest;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class JsonSearchTreeRaoParametersTest extends AbstractConverterTest {

    @Test
    public void roundTrip() throws IOException {
        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(SearchTreeRaoParameters.class, new SearchTreeRaoParameters());
        parameters.getExtension(SearchTreeRaoParameters.class).setPreventiveRaoStopCriterion(SearchTreeRaoParameters.PreventiveRaoStopCriterion.MIN_OBJECTIVE);
        parameters.getExtension(SearchTreeRaoParameters.class).setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE);
        parameters.getExtension(SearchTreeRaoParameters.class).setCurativeRaoMinObjImprovement(983);
        parameters.getExtension(SearchTreeRaoParameters.class).setMaximumSearchDepth(10);
        parameters.getExtension(SearchTreeRaoParameters.class).setRelativeNetworkActionMinimumImpactThreshold(0.1);
        parameters.getExtension(SearchTreeRaoParameters.class).setAbsoluteNetworkActionMinimumImpactThreshold(20);
        parameters.getExtension(SearchTreeRaoParameters.class).setMaxCurativeRa(3);
        parameters.getExtension(SearchTreeRaoParameters.class).setMaxCurativeTso(2);
        parameters.getExtension(SearchTreeRaoParameters.class).setMaxCurativeRaPerTso(Map.of("RTE", 5));
        parameters.getExtension(SearchTreeRaoParameters.class).setCurativeRaoOptimizeOperatorsNotSharingCras(false);
        parameters.getExtension(SearchTreeRaoParameters.class).setSecondPreventiveOptimizationCondition(SearchTreeRaoParameters.SecondPreventiveRaoCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        parameters.getExtension(SearchTreeRaoParameters.class).setNetworkActionIdCombinations(List.of(List.of("na-id-1", "na-id-2"), List.of("na-id-1", "na-id-3", "na-id-4")));

        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/SearchTreeRaoParameters.json");
    }

    @Test
    public void readError() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/SearchTreeRaoParametersError.json")) {
            JsonRaoParameters.read(is);
            fail();
        } catch (FaraoException e) {
            // should throw
            assertTrue(e.getMessage().contains("Unexpected field"));
        }
    }

    @Test
    public void update() {
        RaoParameters parameters = JsonRaoParameters.read(getClass().getResourceAsStream("/RaoParameters_default.json"));
        JsonRaoParameters.update(parameters, getClass().getResourceAsStream("/RaoParameters_update.json"));
        SearchTreeRaoParameters extension = parameters.getExtension(SearchTreeRaoParameters.class);
        assertNotNull(extension);
        assertEquals(SearchTreeRaoParameters.PreventiveRaoStopCriterion.MIN_OBJECTIVE, extension.getPreventiveRaoStopCriterion());
        assertEquals(5, extension.getMaximumSearchDepth());
        assertEquals(0, extension.getRelativeNetworkActionMinimumImpactThreshold(), 1e-6);
        assertEquals(1, extension.getAbsoluteNetworkActionMinimumImpactThreshold(), 1e-6);
        assertEquals(8, extension.getPreventiveLeavesInParallel());
        assertEquals(3, extension.getCurativeLeavesInParallel());
        assertTrue(extension.getSkipNetworkActionsFarFromMostLimitingElement());
        assertEquals(2, extension.getMaxNumberOfBoundariesForSkippingNetworkActions());
        assertFalse(extension.getCurativeRaoOptimizeOperatorsNotSharingCras());
        assertEquals(SearchTreeRaoParameters.SecondPreventiveRaoCondition.COST_INCREASE, extension.getSecondPreventiveOptimizationCondition());

        assertEquals(2, extension.getMaxCurativeTopoPerTso().size());
        assertEquals(3, extension.getMaxCurativeTopoPerTso().get("RTE").intValue());
        assertEquals(5, extension.getMaxCurativeTopoPerTso().get("Elia").intValue());
        assertEquals(1, extension.getMaxCurativePstPerTso().size());
        assertEquals(0, extension.getMaxCurativePstPerTso().get("Amprion").intValue());
        assertEquals(2, extension.getMaxCurativeRaPerTso().size());
        assertEquals(1, extension.getMaxCurativeRaPerTso().get("Tennet").intValue());
        assertEquals(9, extension.getMaxCurativeRaPerTso().get("50Hz").intValue());
    }

    @Test(expected = FaraoException.class)
    public void testWrongStopCriterionError() {
        JsonRaoParameters.read(getClass().getResourceAsStream("/SearchTreeRaoParametersStopCriterionError.json"));
    }

    @Test(expected = FaraoException.class)
    public void curativeRaoStopCriterionError() {
        JsonRaoParameters.read(getClass().getResourceAsStream("/SearchTreeRaoParametersCurativeStopCriterionError.json"));
    }

    @Test(expected = FaraoException.class)
    public void testMapTypeError() {
        JsonRaoParameters.read(getClass().getResourceAsStream("/SearchTreeRaoParametersMapError.json"));
    }

    @Test(expected = FaraoException.class)
    public void testMapNegativeError() {
        JsonRaoParameters.read(getClass().getResourceAsStream("/SearchTreeRaoParametersMapError2.json"));
    }
}
