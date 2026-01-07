/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.tests.steps;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.Monitoring;
import com.powsybl.openrao.monitoring.MonitoringInput;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class VoltageMonitoringSteps {
    private static final double DOUBLE_TOLERANCE = 1e-1;

    @When("I launch voltage monitoring at {string} on {int} threads")
    public void iLaunchVoltageMonitoring(String cracTimestamp, int numberOfLoadFlowsInParallel) throws IOException {
        runVoltageMonitoring(cracTimestamp, numberOfLoadFlowsInParallel);
    }

    @When("I launch voltage monitoring on {int} threads")
    public void iLaunchVoltageMonitoring(int numberOfLoadFlowsInParallel) throws IOException {
        runVoltageMonitoring(null, numberOfLoadFlowsInParallel);
    }

    private void runVoltageMonitoring(String cracTimestamp, int numberOfLoadFlowsInParallel) throws IOException {
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(false);
        CommonTestData.loadData(cracTimestamp);
        Network network = CommonTestData.getNetwork();
        RaoResult raoResult = CommonTestData.getRaoResult();
        MonitoringInput voltageMonitoringInput = MonitoringInput.buildWithVoltage(network, CommonTestData.getCrac(), raoResult).build();
        MonitoringResult voltageMonitoringResult = new Monitoring("OpenLoadFlow", loadFlowParameters).runMonitoring(voltageMonitoringInput, numberOfLoadFlowsInParallel);
        CommonTestData.setVoltageMonitoringResult(voltageMonitoringResult);
    }

    @Then("the voltage monitoring result is {string}")
    public void statusCheck(String expectedStatus) {
        assertEquals(CommonTestData.getMonitoringResult().getStatus().toString(), expectedStatus);
        assertEquals(expectedStatus.equalsIgnoreCase("secure"), CommonTestData.getRaoResult().isSecure(PhysicalParameter.VOLTAGE));
    }

    @Then("the min voltage of CNEC {string} should be {double} kV at {string}")
    public void assertMinVoltageValue(String voltageCnecId, double expectedMinVoltageValue, String instantId) {
        VoltageCnec voltageCnec = CommonTestData.getCrac().getVoltageCnec(voltageCnecId);
        double actualMinVoltageValue = CommonTestData.getRaoResult().getMinVoltage(CommonTestData.getCrac().getInstant(instantId), voltageCnec, Unit.KILOVOLT);
        assertEquals(expectedMinVoltageValue, actualMinVoltageValue, DOUBLE_TOLERANCE);
    }

    @Then("the max voltage of CNEC {string} should be {double} kV at {string}")
    public void assertMaxVoltageValue(String voltageCnecId, double expectedMaxVoltageValue, String instantId) {
        VoltageCnec voltageCnec = CommonTestData.getCrac().getVoltageCnec(voltageCnecId);
        double actualMaxVoltageValue = CommonTestData.getRaoResult().getMaxVoltage(CommonTestData.getCrac().getInstant(instantId), voltageCnec, Unit.KILOVOLT);
        assertEquals(expectedMaxVoltageValue, actualMaxVoltageValue, DOUBLE_TOLERANCE);
    }

    @Then("the voltage margin of CNEC {string} should be {double} kV at {string}")
    public void assertAngleMargin(String voltageCnecId, double expectedVoltageMargin, String instantId) {
        VoltageCnec voltageCnec = CommonTestData.getCrac().getVoltageCnec(voltageCnecId);
        double actualVoltageMargin = CommonTestData.getRaoResult().getMargin(CommonTestData.getCrac().getInstant(instantId), voltageCnec, Unit.KILOVOLT);
        assertEquals(expectedVoltageMargin, actualVoltageMargin, DOUBLE_TOLERANCE);
    }
}

