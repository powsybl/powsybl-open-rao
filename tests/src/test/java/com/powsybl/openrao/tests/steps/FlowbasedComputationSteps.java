/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.steps;

import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.util.converters.GlskPointLinearGlskConverter;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.flowbaseddomain.DataPostContingency;
import com.powsybl.openrao.flowbasedcomputation.FlowbasedComputationParameters;
import com.powsybl.openrao.flowbasedcomputation.FlowbasedComputationProvider;
import com.powsybl.openrao.flowbasedcomputation.FlowbasedComputationResult;
import com.powsybl.openrao.flowbasedcomputation.impl.FlowbasedComputationImpl;
import com.powsybl.sensitivity.SensitivityVariableSet;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.powsybl.openrao.tests.utils.Helpers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class FlowbasedComputationSteps {

    private String glskBePath;
    private String glskDePath;
    private String glskFrPath;
    private String glskBeTimeInterval;
    private FlowbasedComputationResult flowBasedComputationResult;
    private String zoneDe;

    @Given("French glsk file is {string}")
    public void glskFrFileIs(String glskFrPath) {
        this.glskFrPath = CommonTestData.getResourcesPath().concat("glsks/").concat(glskFrPath);
    }

    @Given("Belgian glsk file is {string}")
    public void glskBeFileIs(String glskBePath) {
        this.glskBePath = CommonTestData.getResourcesPath().concat("glsks/").concat(glskBePath);
    }

    @Given("German glsk file is {string}")
    public void glskDeFileIs(String glskDePath) {
        this.glskDePath = CommonTestData.getResourcesPath().concat("glsks/").concat(glskDePath);
    }

    @When("I launch flowbased computation with {string}")
    public void iLaunchFlowbasedComputation(String sensitivityProvider) {
        launchFlowbased(sensitivityProvider);
    }

    @Then("for an exchange from zone {string} to zone DE, the ptdf value on branch {string} before outage is {double}")
    public void ptdfValueCheck(String zone, String branchId, double expectedPtdf) {
        assertEquals(expectedPtdf, ptdfValueBeforeOutage(zone, branchId), 0.01);
    }

    @Then("for an exchange from zone {string} to zone DE, the ptdf value on branch {string} at instant {string}, after outage {string} is {double}")
    public void ptdfValueCheck(String zone, String branchId, String instantId, String outageId, double expectedPtdf) {
        assertEquals(expectedPtdf, ptdfValueAfterOutage(zone, branchId, instantId, outageId), 0.01);
    }

    @Then("the flow on branch {string} before outage is {double}")
    public void flowValueCheck(String branchId, double expectedFref) {
        assertEquals(expectedFref, flowValueBeforeOutage(branchId), 1.5);
    }

    @Then("the flow on branch {string} at instant {string}, after outage {string} is {double}")
    public void flowValueCheck(String branchId, String instantId, String outageId, double expectedFref) {
        assertEquals(expectedFref, flowValueAfterOutage(branchId, instantId, outageId), 1.5);
    }

    private double ptdfValueBeforeOutage(String country, String branchId) {
        String zone = new CountryEICode(Country.valueOf(country)).getCode() + ":" + glskBeTimeInterval;

        return flowBasedComputationResult.getFlowBasedDomain().getDataPreContingency().findMonitoredBranchById(branchId).findPtdfByCountry(zone).getPtdf()
            - flowBasedComputationResult.getFlowBasedDomain().getDataPreContingency().findMonitoredBranchById(branchId).findPtdfByCountry(zoneDe).getPtdf();
    }

    private double ptdfValueAfterOutage(String country, String branchId, String instantId, String outageId) {
        String zone = new CountryEICode(Country.valueOf(country)).getCode() + ":" + glskBeTimeInterval;
        Optional<DataPostContingency> optional = flowBasedComputationResult.getFlowBasedDomain().getDataPostContingency().stream().filter(dataPostContingency -> dataPostContingency.getContingencyId().equals(outageId)).findFirst();
        if (optional.isPresent()) {
            DataPostContingency dataOutage = optional.get();
            return dataOutage.findMonitoredBranchByIdAndInstant(branchId, instantId).findPtdfByCountry(zone).getPtdf()
                - dataOutage.findMonitoredBranchByIdAndInstant(branchId, instantId).findPtdfByCountry(zoneDe).getPtdf();
        } else {
            throw new OpenRaoException(String.format("No dataPostContingency for outage %s in tests!", outageId));
        }
    }

    private double flowValueBeforeOutage(String branchId) {
        return flowBasedComputationResult.getFlowBasedDomain().getDataPreContingency().findMonitoredBranchById(branchId).getFref();
    }

    private double flowValueAfterOutage(String branchId, String instantId, String outageId) {
        Optional<DataPostContingency> optional = flowBasedComputationResult.getFlowBasedDomain().getDataPostContingency().stream().filter(dataPostContingency -> dataPostContingency.getContingencyId().equals(outageId)).findFirst();
        if (optional.isPresent()) {
            DataPostContingency dataOutage = optional.get();
            return dataOutage.findMonitoredBranchByIdAndInstant(branchId, instantId).getFref();
        } else {
            throw new OpenRaoException(String.format("No dataPostContingency for outage %s in tests!", outageId));
        }
    }

    private void launchFlowbased(String sensitivityProvider) {
        try {
            CommonTestData.loadData(null);
            Network network = CommonTestData.getNetwork();
            Crac crac = CommonTestData.getCrac();

            // Glsk
            GlskPoint glskBE = CimGlskDocument.importGlsk(new FileInputStream(getFile(glskBePath))).getGlskPoints().get(0);
            GlskPoint glskDE = CimGlskDocument.importGlsk(new FileInputStream(getFile(glskDePath))).getGlskPoints().get(0);
            GlskPoint glskFR = CimGlskDocument.importGlsk(new FileInputStream(getFile(glskFrPath))).getGlskPoints().get(0);

            glskBeTimeInterval = glskBE.getPointInterval().getStart().toString() + "/" + glskBE.getPointInterval().getEnd().toString();
            zoneDe = new CountryEICode(Country.valueOf("DE")).getCode() + ":" + glskBeTimeInterval;

            SensitivityVariableSet linearBe = GlskPointLinearGlskConverter.convert(network, glskBE);
            SensitivityVariableSet linearDe = GlskPointLinearGlskConverter.convert(network, glskDE);
            SensitivityVariableSet linearFr = GlskPointLinearGlskConverter.convert(network, glskFR);

            Map<String, SensitivityVariableSet> glsks = new HashMap<>();
            glsks.put("BE", linearBe);
            glsks.put("DE", linearDe);
            glsks.put("FR", linearFr);

            FlowbasedComputationParameters fbParams = FlowbasedComputationParameters.load();
            fbParams.setSensitivityProvider(sensitivityProvider);
            ZonalData<SensitivityVariableSet> linGlsk = new ZonalDataImpl<>(glsks);

            FlowbasedComputationProvider flowbasedComputationProvider = new FlowbasedComputationImpl();
            flowBasedComputationResult = flowbasedComputationProvider.run(network, crac, null, linGlsk, fbParams).join();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
