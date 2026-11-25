/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.OptimizationPerimeter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeActionImpactsTest extends AbstractFillerTest {
    private OptimizationPerimeter perimeter;

    @BeforeEach
    void setUp() throws IOException {
        init();
        perimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(perimeter.getRangeActionsPerState()).thenReturn(Map.of(
            cnec1.getState(), Set.<RangeAction<?>>of(pstRangeAction),
            cnec2.getState(), Set.<RangeAction<?>>of(pstRangeAction)));
    }

    private SearchTreeRaoRangeActionsOptimizationParameters parameters(double pstSensitivityThreshold) {
        RaoParameters raoParameters = new RaoParameters(ReportNode.NO_OP);
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters(ReportNode.NO_OP));
        SearchTreeRaoRangeActionsOptimizationParameters parameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters();
        parameters.setPstSensitivityThreshold(pstSensitivityThreshold);
        return parameters;
    }

    @Test
    void belowThresholdImpactIsSkipped() {
        Mockito.when(sensitivityResult.getSensitivityValue(cnec1, TwoSides.ONE, pstRangeAction, Unit.MEGAWATT)).thenReturn(1.0);
        RangeActionImpacts impacts = new RangeActionImpacts(perimeter, parameters(2.1), Unit.MEGAWATT);

        assertTrue(impacts.getImpacts(cnec1, TwoSides.ONE, sensitivityResult).isEmpty());
    }

    @Test
    void aboveThresholdImpactIsKeptWithItsSign() {
        Mockito.when(sensitivityResult.getSensitivityValue(cnec1, TwoSides.ONE, pstRangeAction, Unit.MEGAWATT)).thenReturn(-2.1);
        RangeActionImpacts impacts = new RangeActionImpacts(perimeter, parameters(2.1), Unit.MEGAWATT);

        List<RangeActionImpacts.Impact> result = impacts.getImpacts(cnec1, TwoSides.ONE, sensitivityResult);

        assertEquals(1, result.size());
        assertSame(pstRangeAction, result.get(0).rangeAction());
        assertEquals(cnec1.getState(), result.get(0).state());
        assertEquals(-2.1, result.get(0).sensitivity(), 1e-12);
    }

    @Test
    void sameElementInPreventiveAndCurativeCountedOnceAtCurativeState() {
        State curativeState = cnec2.getState();
        RangeActionImpacts impacts = new RangeActionImpacts(perimeter, parameters(1e-6), Unit.MEGAWATT);

        List<RangeActionImpacts.Impact> result = impacts.getImpacts(cnec2, TwoSides.TWO, sensitivityResult);

        assertEquals(1, result.size());
        assertEquals(curativeState, result.get(0).state());
        assertEquals(SENSI_CNEC2_IT1, result.get(0).sensitivity(), 1e-12);
    }
}
