/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.tests.steps;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.io.fbconstraint.parameters.FbConstraintCracCreationParameters;
import com.powsybl.openrao.data.crac.loopflowextension.LoopFlowThreshold;
import com.powsybl.openrao.data.icsimporter.IcsData;
import com.powsybl.openrao.data.icsimporter.IcsDataImporter;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgramBuilder;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.data.timecoupledconstraints.io.JsonTimeCoupledConstraints;
import com.powsybl.openrao.loopflowcomputation.LoopFlowComputation;
import com.powsybl.openrao.loopflowcomputation.LoopFlowComputationImpl;
import com.powsybl.openrao.loopflowcomputation.LoopFlowResult;
import com.powsybl.openrao.raoapi.LazyNetwork;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.marmot.results.extensions.PreTimeCouplingOverloadedCnecs;
import com.powsybl.openrao.tests.utils.CoreCcPreprocessor;
import com.powsybl.openrao.tests.utils.RaoUtils;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityVariableSet;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.MDC;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.getFlowUnit;
import static com.powsybl.openrao.tests.steps.CommonTestData.buildConfig;
import static com.powsybl.openrao.tests.steps.CommonTestData.cracPath;
import static com.powsybl.openrao.tests.steps.CommonTestData.getRaoParameters;
import static com.powsybl.openrao.tests.steps.CommonTestData.getResourcesPath;
import static com.powsybl.openrao.tests.steps.CommonTestData.raoParameters;
import static com.powsybl.openrao.tests.steps.CommonTestData.raoParametersPath;
import static com.powsybl.openrao.tests.utils.Helpers.getFile;
import static com.powsybl.openrao.tests.utils.Helpers.getOffsetDateTimeFromBrusselsTimestamp;
import static com.powsybl.openrao.tests.utils.Helpers.importCrac;
import static com.powsybl.openrao.tests.utils.Helpers.importNetwork;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class RaoSteps {
    private static final double TOLERANCE_FLOW_IN_AMPERE = 5.0;
    static final double TOLERANCE_FLOW_IN_MEGAWATT = 5.0;
    private static final double TOLERANCE_FLOW_RELATIVE = 1.5 / 100;
    private static final double TOLERANCE_PTDF = 1e-3;
    private static final double TOLERANCE_RANGEACTION_SETPOINT = 5.0;
    static final String DEFAULT_CRAC_CREATION_PARAMETERS_PATH = "cracCreationParameters/4_time_coupled/crac_creation_parameters_4_1.json";
    static final double TOLERANCE_REDISPATCHING_VALUE = 1.0;

    private Crac crac;
    private RaoResult raoResult;
    private LoopFlowResult loopFlowResult;
    private Network network;
    private State preventiveState;
    private final String raoImplementation;
    private final boolean forceFastRao;

    private static String networkFolderPath;
    private static String cracFolderPath;
    private static boolean useIndividualCracs = false;
    private static String networkFolderPathPostIcsImport;
    private static String icsStaticPath;
    private static String icsSeriesPath;
    private static String icsGskPath;
    private static String refProgPath;
    private static TimeCoupledRaoResult timeCoupledRaoResult;
    private static Map<OffsetDateTime, CracCreationContext> cracCreationContexts;

    {
        try {
            final Properties configProperties = new Properties();
            final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("steps-config.properties");
            configProperties.load(inputStream);
            raoImplementation = (String) configProperties.get("rao-implementation");
            forceFastRao = Boolean.parseBoolean((String) configProperties.get("force-fast-rao"));
        } catch (final IOException e) {
            throw new OpenRaoException("Unable to load steps-config.properties", e);
        }
    }

    private static double flowAmpereTolerance(double expectedValue) {
        return Math.max(TOLERANCE_FLOW_IN_AMPERE, TOLERANCE_FLOW_RELATIVE * Math.abs(expectedValue));
    }

    private static double flowMegawattTolerance(Double expectedValue) {
        return Math.max(TOLERANCE_FLOW_IN_MEGAWATT, TOLERANCE_FLOW_RELATIVE * Math.abs(expectedValue));
    }

    public TwoSides getTwoSideFromInteger(Integer side) {
        if (side.equals(1)) {
            return TwoSides.ONE;
        } else {
            return TwoSides.TWO;
        }
    }

    @When("I launch rao")
    public void iLaunchRao() {
        iLaunchRao(null);
    }

    @When("I launch rao with a time limit of {int} seconds")
    public void iLaunchRaoWithTimeLimit(int timeLimit) {
        launchRao(timeLimit);
    }

    @When("I launch rao at {string}")
    public void iLaunchRao(String timestamp) {
        launchRao(timestamp, raoImplementation);
    }

    @When("I launch loopflow rao with default loopflow limit as {double} percent of pmax")
    public void iLaunchRaoWithDefaultLoopflowLimit(double percentage) {
        launchRao(null, percentage, raoImplementation, null);
    }

    @When("I launch loopflow rao at {string} with default loopflow limit as {double} percent of pmax")
    public void iLaunchRaoWithDefaultLoopflowLimit(String timestamp, double percentage) {
        launchRao(timestamp, percentage, raoImplementation, null);
    }

    @When("I launch loopflow_computation with OpenLoadFlow")
    public void iLaunchLoopflowComputation() {
        iLaunchLoopflowComputation(null);
    }

    @When("I launch loopflow_computation with OpenLoadFlow at {string}")
    public void iLaunchLoopflowComputation(String timestamp) {
        launchLoopflowComputation(timestamp, "OpenLoadFlow", "OpenLoadFlow");
    }

    @When("I export rao reports to {string}")
    public void iExportRaoReports(String outputPath) throws IOException {
        CommonTestData.getReportNode().print(Path.of(getResourcesPath().concat(outputPath)));
    }

    @Then("the calculation succeeds")
    public void theCalculationSucceeds() {
        assertEquals(ComputationStatus.DEFAULT, CommonTestData.getRaoResult().getComputationStatus());
    }

    @Then("the calculation partially fails")
    public void theCalculationPartiallyFails() {
        assertEquals(ComputationStatus.PARTIAL_FAILURE, CommonTestData.getRaoResult().getComputationStatus());
    }

    @Then("the calculation fails")
    public void theCalculationFails() {
        assertEquals(ComputationStatus.FAILURE, CommonTestData.getRaoResult().getComputationStatus());
    }

    @Then("its security status should be {string}")
    public void statusShouldBe(String status) {
        assertEquals(
            "secured".equalsIgnoreCase(status),
            CommonTestData.getRaoResult().isSecure(PhysicalParameter.FLOW)
        );
    }

    @Then("the value of the objective function initially should be {double}")
    public void objectiveFunctionValueInitialShouldBe(double expectedValue) {
        assertEquals(expectedValue, CommonTestData.getRaoResult().getCost(null), flowAmpereTolerance(expectedValue));
    }

    @Then("the value of the objective function after PRA should be {double}")
    public void objectiveFunctionValueAfterPraShouldBe(double expectedValue) {
        assertEquals(expectedValue, CommonTestData.getRaoResult().getCost(crac.getPreventiveInstant()), flowAmpereTolerance(expectedValue));
    }

    @Then("the value of the objective function after ARA should be {double}")
    public void objectiveFunctionValueAfterAraShouldBe(double expectedValue) {
        Instant instant = crac.hasAutoInstant() ? crac.getInstant(InstantKind.AUTO) : crac.getOutageInstant();
        assertEquals(expectedValue, CommonTestData.getRaoResult().getCost(instant), flowAmpereTolerance(expectedValue));
    }

    @Then("the value of the objective function after CRA should be {double}")
    public void objectiveFunctionValueAfterCraShouldBe(double expectedValue) {
        assertEquals(expectedValue, CommonTestData.getRaoResult().getCost(crac.getLastInstant()), flowAmpereTolerance(expectedValue));
    }

    @Then("the value of the objective function before optimisation should be {double}")
    public void objectiveFunctionValueBeforeOptShouldBe(double expectedValue) {
        assertEquals(expectedValue, CommonTestData.getRaoResult().getCost(null), flowAmpereTolerance(expectedValue));
    }

    @Then("{int} remedial actions are used in preventive")
    public void countPra(int expectedCount) {
        assertEquals(
            expectedCount,
            CommonTestData.getRaoResult().getActivatedNetworkActionsDuringState(preventiveState).size() + raoResult.getActivatedRangeActionsDuringState(preventiveState).size(),
            0
        );
    }

    @Then("{int} network actions are used in preventive")
    public void countNetworkActionPra(int expectedCount) {
        assertEquals(expectedCount, CommonTestData.getRaoResult().getActivatedNetworkActionsDuringState(preventiveState).size());
    }

    @Then("{int} ± {int} remedial actions are used in preventive")
    public void countPra(int expectedCount, int tolerance) {
        assertEquals(
            expectedCount,
            CommonTestData.getRaoResult().getActivatedNetworkActionsDuringState(preventiveState).size() + raoResult.getActivatedRangeActionsDuringState(preventiveState).size(),
            tolerance
        );
    }

    @Then("{int} remedial actions are used after {string} at {string}")
    public void countCra(int expectedCount, String contingencyId, String instant) {
        State state = getState(contingencyId, instant);
        assertEquals(expectedCount, CommonTestData.getRaoResult().getActivatedRangeActionsDuringState(state).size() + raoResult.getActivatedNetworkActionsDuringState(state).size());
    }

    @Then("the remedial action {string} is used in preventive")
    public void remedialActionUsedInPreventive(String remedialAction) {
        assertTrue(raoResult.isActivatedDuringState(preventiveState, crac.getRemedialAction(remedialAction)));
    }

    @Then("the remedial action {string} is used after {string} at {string}")
    public void remedialActionUsed(String remedialAction, String contingencyId, String instant) {
        assertTrue(raoResult.isActivatedDuringState(getState(contingencyId, instant), crac.getRemedialAction(remedialAction)));
    }

    @Then("the remedial action {string} is not used after {string} at {string}")
    public void remedialActionNotUsed(String remedialAction, String contingencyId, String instant) {
        assertFalse(raoResult.isActivatedDuringState(getState(contingencyId, instant), crac.getRemedialAction(remedialAction)));
    }

    @Then("line {string} in network file with PRA has connection status to {string}")
    public void lineConnectionStatusInNetworkWithPra(String line, String isConnectedStr) {
        CommonTestData.getNetwork().getVariantManager().setWorkingVariant("PreventiveScenario");
        boolean isConnected = Boolean.parseBoolean(isConnectedStr);
        assertEquals(network.getBranch(line).getTerminal1().isConnected(), isConnected);
        assertEquals(network.getBranch(line).getTerminal2().isConnected(), isConnected);
        CommonTestData.getNetwork().getVariantManager().setWorkingVariant("InitialState");
    }

    @Then("PST {string} in network file with PRA is on tap {int}")
    public void pstTapInNetworkWithPra(String pst, int tap) {
        CommonTestData.getNetwork().getVariantManager().setWorkingVariant("PreventiveScenario");
        assertEquals(tap, network.getTwoWindingsTransformer(pst).getPhaseTapChanger().getTapPosition());
        CommonTestData.getNetwork().getVariantManager().setWorkingVariant("InitialState");
    }

    @Then("the remedial action {string} is not used in preventive")
    public void remedialActionNotUsedInPreventive(String remedialAction) {
        RangeAction<?> rangeAction = crac.getRangeAction(remedialAction);
        if (!Objects.isNull(rangeAction)) {
            assertFalse(raoResult.isActivatedDuringState(preventiveState, rangeAction));
        } else {
            NetworkAction networkAction = crac.getNetworkAction(remedialAction);
            assertFalse(raoResult.isActivatedDuringState(preventiveState, networkAction));
        }
    }

    @Then("the tap of PstRangeAction {string} should be {int} in preventive")
    public void theTapOfPstRangeActionShouldBe(String pstRangeActionId, int chosenPstTap) {
        assertEquals(chosenPstTap, CommonTestData.getRaoResult().getOptimizedTapOnState(preventiveState, (PstRangeAction) crac.getRangeAction(pstRangeActionId)));
    }

    @Then("the tap of PstRangeAction {string} should be {int} ± {int} in preventive")
    public void theTapOfPstRangeActionShouldBe(String pstRangeActionId, int chosenPstTap, int tolerance) {
        assertEquals(chosenPstTap, CommonTestData.getRaoResult().getOptimizedTapOnState(preventiveState, (PstRangeAction) crac.getRangeAction(pstRangeActionId)), tolerance);
    }

    @Then("the tap of PstRangeAction {string} should be {int} after {string} at {string}")
    public void theTapOfPstRangeActionShouldBe(String pstRangeActionId, int chosenPstTap, String contingencyId, String instant) {
        PstRangeAction rangeAction = (PstRangeAction) crac.getRangeAction(pstRangeActionId);
        assertEquals(chosenPstTap, CommonTestData.getRaoResult().getOptimizedTapOnState(getState(contingencyId, instant), rangeAction));
    }

    @Then("the setpoint of RangeAction {string} should be {double} MW in preventive")
    public void theSetpointOfRangeActionShouldBe(String rangeActionId, double chosenSetpoint) {
        assertEquals(chosenSetpoint, CommonTestData.getRaoResult().getOptimizedSetPointOnState(preventiveState, crac.getRangeAction(rangeActionId)), TOLERANCE_RANGEACTION_SETPOINT);
    }

    @Then("the initial setpoint of RangeAction {string} should be {double}")
    public void theInitialSetpointOfRangeActionShouldBe(String rangeActionId, double chosenSetpoint) {
        assertEquals(chosenSetpoint, CommonTestData.getRaoResult().getPreOptimizationSetPointOnState(preventiveState, crac.getRangeAction(rangeActionId)), TOLERANCE_RANGEACTION_SETPOINT);
    }

    @Then("the initial tap of PstRangeAction {string} should be {int}")
    public void theInitialTapOfPstRangeActionShouldBe(String pstRangeActionId, int chosenTap) {
        assertEquals(chosenTap, CommonTestData.getRaoResult().getPreOptimizationTapOnState(preventiveState, crac.getPstRangeAction(pstRangeActionId)));
    }

    @Then("the setpoint of RangeAction {string} should be {double} MW after {string} at {string}")
    public void theSetpointOfRangeActionShouldBe(String rangeActionId, double chosenSetpoint, String contingencyId, String instant) {
        RangeAction<?> rangeAction = crac.getRangeAction(rangeActionId);
        assertEquals(chosenSetpoint, CommonTestData.getRaoResult().getOptimizedSetPointOnState(getState(contingencyId, instant), rangeAction), TOLERANCE_RANGEACTION_SETPOINT);
    }

    @Then("the setpoint of RangeAction {string} should be {double} before {string} at {string}")
    public void theSetpointOfRangeActionShouldBeBefore(String rangeActionId, double chosenSetpoint, String contingencyId, String instant) {
        RangeAction<?> rangeAction = crac.getRangeAction(rangeActionId);
        assertEquals(chosenSetpoint, CommonTestData.getRaoResult().getPreOptimizationSetPointOnState(getState(contingencyId, instant), rangeAction), TOLERANCE_RANGEACTION_SETPOINT);
    }

    @Then("the tap of PstRangeAction {string} should be {int} before {string} at {string}")
    public void theSetpointOfRangeActionShouldBeBefore(String pstRangeActionId, int chosenTap, String contingencyId, String instant) {
        PstRangeAction pstRangeAction = crac.getPstRangeAction(pstRangeActionId);
        assertEquals(chosenTap, CommonTestData.getRaoResult().getPreOptimizationTapOnState(getState(contingencyId, instant), pstRangeAction));
    }

    private State getState(String contingencyId, String instantId) {
        if ("preventive".equalsIgnoreCase(instantId)) {
            return crac.getPreventiveState();
        } else {
            return crac.getState(contingencyId, crac.getInstant(instantId));
        }
    }

    @Then("the generator {string} should have a targetP of {double} MW at initial state")
    public void theGeneratorShouldHaveATargetPOfAtInitialState(String generatorId, Double expectedTargetP) {
        final double targetPAfterPRA = getTargetPFromVariant(generatorId, "InitialState");
        assertEquals(expectedTargetP, targetPAfterPRA);
    }

    @Then("the generator {string} should have a targetP of {double} MW after PRA")
    public void theGeneratorShouldHaveATargetPOfAfterPra(String generatorId, Double expectedTargetP) {
        final double targetPAfterPRA = getTargetPFromVariant(generatorId, "PreventiveScenario");
        assertEquals(expectedTargetP, targetPAfterPRA);
    }

    @Then("the generator {string} should have a targetP of {double} MW after 2PRA")
    public void theGeneratorShouldHaveATargetPOfAfter2Pra(String generatorId, Double expectedTargetP) {
        final double targetPAfterPRA = getTargetPFromVariant(generatorId, "SecondPreventiveScenario");
        assertEquals(expectedTargetP, targetPAfterPRA);
    }

    @Then("the generator {string} should have a targetP of {double} MW after CRA")
    public void theGeneratorShouldHaveATargetPOfAfterCra(String generatorId, Double expectedTargetP) {
        final double targetPAfterCRA = getTargetPFromVariant(generatorId, "SearchTreeWorkingVariantId");
        assertEquals(expectedTargetP, targetPAfterCRA);
    }

    private double getTargetPFromVariant(final String generatorId, final String searchTreeWorkingVariantId) {
        final String previousWorkingVariantId = network.getVariantManager().getWorkingVariantId();
        network.getVariantManager().setWorkingVariant(searchTreeWorkingVariantId);
        final double targetPAfterCRA = network.getGenerator(generatorId).getTargetP();
        network.getVariantManager().setWorkingVariant(previousWorkingVariantId);
        return targetPAfterCRA;
    }

    /*
    Margins in A
    */

    @Then("the initial margin on cnec {string} should be {double} A")
    public void initialMarginInA(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, CommonTestData.getRaoResult().getMargin(null, crac.getFlowCnec(cnecId), Unit.AMPERE), flowAmpereTolerance(expectedMargin));
    }

    @Then("the margin on cnec {string} after PRA should be {double} A")
    public void afterPraMarginInA(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, CommonTestData.getRaoResult().getMargin(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), Unit.AMPERE), flowAmpereTolerance(expectedMargin));
    }

    @Then("the margin on cnec {string} after ARA should be {double} A")
    public void afterAraMarginInA(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, CommonTestData.getRaoResult().getMargin(crac.getInstant(InstantKind.AUTO), crac.getFlowCnec(cnecId), Unit.AMPERE), flowAmpereTolerance(expectedMargin));
    }

    @Then("the margin on cnec {string} after CRA should be {double} A")
    public void afterCraMarginInA(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, CommonTestData.getRaoResult().getMargin(crac.getInstant(InstantKind.CURATIVE), crac.getFlowCnec(cnecId), Unit.AMPERE), flowAmpereTolerance(expectedMargin));
    }

    @Then("the worst margin is {double} A")
    public void worstMarginInA(double expectedMargin) {
        Pair<FlowCnec, Double> worstCnec = getWorstCnec(Unit.AMPERE, false);
        assertEquals(expectedMargin, worstCnec.getValue(), flowAmpereTolerance(expectedMargin));
    }

    @Then("the worst margin is {double} MW with a tolerance of {double} MW")
    public void worstMarginInMW(double expectedMargin, double delta) {
        Pair<FlowCnec, Double> worstCnec = getWorstCnec(Unit.MEGAWATT, false);
        assertEquals(expectedMargin, worstCnec.getValue(), delta);
    }

    @Then("the worst margin is {double} A on cnec {string}")
    public void worstMarginAndCnecInA(double expectedMargin, String expectedCnecName) {
        Pair<FlowCnec, Double> worstCnec = getWorstCnec(Unit.AMPERE, false);
        assertNotNull(worstCnec.getKey());
        assertEquals(expectedMargin, worstCnec.getValue(), flowAmpereTolerance(expectedMargin));
        assertEquals(expectedCnecName, worstCnec.getKey().getId());
    }

    /*
    Margins in MW
    */

    @Then("the initial margin on cnec {string} should be {double} MW")
    public void initialMarginInMW(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, CommonTestData.getRaoResult().getMargin(null, crac.getFlowCnec(cnecId), Unit.MEGAWATT), flowMegawattTolerance(expectedMargin));
    }

    @Then("the margin on cnec {string} after PRA should be {double} MW")
    public void afterPraMarginInMW(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, CommonTestData.getRaoResult().getMargin(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), Unit.MEGAWATT), flowMegawattTolerance(expectedMargin));
    }

    @Then("the margin on cnec {string} after ARA should be {double} MW")
    public void afterAraMarginInMW(String cnecId, Double expectedMargin) {
        Instant instant = crac.hasAutoInstant() ? crac.getInstant(InstantKind.AUTO) : crac.getOutageInstant();
        assertEquals(expectedMargin, CommonTestData.getRaoResult().getMargin(instant, crac.getFlowCnec(cnecId), Unit.MEGAWATT), flowMegawattTolerance(expectedMargin));
    }

    @Then("the margin on cnec {string} after CRA should be {double} MW")
    public void afterCraMarginInMW(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, CommonTestData.getRaoResult().getMargin(crac.getInstant(InstantKind.CURATIVE), crac.getFlowCnec(cnecId), Unit.MEGAWATT), flowMegawattTolerance(expectedMargin));
    }

    @Then("the worst margin is {double} MW")
    public void worstMarginInMW(double expectedMargin) {
        Pair<FlowCnec, Double> worstCnec = getWorstCnec(Unit.MEGAWATT, false);
        assertEquals(expectedMargin, worstCnec.getValue(), flowMegawattTolerance(expectedMargin));
    }

    @Then("the worst margin is {double} MW on cnec {string}")
    public void worstMarginAndCnecInMW(double expectedMargin, String expectedCnecName) {
        Pair<FlowCnec, Double> worstCnec = getWorstCnec(Unit.MEGAWATT, false);
        assertNotNull(worstCnec.getKey());
        assertEquals(expectedMargin, worstCnec.getValue(), flowMegawattTolerance(expectedMargin));
        assertEquals(expectedCnecName, worstCnec.getKey().getId());
    }

    /*
    Relative margins in A
     */

    @Then("the initial relative margin on cnec {string} should be {double} A")
    public void initialRelativeMarginInA(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, CommonTestData.getRaoResult().getRelativeMargin(null, crac.getFlowCnec(cnecId), Unit.AMPERE), flowAmpereTolerance(expectedMargin));
    }

    @Then("the relative margin on cnec {string} after PRA should be {double} A")
    public void afterPraRelativeMarginInA(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, CommonTestData.getRaoResult().getRelativeMargin(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), Unit.AMPERE), flowAmpereTolerance(expectedMargin));
    }

    @Then("the relative margin on cnec {string} after ARA should be {double} A")
    public void afterAraRelativeMarginInA(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, CommonTestData.getRaoResult().getRelativeMargin(crac.getInstant(InstantKind.AUTO), crac.getFlowCnec(cnecId), Unit.AMPERE), flowAmpereTolerance(expectedMargin));
    }

    @Then("the relative margin on cnec {string} after CRA should be {double} A")
    public void afterCraRelativeMarginInA(String cnecId, Double expectedMargin) {
        assertEquals(
            expectedMargin,
            CommonTestData.getRaoResult().getRelativeMargin(crac.getInstant(InstantKind.CURATIVE), crac.getFlowCnec(cnecId), Unit.AMPERE),
            flowAmpereTolerance(expectedMargin)
        );
    }

    @Then("the worst relative margin is {double} A")
    public void worstRelativeMarginInA(double expectedMargin) {
        Pair<FlowCnec, Double> worstCnec = getWorstCnec(Unit.AMPERE, true);
        assertEquals(expectedMargin, worstCnec.getValue(), flowAmpereTolerance(expectedMargin));
    }

    @Then("the worst relative margin is {double} A on cnec {string}")
    public void worstRelativeMarginAndCnecInA(double expectedMargin, String expectedCnecName) {
        Pair<FlowCnec, Double> worstCnec = getWorstCnec(Unit.AMPERE, true);
        assertNotNull(worstCnec.getKey());
        assertEquals(expectedMargin, worstCnec.getValue(), flowAmpereTolerance(expectedMargin));
        assertEquals(expectedCnecName, worstCnec.getKey().getId());
    }

    /*
    Relative margins in MW
    */

    @Then("the initial relative margin on cnec {string} should be {double} MW")
    public void initialRelativeMarginInMW(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, CommonTestData.getRaoResult().getRelativeMargin(null, crac.getFlowCnec(cnecId), Unit.MEGAWATT), flowMegawattTolerance(expectedMargin));
    }

    @Then("the relative margin on cnec {string} after PRA should be {double} MW")
    public void afterPraRelativeMarginInMW(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, CommonTestData.getRaoResult().getRelativeMargin(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), Unit.MEGAWATT), flowMegawattTolerance(expectedMargin));
    }

    @Then("the relative margin on cnec {string} after ARA should be {double} MW")
    public void afterAraRelativeMarginInMW(String cnecId, Double expectedMargin) {
        assertEquals(
            expectedMargin,
            CommonTestData.getRaoResult().getRelativeMargin(crac.getInstant(InstantKind.AUTO), crac.getFlowCnec(cnecId), Unit.MEGAWATT),
            flowMegawattTolerance(expectedMargin)
        );
    }

    @Then("the relative margin on cnec {string} after CRA should be {double} MW")
    public void afterCraRelativeMarginInMW(String cnecId, Double expectedMargin) {
        assertEquals(
            expectedMargin,
            CommonTestData.getRaoResult().getRelativeMargin(crac.getInstant(InstantKind.CURATIVE), crac.getFlowCnec(cnecId), Unit.MEGAWATT),
            flowMegawattTolerance(expectedMargin)
        );
    }

    @Then("the worst relative margin is {double} MW")
    public void worstRelativeMarginInMW(double expectedMargin) {
        Pair<FlowCnec, Double> worstCnec = getWorstCnec(Unit.MEGAWATT, true);
        assertEquals(expectedMargin, worstCnec.getValue(), flowMegawattTolerance(expectedMargin));
    }

    @Then("the worst relative margin is {double} MW on cnec {string}")
    public void worstRelativeMarginAndCnecInMW(double expectedMargin, String expectedCnecName) {
        Pair<FlowCnec, Double> worstCnec = getWorstCnec(Unit.MEGAWATT, true);
        assertNotNull(worstCnec.getKey());
        assertEquals(expectedMargin, worstCnec.getValue(), flowMegawattTolerance(expectedMargin));
        assertEquals(expectedCnecName, worstCnec.getKey().getId());
    }

    /*
    Flows in A
     */

    @Then("the initial flow on cnec {string} should be {double} A on side 1 and {double} A on side 2")
    public void initialFlowInA(String cnecId, Double expectedFlow1, Double expectedFlow2) {
        assertEquals(expectedFlow1, CommonTestData.getRaoResult().getFlow(null, crac.getFlowCnec(cnecId), TwoSides.ONE, Unit.AMPERE), flowAmpereTolerance(expectedFlow1));
        assertEquals(expectedFlow2, CommonTestData.getRaoResult().getFlow(null, crac.getFlowCnec(cnecId), TwoSides.TWO, Unit.AMPERE), flowAmpereTolerance(expectedFlow2));
    }

    @Then("the flow on cnec {string} after PRA should be {double} A on side 1 and {double} A on side 2")
    public void afterPraFlowInA(String cnecId, Double expectedFlow1, Double expectedFlow2) {
        assertEquals(expectedFlow1, CommonTestData.getRaoResult().getFlow(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), TwoSides.ONE, Unit.AMPERE), flowAmpereTolerance(expectedFlow1));
        assertEquals(expectedFlow2, CommonTestData.getRaoResult().getFlow(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), TwoSides.TWO, Unit.AMPERE), flowAmpereTolerance(expectedFlow2));
    }

    @Then("the flow on cnec {string} after ARA should be {double} A on side 1 and {double} A on side 2")
    public void afterAraFlowInA(String cnecId, Double expectedFlow1, Double expectedFlow2) {
        assertEquals(expectedFlow1, CommonTestData.getRaoResult().getFlow(crac.getInstant(InstantKind.AUTO), crac.getFlowCnec(cnecId), TwoSides.ONE, Unit.AMPERE), flowAmpereTolerance(expectedFlow1));
        assertEquals(expectedFlow2, CommonTestData.getRaoResult().getFlow(crac.getInstant(InstantKind.AUTO), crac.getFlowCnec(cnecId), TwoSides.TWO, Unit.AMPERE), flowAmpereTolerance(expectedFlow2));
    }

    @Then("the flow on cnec {string} after CRA should be {double} A on side 1 and {double} A on side 2")
    public void afterCraFlowInA(String cnecId, Double expectedFlow1, Double expectedFlow2) {
        assertEquals(
            expectedFlow1,
            CommonTestData.getRaoResult().getFlow(crac.getInstant(InstantKind.CURATIVE), crac.getFlowCnec(cnecId), TwoSides.TWO, Unit.AMPERE),
            flowAmpereTolerance(expectedFlow1)
        );
        assertEquals(
            expectedFlow2,
            CommonTestData.getRaoResult().getFlow(crac.getInstant(InstantKind.CURATIVE), crac.getFlowCnec(cnecId), TwoSides.ONE, Unit.AMPERE),
            flowAmpereTolerance(expectedFlow2)
        );
    }

    @Then("the initial flow on cnec {string} should be {double} A on side {int}")
    public void initialFlowInABothSides(String cnecId, Double expectedFlow, Integer side) {
        assertEquals(expectedFlow, CommonTestData.getRaoResult().getFlow(null, crac.getFlowCnec(cnecId), getTwoSideFromInteger(side), Unit.AMPERE), flowAmpereTolerance(expectedFlow));
    }

    @Then("the flow on cnec {string} after PRA should be {double} A on side {int}")
    public void afterPraFlowInABothSides(String cnecId, Double expectedFlow, Integer side) {
        assertEquals(
            expectedFlow,
            CommonTestData.getRaoResult().getFlow(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), getTwoSideFromInteger(side), Unit.AMPERE),
            flowAmpereTolerance(expectedFlow)
        );
    }

    @Then("the flow on cnec {string} after ARA should be {double} A on side {int}")
    public void afterAraFlowInABothSides(String cnecId, Double expectedFlow, Integer side) {
        assertEquals(
            expectedFlow,
            CommonTestData.getRaoResult().getFlow(crac.getInstant(InstantKind.AUTO), crac.getFlowCnec(cnecId), getTwoSideFromInteger(side), Unit.AMPERE),
            flowAmpereTolerance(expectedFlow)
        );
    }

    @Then("the flow on cnec {string} after CRA should be {double} A on side {int}")
    public void afterCraFlowInABothSides(String cnecId, Double expectedFlow, Integer side) {
        Instant lastCurativeInstant = crac.getInstants(InstantKind.CURATIVE).stream()
            .sorted(Comparator.comparingInt(instant -> -instant.getOrder()))
            .toList().get(0);
        assertEquals(expectedFlow, CommonTestData.getRaoResult().getFlow(lastCurativeInstant, crac.getFlowCnec(cnecId), getTwoSideFromInteger(side), Unit.AMPERE), flowAmpereTolerance(expectedFlow));
    }

    @Then("the flow on cnec {string} after {string} instant remedial actions should be {double} A on side {int}")
    public void afterInstantFlowInABothSides(String cnecId, String instantId, Double expectedFlow, Integer side) {
        Instant instant = crac.getInstant(instantId);
        assertEquals(expectedFlow, CommonTestData.getRaoResult().getFlow(instant, crac.getFlowCnec(cnecId), getTwoSideFromInteger(side), Unit.AMPERE), flowAmpereTolerance(expectedFlow));
    }

    /*
    Flows in MW
    */

    @Then("the initial flow on cnec {string} should be {double} MW on side 1 and {double} MW on side 2")
    public void initialFlowInMW(String cnecId, Double expectedFlow1, Double expectedFlow2) {
        assertEquals(expectedFlow1, CommonTestData.getRaoResult().getFlow(null, crac.getFlowCnec(cnecId), TwoSides.ONE, Unit.MEGAWATT), flowMegawattTolerance(expectedFlow1));
        assertEquals(expectedFlow2, CommonTestData.getRaoResult().getFlow(null, crac.getFlowCnec(cnecId), TwoSides.TWO, Unit.MEGAWATT), flowMegawattTolerance(expectedFlow2));
    }

    @Then("the flow on cnec {string} after PRA should be {double} MW on side 1 and {double} MW on side 2")
    public void afterPraFlowInMW(String cnecId, Double expectedFlow1, Double expectedFlow2) {
        assertEquals(expectedFlow1, CommonTestData.getRaoResult().getFlow(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), TwoSides.ONE, Unit.MEGAWATT), flowMegawattTolerance(expectedFlow1));
        assertEquals(expectedFlow2, CommonTestData.getRaoResult().getFlow(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), TwoSides.TWO, Unit.MEGAWATT), flowMegawattTolerance(expectedFlow2));
    }

    @Then("the flow on cnec {string} after ARA should be {double} MW on side 1 and {double} MW on side 2")
    public void afterAraFlowInMW(String cnecId, Double expectedFlow1, Double expectedFlow2) {
        assertEquals(
            expectedFlow1,
            CommonTestData.getRaoResult().getFlow(crac.getInstant(InstantKind.AUTO), crac.getFlowCnec(cnecId), TwoSides.ONE, Unit.MEGAWATT),
            flowMegawattTolerance(expectedFlow1)
        );
        assertEquals(
            expectedFlow2,
            CommonTestData.getRaoResult().getFlow(crac.getInstant(InstantKind.AUTO), crac.getFlowCnec(cnecId), TwoSides.TWO, Unit.MEGAWATT),
            flowMegawattTolerance(expectedFlow2)
        );
    }

    @Then("the flow on cnec {string} after CRA should be {double} MW on side 1 and {double} MW on side 2")
    public void afterCraFlowInMW(String cnecId, Double expectedFlow1, Double expectedFlow2) {
        Instant lastCurativeInstant = crac.getInstants(InstantKind.CURATIVE).stream().sorted(Comparator.comparingInt(instant -> -instant.getOrder())).toList().get(0);
        assertEquals(expectedFlow1, CommonTestData.getRaoResult().getFlow(lastCurativeInstant, crac.getFlowCnec(cnecId), TwoSides.ONE, Unit.MEGAWATT), flowMegawattTolerance(expectedFlow1));
        assertEquals(expectedFlow2, CommonTestData.getRaoResult().getFlow(lastCurativeInstant, crac.getFlowCnec(cnecId), TwoSides.TWO, Unit.MEGAWATT), flowMegawattTolerance(expectedFlow2));
    }

    @Then("the flow on cnec {string} after {string} instant remedial actions should be {double} MW on side 1 and {double} MW on side 2")
    public void afterInstantFlowInMW(String cnecId, String instantId, Double expectedFlow1, Double expectedFlow2) {
        Instant instant = crac.getInstant(instantId);
        assertEquals(expectedFlow1, CommonTestData.getRaoResult().getFlow(instant, crac.getFlowCnec(cnecId), TwoSides.ONE, Unit.MEGAWATT), flowMegawattTolerance(expectedFlow1));
        assertEquals(expectedFlow2, CommonTestData.getRaoResult().getFlow(instant, crac.getFlowCnec(cnecId), TwoSides.TWO, Unit.MEGAWATT), flowMegawattTolerance(expectedFlow2));
    }

    @Then("the initial flow on cnec {string} should be {double} MW on side {int}")
    public void initialFlowInMWBothSides(String cnecId, Double expectedFlow, Integer side) {
        assertEquals(expectedFlow, CommonTestData.getRaoResult().getFlow(null, crac.getFlowCnec(cnecId), getTwoSideFromInteger(side), Unit.MEGAWATT), flowMegawattTolerance(expectedFlow));
    }

    @Then("the flow on cnec {string} after PRA should be {double} MW on side {int}")
    public void afterPraFlowInMWBothSides(String cnecId, Double expectedFlow, Integer side) {
        assertEquals(
            expectedFlow,
            CommonTestData.getRaoResult().getFlow(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), getTwoSideFromInteger(side), Unit.MEGAWATT),
            flowMegawattTolerance(expectedFlow)
        );
    }

    @Then("the flow on cnec {string} after ARA should be {double} MW on side {int}")
    public void afterAraFlowInMWBothSides(String cnecId, Double expectedFlow, Integer side) {
        assertEquals(
            expectedFlow,
            CommonTestData.getRaoResult().getFlow(crac.getInstant(InstantKind.AUTO), crac.getFlowCnec(cnecId), getTwoSideFromInteger(side), Unit.MEGAWATT),
            flowMegawattTolerance(expectedFlow)
        );
    }

    @Then("the flow on cnec {string} after CRA should be {double} MW on side {int}")
    public void afterCraFlowInMWBothSides(String cnecId, Double expectedFlow, Integer side) {
        Instant lastCurativeInstant = crac.getInstants(InstantKind.CURATIVE).stream()
            .sorted(Comparator.comparingInt(instant -> -instant.getOrder()))
            .toList().get(0);
        assertEquals(
            expectedFlow,
            CommonTestData.getRaoResult().getFlow(lastCurativeInstant, crac.getFlowCnec(cnecId), getTwoSideFromInteger(side), Unit.MEGAWATT),
            flowMegawattTolerance(expectedFlow)
        );
    }

    @Then("the flow on cnec {string} after {string} instant remedial actions should be {double} MW on side {int}")
    public void afterInstantFlowInMWBothSides(String cnecId, String instantId, Double expectedFlow, Integer side) {
        Instant instant = crac.getInstant(instantId);
        assertEquals(expectedFlow, CommonTestData.getRaoResult().getFlow(instant, crac.getFlowCnec(cnecId), getTwoSideFromInteger(side), Unit.MEGAWATT), flowMegawattTolerance(expectedFlow));
    }

    /*
    Thresholds
     */

    private void testThreshold(String upperOrLower, String cnecId, Double expectedBound, Unit unit) {
        FlowCnec cnec = crac.getFlowCnec(cnecId);
        if (cnec.getMonitoredSides().size() != 1) {
            throw new OpenRaoException("Cannot chose side");
        }
        TwoSides side = cnec.getMonitoredSides().iterator().next();
        Double bound = null;
        if ("upper".equalsIgnoreCase(upperOrLower)) {
            bound = crac.getFlowCnec(cnecId).getUpperBound(side, unit).orElseThrow();
        } else if ("lower".equalsIgnoreCase(upperOrLower)) {
            bound = crac.getFlowCnec(cnecId).getLowerBound(side, unit).orElseThrow();
        }
        assertEquals(expectedBound, bound, flowAmpereTolerance(expectedBound));
    }

    @Then("the {string} threshold on cnec {string} should be {double} A")
    public void thresholdOnCnecInA(String upperOrLower, String cnecId, Double expectedBound) {
        testThreshold(upperOrLower, cnecId, expectedBound, Unit.AMPERE);
    }

    @Then("the {string} threshold on cnec {string} should be {double} MW")
    public void thresholdOnCnecInMW(String upperOrLower, String cnecId, Double expectedBound) {
        testThreshold(upperOrLower, cnecId, expectedBound, Unit.MEGAWATT);
    }

    /*
    Loopflows
     */

    @Then("the initial loopflow on cnec {string} should be {double} MW")
    public void initialLoopflowInMW(String cnecId, Double expectedFlow) {
        assertEquals(expectedFlow,
            crac.getFlowCnec(cnecId).getMonitoredSides().stream()
                .map(side -> raoResult.getLoopFlow(null, crac.getFlowCnec(cnecId), side, Unit.MEGAWATT))
                .max(Double::compareTo).orElseThrow(),
            flowMegawattTolerance(expectedFlow));
    }

    @Then("the loopflow on cnec {string} after PRA should be {double} MW")
    public void afterPraLoopflowInMW(String cnecId, Double expectedFlow) {
        assertEquals(expectedFlow,
            crac.getFlowCnec(cnecId).getMonitoredSides().stream()
                .map(side -> raoResult.getLoopFlow(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), side, Unit.MEGAWATT))
                .max(Double::compareTo).orElseThrow(),
            flowMegawattTolerance(expectedFlow));
    }

    @Then("the loopflow on cnec {string} after ARA should be {double} MW")
    public void afterAraLoopflowInMW(String cnecId, Double expectedFlow) {
        assertEquals(expectedFlow,
            crac.getFlowCnec(cnecId).getMonitoredSides().stream()
                .map(side -> raoResult.getLoopFlow(crac.getInstant(InstantKind.AUTO), crac.getFlowCnec(cnecId), side, Unit.MEGAWATT))
                .max(Double::compareTo).orElseThrow(),
            flowMegawattTolerance(expectedFlow));
    }

    @Then("the loopflow on cnec {string} after CRA should be {double} MW")
    public void afterCraLoopflowInMW(String cnecId, Double expectedFlow) {
        assertEquals(expectedFlow,
            crac.getFlowCnec(cnecId).getMonitoredSides().stream()
                .map(side -> raoResult.getLoopFlow(crac.getInstant(InstantKind.CURATIVE), crac.getFlowCnec(cnecId), side, Unit.MEGAWATT))
                .max(Double::compareTo).orElseThrow(),
            flowMegawattTolerance(expectedFlow));
    }

    @Then("the loopflow on cnec {string} after loopflow computation should be {double} MW")
    public void loopflowComputationLoopflowInMW(String cnecId, Double expectedFlow) {
        assertEquals(expectedFlow,
            crac.getFlowCnec(cnecId).getMonitoredSides().stream()
                .map(side -> loopFlowResult.getLoopFlow(crac.getFlowCnec(cnecId), side, Unit.MEGAWATT))
                .max(Double::compareTo).orElseThrow(),
            flowMegawattTolerance(expectedFlow));
    }

    @Then("the loopflow on cnec {string} after loopflow computation should be {double} A")
    public void loopflowComputationLoopflowInA(String cnecId, Double expectedFlow) {
        assertEquals(expectedFlow,
            crac.getFlowCnec(cnecId).getMonitoredSides().stream()
                .map(side -> loopFlowResult.getLoopFlow(crac.getFlowCnec(cnecId), side, Unit.AMPERE))
                .max(Double::compareTo).orElseThrow(),
            flowMegawattTolerance(expectedFlow));
    }

    @Then("the loopflow threshold on cnec {string} should be {double} MW")
    public void loopflowThresholdInMW(String cnecId, Double expectedFlow) {
        FlowCnec cnec = crac.getFlowCnec(cnecId);
        assertEquals(expectedFlow, cnec.getExtension(LoopFlowThreshold.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT), flowMegawattTolerance(expectedFlow));
    }

    /*
    PTDF sums
     */

    @Then("the absolute PTDF sum on cnec {string} initially should be {double}")
    public void absPtdfSum(String cnecId, Double expectedPtdfSum) {
        FlowCnec cnec = crac.getFlowCnec(cnecId);
        assertEquals(expectedPtdfSum, CommonTestData.getRaoResult().getPtdfZonalSum(null, cnec, cnec.getMonitoredSides().iterator().next()), TOLERANCE_PTDF);
    }

    @Then("the absolute PTDF sum on cnec {string} after {string} should be {double}")
    public void absPtdfSumAfterInstant(String cnecId, String instantKind, Double expectedPtdfSum) {
        FlowCnec cnec = crac.getFlowCnec(cnecId);
        assertEquals(
            expectedPtdfSum,
            CommonTestData.getRaoResult().getPtdfZonalSum(crac.getInstant(InstantKind.valueOf(instantKind.toUpperCase())), cnec, cnec.getMonitoredSides().iterator().next()),
            TOLERANCE_PTDF
        );
    }

    private void launchRao(int timeLimit) {
        launchRao(null, null, raoImplementation, timeLimit);
    }

    private void launchRao(String timestamp, String raoType) {
        launchRao(timestamp, 0.0, raoType, null);
    }

    private void launchRao(String timestamp, Double loopflowLimitAsPmaxPercentageInput, String raoType, Integer timeLimit) {
        if (CommonTestData.getTimeCoupledRaoInput() != null) {
            CommonTestData.setReportNode(
                ReportNode.newRootReportNode()
                    .withAllResourceBundlesFromClasspath()
                    .withMessageTemplate("test.rootnode")
                    .build()
            );
            timeCoupledRaoResult = Rao.find(raoImplementation)
                .run(CommonTestData.getTimeCoupledRaoInput(), getRaoParameters(), CommonTestData.getReportNode());
        } else {
            try {
                CommonTestData.loadData(timestamp);
                network = CommonTestData.getNetwork();
                crac = CommonTestData.getCrac();
                preventiveState = crac.getPreventiveState();
                CommonTestData.setReportNode(
                    ReportNode.newRootReportNode()
                        .withAllResourceBundlesFromClasspath()
                        .withMessageTemplate("test.rootnode")
                        .build()
                );
                raoResult = RaoUtils.runRao(raoType, loopflowLimitAsPmaxPercentageInput, timeLimit, CommonTestData.getReportNode(), forceFastRao);
                CommonTestData.setRaoResult(raoResult);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void launchLoopflowComputation(String timestamp, String sensitivityProvider, String loadFlowProvider) {
        try {
            CommonTestData.loadData(timestamp);
            network = CommonTestData.getNetwork();
            crac = CommonTestData.getCrac();
            RaoParameters raoParameters = CommonTestData.getRaoParameters();
            SensitivityAnalysisParameters sensitivityAnalysisParameters = getSensitivityWithLoadFlowParameters(raoParameters);
            ReferenceProgram referenceProgram = CommonTestData.getReferenceProgram() != null ?
                CommonTestData.getReferenceProgram() :
                ReferenceProgramBuilder.buildReferenceProgram(network, loadFlowProvider, sensitivityAnalysisParameters.getLoadFlowParameters());
            ZonalData<SensitivityVariableSet> glsks = CommonTestData.getLoopflowGlsks();

            // run loopFlowComputation
            LoopFlowComputation loopFlowComputation = new LoopFlowComputationImpl(glsks, referenceProgram, getFlowUnit(raoParameters));

            this.loopFlowResult = loopFlowComputation.calculateLoopFlows(network, sensitivityProvider, sensitivityAnalysisParameters, crac.getFlowCnecs(), crac.getOutageInstant());

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Pair<FlowCnec, Double> getWorstCnec(Unit unit, boolean relative) {
        Set<FlowCnec> flowCnecs = crac.getFlowCnecs().stream().filter(Cnec::isOptimized).collect(Collectors.toSet());
        // QUESTION: what about TSOs not sharing CRAs for which curative CNECs should not be optimized?
        double worstMargin = Double.MAX_VALUE;
        Set<FlowCnec> worstCnecs = new HashSet<>();
        double margin;
        //Filter flow cnecs from failed perimeters
        final RaoResult commonTestRaoResult = CommonTestData.getRaoResult();
        Set<State> failedStates = crac.getStates().stream()
                .filter(state -> commonTestRaoResult.getComputationStatus(state).equals(ComputationStatus.FAILURE))
                        .collect(Collectors.toSet());
        for (FlowCnec flowCnec : flowCnecs) {
            if (failedStates.contains(flowCnec.getState())) {
                continue;
            }
            try {
                if (relative) {
                    margin = commonTestRaoResult.getRelativeMargin(flowCnec.getState().getInstant(), flowCnec, unit);
                } else {
                    margin = commonTestRaoResult.getMargin(flowCnec.getState().getInstant(), flowCnec, unit);
                }
                if (Double.isNaN(margin)) {
                    // Try getting margin before flowCnec's state afterOptimizing state
                    // This could happen for instance if optimization on said state failed
                    if (relative) {
                        margin = commonTestRaoResult.getRelativeMargin(crac.getInstantBefore(flowCnec.getState().getInstant()), flowCnec, unit);
                    } else {
                        margin = commonTestRaoResult.getMargin(crac.getInstantBefore(flowCnec.getState().getInstant()), flowCnec, unit);
                    }
                }
            } catch (OpenRaoException e) {
                margin = Double.MAX_VALUE;
            }

            if (margin == worstMargin) {
                worstCnecs.add(flowCnec);
            } else if (margin < worstMargin) {
                worstMargin = margin;
                worstCnecs = new HashSet<>();
                worstCnecs.add(flowCnec);
            }
        }

        FlowCnec worstCnecAlphabetical = worstCnecs.stream().sorted(Comparator.comparing(Identifiable::getId)).toList().getFirst();
        return new ImmutablePair<>(worstCnecAlphabetical, worstMargin);
    }

    /*
    RaoResult infos
     */

    @Then("the execution details should be {string}")
    public void getOptimizationSteps(String string) {
        assertEquals(string, CommonTestData.getRaoResult().getExecutionDetails());
    }

    /*
    Time-coupled steps
     */

    @Given("time-coupled RefProg file is {string}")
    public static void refProgFileIs(String path) {
        refProgPath = getResourcesPath().concat("refprogs/").concat(path);
    }

    @After()
    public void cleanModifiedNetworks() throws IOException {
        if (networkFolderPathPostIcsImport != null) {
            deleteDirectoryRecursively(Paths.get(networkFolderPathPostIcsImport));
        }
    }

    @Before()
    public void loggerConfiguration(Scenario scenario) {
        String scenarioNameFinal = scenario.getName()
            .replaceAll("\\s+", "-")
            .replaceAll("/", "-")
            .replaceAll(":", "_");
        MDC.put("scenarioName", scenarioNameFinal);
    }

    @Given("network files are in folder {string}")
    public static void networkFilesAreIn(String folderPath) throws IOException {
        setNetworkInputs(folderPath);
        setNetworkInputsPostIcs(folderPath);
    }

    @Given("crac files are in folder {string}")
    public static void cracFilesAreIn(String folderPath) {
        setCracInputs(folderPath);
        useIndividualCracs = true;
    }

    private static void setNetworkInputs(String folderPath) {
        networkFolderPath = getResourcesPath().concat("cases/").concat(folderPath + "/");
    }

    private static void setCracInputs(String folderPath) {
        cracFolderPath = getResourcesPath().concat("crac/").concat(folderPath + "/");

    }

    private static void setNetworkInputsPostIcs(String folderPath) throws IOException {
        networkFolderPathPostIcsImport = getResourcesPath().concat("cases/").concat(folderPath + "-postIcsImport/");
        if (!Files.isDirectory(Path.of(networkFolderPathPostIcsImport))) {
            Files.createDirectories(Path.of(networkFolderPathPostIcsImport));
        }
    }

    @Given("ics static file is {string}")
    public static void icsStaticFileIs(String path) {
        icsStaticPath = getResourcesPath().concat("ics/static/").concat(path);
    }

    @Given("ics series file is {string}")
    public static void icsSeriesFileIs(String path) {
        icsSeriesPath = getResourcesPath().concat("ics/series/").concat(path);
    }

    @Given("ics gsk file is {string}")
    public static void icsGskFileIs(String path) {
        icsGskPath = getResourcesPath().concat("ics/gsk/").concat(path);
    }

    @Given("time-coupled constraints are in file {string} and rao inputs are:")
    public static void timeCoupledRaoInputsAre(String timeCoupledConstraintsPath, DataTable arg1) throws IOException {
        loadDataForTimeCoupledRao(timeCoupledConstraintsPath, arg1);
    }

    public static void loadDataForTimeCoupledRao(String timeCoupledConstraintsPath, DataTable arg1) throws IOException {
        raoParameters = buildConfig(getFile(raoParametersPath));

        String timeCoupledConstraintsFolderPath = getResourcesPath().concat("timeCoupledConstraints/");

        TemporalData<RaoInput> raoInputs = new TemporalDataImpl<>();
        List<Map<String, String>> inputs = arg1.asMaps(String.class, String.class);
        for (Map<String, String> tsInput : inputs) {
            OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(tsInput.get("Timestamp"));
            LazyNetwork lazyNetwork = new LazyNetwork(importNetwork(getFile(networkFolderPath.concat(tsInput.get("Network"))), false));
            Crac crac = importCrac(getFile(cracFolderPath.concat(tsInput.get("CRAC"))), lazyNetwork, null).getLeft();
            raoInputs.put(offsetDateTime, RaoInput.build(lazyNetwork, crac).build());
            lazyNetwork.release();
        }

        TimeCoupledConstraints timeCoupledConstraints = JsonTimeCoupledConstraints.read(new FileInputStream(timeCoupledConstraintsFolderPath.concat(timeCoupledConstraintsPath)));
        CommonTestData.setTimeCoupledRaoInput(new TimeCoupledRaoInput(raoInputs, timeCoupledConstraints));
    }

    @Given("time-coupled rao inputs for CORE are:")
    public static void coreTimeCoupledRaoInputsAre(DataTable arg1) throws IOException {
        loadDataForCoreTimeCoupledRao(arg1);
    }

    public static void loadDataForCoreTimeCoupledRao(DataTable arg1) throws IOException {
        cracCreationContexts = new HashMap<>();
        CracCreationParameters cracCreationParameters;
        try {
            String ccpToImport = getResourcesPath().concat(DEFAULT_CRAC_CREATION_PARAMETERS_PATH);
            InputStream cracCreationParametersInputStream = new BufferedInputStream(new FileInputStream(getFile(ccpToImport)));
            cracCreationParameters = JsonCracCreationParameters.read(cracCreationParametersInputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        File cracFile = null;
        if (!useIndividualCracs) {
            cracFile = getFile(cracPath);
        }

        raoParameters = buildConfig(getFile(raoParametersPath));

        TemporalData<RaoInput> raoInputs = new TemporalDataImpl<>();
        List<Map<String, String>> inputs = arg1.asMaps(String.class, String.class);
        for (Map<String, String> tsInput : inputs) {
            OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(tsInput.get("Timestamp"));
            TECHNICAL_LOGS.info("**** Loading data for TS {} ****", offsetDateTime);
            Network network = importNetwork(getFile(networkFolderPath.concat(tsInput.get("Network"))), false);
            CoreCcPreprocessor.applyCoreCcNetworkPreprocessing(network);
            LazyNetwork lazyNetwork = new LazyNetwork(network);
            // Crac
            Pair<Crac, CracCreationContext> cracImportResult;
            if (useIndividualCracs) { // only works with json
                cracImportResult = importCrac(getFile(cracFolderPath.concat(tsInput.get("Crac"))), lazyNetwork, null);
            } else {
                cracCreationParameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(offsetDateTime);
                cracImportResult = importCrac(cracFile, lazyNetwork, cracCreationParameters);
            }
            RaoInput raoInput = RaoInput
                .build(lazyNetwork, cracImportResult.getLeft())
                .build();
            raoInputs.put(offsetDateTime, raoInput);
            cracCreationContexts.put(offsetDateTime, cracImportResult.getRight());
            lazyNetwork.release();
        }
        InputStream gskInputStream = icsGskPath == null ? null : new FileInputStream(getFile(icsGskPath));

        FbConstraintCracCreationParameters fbConstraintParameters = cracCreationParameters.getExtension(FbConstraintCracCreationParameters.class);
        if (fbConstraintParameters == null) {
            TECHNICAL_LOGS.warn("No FB Constraint CRAC creation parameters found. Default parameters will be used.");
            fbConstraintParameters = new FbConstraintCracCreationParameters();
        }

        IcsData icsData = IcsDataImporter.read(
            new FileInputStream(getFile(icsStaticPath)),
            new FileInputStream(getFile(icsSeriesPath)),
            gskInputStream,
            raoInputs.getTimestamps().stream().sorted().toList());

        CommonTestData.setTimeCoupledRaoInput(icsData.processAllRedispatchingActions(
            new TimeCoupledRaoInput(raoInputs, new TimeCoupledConstraints()),
            fbConstraintParameters.getIcsCostUp(),
            fbConstraintParameters.getIcsCostDown(),
            networkFolderPathPostIcsImport.concat(inputs.getFirst().get("Network")).split(".uct")[0]));
    }

    private static void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Then("the optimized margin on {string} for timestamp {string} is {double} MW")
    public static void theOptimizedMarginOnCnecForTimestampIsMW(String cnecId, String timestamp, double margin) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        FlowCnec flowCnec = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac().getFlowCnec(cnecId);
        Instant afterCra = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac().getLastInstant();
        assertEquals(margin,
            timeCoupledRaoResult.getIndividualRaoResult(offsetDateTime).getMargin(afterCra, flowCnec, Unit.MEGAWATT),
            RaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the functional cost for timestamp {string} is {double}")
    public static void theFunctionalCostForTimestampIs(String timestamp, double functionalCost) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Instant afterCra = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac().getLastInstant();
        assertEquals(functionalCost,
            timeCoupledRaoResult.getFunctionalCost(afterCra, offsetDateTime),
            RaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the total cost for timestamp {string} is {double}")
    public static void theTotalCostForTimestampIs(String timestamp, double totalCost) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Instant afterCra = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac().getLastInstant();
        assertEquals(totalCost,
            timeCoupledRaoResult.getCost(afterCra, offsetDateTime),
            RaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the functional cost for all timestamps is {double}")
    public static void theFunctionalCostForAllTimestampsIs(double functionalCost) {
        OffsetDateTime firstTimestamp = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getTimestamps().getFirst();
        Instant lastInstant = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(firstTimestamp).orElseThrow().getCrac().getLastInstant();
        assertEquals(functionalCost,
            timeCoupledRaoResult.getGlobalFunctionalCost(lastInstant),
            RaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the total cost for all timestamps is {double}")
    public static void theTotalCostForAllTimestampsIs(double totalCost) {
        OffsetDateTime firstTimestamp = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getTimestamps().getFirst();
        Instant lastInstant = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(firstTimestamp).orElseThrow().getCrac().getLastInstant();
        assertEquals(totalCost,
            timeCoupledRaoResult.getGlobalCost(lastInstant),
            RaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the preventive power of generator {string} at timestamp {string} is {double} MW")
    public static void getGeneratorPower(String networkElementId, String timestamp, double expectedPower) {
        assertPowerValue(networkElementId, timestamp, expectedPower);
    }

    @Then("the preventive power of load {string} at timestamp {string} is {double} MW")
    public static void getLoadPower(String networkElementId, String timestamp, double expectedPower) {
        assertPowerValue(networkElementId, timestamp, -expectedPower);
    }

    @Then("the remedial action {string} is used at timestamp {string} in preventive")
    public static void remedialActionUsedInPreventive(String remedialActionId, String timestamp) {
        assertTrue(isRemedialActionUsed(remedialActionId, timestamp, "", "preventive"));
    }

    @Then("the remedial action {string} is used at timestamp {string} after {string} at {string}")
    public void remedialActionUsedPostContingency(String remedialActionId, String timestamp, String contingencyId, String instant) {
        assertTrue(isRemedialActionUsed(remedialActionId, timestamp, contingencyId, instant));
    }

    @Then("the remedial action {string} is not used at timestamp {string} in preventive")
    public static void remedialActionNotUsedInPreventive(String remedialActionId, String timestamp) {
        assertFalse(isRemedialActionUsed(remedialActionId, timestamp, "", "preventive"));
    }

    @Then("the remedial action {string} is not used at timestamp {string} after {string} at {string}")
    public void remedialActionNotUsedPostContingency(String remedialActionId, String timestamp, String contingencyId, String instant) {
        assertFalse(isRemedialActionUsed(remedialActionId, timestamp, contingencyId, instant));
    }

    @Then("the time-coupled security status should be {string}")
    public void timeCoupledStatusShouldBe(String status) {
        assertEquals(
            "secured".equalsIgnoreCase(status),
            timeCoupledRaoResult.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE, PhysicalParameter.VOLTAGE)
        );
    }

    @Then("the tap of PstRangeAction {string} at timestamp {string} after {string} at {string} should be {int}")
    public void theTapOfPstRangeActionPostContingencyShouldBe(String pstRangeActionId, String timestamp, String contingencyId, String instant, int chosenPstTap) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Crac crac = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac();
        assertEquals(
            chosenPstTap,
            timeCoupledRaoResult
                .getIndividualRaoResult(offsetDateTime)
                .getOptimizedTapOnState(crac.getState(contingencyId, crac.getInstant(instant)), (PstRangeAction) crac.getRangeAction(pstRangeActionId))
        );
    }

    @Then("the preventive tap of PstRangeAction {string} at timestamp {string} should be {int}")
    public void theTapOfPreventivePstRangeActionShouldBe(String pstRangeActionId, String timestamp, int chosenPstTap) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Crac crac = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac();
        assertEquals(
            chosenPstTap,
            timeCoupledRaoResult
                .getIndividualRaoResult(offsetDateTime)
                .getOptimizedTapOnState(crac.getPreventiveState(), (PstRangeAction) crac.getRangeAction(pstRangeActionId)));
    }

    @Then("the CNEC {string} is overloaded before time-coupled optimization")
    public void cnecIsOverloadedBeforeTimeCoupledOptimization(String cnecId) {
        assertNotNull(timeCoupledRaoResult.getExtension(PreTimeCouplingOverloadedCnecs.class));
        assertTrue(timeCoupledRaoResult.getExtension(PreTimeCouplingOverloadedCnecs.class).getCriticalCnecIds().contains(cnecId));
    }

    private static void assertPowerValue(String networkElementId, String timestamp, double expectedPower) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Crac crac = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac();
        State preventiveState = crac.getPreventiveState();
        Optional<InjectionRangeAction> injectionRangeAction = crac.getRangeActions(preventiveState)
            .stream()
            .filter(InjectionRangeAction.class::isInstance)
            .map(InjectionRangeAction.class::cast)
            .filter(injection -> injection.getInjectionDistributionKeysIds().keySet().stream().anyMatch(networkElement -> networkElement.equals(networkElementId)))
            .findFirst();
        assertTrue(injectionRangeAction.isPresent());
        assertEquals(
            expectedPower,
            timeCoupledRaoResult.getOptimizedSetPointOnState(preventiveState, injectionRangeAction.get()) / injectionRangeAction.get().getInjectionDistributionKeysIds().get(networkElementId),
            TOLERANCE_REDISPATCHING_VALUE
        );
    }

    private static boolean isRemedialActionUsed(String rangeActionId, String timestamp, String contingencyId, String instant) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Crac crac = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac();
        State state = getState(crac, contingencyId, instant);
        return timeCoupledRaoResult.getIndividualRaoResult(offsetDateTime).isActivatedDuringState(state, crac.getRemedialAction(rangeActionId));
    }

    private static State getState(Crac crac, String contingencyId, String instantId) {
        if ("preventive".equalsIgnoreCase(instantId)) {
            return crac.getPreventiveState();
        } else {
            return crac.getState(contingencyId, crac.getInstant(instantId));
        }
    }
}
