/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.steps;

import com.powsybl.openrao.tests.utils.CneHelper;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.powsybl.openrao.tests.utils.Helpers.getFile;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CneExportSteps {

    private String exportedCne;
    private CneHelper.CneVersion cneVersion;

    @When("I export CORE CNE at {string}")
    public void iExportCoreCne(String timestamp) throws IOException {
        cneVersion = CneHelper.CneVersion.CORE;
        CommonTestData.loadData(timestamp);
        exportedCne = CneHelper.exportCoreCne(CommonTestData.getCrac(), CommonTestData.getCracCreationContext(), CommonTestData.getNetwork(), CommonTestData.getRaoResult(), CommonTestData.getRaoParameters());
    }

    @Then("the CORE CNE file is xsd-compliant")
    public void coreCneXsdCompliant() {
        assertTrue(CneHelper.isCoreCneValid(exportedCne));
    }

    @When("I export SWE CNE")
    public void iExportSweCne() throws IOException {
        exportSweCne(null);
    }

    @When("I export SWE CNE at {string}")
    public void iExportSweCne(String timestamp) throws IOException {
        exportSweCne(timestamp);
    }

    private void exportSweCne(String dataTimestamp) throws IOException {
        cneVersion = CneHelper.CneVersion.SWE;
        if (dataTimestamp != null) {
            CommonTestData.loadData(dataTimestamp);
        }
        exportedCne = CneHelper.exportSweCne(CommonTestData.getCrac(), CommonTestData.getCracCreationContext(), CommonTestData.getNetwork(), CommonTestData.getRaoResult(), CommonTestData.getRaoParameters());
        // The following crashes when running cucumber tests from jar-with-dependencies,
        // maybe because "urn-entsoe-eu-local-extension-types.xsd" is missing in the jar.
        // We don't really need to fix this (will be moved to gridcapa)
        // + there are some unit tests in farao-core
    }

    @Then("the exported CNE file is the same as {string}")
    public void cneIsEqual(String filePath) throws IOException {
        compareCne(filePath, false);
    }

    @Then("the exported CNE file is similar to {string}")
    public void cneIsSimilar(String filePath) throws IOException {
        compareCne(filePath, true);
    }

    private void compareCne(String expectedCnePath, boolean onlySimilarity) throws IOException {
        String fullExpectedCnePath = CommonTestData.getResourcesPath().concat("cne/").concat(expectedCnePath);
        try (InputStream expectedCneInputStream = new FileInputStream(getFile(fullExpectedCnePath))) {
            InputStream actualCneInputStream = new ByteArrayInputStream(exportedCne.getBytes());
            CneHelper.compareCneFiles(expectedCneInputStream, actualCneInputStream, onlySimilarity, cneVersion);
        } catch (IOException | AssertionError e) {
            e.printStackTrace();
            fail();
        }
    }

}
