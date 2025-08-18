/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.tests.steps;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.MonitoringInput;
import com.powsybl.openrao.monitoring.angle.AngleCnecValue;
import com.powsybl.openrao.monitoring.angle.AngleMonitoring;
import com.powsybl.openrao.monitoring.results.CnecResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class AngleMonitoringSteps {
    private static final double DOUBLE_TOLERANCE = 1e-1;

    @When("I launch angle monitoring at {string} on {int} threads")
    public void iLaunchAngleMonitoring(String cracTimestamp, int numberOfLoadFlowsInParallel) throws IOException {
        runAngleMonitoring(cracTimestamp, numberOfLoadFlowsInParallel);
    }

    @When("I launch angle monitoring on {int} threads")
    public void iLaunchAngleMonitoring(int numberOfLoadFlowsInParallel) throws IOException {
        runAngleMonitoring(null, numberOfLoadFlowsInParallel);
    }

    private void runAngleMonitoring(String cracTimestamp, int numberOfLoadFlowsInParallel) throws IOException {
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(false);
        CommonTestData.loadData(cracTimestamp);
        Network network = CommonTestData.getNetwork();
        RaoResult raoResult = CommonTestData.getRaoResult();
        MonitoringInput angleMonitoringInput = new MonitoringInput(CommonTestData.getCrac(), network, raoResult, CommonTestData.getMonitoringGlsks());
        MonitoringResult<AngleCnec> angleMonitoringResult = new AngleMonitoring("OpenLoadFlow", loadFlowParameters).runMonitoring(angleMonitoringInput, numberOfLoadFlowsInParallel);
        CommonTestData.setMonitoringResult(angleMonitoringResult);
    }

    @Then("the angle monitoring result is {string}")
    public void statusCheck(String expectedStatus) {
        assertEquals(CommonTestData.getMonitoringResult().getStatus().toString(), expectedStatus);
        assertEquals(expectedStatus.equalsIgnoreCase("secure"), CommonTestData.getRaoResult().isSecure(PhysicalParameter.ANGLE));
    }

    @Then("the applied remedial actions should be:")
    public void appliedCras(DataTable arg1) {
        List<Map<String, String>> expectedCras = arg1.asMaps(String.class, String.class);
        for (Map<String, String> expectedCra : expectedCras) {
            String craName = expectedCra.get("Name");
            String contingency = expectedCra.get("Contingency");
            int numberOfCras = Integer.parseInt(expectedCra.get("NumberOfCras"));
            Instant instant = CommonTestData.getCrac().getInstant(expectedCra.get("Instant").toLowerCase());
            State state;
            if (instant.isPreventive()) {
                state = CommonTestData.getCrac().getPreventiveState();
            } else {
                state = CommonTestData.getCrac().getState(contingency, instant);
            }
            assertTrue(CommonTestData.getMonitoringResult().getAppliedRas().containsKey(state));
            assertEquals(numberOfCras, CommonTestData.getMonitoringResult().getAppliedRas(state).size());
            assertTrue(CommonTestData.getMonitoringResult().getAppliedRas(state).stream().anyMatch(networkAction -> networkAction.getId().equals(craName)));
        }
    }

    @Then("the AngleCnecs should have the following angles:")
    public void angleCnecValues(DataTable arg1) {
        List<Map<String, String>> expectedCnecs = arg1.asMaps(String.class, String.class);
        assertEquals(expectedCnecs.size(), CommonTestData.getMonitoringResult().getCnecResults().size());
        for (Map<String, String> expectedCnec : expectedCnecs) {
            String cnecId = expectedCnec.get("AngleCnecId");
            String cnecName = expectedCnec.get("Name");
            String contingency = expectedCnec.get("Contingency");
            Instant instant = CommonTestData.getCrac().getInstant(expectedCnec.get("Instant").toLowerCase());
            Double expectedAngle = Double.parseDouble(expectedCnec.get("Angle"));

            State state;
            if (instant.isPreventive()) {
                state = CommonTestData.getCrac().getPreventiveState();
            } else {
                state = CommonTestData.getCrac().getState(contingency, instant);
            }

            Set<CnecResult<AngleCnec>> angleResults = CommonTestData.getMonitoringResult().getCnecResults().stream().filter(angleResult -> angleResult.getCnec().getId().equals(cnecId)
                    && angleResult.getCnec().getName().equals(cnecName)
                    && angleResult.getCnec().getState().equals(state))
                    .collect(Collectors.toSet());
            assertNotNull(angleResults);
            assertEquals(1, angleResults.size());
            AngleCnec angleCnec = (AngleCnec) angleResults.iterator().next().getCnec();
            AngleCnecValue angleValue = (AngleCnecValue) angleResults.iterator().next().getValue();

            assertEquals(expectedAngle, angleValue.value(), DOUBLE_TOLERANCE);

            if (expectedCnec.get("LowerBound") != null) {
                Optional<Double> lowerBound = angleCnec.getLowerBound(Unit.DEGREE);
                if (lowerBound.isPresent()) {
                    assertEquals(Double.parseDouble(expectedCnec.get("LowerBound")), lowerBound.get(), DOUBLE_TOLERANCE);
                } else {
                    assertEquals("null", expectedCnec.get("LowerBound"));
                }
            }
            if (expectedCnec.get("UpperBound") != null) {
                Optional<Double> upperBound = angleCnec.getUpperBound(Unit.DEGREE);
                if (upperBound.isPresent()) {
                    assertEquals(Double.parseDouble(expectedCnec.get("UpperBound")), upperBound.get(), DOUBLE_TOLERANCE);
                } else {
                    assertEquals("null", expectedCnec.get("UpperBound"));
                }
            }
        }
    }
}
