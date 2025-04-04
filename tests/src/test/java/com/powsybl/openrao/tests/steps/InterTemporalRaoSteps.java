
/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.steps;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.*;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.InterTemporalRaoResult;
import com.powsybl.openrao.raoapi.*;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.MDC;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.tests.steps.CommonTestData.*;
import static com.powsybl.openrao.tests.utils.Helpers.*;
import static com.powsybl.openrao.tests.utils.Helpers.getFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class InterTemporalRaoSteps {
    private static String networkFolderPath;
    private static String cracFolderPath;
    private static boolean useIndividualCracs = false;
    private static String networkFolderPathPostIcsImport;
    private static String icsStaticPath;
    private static String icsSeriesPath;
    private static String icsGskPath;
    private static InterTemporalRaoInputWithNetworkPaths interTemporalRaoInput;
    private static InterTemporalRaoResult interTemporalRaoResult;

    public InterTemporalRaoSteps() {
        // should not be instantiated
    }

    // TODO : add after to run after all @intertemporal scenarios
    private static void cleanModifiedNetworks() {
        interTemporalRaoResult.getTimestamps().forEach(offsetDateTime -> {
            File file = new File(interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).orElseThrow().getPostIcsImportNetworkPath());
            if (file.exists()) {
                file.delete();
            }
        });
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
        File cracFile = null;
        if (!useIndividualCracs) {
            cracFile = getFile(CommonTestData.cracPath);
        }

        CommonTestData.raoParameters = buildConfig(getFile(CommonTestData.raoParametersPath));

        TemporalData<RaoInputWithNetworkPaths> raoInputs = new TemporalDataImpl<>();
        List<Map<String, String>> inputs = arg1.asMaps(String.class, String.class);
        for (Map<String, String> tsInput : inputs) {
            OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(tsInput.get("Timestamp"));
            TECHNICAL_LOGS.info("**** Loading data for TS {} ****", offsetDateTime);
            // Network
            String initialNetworkPath = networkFolderPath.concat(tsInput.get("Network"));
            String postIcsNetworkPath = networkFolderPathPostIcsImport.concat(tsInput.get("Network")).split(".uct")[0].concat(".jiidm");
            Network network = importNetwork(getFile(networkFolderPath.concat(tsInput.get("Network"))), false);
            // Crac
            Pair<Crac, CracCreationContext> cracImportResult;
            if (useIndividualCracs) { // only works with json
                cracImportResult = importCrac(getFile(cracFolderPath.concat(tsInput.get("Crac"))), network, null);
            } else {
                addTimestampToCracCreationParameters("FlowBasedConstraintDocument", offsetDateTime, cracCreationParameters);
                cracImportResult = importCrac(cracFile, network, cracCreationParameters);
            }
            RaoInputWithNetworkPaths raoInput = RaoInputWithNetworkPaths
                .build(initialNetworkPath, postIcsNetworkPath, cracImportResult.getLeft())
                .build();
            raoInputs.put(offsetDateTime, raoInput);
        }
        interTemporalRaoInput = new InterTemporalRaoInputWithNetworkPaths(raoInputs, new HashSet<>());
        InputStream gskInputStream = icsGskPath == null ? null : new FileInputStream(getFile(icsGskPath));
        IcsImporter.populateInputWithICS(interTemporalRaoInput, new FileInputStream(getFile(icsStaticPath)), new FileInputStream(getFile(icsSeriesPath)), gskInputStream);
    }

    @When("I launch marmot")
    public static void iLaunchMarmot() {
        interTemporalRaoResult = InterTemporalRao.run(interTemporalRaoInput, CommonTestData.getRaoParameters());
    }

    @When("I export marmot results to {string}")
    public static void iExportMarmotResults(String outputPath) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(getFile(getResourcesPath().concat(outputPath)));
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
        Properties properties = new Properties();
        properties.put("rao-result.export.json.flows-in-megawatts", "true");
        properties.put("inter-temporal-rao-result.export.filename-template", "'RAO_RESULT_'yyyy-MM-dd'T'HH:mm:ss'.json'");
        properties.put("inter-temporal-rao-result.export.summary-filename", "summary.json");
        interTemporalRaoResult.write(zipOutputStream, interTemporalRaoInput.getRaoInputs().map(RaoInputWithNetworkPaths::getCrac), properties);
    }

    @When("I export networks with PRAs to {string}")
    public static void iExportNetworksWithPras(String outputPath) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(getFile(getResourcesPath().concat(outputPath)));
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

        for (OffsetDateTime offsetDateTime : interTemporalRaoResult.getTimestamps()) {
            Set<NetworkAction> preventiveNetworkActions = interTemporalRaoResult.getIndividualRaoResult(offsetDateTime).getActivatedNetworkActionsDuringState(interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).get().getCrac().getPreventiveState());
            Set<RangeAction<?>> preventiveRangeActions = interTemporalRaoResult.getIndividualRaoResult(offsetDateTime).getActivatedRangeActionsDuringState(interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).get().getCrac().getPreventiveState());
            Network modifiedNetwork = Network.read(interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).orElseThrow().getPostIcsImportNetworkPath());
            Network initialNetwork = Network.read(interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).orElseThrow().getInitialNetworkPath());

            // Apply PRAs on modified network
            preventiveNetworkActions.forEach(networkAction -> networkAction.apply(modifiedNetwork));
            preventiveRangeActions.forEach(rangeAction -> {
                double optimizedSetpoint = interTemporalRaoResult.getIndividualRaoResult(offsetDateTime).getOptimizedSetPointOnState(interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).get().getCrac().getPreventiveState(), rangeAction);
                if (rangeAction instanceof InjectionRangeAction) {
                    applyRedispatchingAction((InjectionRangeAction) rangeAction, optimizedSetpoint, modifiedNetwork, initialNetwork);
                } else {
                    rangeAction.apply(modifiedNetwork, optimizedSetpoint);
                }
            });
            // Write network
            String path = interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).orElseThrow().getPostIcsImportNetworkPath().split(".jiidm")[0].concat(".uct");
            String name = path.substring(path.lastIndexOf("/") + 1);
            initialNetwork.write("UCTE", new Properties(), Path.of(path));

            // Add network to zip
            ZipEntry entry = new ZipEntry(name);
            zipOutputStream.putNextEntry(entry);
            File generatedNetwork = new File(path);
            byte[] fileInByte = FileUtils.readFileToByteArray(generatedNetwork);
            InputStream is = new ByteArrayInputStream(fileInByte);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = is.read(bytes)) >= 0) {
                zipOutputStream.write(bytes, 0, length);
            }
            is.close();
            generatedNetwork.delete();
        }
        zipOutputStream.close();
        cleanModifiedNetworks();
    }

    private static void applyRedispatchingAction(InjectionRangeAction injectionRangeAction, double optimizedSetpoint, Network modifiedNetwork, Network initialNetwork) {
        double initialSetpoint = injectionRangeAction.getInitialSetpoint();
        for (NetworkElement networkElement : injectionRangeAction.getNetworkElements().stream().collect(Collectors.toSet())) {
            String busId = modifiedNetwork.getGenerator(networkElement.getId()).getTerminal().getBusBreakerView().getBus().getId();
            Bus busInInitialNetwork = initialNetwork.getBusBreakerView().getBus(busId);
            // If no generator defined in initial network, create one
            // For now, minimumPermissibleReactivePowerGeneration and maximumPermissibleReactivePowerGeneration are hardcoded to -999 and 999
            // to prevent infinite values that generate a UCT writer crash. TODO : compute realistic values
            String generatorId = busId + "_generator";
            if (initialNetwork.getGenerator(generatorId) == null) {
                Generator generator = busInInitialNetwork.getVoltageLevel().newGenerator()
                    .setBus(busId)
                    .setEnsureIdUnicity(true)
                    .setId(generatorId)
                    .setMaxP(999999)
                    .setMinP(0)
                    .setTargetP(0)
                    .setTargetQ(0)
                    .setTargetV(busInInitialNetwork.getVoltageLevel().getNominalV())
                    .setVoltageRegulatorOn(false)
                    .add();
                generator.setFictitious(true);
                generator.newMinMaxReactiveLimits().setMinQ(-999).setMaxQ(999).add();
            }
            for (Generator generator : busInInitialNetwork.getGenerators()) {
                generator.setTargetP(generator.getTargetP()
                    + (optimizedSetpoint - initialSetpoint) * injectionRangeAction.getInjectionDistributionKeys().get(networkElement));
            }
        }
    }

    @Then("the optimized margin on {string} for timestamp {string} is {double} MW")
    public static void theOptimizedMarginOnCnecForTimestampIsMW(String cnecId, String timestamp, double margin) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        FlowCnec flowCnec = interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac().getFlowCnec(cnecId);
        Instant afterCra = interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac().getLastInstant();
        assertEquals(margin,
            interTemporalRaoResult.getIndividualRaoResult(offsetDateTime).getMargin(afterCra, flowCnec, Unit.MEGAWATT),
            SearchTreeRaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the functional cost for timestamp {string} is {double}")
    public static void theFunctionalCostForTimestampIs(String timestamp, double functionalCost) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Instant afterCra = interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac().getLastInstant();
        assertEquals(functionalCost,
            interTemporalRaoResult.getFunctionalCost(afterCra, offsetDateTime),
            SearchTreeRaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the total cost for timestamp {string} is {double}")
    public static void theTotalCostForTimestampIs(String timestamp, double totalCost) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Instant afterCra = interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac().getLastInstant();
        assertEquals(totalCost,
            interTemporalRaoResult.getCost(afterCra, offsetDateTime),
            SearchTreeRaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the functional cost for all timestamps is {double}")
    public static void theFunctionalCostForAllTimestampsIs(double functionalCost) {
        assertEquals(functionalCost,
            interTemporalRaoResult.getGlobalFunctionalCost(InstantKind.CURATIVE),
            SearchTreeRaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the total cost for all timestamps is {double}")
    public static void theTotalCostForAllTimestampsIs(double totalCost) {
        assertEquals(totalCost,
            interTemporalRaoResult.getGlobalCost(InstantKind.CURATIVE),
            SearchTreeRaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }
}
