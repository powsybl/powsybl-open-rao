/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.steps;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracloopflowextension.LoopFlowThreshold;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgramBuilder;
import com.powsybl.openrao.loopflowcomputation.LoopFlowComputation;
import com.powsybl.openrao.loopflowcomputation.LoopFlowComputationImpl;
import com.powsybl.openrao.loopflowcomputation.LoopFlowResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.openrao.tests.utils.RaoUtils;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class SearchTreeRaoSteps {

    private static final String SEARCH_TREE_RAO = "SearchTreeRao";
    private static final double TOLERANCE_FLOW_IN_AMPERE = 5.0;
    private static final double TOLERANCE_FLOW_IN_MEGAWATT = 5.0;
    private static final double TOLERANCE_FLOW_RELATIVE = 1.5 / 100;
    private static final double TOLERANCE_PTDF = 1e-3;
    private static final double TOLERANCE_RANGEACTION_SETPOINT = 5.0;

    private Crac crac;
    private RaoResult raoResult;
    private LoopFlowResult loopFlowResult;
    private Network network;
    private State preventiveState;

    private static double flowAmpereTolerance(double expectedValue) {
        return Math.max(TOLERANCE_FLOW_IN_AMPERE, TOLERANCE_FLOW_RELATIVE * Math.abs(expectedValue));
    }

    private static double flowMegawattTolerance(Double expectedValue) {
        return Math.max(TOLERANCE_FLOW_IN_MEGAWATT, TOLERANCE_FLOW_RELATIVE * Math.abs(expectedValue));
    }

    @When("I launch search_tree_rao")
    public void iLaunchSearchTreeRao() {
        iLaunchSearchTreeRao(null);
    }

    @When("I launch search_tree_rao with a time limit of {int} seconds")
    public void iLaunchSearchTreeRaoWithTimeLimit(int timeLimit) {
        launchRao(timeLimit);
    }

    @When("I launch search_tree_rao at {string}")
    public void iLaunchSearchTreeRao(String timestamp) {
        launchRao(null, null, timestamp, SEARCH_TREE_RAO);
    }

    @When("I launch search_tree_rao at {string} on {string}")
    public void iLaunchSearchTreeRaoAtTimestampOnContingency(String timestamp, String contingencyId) {
        launchRao(contingencyId, null, timestamp, SEARCH_TREE_RAO);
    }

    @When("I launch search_tree_rao at {string} on preventive state")
    public void iLaunchSearchTreeRaoOnPreventiveState(String timestamp) {
        launchRao(null, InstantKind.PREVENTIVE, timestamp, SEARCH_TREE_RAO);
    }

    @When("I launch search_tree_rao at {string} after {string} at {string}")
    public void iLaunchSearchTreeRao(String timestamp, String contingencyId, String instantKind) {
        launchRao(contingencyId, InstantKind.valueOf(instantKind.toUpperCase()), timestamp, SEARCH_TREE_RAO);
    }

    @When("I launch search_tree_rao on preventive state")
    public void iLaunchSearchTreeRaoOnPreventiveState() {
        launchRao(null, InstantKind.PREVENTIVE, null, SEARCH_TREE_RAO);
    }

    @When("I launch search_tree_rao after {string} at {string}")
    public void iLaunchSearchTreeRao(String contingencyId, String instantKind) {
        launchRao(contingencyId, InstantKind.valueOf(instantKind.toUpperCase()), null, null, SEARCH_TREE_RAO, null);
    }

    @When("I launch loopflow search_tree_rao with default loopflow limit as {double} percent of pmax")
    public void iLaunchSearchTreeRaoWithDefaultLoopflowLimit(double percentage) {
        launchRao(null, null, null, percentage, SEARCH_TREE_RAO, null);
    }

    @When("I launch loopflow search_tree_rao at {string} with default loopflow limit as {double} percent of pmax")
    public void iLaunchSearchTreeRaoWithDefaultLoopflowLimit(String timestamp, double percentage) {
        launchRao(null, null, timestamp, percentage, SEARCH_TREE_RAO, null);
    }

    @When("I launch loopflow_computation with OpenLoadFlow")
    public void iLaunchLoopflowComputation() {
        iLaunchLoopflowComputation(null);
    }

    @When("I launch loopflow_computation with OpenLoadFlow at {string}")
    public void iLaunchLoopflowComputation(String timestamp) {
        launchLoopflowComputation(timestamp, "OpenLoadFlow", "OpenLoadFlow");
    }

    @Then("the calculation succeeds")
    public void theCalculationSucceeds() {
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus());
    }

    @Then("the calculation partially fails")
    public void theCalculationPartiallyFails() {
        assertEquals(ComputationStatus.PARTIAL_FAILURE, raoResult.getComputationStatus());
    }

    @Then("the calculation fails")
    public void theCalculationFails() {
        assertEquals(ComputationStatus.FAILURE, raoResult.getComputationStatus());
    }

    @Then("its security status should be {string}")
    public void statusShouldBe(String status) {
        assertEquals(status.equalsIgnoreCase("secured"), raoResult.isSecure(PhysicalParameter.FLOW));
    }

    @Then("the value of the objective function initially should be {double}")
    public void objectiveFunctionValueInitialShouldBe(double expectedValue) {
        assertEquals(expectedValue, raoResult.getCost(null), flowAmpereTolerance(expectedValue));
    }

    @Then("the value of the objective function after PRA should be {double}")
    public void objectiveFunctionValueAfterPraShouldBe(double expectedValue) {
        assertEquals(expectedValue, raoResult.getCost(crac.getPreventiveInstant()), flowAmpereTolerance(expectedValue));
    }

    @Then("the value of the objective function after ARA should be {double}")
    public void objectiveFunctionValueAfterAraShouldBe(double expectedValue) {
        Instant instant = crac.hasAutoInstant() ? crac.getInstant(InstantKind.AUTO) : crac.getOutageInstant();
        assertEquals(expectedValue, raoResult.getCost(instant), flowAmpereTolerance(expectedValue));
    }

    @Then("the value of the objective function after CRA should be {double}")
    public void objectiveFunctionValueAfterCraShouldBe(double expectedValue) {
        assertEquals(expectedValue, raoResult.getCost(crac.getInstant(InstantKind.CURATIVE)), flowAmpereTolerance(expectedValue));
    }

    @Then("the value of the objective function before optimisation should be {double}")
    public void objectiveFunctionValueBeforeOptShouldBe(double expectedValue) {
        assertEquals(expectedValue, raoResult.getCost(null), flowAmpereTolerance(expectedValue));
    }

    @Then("{int} remedial actions are used in preventive")
    public void countPra(int expectedCount) {
        assertEquals(expectedCount, raoResult.getActivatedNetworkActionsDuringState(preventiveState).size() + raoResult.getActivatedRangeActionsDuringState(preventiveState).size());
    }

    @Then("{int} network actions are used in preventive")
    public void countNetworkActionPra(int expectedCount) {
        assertEquals(expectedCount, raoResult.getActivatedNetworkActionsDuringState(preventiveState).size());
    }

    @Then("{int} ± {int} remedial actions are used in preventive")
    public void countPra(int expectedCount, int tolerance) {
        assertEquals(expectedCount, raoResult.getActivatedNetworkActionsDuringState(preventiveState).size() + raoResult.getActivatedRangeActionsDuringState(preventiveState).size(), tolerance);
    }

    @Then("{int} remedial actions are used after {string} at {string}")
    public void countCra(int expectedCount, String contingencyId, String instant) {
        State state = getState(contingencyId, instant);
        assertEquals(expectedCount, raoResult.getActivatedRangeActionsDuringState(state).size() + raoResult.getActivatedNetworkActionsDuringState(state).size());
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
        boolean isConnected = Boolean.parseBoolean(isConnectedStr);
        assertEquals(network.getBranch(line).getTerminal1().isConnected(), isConnected);
        assertEquals(network.getBranch(line).getTerminal2().isConnected(), isConnected);
    }

    @Then("PST {string} in network file with PRA is on tap {int}")
    public void pstTapInNetworkWithPra(String pst, int tap) {
        assertEquals(tap, network.getTwoWindingsTransformer(pst).getPhaseTapChanger().getTapPosition());
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
        assertEquals(chosenPstTap, raoResult.getOptimizedTapOnState(preventiveState, (PstRangeAction) crac.getRangeAction(pstRangeActionId)));
    }

    @Then("the tap of PstRangeAction {string} should be {int} ± {int} in preventive")
    public void theTapOfPstRangeActionShouldBe(String pstRangeActionId, int chosenPstTap, int tolerance) {
        assertEquals(chosenPstTap, raoResult.getOptimizedTapOnState(preventiveState, (PstRangeAction) crac.getRangeAction(pstRangeActionId)), tolerance);
    }

    @Then("the tap of PstRangeAction {string} should be {int} after {string} at {string}")
    public void theTapOfPstRangeActionShouldBe(String pstRangeActionId, int chosenPstTap, String contingencyId, String instant) {
        PstRangeAction rangeAction = (PstRangeAction) crac.getRangeAction(pstRangeActionId);
        assertEquals(chosenPstTap, raoResult.getOptimizedTapOnState(getState(contingencyId, instant), rangeAction));
    }

    @Then("the setpoint of RangeAction {string} should be {double} MW in preventive")
    public void theSetpointOfRangeActionShouldBe(String rangeActionId, double chosenSetpoint) {
        assertEquals(chosenSetpoint, raoResult.getOptimizedSetPointOnState(preventiveState, crac.getRangeAction(rangeActionId)), TOLERANCE_RANGEACTION_SETPOINT);
    }

    @Then("the initial setpoint of RangeAction {string} should be {double}")
    public void theInitialSetpointOfRangeActionShouldBe(String rangeActionId, double chosenSetpoint) {
        assertEquals(chosenSetpoint, raoResult.getPreOptimizationSetPointOnState(preventiveState, crac.getRangeAction(rangeActionId)), TOLERANCE_RANGEACTION_SETPOINT);
    }

    @Then("the initial tap of PstRangeAction {string} should be {int}")
    public void theInitialTapOfPstRangeActionShouldBe(String pstRangeActionId, int chosenTap) {
        assertEquals(chosenTap, raoResult.getPreOptimizationTapOnState(preventiveState, crac.getPstRangeAction(pstRangeActionId)));
    }

    @Then("the setpoint of RangeAction {string} should be {double} MW after {string} at {string}")
    public void theSetpointOfRangeActionShouldBe(String rangeActionId, double chosenSetpoint, String contingencyId, String instant) {
        RangeAction<?> rangeAction = crac.getRangeAction(rangeActionId);
        assertEquals(chosenSetpoint, raoResult.getOptimizedSetPointOnState(getState(contingencyId, instant), rangeAction), TOLERANCE_RANGEACTION_SETPOINT);
    }

    @Then("the setpoint of RangeAction {string} should be {double} before {string} at {string}")
    public void theSetpointOfRangeActionShouldBeBefore(String rangeActionId, double chosenSetpoint, String contingencyId, String instant) {
        RangeAction<?> rangeAction = crac.getRangeAction(rangeActionId);
        assertEquals(chosenSetpoint, raoResult.getPreOptimizationSetPointOnState(getState(contingencyId, instant), rangeAction), TOLERANCE_RANGEACTION_SETPOINT);
    }

    @Then("the tap of PstRangeAction {string} should be {int} before {string} at {string}")
    public void theSetpointOfRangeActionShouldBeBefore(String pstRangeActionId, int chosenTap, String contingencyId, String instant) {
        PstRangeAction pstRangeAction = crac.getPstRangeAction(pstRangeActionId);
        assertEquals(chosenTap, raoResult.getPreOptimizationTapOnState(getState(contingencyId, instant), pstRangeAction));
    }

    private State getState(String contingencyId, String instantId) {
        if (instantId.equalsIgnoreCase("preventive")) {
            return crac.getPreventiveState();
        } else {
            return crac.getState(contingencyId, crac.getInstant(instantId));
        }
    }

    /*
    Margins in A
    */

    @Then("the initial margin on cnec {string} should be {double} A")
    public void initialMarginInA(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, raoResult.getMargin(null, crac.getFlowCnec(cnecId), Unit.AMPERE), flowAmpereTolerance(expectedMargin));
    }

    @Then("the margin on cnec {string} after PRA should be {double} A")
    public void afterPraMarginInA(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, raoResult.getMargin(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), Unit.AMPERE), flowAmpereTolerance(expectedMargin));
    }

    @Then("the margin on cnec {string} after ARA should be {double} A")
    public void afterAraMarginInA(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, raoResult.getMargin(crac.getInstant(InstantKind.AUTO), crac.getFlowCnec(cnecId), Unit.AMPERE), flowAmpereTolerance(expectedMargin));
    }

    @Then("the margin on cnec {string} after CRA should be {double} A")
    public void afterCraMarginInA(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, raoResult.getMargin(crac.getInstant(InstantKind.CURATIVE), crac.getFlowCnec(cnecId), Unit.AMPERE), flowAmpereTolerance(expectedMargin));
    }

    @Then("the worst margin is {double} A")
    public void worstMarginInA(double expectedMargin) {
        Pair<FlowCnec, Double> worstCnec = getWorstCnec(Unit.AMPERE, false);
        assertEquals(expectedMargin, worstCnec.getValue(), flowAmpereTolerance(expectedMargin));
    }

    @Then("the worst margin is {double} A with a tolerance of {double} A")
    public void worstMarginInA(double expectedMargin, double delta) {
        Pair<FlowCnec, Double> worstCnec = getWorstCnec(Unit.AMPERE, false);
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
        assertEquals(expectedMargin, raoResult.getMargin(null, crac.getFlowCnec(cnecId), Unit.MEGAWATT), flowMegawattTolerance(expectedMargin));
    }

    @Then("the margin on cnec {string} after PRA should be {double} MW")
    public void afterPraMarginInMW(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, raoResult.getMargin(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), Unit.MEGAWATT), flowMegawattTolerance(expectedMargin));
    }

    @Then("the margin on cnec {string} after ARA should be {double} MW")
    public void afterAraMarginInMW(String cnecId, Double expectedMargin) {
        Instant instant = crac.hasAutoInstant() ? crac.getInstant(InstantKind.AUTO) : crac.getOutageInstant();
        assertEquals(expectedMargin, raoResult.getMargin(instant, crac.getFlowCnec(cnecId), Unit.MEGAWATT), flowMegawattTolerance(expectedMargin));
    }

    @Then("the margin on cnec {string} after CRA should be {double} MW")
    public void afterCraMarginInMW(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, raoResult.getMargin(crac.getInstant(InstantKind.CURATIVE), crac.getFlowCnec(cnecId), Unit.MEGAWATT), flowMegawattTolerance(expectedMargin));
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
        assertEquals(expectedMargin, raoResult.getRelativeMargin(null, crac.getFlowCnec(cnecId), Unit.AMPERE), flowAmpereTolerance(expectedMargin));
    }

    @Then("the relative margin on cnec {string} after PRA should be {double} A")
    public void afterPraRelativeMarginInA(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, raoResult.getRelativeMargin(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), Unit.AMPERE), flowAmpereTolerance(expectedMargin));
    }

    @Then("the relative margin on cnec {string} after ARA should be {double} A")
    public void afterAraRelativeMarginInA(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, raoResult.getRelativeMargin(crac.getInstant(InstantKind.AUTO), crac.getFlowCnec(cnecId), Unit.AMPERE), flowAmpereTolerance(expectedMargin));
    }

    @Then("the relative margin on cnec {string} after CRA should be {double} A")
    public void afterCraRelativeMarginInA(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, raoResult.getRelativeMargin(crac.getInstant(InstantKind.CURATIVE), crac.getFlowCnec(cnecId), Unit.AMPERE), flowAmpereTolerance(expectedMargin));
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
        assertEquals(expectedMargin, raoResult.getRelativeMargin(null, crac.getFlowCnec(cnecId), Unit.MEGAWATT), flowMegawattTolerance(expectedMargin));
    }

    @Then("the relative margin on cnec {string} after PRA should be {double} MW")
    public void afterPraRelativeMarginInMW(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, raoResult.getRelativeMargin(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), Unit.MEGAWATT), flowMegawattTolerance(expectedMargin));
    }

    @Then("the relative margin on cnec {string} after ARA should be {double} MW")
    public void afterAraRelativeMarginInMW(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, raoResult.getRelativeMargin(crac.getInstant(InstantKind.AUTO), crac.getFlowCnec(cnecId), Unit.MEGAWATT), flowMegawattTolerance(expectedMargin));
    }

    @Then("the relative margin on cnec {string} after CRA should be {double} MW")
    public void afterCraRelativeMarginInMW(String cnecId, Double expectedMargin) {
        assertEquals(expectedMargin, raoResult.getRelativeMargin(crac.getInstant(InstantKind.CURATIVE), crac.getFlowCnec(cnecId), Unit.MEGAWATT), flowMegawattTolerance(expectedMargin));
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

    // TODO : add steps to check flows on both sides
    @Then("the initial flow on cnec {string} should be {double} A")
    public void initialFlowInA(String cnecId, Double expectedFlow) {
        TwoSides side = crac.getFlowCnec(cnecId).getMonitoredSides().iterator().next();
        assertEquals(expectedFlow, raoResult.getFlow(null, crac.getFlowCnec(cnecId), side, Unit.AMPERE), flowAmpereTolerance(expectedFlow));
    }

    @Then("the flow on cnec {string} after PRA should be {double} A")
    public void afterPraFlowInA(String cnecId, Double expectedFlow) {
        TwoSides side = crac.getFlowCnec(cnecId).getMonitoredSides().iterator().next();
        assertEquals(expectedFlow, raoResult.getFlow(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), side, Unit.AMPERE), flowAmpereTolerance(expectedFlow));
    }

    @Then("the flow on cnec {string} after ARA should be {double} A")
    public void afterAraFlowInA(String cnecId, Double expectedFlow) {
        TwoSides side = crac.getFlowCnec(cnecId).getMonitoredSides().iterator().next();
        assertEquals(expectedFlow, raoResult.getFlow(crac.getInstant(InstantKind.AUTO), crac.getFlowCnec(cnecId), side, Unit.AMPERE), flowAmpereTolerance(expectedFlow));
    }

    @Then("the flow on cnec {string} after CRA should be {double} A")
    public void afterCraFlowInA(String cnecId, Double expectedFlow) {
        TwoSides side = crac.getFlowCnec(cnecId).getMonitoredSides().iterator().next();
        assertEquals(expectedFlow, raoResult.getFlow(crac.getInstant(InstantKind.CURATIVE), crac.getFlowCnec(cnecId), side, Unit.AMPERE), flowAmpereTolerance(expectedFlow));
    }

    /*
    Flows in MW
    */

    @Then("the initial flow on cnec {string} should be {double} MW")
    public void initialFlowInMW(String cnecId, Double expectedFlow) {
        TwoSides side = crac.getFlowCnec(cnecId).getMonitoredSides().iterator().next();
        assertEquals(expectedFlow, raoResult.getFlow(null, crac.getFlowCnec(cnecId), side, Unit.MEGAWATT), flowMegawattTolerance(expectedFlow));
    }

    @Then("the flow on cnec {string} after PRA should be {double} MW")
    public void afterPraFlowInMW(String cnecId, Double expectedFlow) {
        TwoSides side = crac.getFlowCnec(cnecId).getMonitoredSides().iterator().next();
        assertEquals(expectedFlow, raoResult.getFlow(crac.getPreventiveInstant(), crac.getFlowCnec(cnecId), side, Unit.MEGAWATT), flowMegawattTolerance(expectedFlow));
    }

    @Then("the flow on cnec {string} after ARA should be {double} MW")
    public void afterAraFlowInMW(String cnecId, Double expectedFlow) {
        TwoSides side = crac.getFlowCnec(cnecId).getMonitoredSides().iterator().next();
        assertEquals(expectedFlow, raoResult.getFlow(crac.getInstant(InstantKind.AUTO), crac.getFlowCnec(cnecId), side, Unit.MEGAWATT), flowMegawattTolerance(expectedFlow));
    }

    @Then("the flow on cnec {string} after CRA should be {double} MW")
    public void afterCraFlowInMW(String cnecId, Double expectedFlow) {
        TwoSides side = crac.getFlowCnec(cnecId).getMonitoredSides().iterator().next();
        Instant lastCurativeInstant = crac.getInstants(InstantKind.CURATIVE).stream().sorted(Comparator.comparingInt(instant -> -instant.getOrder())).toList().get(0);
        assertEquals(expectedFlow, raoResult.getFlow(lastCurativeInstant, crac.getFlowCnec(cnecId), side, Unit.MEGAWATT), flowMegawattTolerance(expectedFlow));
    }

    @Then("the flow on cnec {string} after {string} instant remedial actions should be {double} MW")
    public void afterInstantFlowInMW(String cnecId, String instantId, Double expectedFlow) {
        TwoSides side = crac.getFlowCnec(cnecId).getMonitoredSides().iterator().next();
        Instant instant = crac.getInstant(instantId);
        assertEquals(expectedFlow, raoResult.getFlow(instant, crac.getFlowCnec(cnecId), side, Unit.MEGAWATT), flowMegawattTolerance(expectedFlow));
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
        if (upperOrLower.equalsIgnoreCase("upper")) {
            bound = crac.getFlowCnec(cnecId).getUpperBound(side, unit).orElseThrow();
        } else if (upperOrLower.equalsIgnoreCase("lower")) {
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
                .map(side -> loopFlowResult.getLoopFlow(crac.getFlowCnec(cnecId), side))
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
        assertEquals(expectedPtdfSum, raoResult.getPtdfZonalSum(null, cnec, cnec.getMonitoredSides().iterator().next()), TOLERANCE_PTDF);
    }

    @Then("the absolute PTDF sum on cnec {string} after {string} should be {double}")
    public void absPtdfSumAfterInstant(String cnecId, String instantKind, Double expectedPtdfSum) {
        FlowCnec cnec = crac.getFlowCnec(cnecId);
        assertEquals(expectedPtdfSum, raoResult.getPtdfZonalSum(crac.getInstant(InstantKind.valueOf(instantKind.toUpperCase())), cnec, cnec.getMonitoredSides().iterator().next()), TOLERANCE_PTDF);
    }

    private void launchRao(int timeLimit) {
        launchRao(null, null, null, null, SEARCH_TREE_RAO, timeLimit);
    }

    private void launchRao(String contingencyId, InstantKind instantKind, String timestamp, String raoType) {
        launchRao(contingencyId, instantKind, timestamp, 0.0, raoType, null);
    }

    private void launchRao(String contingencyId, InstantKind instantKind, String timestamp, Double loopflowLimitAsPmaxPercentageInput, String raoType, Integer timeLimit) {
        try {
            CommonTestData.loadData(timestamp);
            network = CommonTestData.getNetwork();
            crac = CommonTestData.getCrac();
            preventiveState = crac.getPreventiveState();
            raoResult = RaoUtils.runRao(contingencyId, instantKind, raoType, loopflowLimitAsPmaxPercentageInput, timeLimit);
            CommonTestData.setRaoResult(raoResult);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void launchLoopflowComputation(String timestamp, String sensitivityProvider, String loadFlowProvider) {
        try {
            CommonTestData.loadData(timestamp);
            network = CommonTestData.getNetwork();
            crac = CommonTestData.getCrac();
            RaoParameters raoParameters = CommonTestData.getRaoParameters();
            SensitivityAnalysisParameters sensitivityAnalysisParameters = getSensitivityWithLoadFlowParameters(raoParameters);
            ReferenceProgram referenceProgram = CommonTestData.getReferenceProgram() != null ? CommonTestData.getReferenceProgram() : ReferenceProgramBuilder.buildReferenceProgram(network, loadFlowProvider, sensitivityAnalysisParameters.getLoadFlowParameters());
            ZonalData<SensitivityVariableSet> glsks = CommonTestData.getLoopflowGlsks();

            // run loopFlowComputation
            LoopFlowComputation loopFlowComputation = new LoopFlowComputationImpl(glsks, referenceProgram);
            this.loopFlowResult = loopFlowComputation.calculateLoopFlows(network, sensitivityProvider, sensitivityAnalysisParameters, crac.getFlowCnecs(), crac.getOutageInstant());

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Pair<FlowCnec, Double> getWorstCnec(Unit unit, boolean relative) {
        Set<FlowCnec> flowCnecs = crac.getFlowCnecs().stream().filter(Cnec::isOptimized).collect(Collectors.toSet());
        double worstMargin = Double.MAX_VALUE;
        FlowCnec worstCnec = null;
        double margin;
        //Filter flow cnecs from failed perimeters
        Set<State> failedStates = crac.getStates().stream()
                .filter(state -> raoResult.getComputationStatus(state).equals(ComputationStatus.FAILURE))
                        .collect(Collectors.toSet());
        for (FlowCnec flowCnec : flowCnecs) {
            if (failedStates.contains(flowCnec.getState())) {
                continue;
            }
            try {
                if (relative) {
                    margin = raoResult.getRelativeMargin(flowCnec.getState().getInstant(), flowCnec, unit);
                } else {
                    margin = raoResult.getMargin(flowCnec.getState().getInstant(), flowCnec, unit);
                }
                if (Double.isNaN(margin)) {
                    // Try getting margin before flowCnec's state afterOptimizing state
                    // This could happen for instance if optimization on said state failed
                    if (relative) {
                        margin = raoResult.getRelativeMargin(crac.getInstantBefore(flowCnec.getState().getInstant()), flowCnec, unit);
                    } else {
                        margin = raoResult.getMargin(crac.getInstantBefore(flowCnec.getState().getInstant()), flowCnec, unit);
                    }
                }
            } catch (OpenRaoException e) {
                margin = Double.MAX_VALUE;
            }

            if (margin < worstMargin) {
                worstMargin = margin;
                worstCnec = flowCnec;
            }
        }
        return new ImmutablePair<>(worstCnec, worstMargin);
    }

    /*
    RaoResult infos
     */

    @Then("the optimization steps executed by the RAO should be {string}")
    public void getOptimizationSteps(String string) {
        assertEquals(string, raoResult.getOptimizationStepsExecuted().toString());
    }
}
