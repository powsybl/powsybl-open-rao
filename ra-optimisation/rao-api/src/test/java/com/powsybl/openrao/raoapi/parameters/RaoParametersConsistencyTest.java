/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.openrao.commons.EICode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class RaoParametersConsistencyTest {
    private final RaoParameters parameters = new RaoParameters();
    private OpenRaoSearchTreeParameters stParameters;

    @BeforeEach
    public void generalSetUp() {
        parameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        stParameters = parameters.getExtension(OpenRaoSearchTreeParameters.class);
    }

    @Test
    void testSetBoundariesFromCountryCodes() {
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("{FR}-{ES}", "{ES}-{PT}"));
        com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters relativeMarginsParameters = new com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters();
        relativeMarginsParameters.setPtdfBoundariesFromString(stringBoundaries);
        parameters.setRelativeMarginsParameters(relativeMarginsParameters);
        assertEquals(2, parameters.getRelativeMarginsParameters().get().getPtdfBoundaries().size());
        assertEquals(1, parameters.getRelativeMarginsParameters().get().getPtdfBoundaries().get(0).getWeight(new EICode(Country.FR)), 1e-6);
        assertEquals(-1, parameters.getRelativeMarginsParameters().get().getPtdfBoundaries().get(0).getWeight(new EICode(Country.ES)), 1e-6);
        assertEquals(1, parameters.getRelativeMarginsParameters().get().getPtdfBoundaries().get(1).getWeight(new EICode(Country.ES)), 1e-6);
        assertEquals(-1, parameters.getRelativeMarginsParameters().get().getPtdfBoundaries().get(1).getWeight(new EICode(Country.PT)), 1e-6);
    }

    @Test
    void testSetBoundariesFromEiCodes() {
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("{10YBE----------2}-{10YFR-RTE------C}", "{10YBE----------2}-{22Y201903144---9}"));
        com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters relativeMarginsParameters = new com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters();
        relativeMarginsParameters.setPtdfBoundariesFromString(stringBoundaries);
        parameters.setRelativeMarginsParameters(relativeMarginsParameters);
        assertEquals(2, parameters.getRelativeMarginsParameters().get().getPtdfBoundaries().size());
        assertEquals(2, parameters.getRelativeMarginsParameters().get().getPtdfBoundaries().size());
        assertEquals(1, parameters.getRelativeMarginsParameters().get().getPtdfBoundaries().get(0).getWeight(new EICode("10YBE----------2")), 1e-6);
        assertEquals(-1, parameters.getRelativeMarginsParameters().get().getPtdfBoundaries().get(0).getWeight(new EICode("10YFR-RTE------C")), 1e-6);
        assertEquals(1, parameters.getRelativeMarginsParameters().get().getPtdfBoundaries().get(1).getWeight(new EICode("10YBE----------2")), 1e-6);
        assertEquals(-1, parameters.getRelativeMarginsParameters().get().getPtdfBoundaries().get(1).getWeight(new EICode("22Y201903144---9")), 1e-6);
    }

    @Test
    void testSetBoundariesFromMixOfCodes() {
        List<String> stringBoundaries = new ArrayList<>(Collections.singletonList("{BE}-{22Y201903144---9}+{22Y201903145---4}-{DE}"));
        com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters relativeMarginsParameters = new com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters();
        relativeMarginsParameters.setPtdfBoundariesFromString(stringBoundaries);
        parameters.setRelativeMarginsParameters(relativeMarginsParameters);
        assertEquals(1, parameters.getRelativeMarginsParameters().get().getPtdfBoundaries().size());
        assertEquals(1, parameters.getRelativeMarginsParameters().get().getPtdfBoundaries().get(0).getWeight(new EICode(Country.BE)), 1e-6);
        assertEquals(-1, parameters.getRelativeMarginsParameters().get().getPtdfBoundaries().get(0).getWeight(new EICode(Country.DE)), 1e-6);
        assertEquals(1, parameters.getRelativeMarginsParameters().get().getPtdfBoundaries().get(0).getWeight(new EICode("22Y201903145---4")), 1e-6);
        assertEquals(-1, parameters.getRelativeMarginsParameters().get().getPtdfBoundaries().get(0).getWeight(new EICode("22Y201903144---9")), 1e-6);
    }

    @Test
    void testRelativePositiveMargins() {
        assertTrue(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN.relativePositiveMargins());
        assertFalse(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN.relativePositiveMargins());
    }

    @Test
    void testRelativeNetworkActionMinimumImpactThresholdBounds() {
        parameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(-0.5);
        assertEquals(0, parameters.getTopoOptimizationParameters().getRelativeMinImpactThreshold(), 1e-6);
        parameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(1.1);
        assertEquals(1, parameters.getTopoOptimizationParameters().getRelativeMinImpactThreshold(), 1e-6);
    }

    @Test
    void testMaxNumberOfBoundariesForSkippingNetworkActionsBounds() {
        stParameters.getTopoOptimizationParameters().setMaxNumberOfBoundariesForSkippingActions(300);
        assertEquals(300, stParameters.getTopoOptimizationParameters().getMaxNumberOfBoundariesForSkippingActions());
        stParameters.getTopoOptimizationParameters().setMaxNumberOfBoundariesForSkippingActions(-2);
        assertEquals(0, stParameters.getTopoOptimizationParameters().getMaxNumberOfBoundariesForSkippingActions());
    }

    @Test
    void testNegativeCurativeRaoMinObjImprovement() {
        stParameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(100);
        assertEquals(100, stParameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement(), 1e-6);
        stParameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(-100);
        assertEquals(100, stParameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement(), 1e-6);
    }

    @Test
    void testNegativeSensitivityFailureOverCost() {
        stParameters.getLoadFlowAndSensitivityParameters().setSensitivityFailureOvercost(60000);
        assertEquals(60000, stParameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost(), 1e-6);
        stParameters.getLoadFlowAndSensitivityParameters().setSensitivityFailureOvercost(-20000);
        assertEquals(20000, stParameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost(), 1e-6);
    }

    @Test
    void testFailsOnLowSensitivityThreshold() {
        Exception e = assertThrows(OpenRaoException.class, () -> stParameters.getRangeActionsOptimizationParameters().setPstSensitivityThreshold(0.));
        assertEquals("pstSensitivityThreshold should be greater than 1e-6, to avoid numerical issues.", e.getMessage());

        e = assertThrows(OpenRaoException.class, () -> stParameters.getRangeActionsOptimizationParameters().setHvdcSensitivityThreshold(1e-7));
        assertEquals("hvdcSensitivityThreshold should be greater than 1e-6, to avoid numerical issues.", e.getMessage());

        e = assertThrows(OpenRaoException.class, () -> stParameters.getRangeActionsOptimizationParameters().setInjectionRaSensitivityThreshold(0.));
        assertEquals("injectionRaSensitivityThreshold should be greater than 1e-6, to avoid numerical issues.", e.getMessage());
    }
}
