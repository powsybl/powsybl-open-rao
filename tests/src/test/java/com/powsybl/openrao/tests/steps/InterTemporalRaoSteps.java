
/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.steps;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.raoapi.*;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.tests.steps.CommonTestData.*;
import static com.powsybl.openrao.tests.utils.Helpers.*;
import static com.powsybl.openrao.tests.utils.Helpers.getFile;

public final class InterTemporalRaoSteps {
    private static String networkFolderPath;
    private static String networkFolderPathPostIcsImport;
    private static String icsStaticPath;
    private static String icsSeriesPath;
    private static String icsGskPath;
    private static InterTemporalRaoInputWithNetworkPaths interTemporalRaoInput;

    private InterTemporalRaoSteps() {
        // should not be instantiated
    }

    @Given("network files are in folder {string}")
    public static void networkFilesAreIn(String folderPath) throws IOException {
        setNetworkInputs(folderPath);
        setNetworkInputsPostIcs(folderPath);
    }

    private static void setNetworkInputs(String folderPath) {
        networkFolderPath = getResourcesPath().concat("cases/").concat(folderPath + "/");
    }

    private static void setNetworkInputsPostIcs(String folderPath) throws IOException {
        networkFolderPathPostIcsImport = getResourcesPath().concat("cases/").concat(folderPath + "-postIcsImport/");
        if (!Files.isDirectory(Path.of(networkFolderPathPostIcsImport))) {
            Files.createDirectories(Path.of(networkFolderPathPostIcsImport));
        }
    }

    @Given("ics static file is {string}")
    public static void icsStaticFileIs(String path) {
        icsStaticPath = getResourcesPath().concat("ics/").concat(path);
    }

    @Given("ics series file is {string}")
    public static void icsSeriesFileIs(String path) {
        icsSeriesPath = getResourcesPath().concat("ics/").concat(path);
    }

    @Given("ics gsk file is {string}")
    public static void icsGskFileIs(String path) {
        icsGskPath = getResourcesPath().concat("ics/").concat(path);
    }

    @Given("intertemporal rao inputs are:")
    public static void intertemporalRaoInputsAre(DataTable arg1) throws IOException {
        loadDataForInterTemporalRao(arg1);
    }

    public static void loadDataForInterTemporalRao(DataTable arg1) throws IOException {
        CracCreationParameters cracCreationParameters;
        try {
            String ccpToImport = getResourcesPath().concat(DEFAULT_CRAC_CREATION_PARAMETERS_PATH);
            InputStream cracCreationParametersInputStream = new BufferedInputStream(new FileInputStream(getFile(ccpToImport)));
            cracCreationParameters = JsonCracCreationParameters.read(cracCreationParametersInputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        File cracFile = getFile(CommonTestData.cracPath);

        CommonTestData.raoParameters = buildConfig(getFile(CommonTestData.raoParametersPath));
        raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getLinearOptimizationSolver()
            .setSolver(SearchTreeRaoRangeActionsOptimizationParameters.Solver.valueOf("XPRESS"));

        TemporalData<RaoInputWithNetworkPaths> raoInputs = new TemporalDataImpl<>();
        List<Map<String, String>> inputs = arg1.asMaps(String.class, String.class);
        for (Map<String, String> tsInput : inputs) {
            OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(tsInput.get("Timestamp"));
            TECHNICAL_LOGS.info("**** Loading data for TS {} ****", offsetDateTime);
            String initialNetworkPath = networkFolderPath.concat(tsInput.get("Network"));
            String postIcsNetworkPath = networkFolderPathPostIcsImport.concat(tsInput.get("Network")).split(".uct")[0].concat(".jiidm");
            addTimestampToCracCreationParameters("FlowBasedConstraintDocument", offsetDateTime, cracCreationParameters);
            Network network = importNetwork(getFile(networkFolderPath.concat(tsInput.get("Network"))), false);
            Pair<Crac, CracCreationContext> cracImportResult = importCrac(cracFile, network, cracCreationParameters);
            RaoInputWithNetworkPaths raoInput = RaoInputWithNetworkPaths
                .build(initialNetworkPath, postIcsNetworkPath, cracImportResult.getLeft())
                .build();
            raoInputs.add(offsetDateTime, raoInput);
        }
        interTemporalRaoInput = new InterTemporalRaoInputWithNetworkPaths(raoInputs, new HashSet<>());
        InputStream gskInputStream = icsGskPath == null ? null : new FileInputStream(getFile(icsGskPath));
        IcsImporter.populateInputWithICS(interTemporalRaoInput, new FileInputStream(getFile(icsStaticPath)), new FileInputStream(getFile(icsSeriesPath)), gskInputStream);
    }

    @When("I launch marmot")
    public static void iLaunchMarmot() {
        InterTemporalRao.run(interTemporalRaoInput, CommonTestData.getRaoParameters());
    }
}
