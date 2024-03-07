/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.steps;

import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.glsk.virtual.hubs.GlskVirtualHubs;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.monitoring.anglemonitoring.AngleMonitoringResult;
import com.powsybl.openrao.monitoring.anglemonitoring.RaoResultWithAngleMonitoring;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.xml.XmlVirtualHubsConfiguration;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.openrao.tests.utils.CoreCcPreprocessor;
import com.powsybl.openrao.tests.utils.Helpers;
import com.powsybl.openrao.tests.utils.RaoUtils;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;

import static com.powsybl.openrao.tests.utils.Helpers.*;

public final class CommonTestData {

    public static final String RESOURCES_PATH = "";
    private static final String DEFAULT_CRAC_CREATION_PARAMETERS_PATH = "cracCreationParameters/common/CracCreationParameters_default.json";
    private static final String DEFAULT_RAO_PARAMETERS_PATH = "configurations/common/RaoParameters_default.json";

    private static String dataPrefix = "src/test/resources/files/";

    private static String overrideLinearSolver = null;

    private static String networkPath;
    private static Boolean coreCcNetworkPreprocessing = false;
    private static Network network;

    private static String cracPath;
    private static String cracCreationParametersPath;
    private static CracCreationContext cracCreationContext;
    private static Crac crac;

    private static String raoParametersPath;
    private static RaoParameters raoParameters;

    private static String glskPath;
    private static String cimGlskPath;
    private static ZonalData<SensitivityVariableSet> glsks;
    private static CimGlskDocument cimGlskDocument;

    private static String raoResultPath;
    private static RaoResult raoResult;

    private static String refProgPath;
    private static ReferenceProgram referenceProgram;

    private static String virtualHubsConfigPath;

    private static String angleMonitoringResultPath;

    private static AngleMonitoringResult angleMonitoringResult;

    private static String timestamp;

    private CommonTestData() {
        // should not be instantiated
    }

    public static void setRaoResult(RaoResult raoResult) {
        if (CommonTestData.angleMonitoringResult != null) {
            // update RAO result with angle values
            CommonTestData.raoResult = new RaoResultWithAngleMonitoring(raoResult, CommonTestData.angleMonitoringResult);
        } else {
            CommonTestData.raoResult = raoResult;
        }
    }

    public static void setAngleMonitoringResult(AngleMonitoringResult result) {
        CommonTestData.angleMonitoringResult = result;
        if (CommonTestData.raoResult != null) {
            // update RAO result with angle values
            CommonTestData.raoResult = new RaoResultWithAngleMonitoring(CommonTestData.raoResult, CommonTestData.angleMonitoringResult);
        }
    }

    public static void setDataLocation(String location) {
        dataPrefix = location.concat("/");
    }

    public static void resetDataLocation() {
        setDataLocation("src/test/resources/files/");
    }

    public static String getResourcesPath() {
        return RESOURCES_PATH.concat(dataPrefix);
    }

    public static void setLinearSolver(String solver) {
        overrideLinearSolver = solver;
    }

    public static void resetLinearSolver() {
        overrideLinearSolver = null;
    }

    @Before
    // Reset data to null before every scenario
    public static void reset() {
        networkPath = null;
        raoParametersPath = null;
        cracCreationParametersPath = null;
        glskPath = null;
        cimGlskPath = null;
        refProgPath = null;
        cracPath = null;
        raoResultPath = null;
        crac = null;
        cracCreationContext = null;
        network = null;
        virtualHubsConfigPath = null;
        raoParameters = null;
        glsks = null;
        cimGlskDocument = null;
        referenceProgram = null;
        raoResult = null;
        angleMonitoringResultPath = null;
        angleMonitoringResult = null;
    }

    @Given("crac file is {string}")
    public static void cracFileIs(String path) {
        cracPath = getResourcesPath().concat("crac/").concat(path);
    }

    @Given("crac creation parameters file is {string}")
    public static void cracCreationParametersFileIs(String path) {
        cracCreationParametersPath = getResourcesPath().concat("cracCreationParameters/").concat(path);
    }

    @Given("network file is {string}")
    public static void networkFileIs(String path) {
        setNetworkInput(path, false);
    }

    @Given("network file is {string} for CORE CC")
    public static void networkFileIsForCoreCC(String path) {
        // We do this in the code, because replacing UCTE files with XIIDM files multiplies their size by ~10
        setNetworkInput(path, true);
    }

    private static void setNetworkInput(String path, Boolean coreCcPreprocessing) {
        networkPath = getResourcesPath().concat("cases/").concat(path);
        coreCcNetworkPreprocessing = coreCcPreprocessing;
    }

    @Given("configuration file is {string}")
    public static void configurationFileIs(String path) {
        raoParametersPath = getResourcesPath().concat("configurations/").concat(path);
    }

    @Given("Glsk file is {string}")
    public static void glskFileIs(String path) {
        glskPath = getResourcesPath().concat("glsks/").concat(path);
    }

    @Given("cim glsk file is {string}")
    public static void cimGlskFileIs(String path) {
        cimGlskPath = getResourcesPath().concat("glsks/").concat(path);
    }

    @Given("RefProg file is {string}")
    public static void refProgFileIs(String path) {
        refProgPath = getResourcesPath().concat("refprogs/").concat(path);
    }

    @Given("Virtual hubs configuration file is {string}")
    public static void virtualHubsConfigurationFileIs(String path) {
        virtualHubsConfigPath = getResourcesPath().concat("virtualhubs/").concat(path);
    }

