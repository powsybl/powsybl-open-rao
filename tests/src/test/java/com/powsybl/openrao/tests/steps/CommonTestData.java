/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.steps;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.crac.io.cim.parameters.CimCracCreationParameters;
import com.powsybl.openrao.data.crac.io.csaprofiles.parameters.CsaCracCreationParameters;
import com.powsybl.openrao.data.crac.io.fbconstraint.parameters.FbConstraintCracCreationParameters;
import com.powsybl.openrao.data.glsk.virtual.hubs.GlskVirtualHubs;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.monitoring.results.RaoResultWithAngleMonitoring;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.tests.utils.CoreCcPreprocessor;
import com.powsybl.openrao.tests.utils.Helpers;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.xml.XmlVirtualHubsConfiguration;
import com.powsybl.sensitivity.SensitivityVariableSet;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.time.OffsetDateTime;

import static com.powsybl.openrao.tests.utils.Helpers.*;
import static com.powsybl.openrao.tests.utils.Helpers.getOffsetDateTimeFromBrusselsTimestamp;

public final class CommonTestData {

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

    private static String loopflowGlskPath;
    private static String monitoringGlskPath;
    private static ZonalData<Scalable> monitoringGlsks;
    private static ZonalData<SensitivityVariableSet> loopflowGlsks;

    private static String raoResultPath;
    private static RaoResult raoResult;

    private static String refProgPath;
    private static ReferenceProgram referenceProgram;

    private static String virtualHubsConfigPath;

    private static MonitoringResult monitoringResult;

    private static String timestamp;

    private CommonTestData() {
        // should not be instantiated
    }

    public static void setRaoResult(RaoResult raoResult) {
        if (CommonTestData.monitoringResult != null) {
            // update RAO result with angle values
            CommonTestData.raoResult = new RaoResultWithAngleMonitoring(raoResult, CommonTestData.monitoringResult);
        } else {
            CommonTestData.raoResult = raoResult;
        }
    }

    public static void setMonitoringResult(MonitoringResult result) {
        CommonTestData.monitoringResult = result;
        if (CommonTestData.raoResult != null) {
            // update RAO result with angle values
            CommonTestData.raoResult = new RaoResultWithAngleMonitoring(CommonTestData.raoResult, CommonTestData.monitoringResult);
        }
    }

    public static void setDataLocation(String location) {
        dataPrefix = location.concat("/");
    }

    public static void resetDataLocation() {
        setDataLocation("src/test/resources/files/");
    }

