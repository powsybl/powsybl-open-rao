/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.ObjectiveFunctionParameters;
import com.farao_community.farao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.parameters.SecondPreventiveRaoParameters;
import com.farao_community.farao.rao_api.parameters.extensions.LoopFlowParametersExtension;
import com.farao_community.farao.rao_api.parameters.extensions.MnecParametersExtension;
import com.farao_community.farao.rao_api.parameters.extensions.RelativeMarginParametersExtension;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.commons.test.AbstractConverterTest;
import com.powsybl.commons.test.ComparisonUtils;
import com.powsybl.commons.extensions.AbstractExtension;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class JsonRaoParametersTest extends AbstractConverterTest {

    @Test
    public void roundTripDefault() throws IOException {
        RaoParameters parameters = new RaoParameters();
        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/RaoParameters.json");
    }

    @Test
    public void roundTrip() throws IOException {
        RaoParameters parameters = new RaoParameters();
        // Objective Function parameters
        parameters.getObjectiveFunctionParameters().setObjectiveFunctionType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE);
        parameters.getObjectiveFunctionParameters().setForbidCostIncrease(true);
        parameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.MIN_OBJECTIVE);
        parameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE);
        parameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(983);
        // RangeActionsOptimization parameters
        parameters.getRangeActionsOptimizationParameters().setMaxMipIterations(30);
        parameters.getRangeActionsOptimizationParameters().setPstPenaltyCost(10);
        parameters.getRangeActionsOptimizationParameters().setPstSensitivityThreshold(0.2);
        parameters.getRangeActionsOptimizationParameters().setHvdcPenaltyCost(1);
        parameters.getRangeActionsOptimizationParameters().setHvdcSensitivityThreshold(0.3);
        parameters.getRangeActionsOptimizationParameters().setInjectionRaPenaltyCost(1.2);
        parameters.getRangeActionsOptimizationParameters().setInjectionRaSensitivityThreshold(0.7);
        parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setSolverSpecificParameters("TREEMEMORYLIMIT 20");
        parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setSolver(RangeActionsOptimizationParameters.Solver.SCIP);
        parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setRelativeMipGap(1e-5);
        parameters.getRangeActionsOptimizationParameters().setPstModel(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);
        // TopologicalActions optimization parameters
        parameters.getTopoOptimizationParameters().setMaxSearchTreeDepth(10);
        parameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(0.1);
        parameters.getTopoOptimizationParameters().setAbsoluteMinImpactThreshold(20);
        parameters.getTopoOptimizationParameters().setPredefinedCombinations(List.of(List.of("na-id-1", "na-id-2"), List.of("na-id-1", "na-id-3", "na-id-4")));
        // Multi-threading parameters
        parameters.getMultithreadingParameters().setContingencyScenariosInParallel(15);
        // Second preventive RAO parameters
        parameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        parameters.getSecondPreventiveRaoParameters().setReOptimizeCurativeRangeActions(true);
        // RaUsageLimitsPerContingency parameters
        parameters.getRaUsageLimitsPerContingencyParameters().setMaxCurativeRa(3);
        parameters.getRaUsageLimitsPerContingencyParameters().setMaxCurativeTso(2);
        parameters.getRaUsageLimitsPerContingencyParameters().setMaxCurativeRaPerTso(Map.of("RTE", 5));
        // Not optimized cnecs parameters
        parameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCurativeCnecsForTsosWithoutCras(true);
        parameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCnecsSecuredByTheirPst(Map.of("cnec1", "pst1"));
        // LoadFlow and sensitivity parameters
        parameters.getLoadFlowAndSensitivityParameters().setLoadFlowProvider("OpenLoadFlowProvider");
        parameters.getLoadFlowAndSensitivityParameters().setSensitivityProvider("OpenSensitivityAnalysis");
        // Extensions
        // -- LoopFlow parameters
        parameters.addExtension(LoopFlowParametersExtension.class, LoopFlowParametersExtension.loadDefault());
        parameters.getExtension(LoopFlowParametersExtension.class).setAcceptableIncrease(20.);
        parameters.getExtension(LoopFlowParametersExtension.class).setApproximation(LoopFlowParametersExtension.Approximation.UPDATE_PTDF_WITH_TOPO_AND_PST);
        parameters.getExtension(LoopFlowParametersExtension.class).setConstraintAdjustmentCoefficient(0.5);
        List<String> countries = new ArrayList<>();
        countries.add("BE");
        countries.add("FR");
        parameters.getExtension(LoopFlowParametersExtension.class).setCountries(countries);
        // -- Mnec parameters
        parameters.addExtension(MnecParametersExtension.class, MnecParametersExtension.loadDefault());
        parameters.getExtension(MnecParametersExtension.class).setViolationCost(20);
        parameters.getExtension(MnecParametersExtension.class).setAcceptableMarginDecrease(30);
        parameters.getExtension(MnecParametersExtension.class).setConstraintAdjustmentCoefficient(3);
        // -- Relative Margins parameters
        parameters.addExtension(RelativeMarginParametersExtension.class, RelativeMarginParametersExtension.loadDefault());
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("{FR}-{ES}", "{ES}-{PT}", "{BE}-{22Y201903144---9}-{DE}-{22Y201903145---4}"));
        parameters.getExtension(RelativeMarginParametersExtension.class).setPtdfBoundariesFromString(stringBoundaries);
        parameters.getExtension(RelativeMarginParametersExtension.class).setPtdfSumLowerBound(0.05);

        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/RaoParametersSet.json");
    }

    public void update() {
        RaoParameters parameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_default.json"));
        JsonRaoParameters.update(parameters, getClass().getResourceAsStream("/parameters/RaoParameters_update.json"));
        assertEquals(ObjectiveFunctionParameters.PreventiveStopCriterion.MIN_OBJECTIVE, parameters.getObjectiveFunctionParameters().getPreventiveStopCriterion());
        assertEquals(5, parameters.getTopoOptimizationParameters().getMaxSearchTreeDepth());
        assertEquals(0, parameters.getTopoOptimizationParameters().getRelativeMinImpactThreshold(), 1e-6);
        assertEquals(1,  parameters.getTopoOptimizationParameters().getAbsoluteMinImpactThreshold(), 1e-6);
        assertEquals(8,  parameters.getMultithreadingParameters().getPreventiveLeavesInParallel());
        assertEquals(3,  parameters.getMultithreadingParameters().getCurativeLeavesInParallel());
        assertTrue(parameters.getTopoOptimizationParameters().getSkipActionsFarFromMostLimitingElement());
        assertEquals(2, parameters.getTopoOptimizationParameters().getMaxNumberOfBoundariesForSkippingActions());
        assertFalse(parameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCurativeCnecsForTsosWithoutCras());
        assertTrue(parameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCnecsSecuredByTheirPst().isEmpty());
        assertEquals(SecondPreventiveRaoParameters.ExecutionCondition.COST_INCREASE, parameters.getSecondPreventiveRaoParameters().getExecutionCondition());
        assertTrue(parameters.getSecondPreventiveRaoParameters().getHintFromFirstPreventiveRao());

        assertEquals(2, parameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativeTopoPerTso().size());
        assertEquals(3, parameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativeTopoPerTso().get("RTE").intValue());
        assertEquals(5, parameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativeTopoPerTso().get("Elia").intValue());
        assertEquals(1, parameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativePstPerTso().size());
        assertEquals(0, parameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativePstPerTso().get("Amprion").intValue());
        assertEquals(2, parameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativeRaPerTso().size());
        assertEquals(1, parameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativeRaPerTso().get("Tennet").intValue());
        assertEquals(9, parameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativeRaPerTso().get("50Hz").intValue());
    }

    @Test
    public void writeExtension() throws IOException {
        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(DummyExtension.class, new DummyExtension());
        writeTest(parameters, JsonRaoParameters::write, ComparisonUtils::compareTxt, "/RaoParametersWithExtension.json");
    }

    @Test
    public void readExtension() {
        RaoParameters parameters = JsonRaoParameters.read(getClass().getResourceAsStream("/RaoParametersWithExtension.json"));
        assertEquals(1, parameters.getExtensions().size());
        assertNotNull(parameters.getExtension(DummyExtension.class));
        assertNotNull(parameters.getExtensionByName("dummy-extension"));
    }

    @Test(expected = FaraoException.class)
    public void readErrorUnexpectedField() {
        JsonRaoParameters.read(getClass().getResourceAsStream("/RaoParametersError.json"));
    }

    @Test
    public void readErrorUnexpectedExtension() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/parameters/SearchTreeRaoParametersError.json")) {
            JsonRaoParameters.read(is);
            fail();
        } catch (FaraoException e) {
            // should throw
            assertTrue(e.getMessage().contains("Unexpected field"));
        }
    }

    @Test(expected = FaraoException.class)
    public void loopFlowApproximationLevelError() {
        JsonRaoParameters.read(getClass().getResourceAsStream("/RaoParametersWithLoopFlowError.json"));
    }

    @Test(expected = FaraoException.class)
    public void testWrongStopCriterionError() {
        JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/SearchTreeRaoParametersStopCriterionError.json"));
    }

    @Test(expected = FaraoException.class)
    public void curativeRaoStopCriterionError() {
        JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/SearchTreeRaoParametersCurativeStopCriterionError.json"));
    }

    @Test(expected = FaraoException.class)
    public void testMapTypeError() {
        JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/SearchTreeRaoParametersMapError.json"));
    }

    @Test(expected = FaraoException.class)
    public void testMapNegativeError() {
        JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/SearchTreeRaoParametersMapError2.json"));
    }

    static class DummyExtension extends AbstractExtension<RaoParameters> {

        DummyExtension() {
            super();
        }

        @Override
        public String getName() {
            return "dummy-extension";
        }
    }

    @AutoService(JsonRaoParameters.ExtensionSerializer.class)
    public static class DummySerializer implements JsonRaoParameters.ExtensionSerializer<DummyExtension> {

        @Override
        public void serialize(DummyExtension extension, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeEndObject();
        }

        @Override
        public DummyExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
            return new DummyExtension();
        }

        @Override
        public String getExtensionName() {
            return "dummy-extension";
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
