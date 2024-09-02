/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.steps;

import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.monitoring.anglemonitoring.AngleMonitoring;
import com.powsybl.openrao.monitoring.anglemonitoring.AngleMonitoringResult;
import com.powsybl.openrao.monitoring.anglemonitoring.json.AngleMonitoringResultExporter;
import com.powsybl.openrao.monitoring.anglemonitoring.json.AngleMonitoringResultImporter;
import com.powsybl.openrao.tests.utils.Helpers;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
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

    // TODO : harmonize crac and glsk timestamps.
    // Temporary double parameters as long as input cimGlskDocument is poorly defined.
    @When("I launch angle monitoring with crac at {string} and glsk at {string} on {int} threads")
    public void iLaunchAngleMonitoring(String cracTimestamp, String glskTimestamp, int numberOfLoadFlowsInParallel) throws IOException {
        runAngleMonitoring(cracTimestamp, glskTimestamp, numberOfLoadFlowsInParallel);
    }

    private void runAngleMonitoring(String cracTimestamp, String glskTimestamp, int numberOfLoadFlowsInParallel) throws IOException {
        // TODO : why not use RaoParameters' load-flow params?
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(false);
        OffsetDateTime glskOffsetDateTime = Helpers.getOffsetDateTimeFromBrusselsTimestamp(glskTimestamp);
        CommonTestData.loadData(cracTimestamp);
        Network network = CommonTestData.getNetwork();
        RaoResult raoResult = CommonTestData.getRaoResult();
        CimGlskDocument cimGlskDocument = CommonTestData.getCimGlskDocument();
        AngleMonitoringResult result = roundTripOnAngleMonitoringResult(new AngleMonitoring(CommonTestData.getCrac(), network, raoResult, cimGlskDocument, glskOffsetDateTime).run("OpenLoadFlow", loadFlowParameters, numberOfLoadFlowsInParallel), CommonTestData.getCrac());
        CommonTestData.setAngleMonitoringResult(result);
    }

    private AngleMonitoringResult roundTripOnAngleMonitoringResult(AngleMonitoringResult angleMonitoringResult, Crac crac) {

        // export AngleMonitoringResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new AngleMonitoringResultExporter().export(angleMonitoringResult, outputStream);

        // import AngleMonitoringResult
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        return new AngleMonitoringResultImporter().importAngleMonitoringResult(inputStream, crac);
    }

    @Then("the angle monitoring result is {string}")
    public void statusCheck(String expectedStatus) {
        assertEquals(CommonTestData.getAngleMonitoringResult().getStatus().toString(), expectedStatus);
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
            assertTrue(CommonTestData.getAngleMonitoringResult().getAppliedCras().containsKey(state));
            assertEquals(numberOfCras, CommonTestData.getAngleMonitoringResult().getAppliedCras(state).size());
            assertTrue(CommonTestData.getAngleMonitoringResult().getAppliedCras(state).stream().anyMatch(networkAction -> networkAction.getId().equals(craName)));
        }
    }

    @Then("the AngleCnecs should have the following angles:")
    public void angleCnecValues(DataTable arg1) {
        List<Map<String, String>> expectedCnecs = arg1.asMaps(String.class, String.class);
        assertEquals(expectedCnecs.size(), CommonTestData.getAngleMonitoringResult().getAngleCnecsWithAngle().size());
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

            Set<AngleMonitoringResult.AngleResult> angleResults = CommonTestData.getAngleMonitoringResult().getAngleCnecsWithAngle().stream().filter(angleResult -> angleResult.getAngleCnec().getId().equals(cnecId)
                    && angleResult.getAngleCnec().getName().equals(cnecName)
                    && angleResult.getAngleCnec().getState().equals(state))
                    .collect(Collectors.toSet());
            assertNotNull(angleResults);
            assertEquals(1, angleResults.size());
            AngleCnec angleCnec = angleResults.iterator().next().getAngleCnec();
            Double angle = angleResults.iterator().next().getAngle();

            assertEquals(expectedAngle, angle, DOUBLE_TOLERANCE);

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