    public static String getResourcesPath() {
        return dataPrefix;
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
        loopflowGlskPath = null;
        monitoringGlskPath = null;
        refProgPath = null;
        cracPath = null;
        raoResultPath = null;
        crac = null;
        cracCreationContext = null;
        network = null;
        virtualHubsConfigPath = null;
        raoParameters = null;
        loopflowGlsks = null;
        monitoringGlsks = null;
        referenceProgram = null;
        raoResult = null;
        monitoringResult = null;
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

    @Given("loopflow glsk file is {string}")
    public static void loopflowGlskFileIs(String path) {
        loopflowGlskPath = getResourcesPath().concat("glsks/").concat(path);
    }

    @Given("monitoring glsk file is {string}")
    public static void monitoringGlskFileIs(String path) {
        monitoringGlskPath = getResourcesPath().concat("glsks/").concat(path);
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

    public static ZonalData<SensitivityVariableSet> getLoopflowGlsks() {
        return loopflowGlsks;
    }

    public static ZonalData<Scalable> getMonitoringGlsks() {
        return monitoringGlsks;
    }

    public static ReferenceProgram getReferenceProgram() {
        return referenceProgram;
    }

    public static RaoResult getRaoResult() {
        return raoResult;
    }

    public static MonitoringResult getMonitoringResult() {
        return monitoringResult;
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

        OffsetDateTime offsetDateTime = null;
        // Add timestamp manually if no crac creation parameters file were given explicitly but a timestamp is still needed
        if (timestamp != null && cracCreationParametersPath == null) {
            offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
            addTimestampToCracCreationParameters(cracFormat, offsetDateTime, cracCreationParameters);
        } else if (cracCreationParametersPath != null) { //
            offsetDateTime = importTimestampFromCracCreationParameters(cracFormat, cracCreationParameters);
        }

        // Crac
        Pair<Crac, CracCreationContext> cracImportResult = importCrac(getFile(cracPath), network, cracCreationParameters);
        crac = cracImportResult.getLeft();
        cracCreationContext = cracImportResult.getRight();

        // RAO parameters
        if (raoParametersPath != null) {
            raoParameters = buildConfig(getFile(raoParametersPath));
        } else {
            raoParameters = buildDefaultConfig();
        }
        if (overrideLinearSolver != null) {
            raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getLinearOptimizationSolver()
                .setSolver(SearchTreeRaoRangeActionsOptimizationParameters.Solver.valueOf(overrideLinearSolver.toUpperCase()));
        }

        // Loopflow GLSK
        // only work with UCTE GLSK files
        if (loopflowGlskPath != null) {
            loopflowGlsks = importUcteGlskFile(getFile(loopflowGlskPath), offsetDateTime, network);
        }

        // Monitoring GLSK
        if (monitoringGlskPath != null) {
            monitoringGlsks = importMonitoringGlskFile(getFile(monitoringGlskPath), offsetDateTime, network);
        }

        // Reference program
        if (refProgPath != null) {
            referenceProgram = importRefProg(getFile(refProgPath), offsetDateTime);
        }

        // RaoResult
        if (raoResultPath != null) {
            raoResult = importRaoResult(getFile(raoResultPath));
        }

        // Virtual hubs configuration
        if (virtualHubsConfigPath != null) {
            if (referenceProgram != null && loopflowGlsks != null) {
                VirtualHubsConfiguration virtualHubsConfiguration = XmlVirtualHubsConfiguration.importConfiguration(new FileInputStream(getFile(virtualHubsConfigPath)));
                ZonalData<SensitivityVariableSet> glskOfVirtualHubs = GlskVirtualHubs.getVirtualHubGlsks(virtualHubsConfiguration, network, referenceProgram);
                loopflowGlsks.addAll(glskOfVirtualHubs);
            } else {
                throw new OpenRaoException("In order to import a virtual hubs configuration file, you should define a reference program file and a GLSK file.");
            }
        }

    }

    private static OffsetDateTime importTimestampFromCracCreationParameters(String cracFormat, CracCreationParameters cracCreationParameters) {
        if (cracFormat.equals("CimCrac")) {
            return cracCreationParameters.getExtension(CimCracCreationParameters.class).getTimestamp();
        } else if (cracFormat.equals("FlowBasedConstraintDocument")) {
            return cracCreationParameters.getExtension(FbConstraintCracCreationParameters.class).getTimestamp();
        } else if (cracFormat.equals("CsaCrac")) {
            return cracCreationParameters.getExtension(CsaCracCreationParameters.class).getTimestamp();
        } else {
            return null;
        }
    }

    private static void addTimestampToCracCreationParameters(String cracFormat, OffsetDateTime timestamp, CracCreationParameters cracCreationParameters) {

        if (cracFormat.equals("CimCrac")) {
            CimCracCreationParameters cimParams = new CimCracCreationParameters();
            cimParams.setTimestamp(timestamp);
            cracCreationParameters.addExtension(CimCracCreationParameters.class, cimParams);
        } else if (cracFormat.equals("FlowBasedConstraintDocument")) {
            FbConstraintCracCreationParameters fbConstraintParams = new FbConstraintCracCreationParameters();
            fbConstraintParams.setTimestamp(timestamp);
            cracCreationParameters.addExtension(FbConstraintCracCreationParameters.class, fbConstraintParams);
        } else if (cracFormat.equals("CsaCrac")) {
            CsaCracCreationParameters csaParams = new CsaCracCreationParameters();
            csaParams.setTimestamp(timestamp);
            cracCreationParameters.addExtension(CsaCracCreationParameters.class, csaParams);
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
