/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.json;

import com.powsybl.action.*;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.ThreeSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.*;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.PstModel;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking;
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
import java.util.*;

import static com.powsybl.action.PercentChangeLoadAction.QModificationStrategy.CONSTANT_Q;
import static com.powsybl.iidm.network.HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER;
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
        parameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        OpenRaoSearchTreeParameters searchTreeParameters = parameters.getExtension(OpenRaoSearchTreeParameters.class);
        // Objective Function parameters
        parameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        searchTreeParameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(983);
        parameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(true);
        // RangeActionsOptimization parameters
        searchTreeParameters.getRangeActionsOptimizationParameters().setMaxMipIterations(30);
        parameters.getRangeActionsOptimizationParameters().setPstRAMinImpactThreshold(10);
        searchTreeParameters.getRangeActionsOptimizationParameters().setPstSensitivityThreshold(0.2);
        parameters.getRangeActionsOptimizationParameters().setHvdcRAMinImpactThreshold(1);
        searchTreeParameters.getRangeActionsOptimizationParameters().setHvdcSensitivityThreshold(0.3);
        parameters.getRangeActionsOptimizationParameters().setInjectionRAMinImpactThreshold(1.2);
        searchTreeParameters.getRangeActionsOptimizationParameters().setInjectionRaSensitivityThreshold(0.7);
        searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setSolverSpecificParameters("TREEMEMORYLIMIT 20");
        searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setSolver(SearchTreeRaoRangeActionsOptimizationParameters.Solver.SCIP);
        searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setRelativeMipGap(1e-5);
        searchTreeParameters.getRangeActionsOptimizationParameters().setPstModel(PstModel.APPROXIMATED_INTEGERS);
        searchTreeParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(RaRangeShrinking.ENABLED);
        // TopologicalActions optimization parameters
        searchTreeParameters.getTopoOptimizationParameters().setMaxPreventiveSearchTreeDepth(10);
        searchTreeParameters.getTopoOptimizationParameters().setMaxCurativeSearchTreeDepth(10);
        parameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(0.1);
        parameters.getTopoOptimizationParameters().setAbsoluteMinImpactThreshold(20);
        searchTreeParameters.getTopoOptimizationParameters().setPredefinedCombinations(List.of(List.of("na-id-1", "na-id-2"), List.of("na-id-1", "na-id-3", "na-id-4")));
        // Multi-threading parameters
        searchTreeParameters.getMultithreadingParameters().setAvailableCPUs(21);
        // Second preventive RAO parameters
        searchTreeParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        searchTreeParameters.getSecondPreventiveRaoParameters().setHintFromFirstPreventiveRao(true);
        // Not optimized cnecs parameters
        parameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCurativeCnecsForTsosWithoutCras(false);
        // LoadFlow and sensitivity parameters
        searchTreeParameters.getLoadFlowAndSensitivityParameters().setLoadFlowProvider("OpenLoadFlowProvider");
        searchTreeParameters.getLoadFlowAndSensitivityParameters().setSensitivityProvider("OpenSensitivityAnalysis");
        // Extensions
        // -- LoopFlow parameters
        com.powsybl.openrao.raoapi.parameters.LoopFlowParameters loopFlowParameters = new com.powsybl.openrao.raoapi.parameters.LoopFlowParameters();
        SearchTreeRaoLoopFlowParameters loopFlowParametersExtension = new SearchTreeRaoLoopFlowParameters();
        loopFlowParameters.setAcceptableIncrease(20.);
        loopFlowParametersExtension.setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO_AND_PST);
        loopFlowParametersExtension.setConstraintAdjustmentCoefficient(0.5);
        List<String> countries = new ArrayList<>();
        countries.add("BE");
        countries.add("FR");
        loopFlowParameters.setCountries(countries);
        parameters.setLoopFlowParameters(loopFlowParameters);
        searchTreeParameters.setLoopFlowParameters(loopFlowParametersExtension);
        // -- Mnec parameters
        com.powsybl.openrao.raoapi.parameters.MnecParameters mnecParameters = new com.powsybl.openrao.raoapi.parameters.MnecParameters();
        SearchTreeRaoMnecParameters mnecParametersExtension = new SearchTreeRaoMnecParameters();
        mnecParametersExtension.setViolationCost(20);
        mnecParameters.setAcceptableMarginDecrease(30);
        mnecParametersExtension.setConstraintAdjustmentCoefficient(3);
        parameters.setMnecParameters(mnecParameters);
        searchTreeParameters.setMnecParameters(mnecParametersExtension);
        // -- Relative Margins parameters
        com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters relativeMarginsParameters = new com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters();
        SearchTreeRaoRelativeMarginsParameters relativeMarginsParametersExtension = new SearchTreeRaoRelativeMarginsParameters();
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("{FR}-{ES}", "{ES}-{PT}", "{BE}-{22Y201903144---9}-{DE}-{22Y201903145---4}"));
        relativeMarginsParameters.setPtdfBoundariesFromString(stringBoundaries);
        relativeMarginsParametersExtension.setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO);
        relativeMarginsParametersExtension.setPtdfSumLowerBound(0.05);
        parameters.setRelativeMarginsParameters(relativeMarginsParameters);
        searchTreeParameters.setRelativeMarginsParameters(relativeMarginsParametersExtension);
        // -- Min Margins parameters
        SearchTreeRaoCostlyMinMarginParameters minMarginsParameters = new SearchTreeRaoCostlyMinMarginParameters();
        minMarginsParameters.setShiftedViolationPenalty(800.0);
        minMarginsParameters.setShiftedViolationThreshold(3.0);
        searchTreeParameters.setMinMarginsParameters(minMarginsParameters);
        // -- Fast Rao Parameters
        parameters.addExtension(FastRaoParameters.class, new FastRaoParameters());
        FastRaoParameters fastRaoParameters = parameters.getExtension(FastRaoParameters.class);
        fastRaoParameters.setMarginLimit(5);
        fastRaoParameters.setAddUnsecureCnecs(false);
        fastRaoParameters.setNumberOfCnecsToAdd(20);

        // -- PST regulation parameters
        SearchTreeRaoPstRegulationParameters pstRegulationParameters = new SearchTreeRaoPstRegulationParameters();
        pstRegulationParameters.setPstsToRegulate(Map.of("pst-1", "network-element-1", "pst-2", "network-element-2"));
        searchTreeParameters.setPstRegulationParameters(pstRegulationParameters);

        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/RaoParametersSet_v2.json");
    }

    @Test
    void update() {
        RaoParameters parameters = JsonRaoParameters.read(getClass().getResourceAsStream("/RaoParameters_default_v2.json"));
        assertEquals(1, parameters.getExtensions().size());
        JsonRaoParameters.update(parameters, getClass().getResourceAsStream("/RaoParameters_update_v2.json"));
        assertEquals(1, parameters.getExtensions().size());
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN, parameters.getObjectiveFunctionParameters().getType());
        OpenRaoSearchTreeParameters searchTreeParameters = parameters.getExtension(OpenRaoSearchTreeParameters.class);
        assertEquals(5, searchTreeParameters.getTopoOptimizationParameters().getMaxPreventiveSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(5, searchTreeParameters.getTopoOptimizationParameters().getMaxCurativeSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(0, parameters.getTopoOptimizationParameters().getRelativeMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertEquals(1, parameters.getTopoOptimizationParameters().getAbsoluteMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertEquals(8, searchTreeParameters.getMultithreadingParameters().getAvailableCPUs());
        assertTrue(searchTreeParameters.getTopoOptimizationParameters().getSkipActionsFarFromMostLimitingElement());
        assertEquals(2, searchTreeParameters.getTopoOptimizationParameters().getMaxNumberOfBoundariesForSkippingActions());
        assertTrue(parameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCurativeCnecsForTsosWithoutCras());
        assertEquals(SecondPreventiveRaoParameters.ExecutionCondition.COST_INCREASE, searchTreeParameters.getSecondPreventiveRaoParameters().getExecutionCondition());
        assertTrue(searchTreeParameters.getSecondPreventiveRaoParameters().getHintFromFirstPreventiveRao());
        // Extensions
        assertEquals(888, parameters.getMnecParameters().get().getAcceptableMarginDecrease(), DOUBLE_TOLERANCE);
        assertEquals(23, searchTreeParameters.getMnecParameters().get().getViolationCost(), DOUBLE_TOLERANCE);
        assertEquals(4, searchTreeParameters.getMnecParameters().get().getConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
        assertEquals(0.06, searchTreeParameters.getRelativeMarginsParameters().get().getPtdfSumLowerBound(), DOUBLE_TOLERANCE);
        assertEquals(List.of("{FR}-{ES}"), parameters.getRelativeMarginsParameters().get().getPtdfBoundariesAsString());

    }

    @Test
    void testForcedActionsRoundTrip() throws IOException {
        List<Action> actions = new ArrayList<>();
        actions.add(new SwitchAction("id1", "switchId1", true));
        actions.add(new MultipleActionsAction("id2", Collections.singletonList(new SwitchAction("id3", "switchId2", true))));
        actions.add(new TerminalsConnectionAction("id3", "lineId3", true)); // both sides.
        actions.add(new TerminalsConnectionAction("id4", "lineId4", false)); // both sides.
        actions.add(new PhaseTapChangerTapPositionAction("id5", "transformerId1", true, 5, ThreeSides.TWO));
        actions.add(new PhaseTapChangerTapPositionAction("id6", "transformerId2", false, 12));
        actions.add(new PhaseTapChangerTapPositionAction("id7", "transformerId3", true, -5, ThreeSides.ONE));
        actions.add(new PhaseTapChangerTapPositionAction("id8", "transformerId3", false, 2, ThreeSides.THREE));
        actions.add(new GeneratorActionBuilder().withId("id9").withGeneratorId("generatorId1").withActivePowerRelativeValue(true).withActivePowerValue(100.0).build());
        actions.add(new GeneratorActionBuilder().withId("id10").withGeneratorId("generatorId2").withVoltageRegulatorOn(true).withTargetV(225.0).build());
        actions.add(new GeneratorActionBuilder().withId("id11").withGeneratorId("generatorId2").withVoltageRegulatorOn(false).withTargetQ(400.0).build());
        actions.add(new LoadActionBuilder().withId("id12").withLoadId("loadId1").withRelativeValue(false).withActivePowerValue(50.0).build());
        actions.add(new LoadActionBuilder().withId("id13").withLoadId("loadId1").withRelativeValue(true).withReactivePowerValue(5.0).build());
        actions.add(new PercentChangeLoadActionBuilder().withId("id26").withLoadId("loadId1").withP0PercentChange(5.0).withQModificationStrategy(CONSTANT_Q).build());
        actions.add(new DanglingLineActionBuilder().withId("id17").withDanglingLineId("dlId1").withRelativeValue(true).withReactivePowerValue(5.0).build());
        actions.add(new RatioTapChangerTapPositionAction("id14", "transformerId4", false, 2, ThreeSides.THREE));
        actions.add(new RatioTapChangerTapPositionAction("id15", "transformerId5", true, 1));
        actions.add(RatioTapChangerRegulationAction.activateRegulation("id16", "transformerId5", ThreeSides.THREE));
        actions.add(PhaseTapChangerRegulationAction.activateAndChangeRegulationMode("id17", "transformerId5", ThreeSides.ONE,
            PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL, 10.0));
        actions.add(PhaseTapChangerRegulationAction.deactivateRegulation("id18",
            "transformerId6", ThreeSides.ONE));
        actions.add(PhaseTapChangerRegulationAction.activateAndChangeRegulationMode("id19",
            "transformerId6", ThreeSides.ONE,
            PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL, 15.0));
        actions.add(RatioTapChangerRegulationAction.activateRegulationAndChangeTargetV("id20", "transformerId5", 90.0));
        actions.add(RatioTapChangerRegulationAction.deactivateRegulation("id21", "transformerId5", ThreeSides.THREE));
        actions.add(new HvdcActionBuilder()
            .withId("id22")
            .withHvdcId("hvdc1")
            .withAcEmulationEnabled(false)
            .build());
        actions.add(new HvdcActionBuilder()
            .withId("id23")
            .withHvdcId("hvdc2")
            .withAcEmulationEnabled(true)
            .build());
        actions.add(new HvdcActionBuilder()
            .withId("id24")
            .withHvdcId("hvdc2")
            .withAcEmulationEnabled(true)
            .withDroop(121.0)
            .withP0(42.0)
            .withConverterMode(SIDE_1_RECTIFIER_SIDE_2_INVERTER)
            .withRelativeValue(false)
            .build());
        actions.add(new HvdcActionBuilder()
            .withId("id25")
            .withHvdcId("hvdc1")
            .withAcEmulationEnabled(false)
            .withActivePowerSetpoint(12.0)
            .withRelativeValue(true)
            .build());
        actions.add(new ShuntCompensatorPositionActionBuilder().withId("id22").withShuntCompensatorId("shuntId1").withSectionCount(5).build());
        actions.add(new StaticVarCompensatorActionBuilder().withId("id23")
            .withStaticVarCompensatorId("svc").withRegulationMode(StaticVarCompensator.RegulationMode.VOLTAGE)
            .withVoltageSetpoint(56.0).build());
        actions.add(new StaticVarCompensatorActionBuilder().withId("id24")
            .withStaticVarCompensatorId("svc").withRegulationMode(StaticVarCompensator.RegulationMode.REACTIVE_POWER)
            .withReactivePowerSetpoint(120.0).build());
        actions.add(new TerminalsConnectionAction("id4", "transformerId25", ThreeSides.THREE, true)); // only one side.
        actions.add(new AreaInterchangeTargetAction("id99", "AreaA", 101.0));
        actions.add(new AreaInterchangeTargetAction("idDisabledTarget", "AreaA", Double.NaN));

        RaoParameters raoParameters = new RaoParameters();
        ForcedActions forcedActions = new ForcedActions(actions);
        raoParameters.addExtension(ForcedActions.class, forcedActions);

        roundTripTest(raoParameters, JsonRaoParameters::write, JsonRaoParameters::read, "/RaoParameters_with_ForcedActions.json");
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

    @Test
    void testWrongForcedActions() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> JsonRaoParameters.read(getClass().getResourceAsStream("/RaoParameters_with_wrong_ForcedActions.json")));
        assertEquals("Unexpected token: wrong-key", exception.getMessage());
    }

    @Test
    void testEmptyForcedActions() {
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/RaoParameters_with_empty_ForcedActions.json"));
        ForcedActions forcedActions = raoParameters.getExtension(ForcedActions.class);
        assertNotNull(forcedActions);
        assertTrue(forcedActions.getPreventiveActions().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"LoopFlowError", "ObjFuncTypeError", "WrongField"})
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
