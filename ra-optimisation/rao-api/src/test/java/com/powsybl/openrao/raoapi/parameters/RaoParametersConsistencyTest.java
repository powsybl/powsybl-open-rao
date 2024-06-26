/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.EICode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class RaoParametersConsistencyTest {
    private final RaoParameters parameters = new RaoParameters(ReportNode.NO_OP);

    private static ReportNode buildNewRootNode() {
        return ReportNode.newRootReportNode().withMessageTemplate("Test report node", "This is a parent report node for report tests").build();
    }

    @Test
    void testSetBoundariesFromCountryCodes() {
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("{FR}-{ES}", "{ES}-{PT}"));
        parameters.addExtension(RelativeMarginsParametersExtension.class, new RelativeMarginsParametersExtension());
        parameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfBoundariesFromString(stringBoundaries);
        assertEquals(2, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().size());
        assertEquals(1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode(Country.FR)), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode(Country.ES)), 1e-6);
        assertEquals(1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(1).getWeight(new EICode(Country.ES)), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(1).getWeight(new EICode(Country.PT)), 1e-6);
    }

    @Test
    void testSetBoundariesFromEiCodes() {
        parameters.addExtension(RelativeMarginsParametersExtension.class, new RelativeMarginsParametersExtension());
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("{10YBE----------2}-{10YFR-RTE------C}", "{10YBE----------2}-{22Y201903144---9}"));
        parameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfBoundariesFromString(stringBoundaries);
        assertEquals(2, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().size());
        assertEquals(2, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().size());
        assertEquals(1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode("10YBE----------2")), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode("10YFR-RTE------C")), 1e-6);
        assertEquals(1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(1).getWeight(new EICode("10YBE----------2")), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(1).getWeight(new EICode("22Y201903144---9")), 1e-6);
    }

    @Test
    void testSetBoundariesFromMixOfCodes() {
        parameters.addExtension(RelativeMarginsParametersExtension.class, new RelativeMarginsParametersExtension());
        List<String> stringBoundaries = new ArrayList<>(Collections.singletonList("{BE}-{22Y201903144---9}+{22Y201903145---4}-{DE}"));
        parameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfBoundariesFromString(stringBoundaries);
        assertEquals(1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().size());
        assertEquals(1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode(Country.BE)), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode(Country.DE)), 1e-6);
        assertEquals(1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode("22Y201903145---4")), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode("22Y201903144---9")), 1e-6);
    }

    @Test
    void testRelativePositiveMargins() {
        assertTrue(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE.relativePositiveMargins());
        assertTrue(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT.relativePositiveMargins());
        assertFalse(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE.relativePositiveMargins());
        assertFalse(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT.relativePositiveMargins());
    }

    @Test
    void testRelativeNetworkActionMinimumImpactThresholdBounds() {
        ReportNode reportNode = buildNewRootNode();
        RaoParameters localParameters = new RaoParameters(reportNode);
        localParameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(-0.5);
        assertEquals(0, localParameters.getTopoOptimizationParameters().getRelativeMinImpactThreshold(), 1e-6);
        localParameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(1.1);
        assertEquals(1, localParameters.getTopoOptimizationParameters().getRelativeMinImpactThreshold(), 1e-6);
        assertEquals(2, reportNode.getChildren().size());
        assertEquals("The value -0.5 provided for relative minimum impact threshold is smaller than 0. It will be set to 0.", reportNode.getChildren().get(0).getMessage());
        assertEquals(0, reportNode.getChildren().get(0).getChildren().size());
        assertEquals("The value 1.1 provided for relative minimum impact threshold is greater than 1. It will be set to 1.", reportNode.getChildren().get(1).getMessage());
        assertEquals(0, reportNode.getChildren().get(1).getChildren().size());
    }

    @Test
    void testMaxNumberOfBoundariesForSkippingNetworkActionsBounds() {
        ReportNode reportNode = buildNewRootNode();
        RaoParameters localParameters = new RaoParameters(reportNode);
        TopoOptimizationParameters topoOptimizationParameters = localParameters.getTopoOptimizationParameters();
        topoOptimizationParameters.setMaxNumberOfBoundariesForSkippingActions(300);
        assertEquals(300, topoOptimizationParameters.getMaxNumberOfBoundariesForSkippingActions());
        topoOptimizationParameters.setMaxNumberOfBoundariesForSkippingActions(-2);
        assertEquals(0, topoOptimizationParameters.getMaxNumberOfBoundariesForSkippingActions());
        assertEquals(1, reportNode.getChildren().size());
        assertEquals("The value -2 provided for max number of boundaries for skipping actions is smaller than 0. It will be set to 0.", reportNode.getChildren().get(0).getMessage());
        assertEquals(0, reportNode.getChildren().get(0).getChildren().size());
    }

    @Test
    void testNegativeCurativeRaoMinObjImprovement() {
        ReportNode reportNode = buildNewRootNode();
        RaoParameters localParameters = new RaoParameters(reportNode);
        ObjectiveFunctionParameters objectiveFunctionParameters = localParameters.getObjectiveFunctionParameters();
        objectiveFunctionParameters.setCurativeMinObjImprovement(100);
        assertEquals(100, objectiveFunctionParameters.getCurativeMinObjImprovement(), 1e-6);
        objectiveFunctionParameters.setCurativeMinObjImprovement(-100);
        assertEquals(100, objectiveFunctionParameters.getCurativeMinObjImprovement(), 1e-6);
        assertEquals(1, reportNode.getChildren().size());
        assertEquals("The value -100.0 provided for curative RAO minimum objective improvement is smaller than 0. It will be set to + 100.0", reportNode.getChildren().get(0).getMessage());
        assertEquals(0, reportNode.getChildren().get(0).getChildren().size());
    }

    @Test
    void testNegativeSensitivityFailureOverCost() {
        ReportNode reportNode = buildNewRootNode();
        RaoParameters localParameters = new RaoParameters(reportNode);
        LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = localParameters.getLoadFlowAndSensitivityParameters();
        loadFlowAndSensitivityParameters.setSensitivityFailureOvercost(60000);
        assertEquals(60000, loadFlowAndSensitivityParameters.getSensitivityFailureOvercost(), 1e-6);
        loadFlowAndSensitivityParameters.setSensitivityFailureOvercost(-20000);
        assertEquals(20000, loadFlowAndSensitivityParameters.getSensitivityFailureOvercost(), 1e-6);
        assertEquals(1, reportNode.getChildren().size());
        assertEquals("The value -20000.0 for `sensitivity-failure-overcost` is smaller than 0. This would encourage the optimizer to make the loadflow diverge. Thus, it will be set to + 20000.0", reportNode.getChildren().get(0).getMessage());
        assertEquals(0, reportNode.getChildren().get(0).getChildren().size());
    }

    @Test
    void testFailsOnLowSensitivityThreshold() {
        RangeActionsOptimizationParameters rangeActionsOptimizationParameters = parameters.getRangeActionsOptimizationParameters();

        Exception e = assertThrows(OpenRaoException.class, () -> rangeActionsOptimizationParameters.setPstSensitivityThreshold(0.));
        assertEquals("pstSensitivityThreshold should be greater than 1e-6, to avoid numerical issues.", e.getMessage());

        e = assertThrows(OpenRaoException.class, () -> rangeActionsOptimizationParameters.setHvdcSensitivityThreshold(1e-7));
        assertEquals("hvdcSensitivityThreshold should be greater than 1e-6, to avoid numerical issues.", e.getMessage());

        e = assertThrows(OpenRaoException.class, () -> rangeActionsOptimizationParameters.setInjectionRaSensitivityThreshold(0.));
        assertEquals("injectionRaSensitivityThreshold should be greater than 1e-6, to avoid numerical issues.", e.getMessage());
    }
}
