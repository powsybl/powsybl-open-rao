/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.json;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.SecondPreventiveRaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.PtdfApproximation;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.commons.test.AbstractSerDeTest;
import com.powsybl.commons.test.ComparisonUtils;
import com.powsybl.commons.extensions.AbstractExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.RAO_PARAMETERS_VERSION;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class JsonRaoParametersTest extends AbstractSerDeTest {
    static final double DOUBLE_TOLERANCE = 1e-6;

    @Test
    void roundTripDefault() throws IOException {
        RaoParameters parameters = new RaoParameters();
        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/RaoParameters_v2.json");
    }

    @Test
    void roundTrip() throws IOException {
        RaoParameters parameters = new RaoParameters();
        // Objective Function parameters
        parameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE);
        parameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.MIN_OBJECTIVE);
        parameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(983);
        parameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(true);
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
        parameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(RangeActionsOptimizationParameters.RaRangeShrinking.ENABLED);
        // TopologicalActions optimization parameters
        parameters.getTopoOptimizationParameters().setMaxPreventiveSearchTreeDepth(10);
        parameters.getTopoOptimizationParameters().setMaxAutoSearchTreeDepth(3);
        parameters.getTopoOptimizationParameters().setMaxCurativeSearchTreeDepth(10);
        parameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(0.1);
        parameters.getTopoOptimizationParameters().setAbsoluteMinImpactThreshold(20);
        parameters.getTopoOptimizationParameters().setPredefinedCombinations(List.of(List.of("na-id-1", "na-id-2"), List.of("na-id-1", "na-id-3", "na-id-4")));
        // Multi-threading parameters
        parameters.getMultithreadingParameters().setContingencyScenariosInParallel(15);
        parameters.getMultithreadingParameters().setPreventiveLeavesInParallel(21);
        parameters.getMultithreadingParameters().setAutoLeavesInParallel(30);
        parameters.getMultithreadingParameters().setCurativeLeavesInParallel(22);
        // Second preventive RAO parameters
        parameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        parameters.getSecondPreventiveRaoParameters().setReOptimizeCurativeRangeActions(true);
        parameters.getSecondPreventiveRaoParameters().setHintFromFirstPreventiveRao(true);
        // Not optimized cnecs parameters
        parameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCurativeCnecsForTsosWithoutCras(false);
        // LoadFlow and sensitivity parameters
        parameters.getLoadFlowAndSensitivityParameters().setLoadFlowProvider("OpenLoadFlowProvider");
        parameters.getLoadFlowAndSensitivityParameters().setSensitivityProvider("OpenSensitivityAnalysis");
        // Extensions
        // -- LoopFlow parameters
        parameters.addExtension(LoopFlowParametersExtension.class, new LoopFlowParametersExtension());
        parameters.getExtension(LoopFlowParametersExtension.class).setAcceptableIncrease(20.);
        parameters.getExtension(LoopFlowParametersExtension.class).setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO_AND_PST);
        parameters.getExtension(LoopFlowParametersExtension.class).setConstraintAdjustmentCoefficient(0.5);
        List<String> countries = new ArrayList<>();
        countries.add("BE");
        countries.add("FR");
        parameters.getExtension(LoopFlowParametersExtension.class).setCountries(countries);
        // -- Mnec parameters
        parameters.addExtension(MnecParametersExtension.class, new MnecParametersExtension());
        parameters.getExtension(MnecParametersExtension.class).setViolationCost(20);
        parameters.getExtension(MnecParametersExtension.class).setAcceptableMarginDecrease(30);
        parameters.getExtension(MnecParametersExtension.class).setConstraintAdjustmentCoefficient(3);
        // -- Relative Margins parameters
        parameters.addExtension(RelativeMarginsParametersExtension.class, new RelativeMarginsParametersExtension());
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("{FR}-{ES}", "{ES}-{PT}", "{BE}-{22Y201903144---9}-{DE}-{22Y201903145---4}"));
        parameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfBoundariesFromString(stringBoundaries);
        parameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO);
        parameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfSumLowerBound(0.05);

        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/RaoParametersSet_v2.json");
    }

    @Test
    void update() {
        RaoParameters parameters = JsonRaoParameters.read(getClass().getResourceAsStream("/RaoParameters_default_v2.json"));
        assertEquals(1, parameters.getExtensions().size());
        JsonRaoParameters.update(parameters, getClass().getResourceAsStream("/RaoParameters_update_v2.json"));
        assertEquals(2, parameters.getExtensions().size());
        assertEquals(ObjectiveFunctionParameters.PreventiveStopCriterion.MIN_OBJECTIVE, parameters.getObjectiveFunctionParameters().getPreventiveStopCriterion());
        assertEquals(5, parameters.getTopoOptimizationParameters().getMaxPreventiveSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(2, parameters.getTopoOptimizationParameters().getMaxAutoSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(5, parameters.getTopoOptimizationParameters().getMaxCurativeSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(0, parameters.getTopoOptimizationParameters().getRelativeMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertEquals(1, parameters.getTopoOptimizationParameters().getAbsoluteMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertEquals(8, parameters.getMultithreadingParameters().getPreventiveLeavesInParallel());
        assertEquals(4, parameters.getMultithreadingParameters().getAutoLeavesInParallel());
        assertEquals(3, parameters.getMultithreadingParameters().getCurativeLeavesInParallel());
        assertTrue(parameters.getTopoOptimizationParameters().getSkipActionsFarFromMostLimitingElement());
        assertEquals(2, parameters.getTopoOptimizationParameters().getMaxNumberOfBoundariesForSkippingActions());
        assertTrue(parameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCurativeCnecsForTsosWithoutCras());
        assertEquals(SecondPreventiveRaoParameters.ExecutionCondition.COST_INCREASE, parameters.getSecondPreventiveRaoParameters().getExecutionCondition());
        assertTrue(parameters.getSecondPreventiveRaoParameters().getHintFromFirstPreventiveRao());
        // Extensions
        MnecParametersExtension mnecParameters = parameters.getExtension(MnecParametersExtension.class);
        assertEquals(888, mnecParameters.getAcceptableMarginDecrease(), DOUBLE_TOLERANCE);
        assertEquals(23, mnecParameters.getViolationCost(), DOUBLE_TOLERANCE);
        assertEquals(4, mnecParameters.getConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
        RelativeMarginsParametersExtension relativeMarginsParameters = parameters.getExtension(RelativeMarginsParametersExtension.class);
        assertEquals(0.06, relativeMarginsParameters.getPtdfSumLowerBound(), DOUBLE_TOLERANCE);
        assertEquals(List.of("{FR}-{ES}"), relativeMarginsParameters.getPtdfBoundariesAsString());
    }

    @Test
    void writeExtension() throws IOException {
        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(DummyExtension.class, new DummyExtension());
        writeTest(parameters, JsonRaoParameters::write, ComparisonUtils::assertTxtEquals, "/RaoParametersWithExtension_v2.json");
    }

    @Test
    void readExtension() {
        RaoParameters parameters = JsonRaoParameters.read(getClass().getResourceAsStream("/RaoParametersWithExtension_v2.json"));
        assertEquals(1, parameters.getExtensions().size());
        assertNotNull(parameters.getExtension(DummyExtension.class));
        assertNotNull(parameters.getExtensionByName("dummy-extension"));
    }

    @Test
    void readErrorUnexpectedExtension() {
        InputStream inputStream = getClass().getResourceAsStream("/RaoParametersError_v2.json");
        OpenRaoException e = assertThrows(OpenRaoException.class, () -> JsonRaoParameters.read(inputStream));
        assertEquals("Unexpected field in rao parameters: unknownField", e.getMessage());
    }

    @Test
    void testFailOnOldVersion() {
        InputStream inputStream = getClass().getResourceAsStream("/RaoParameters_oldVersion.json");
        OpenRaoException e = assertThrows(OpenRaoException.class, () -> JsonRaoParameters.read(inputStream));
        assertEquals(String.format("RaoParameters version '2.0' cannot be deserialized. The only supported version currently is '%s'.", RAO_PARAMETERS_VERSION), e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"LoopFlowError", "PrevStopCriterionError", "WrongField"})
    void importNokTest(String source) {
        InputStream inputStream = getClass().getResourceAsStream("/RaoParametersWith" + source + "_v2.json");
        assertThrows(OpenRaoException.class, () -> JsonRaoParameters.read(inputStream));
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