    @Given("RaoResult file is {string}")
    public static void raoResultIs(String path) {
        raoResultPath = getResourcesPath().concat("raoresults/").concat(path);
    }

    @Given("AngleMonitoringResult file is {string}")
    public static void angleMonitoringResultIs(String path) {
        angleMonitoringResultPath = getResourcesPath().concat("anglemonitoringresults/").concat(path);
    }

    @When("I import data")
    public static void iImportData() throws IOException {
        loadData(null);
    }

    @When("I import data at {string}")
    public static void iImportDataAt(String timestamp) throws IOException {
        loadData(timestamp);
    }

    public static Network getNetwork() {
        return network;
    }

    public static Network getNetworkClone() {
        return NetworkSerDe.copy(network);
    }

    public static Crac getCrac() {
        return crac;
    }

    public static CracCreationContext getCracCreationContext() {
        return cracCreationContext;
    }

    public static RaoParameters getRaoParameters() {
        return raoParameters;
    }

    public static ZonalData<SensitivityVariableSet> getGlsks() {
        return glsks;
    }

    public static CimGlskDocument getCimGlskDocument() {
        return cimGlskDocument;
    }

    public static ReferenceProgram getReferenceProgram() {
        return referenceProgram;
    }

    public static RaoResult getRaoResult() {
        return raoResult;
    }

    public static AngleMonitoringResult getAngleMonitoringResult() {
        return angleMonitoringResult;
    }

    public static String getTimestamp() {
        return timestamp;
    }

    public static void loadData(String timestamp) throws IOException {
        // Detect the CRAC format first. If CIM, we have to import the network using RDF IDs as identifiable IDs
        String cracFormat = null;
        if (cracPath != null) {
            cracFormat = Helpers.getCracFormat(getFile(cracPath));
        } else {
            throw new OpenRaoException("You have not defined a CRAC file. All tests need a CRAC file.");
        }

        // Network
        if (networkPath != null) {
            network = importNetwork(getFile(networkPath), "CimCrac".equals(cracFormat));
        } else {
            throw new OpenRaoException("You have not defined a network file. All tests need a network file.");
        }
        if (coreCcNetworkPreprocessing) {
            CoreCcPreprocessor.applyCoreCcNetworkPreprocessing(network);
        }

        // CracCreationParameters
        CracCreationParameters cracCreationParameters = null;
        String ccpToImport = (cracCreationParametersPath == null) ? getResourcesPath().concat(DEFAULT_CRAC_CREATION_PARAMETERS_PATH) : cracCreationParametersPath;
        InputStream cracCreationParametersInputStream;
        try {
            cracCreationParametersInputStream = new BufferedInputStream(new FileInputStream(getFile(ccpToImport)));
            cracCreationParameters = JsonCracCreationParameters.read(cracCreationParametersInputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Crac
        Pair<Crac, CracCreationContext> cracImportResult = importCrac(getFile(cracPath), network, timestamp, cracCreationParameters);
        crac = cracImportResult.getLeft();
        cracCreationContext = cracImportResult.getRight();

        // RAO parameters
        if (raoParametersPath != null) {
            raoParameters = buildConfig(getFile(raoParametersPath));
        } else {
            raoParameters = buildDefaultConfig();
        }
        if (overrideLinearSolver != null) {
            raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setSolver(RangeActionsOptimizationParameters.Solver.valueOf(overrideLinearSolver.toUpperCase()));
        }

        // GLSK
        // for now, only work with UCTE GLSK files, not CIME GLSK file
        if (glskPath != null) {
            glsks = importUcteGlskFile(getFile(glskPath), timestamp, network);
        }

        if (cimGlskPath != null) {
            cimGlskDocument = importCimGlskFile(getFile(cimGlskPath));
        }

        // Reference program
        if (refProgPath != null) {
            referenceProgram = importRefProg(getFile(refProgPath), timestamp);
        }

        // RaoResult
        if (raoResultPath != null) {
            raoResult = importRaoResult(getFile(raoResultPath));
        }

        // Virtual hubs configuration
        if (virtualHubsConfigPath != null) {
            if (referenceProgram != null && glsks != null) {
                VirtualHubsConfiguration virtualHubsConfiguration = XmlVirtualHubsConfiguration.importConfiguration(new FileInputStream(getFile(virtualHubsConfigPath)));
                ZonalData<SensitivityVariableSet> glskOfVirtualHubs = GlskVirtualHubs.getVirtualHubGlsks(virtualHubsConfiguration, network, referenceProgram);
                glsks.addAll(glskOfVirtualHubs);
            } else {
                throw new OpenRaoException("In order to import a virtual hubs configuration file, you should define a reference program file and a GLSK file.");
            }
        }

        if (angleMonitoringResultPath != null) {
            angleMonitoringResult = importAngleMonitoringResult(getFile(angleMonitoringResultPath));
        }
    }

    private static RaoParameters buildDefaultConfig() {
        try (InputStream configStream = new FileInputStream(getFile(getResourcesPath().concat(DEFAULT_RAO_PARAMETERS_PATH)))) {
            return JsonRaoParameters.read(configStream);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not load default configuration file", e);
        }
    }

    private static RaoParameters buildConfig(File configFile) {
        RaoParameters config = buildDefaultConfig();
        try (InputStream configStream = new FileInputStream(configFile)) {
            JsonRaoParameters.update(config, configStream);
        } catch (IOException | UncheckedIOException e) {
            throw new IllegalArgumentException("Configuration file is not in expected JSON format", e);
        } catch (AssertionError e) {
            throw new IllegalArgumentException("Unknown parameter in configuration file", e);
        }
        return config;
    }
}
