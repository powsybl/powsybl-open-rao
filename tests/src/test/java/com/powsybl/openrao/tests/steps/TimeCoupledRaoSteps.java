
/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.steps;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.io.fbconstraint.FbConstraintCreationContext;
import com.powsybl.openrao.data.crac.io.fbconstraint.parameters.FbConstraintCracCreationParameters;
import com.powsybl.openrao.data.icsimporter.IcsData;
import com.powsybl.openrao.data.icsimporter.IcsDataImporter;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.data.raoresult.io.idcc.core.F711Utils;
import com.powsybl.openrao.data.refprog.refprogxmlimporter.TimeCoupledRefProg;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.data.timecoupledconstraints.io.JsonTimeCoupledConstraints;
import com.powsybl.openrao.raoapi.LazyNetwork;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRao;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.searchtreerao.marmot.MarmotUtils;
import com.powsybl.openrao.tests.utils.CoreCcPreprocessor;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.MDC;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.tests.steps.CommonTestData.buildConfig;
import static com.powsybl.openrao.tests.steps.CommonTestData.cracPath;
import static com.powsybl.openrao.tests.steps.CommonTestData.getRaoParameters;
import static com.powsybl.openrao.tests.steps.CommonTestData.getResourcesPath;
import static com.powsybl.openrao.tests.steps.CommonTestData.raoParameters;
import static com.powsybl.openrao.tests.steps.CommonTestData.raoParametersPath;
import static com.powsybl.openrao.tests.utils.Helpers.getFile;
import static com.powsybl.openrao.tests.utils.Helpers.getOffsetDateTimeFromBrusselsTimestamp;
import static com.powsybl.openrao.tests.utils.Helpers.importCrac;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TimeCoupledRaoSteps {
    private static String networkFolderPath;
    private static String cracFolderPath;
    private static boolean useIndividualCracs = false;
    private static String networkFolderPathPostIcsImport;
    private static String icsStaticPath;
    private static String icsSeriesPath;
    private static String icsGskPath;
    private static String refProgPath;
    private static TimeCoupledRaoResult timeCoupledRaoResult;
    private static Map<OffsetDateTime, CracCreationContext> cracCreationContexts;

    private static final List<String> DE_TSOS = List.of("D2", "D4", "D7", "D8");
    static final String DEFAULT_CRAC_CREATION_PARAMETERS_PATH = "cracCreationParameters/epic93/CracCreationParameters_93.json";
    static final double TOLERANCE_REDISPATCHING_VALUE = 1.0;

    public TimeCoupledRaoSteps() {
        // should not be instantiated
    }

    @Given("time-coupled RefProg file is {string}")
    public static void refProgFileIs(String path) {
        refProgPath = getResourcesPath().concat("refprogs/").concat(path);
    }

    @After()
    public void cleanModifiedNetworks() throws IOException {
        if (networkFolderPathPostIcsImport != null) {
            deleteDirectoryRecursively(Paths.get(networkFolderPathPostIcsImport));
        }
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

    @Given("time-coupled constraints are in file {string} and rao inputs are:")
    public static void timeCoupledRaoInputsAre(String timeCoupledConstraintsPath, DataTable arg1) throws IOException {
        loadDataForTimeCoupledRao(timeCoupledConstraintsPath, arg1);
    }

    public static void loadDataForTimeCoupledRao(String timeCoupledConstraintsPath, DataTable arg1) throws IOException {
        raoParameters = buildConfig(getFile(raoParametersPath));

        networkFolderPath = getResourcesPath().concat("cases/");
        cracFolderPath = getResourcesPath().concat("crac/");
        String timeCoupledConstraintsFolderPath = getResourcesPath().concat("timeCoupledConstraints/");

        TemporalData<RaoInput> raoInputs = new TemporalDataImpl<>();
        List<Map<String, String>> inputs = arg1.asMaps(String.class, String.class);
        for (Map<String, String> tsInput : inputs) {
            OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(tsInput.get("Timestamp"));
            LazyNetwork lazyNetwork = new LazyNetwork(networkFolderPath.concat(tsInput.get("Network")));
            Crac crac = importCrac(getFile(cracFolderPath.concat(tsInput.get("CRAC"))), lazyNetwork, null).getLeft();
            raoInputs.put(offsetDateTime, RaoInput.build(lazyNetwork, crac).build());
            lazyNetwork.release();
        }

        TimeCoupledConstraints timeCoupledConstraints = JsonTimeCoupledConstraints.read(new FileInputStream(timeCoupledConstraintsFolderPath.concat(timeCoupledConstraintsPath)));
        CommonTestData.setTimeCoupledRaoInput(new TimeCoupledRaoInput(raoInputs, timeCoupledConstraints));
    }

    @Given("time-coupled rao inputs for CORE are:")
    public static void coreTimeCoupledRaoInputsAre(DataTable arg1) throws IOException {
        loadDataForCoreTimeCoupledRao(arg1);
    }

    public static void loadDataForCoreTimeCoupledRao(DataTable arg1) throws IOException {
        cracCreationContexts = new HashMap<>();
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
            cracFile = getFile(cracPath);
        }

        raoParameters = buildConfig(getFile(raoParametersPath));

        TemporalData<RaoInput> raoInputs = new TemporalDataImpl<>();
        List<Map<String, String>> inputs = arg1.asMaps(String.class, String.class);
        for (Map<String, String> tsInput : inputs) {
            OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(tsInput.get("Timestamp"));
            TECHNICAL_LOGS.info("**** Loading data for TS {} ****", offsetDateTime);
            LazyNetwork lazyNetwork = new LazyNetwork(networkFolderPath.concat(tsInput.get("Network")));
            CoreCcPreprocessor.applyCoreCcNetworkPreprocessing(lazyNetwork);
            // Crac
            Pair<Crac, CracCreationContext> cracImportResult;
            if (useIndividualCracs) { // only works with json
                cracImportResult = importCrac(getFile(cracFolderPath.concat(tsInput.get("Crac"))), lazyNetwork, null);
            } else {
                cracCreationParameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(offsetDateTime);
                cracImportResult = importCrac(cracFile, lazyNetwork, cracCreationParameters);
            }
            RaoInput raoInput = RaoInput
                .build(lazyNetwork, cracImportResult.getLeft())
                .build();
            raoInputs.put(offsetDateTime, raoInput);
            cracCreationContexts.put(offsetDateTime, cracImportResult.getRight());
            lazyNetwork.release();
        }
        InputStream gskInputStream = icsGskPath == null ? null : new FileInputStream(getFile(icsGskPath));

        FbConstraintCracCreationParameters fbConstraintParameters = cracCreationParameters.getExtension(FbConstraintCracCreationParameters.class);
        if (fbConstraintParameters == null) {
            TECHNICAL_LOGS.warn("No FB Constraint CRAC creation parameters found. Default parameters will be used.");
            fbConstraintParameters = new FbConstraintCracCreationParameters();
        }

        IcsData icsData = IcsDataImporter.read(
            new FileInputStream(getFile(icsStaticPath)),
            new FileInputStream(getFile(icsSeriesPath)),
            gskInputStream,
            raoInputs.getTimestamps().stream().sorted().toList());

        CommonTestData.setTimeCoupledRaoInput(icsData.processAllRedispatchingActions(
            new TimeCoupledRaoInput(raoInputs, new TimeCoupledConstraints()),
            fbConstraintParameters.getIcsCostUp(),
            fbConstraintParameters.getIcsCostDown(),
            networkFolderPathPostIcsImport.concat(inputs.getFirst().get("Network")).split(".uct")[0]));
    }

    @When("I launch marmot")
    public static void iLaunchMarmot() {
        timeCoupledRaoResult = TimeCoupledRao.run(CommonTestData.getTimeCoupledRaoInput(), getRaoParameters());
    }

    @When("I export marmot results to {string}")
    public static void iExportMarmotResults(String outputPath) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(getFile(getResourcesPath().concat(outputPath)));
        Properties properties = new Properties();
        properties.put("rao-result.export.json.flows-in-megawatts", "true");
        properties.put("time-coupled-rao-result.export.filename-template", "'RAO_RESULT_'yyyy-MM-dd'T'HH:mm:ss'.json'");
        properties.put("time-coupled-rao-result.export.summary-filename", "summary.json");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
            timeCoupledRaoResult.write(zipOutputStream, CommonTestData.getTimeCoupledRaoInput().getRaoInputs().map(RaoInput::getCrac), properties);
        }
    }

    @When("I export RefProg after redispatching to {string} based on raoResults zip {string}")
    public static void generateRefProg(String outputPath, String raoResultsZipPath) throws IOException {

        //Unzip the given zip file to a temp folder
        Path tempDir = Files.createTempDirectory("raoResults_");
        unzipZipToFolder(getResourcesPath().concat(raoResultsZipPath), tempDir);
        try {
            // Load networks in networkTemporalData
            // Load redispatchingVolume per timestamp per operator
            Map<OffsetDateTime, Map<String, Double>> rdVolumes = new HashMap<>();
            for (OffsetDateTime offsetDateTime : CommonTestData.getTimeCoupledRaoInput().getTimestampsToRun()) {
                rdVolumes.put(offsetDateTime, new HashMap<>());
                String filename = "RAO_RESULT_" + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + ".json";
                FileInputStream raoResultInputStream = new FileInputStream(getFile(String.valueOf(tempDir.resolve(filename))));
                RaoResult raoResult = RaoResult.read(raoResultInputStream, CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac());

                // Load redispatching volumes
                Crac crac = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).get().getCrac();
                Set<RangeAction<?>> preventiveRangeActions = raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState());
                Set<InjectionRangeAction> redispatchingRangeActions = preventiveRangeActions.stream()
                    .filter(InjectionRangeAction.class::isInstance)
                    .map(InjectionRangeAction.class::cast)
                    .collect(Collectors.toSet());
                redispatchingRangeActions.forEach(rangeAction -> {
                    double redispatchedVolumeForRa = raoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), rangeAction) - rangeAction.getInitialSetpoint();
                    for (String subString : rangeAction.getId().toUpperCase().split("_")) {
                        if ("RA".equals(subString)) {
                            continue;
                        } else if (DE_TSOS.contains(subString)) {
                            rdVolumes.get(offsetDateTime).put("DE", rdVolumes.get(offsetDateTime).getOrDefault("DE", 0.) + redispatchedVolumeForRa);
                            return;
                        } else {
                            rdVolumes.get(offsetDateTime).put(subString, rdVolumes.get(offsetDateTime).getOrDefault(subString, 0.) + redispatchedVolumeForRa);
                            return;
                        }
                    }
                });
            }

            Map<String, Map<String, Map<String, Double>>> becValues = importBecValues();

            if (refProgPath != null) {
                InputStream refProgInputStream = new FileInputStream(getFile(refProgPath));
                TimeCoupledRefProg.updateRefProg(refProgInputStream, new TemporalDataImpl<>(rdVolumes), becValues, getResourcesPath().concat(outputPath));
            }
        } finally {
            deleteDirectoryRecursively(tempDir);
        }
    }

    private static Map<String, Map<String, Map<String, Double>>> importBecValues() {
        List<String> coreCountries = List.of("AT", "BE", "CZ", "DE", "FR", "HR", "HU", "NL", "PL", "RO", "SI", "SK");
        Map<String, Map<String, Map<String, Double>>> becValues = new HashMap<>();
        try (InputStream inputStream = new FileInputStream(getResourcesPath().concat("sharingKeysBEC.csv"))) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(";")
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();
            Iterable<CSVRecord> becRecords = csvFormat.parse(new InputStreamReader(inputStream));

            for (CSVRecord record : becRecords) {
                List<String> countries = List.of(record.get(0).split("->"));
                String country1 = countries.get(0).toUpperCase();
                String country2 = countries.get(1).toUpperCase();

                becValues.putIfAbsent(country1, new HashMap<>());
                becValues.get(country1).putIfAbsent(country2, new HashMap<>());

                becValues.putIfAbsent(country2, new HashMap<>());
                becValues.get(country2).putIfAbsent(country1, new HashMap<>());

                coreCountries.forEach(coreCountry -> {
                    double value = Double.parseDouble(record.get(coreCountry).replaceAll(",", "."));
                    becValues.get(country1).get(country2).put(coreCountry, value);
                    becValues.get(country2).get(country1).put(coreCountry, -value);
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return becValues;
    }

    private static void unzipZipToFolder(String zipFilePathStr, Path outputDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePathStr))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path newFilePath = outputDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(newFilePath);
                } else {
                    Files.createDirectories(newFilePath.getParent());
                    Files.copy(zis, newFilePath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Then("the optimized margin on {string} for timestamp {string} is {double} MW")
    public static void theOptimizedMarginOnCnecForTimestampIsMW(String cnecId, String timestamp, double margin) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        FlowCnec flowCnec = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac().getFlowCnec(cnecId);
        Instant afterCra = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac().getLastInstant();
        assertEquals(margin,
            timeCoupledRaoResult.getIndividualRaoResult(offsetDateTime).getMargin(afterCra, flowCnec, Unit.MEGAWATT),
            RaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the functional cost for timestamp {string} is {double}")
    public static void theFunctionalCostForTimestampIs(String timestamp, double functionalCost) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Instant afterCra = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac().getLastInstant();
        assertEquals(functionalCost,
            timeCoupledRaoResult.getFunctionalCost(afterCra, offsetDateTime),
            RaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the total cost for timestamp {string} is {double}")
    public static void theTotalCostForTimestampIs(String timestamp, double totalCost) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Instant afterCra = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac().getLastInstant();
        assertEquals(totalCost,
            timeCoupledRaoResult.getCost(afterCra, offsetDateTime),
            RaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the functional cost for all timestamps is {double}")
    public static void theFunctionalCostForAllTimestampsIs(double functionalCost) {
        OffsetDateTime firstTimestamp = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getTimestamps().getFirst();
        Instant lastInstant = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(firstTimestamp).orElseThrow().getCrac().getLastInstant();
        assertEquals(functionalCost,
            timeCoupledRaoResult.getGlobalFunctionalCost(lastInstant),
            RaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the total cost for all timestamps is {double}")
    public static void theTotalCostForAllTimestampsIs(double totalCost) {
        OffsetDateTime firstTimestamp = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getTimestamps().getFirst();
        Instant lastInstant = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(firstTimestamp).orElseThrow().getCrac().getLastInstant();
        assertEquals(totalCost,
            timeCoupledRaoResult.getGlobalCost(lastInstant),
            RaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @When("I export F711 for business date {string}") // expected format yyyyMMdd
    public static void exportF711(String businessDate) {
        Map<OffsetDateTime, RaoResult> raoResults = new HashMap<>();
        CommonTestData.getTimeCoupledRaoInput().getTimestampsToRun().forEach(timestamp -> raoResults.put(timestamp, timeCoupledRaoResult.getIndividualRaoResult(timestamp)));
        F711Utils.write(
            new TemporalDataImpl<>(raoResults),
            new TemporalDataImpl<>(cracCreationContexts).map(FbConstraintCreationContext.class::cast),
            cracPath,
            getResourcesPath().concat("raoresults/%s-FID2-711-v1-10V1001C--00264T-to-10V1001C--00085T.xml").formatted(businessDate)
        );
    }

    @When("I export F711 for business date {string} based on raoResults zip {string}") // expected format yyyyMMdd
    public static void exportF711(String businessDate, String raoResultsZipPath) throws IOException {
        //Unzip the given zip file to a temp folder
        Path tempDir = Files.createTempDirectory("raoResults_");
        unzipZipToFolder(getResourcesPath().concat(raoResultsZipPath), tempDir);
        try {
            Map<OffsetDateTime, RaoResult> raoResults = new HashMap<>();
            for (OffsetDateTime timestamp : CommonTestData.getTimeCoupledRaoInput().getTimestampsToRun()) {
                String filename = "RAO_RESULT_" + timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + ".json";
                FileInputStream raoResultInputStream = new FileInputStream(getFile(String.valueOf(tempDir.resolve(filename))));
                RaoResult raoResult = RaoResult.read(raoResultInputStream, CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(timestamp).orElseThrow().getCrac());
                raoResults.put(timestamp, raoResult);
            }
            F711Utils.write(
                new TemporalDataImpl<>(raoResults),
                new TemporalDataImpl<>(cracCreationContexts).map(FbConstraintCreationContext.class::cast),
                cracPath,
                getResourcesPath().concat("generatedF711/%s-FID2-711-v1-10V1001C--00264T-to-10V1001C--00085T.xml").formatted(businessDate)
            );
        } finally {
            deleteDirectoryRecursively(tempDir);
        }
    }

    @Then("the preventive power of generator {string} at timestamp {string} is {double} MW")
    public static void getGeneratorPower(String networkElementId, String timestamp, double expectedPower) {
        assertPowerValue(networkElementId, timestamp, expectedPower);
    }

    @Then("the preventive power of load {string} at timestamp {string} is {double} MW")
    public static void getLoadPower(String networkElementId, String timestamp, double expectedPower) {
        assertPowerValue(networkElementId, timestamp, -expectedPower);
    }

    @Then("the remedial action {string} is used at timestamp {string} in preventive")
    public static void remedialActionUsedInPreventive(String remedialActionId, String timestamp) {
        assertTrue(isRemedialActionUsed(remedialActionId, timestamp, "", "preventive"));
    }

    @Then("the remedial action {string} is used at timestamp {string} after {string} at {string}")
    public void remedialActionUsedPostContingency(String remedialActionId, String timestamp, String contingencyId, String instant) {
        assertTrue(isRemedialActionUsed(remedialActionId, timestamp, contingencyId, instant));
    }

    @Then("the remedial action {string} is not used at timestamp {string} in preventive")
    public static void remedialActionNotUsedInPreventive(String remedialActionId, String timestamp) {
        assertFalse(isRemedialActionUsed(remedialActionId, timestamp, "", "preventive"));
    }

    @Then("the remedial action {string} is not used at timestamp {string} after {string} at {string}")
    public void remedialActionNotUsedPostContingency(String remedialActionId, String timestamp, String contingencyId, String instant) {
        assertFalse(isRemedialActionUsed(remedialActionId, timestamp, contingencyId, instant));
    }

    @Then("its time coupled security status should be {string}")
    public void statusShouldBe(String status) {
        assertEquals(status.equalsIgnoreCase("secured"), timeCoupledRaoResult.isSecure(PhysicalParameter.FLOW));
    }

    @Then("the tap of PstRangeAction {string} at timestamp {string} after {string} at {string} should be {int}")
    public void theTapOfPstRangeActionPostContingencyShouldBe(String pstRangeActionId, String timestamp, String contingencyId, String instant, int chosenPstTap) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Crac crac = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac();
        assertEquals(chosenPstTap, timeCoupledRaoResult.getIndividualRaoResult(offsetDateTime).getOptimizedTapOnState(crac.getState(contingencyId, crac.getInstant(instant)), (PstRangeAction) crac.getRangeAction(pstRangeActionId)));
    }

    @Then("the preventive tap of PstRangeAction {string} at timestamp {string} should be {int}")
    public void theTapOfPreventivePstRangeActionShouldBe(String pstRangeActionId, String timestamp, int chosenPstTap) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Crac crac = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac();
        assertEquals(chosenPstTap, timeCoupledRaoResult.getIndividualRaoResult(offsetDateTime).getOptimizedTapOnState(crac.getPreventiveState(), (PstRangeAction) crac.getRangeAction(pstRangeActionId)));
    }

    private static void assertPowerValue(String networkElementId, String timestamp, double expectedPower) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Crac crac = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac();
        State preventiveState = crac.getPreventiveState();
        Optional<InjectionRangeAction> injectionRangeAction = crac.getRangeActions(preventiveState)
            .stream()
            .filter(InjectionRangeAction.class::isInstance)
            .map(InjectionRangeAction.class::cast)
            .filter(injection -> injection.getInjectionDistributionKeys().keySet().stream().anyMatch(networkElement -> networkElement.getId().equals(networkElementId)))
            .findFirst();
        assertTrue(injectionRangeAction.isPresent());
        NetworkElement networkElement = injectionRangeAction.get().getNetworkElements().stream().filter(ne -> ne.getId().equals(networkElementId)).findFirst().orElseThrow();
        assertEquals(
            expectedPower,
            timeCoupledRaoResult.getOptimizedSetPointOnState(preventiveState, injectionRangeAction.get()) / injectionRangeAction.get().getInjectionDistributionKeys().get(networkElement),
            TOLERANCE_REDISPATCHING_VALUE
        );
    }

    private static boolean isRemedialActionUsed(String rangeActionId, String timestamp, String contingencyId, String instant) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Crac crac = CommonTestData.getTimeCoupledRaoInput().getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac();
        State state = getState(crac, contingencyId, instant);
        return timeCoupledRaoResult.getIndividualRaoResult(offsetDateTime).isActivatedDuringState(state, crac.getRemedialAction(rangeActionId));
    }

    private static State getState(Crac crac, String contingencyId, String instantId) {
        if (instantId.equalsIgnoreCase("preventive")) {
            return crac.getPreventiveState();
        } else {
            return crac.getState(contingencyId, crac.getInstant(instantId));
        }
    }

    /**
     * Imports data from preprocessed files for a specific business date and runs MARMOT.
     * This method retrieves and processes data, including time-coupled constraints, networks, CRACs,
     * and RAO results.
     * <p>
     * This method looks into the marmot/sensitive/ folder and expects to find preprocessed files
     * for the specified business date in a folder named after the business date (with format YYYYMMDD).
     * Four subfolders are expected: time-coupled-constraints, networks, cracs, and intermediate-rao-results.
     * The constraints file should be named {@code time-coupled-constraints.json}, while the other files should be
     * named according to their timestamp (date-time parsing is automatically performed).
     *
     * @param businessDate the business date for which the preprocessed data is to be imported,
     *                     formatted as a string (YYYYMMDD)
     * @throws IOException if an I/O error occurs while reading the files
     */
    @When("I run MARMOT from preprocessed files for business date {string}")
    public void runMarmotFromPreprocessedFiles(String businessDate) throws IOException {
        String importPath = getResourcesPath().concat("marmot/sensitive/").concat(businessDate).concat("/");

        // 1. Import time-coupled constraints
        TimeCoupledConstraints timeCoupledConstraints;
        BUSINESS_LOGS.info("----- Importing time-coupled constraints [start]");
        try (FileInputStream fileInputStream = new FileInputStream(importPath.concat("time-coupled-constraints/time-coupled-constraints.json"))) {
            timeCoupledConstraints = JsonTimeCoupledConstraints.read(fileInputStream);
        }
        BUSINESS_LOGS.info("----- Importing time-coupled constraints [end]");

        // 2. Import networks
        BUSINESS_LOGS.info("----- Importing networks [start]");
        TemporalData<Network> networks = new TemporalDataImpl<>();
        File[] networkFiles = new File(importPath.concat("networks/")).listFiles((dir, name) -> name.endsWith(".jiidm"));

        if (networkFiles != null) {
            for (File networkFile : networkFiles) {
                String timestamp = networkFile.getName().replace(".jiidm", "");
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(timestamp);
                LazyNetwork network = new LazyNetwork(networkFile.toPath().toString());
                networks.put(offsetDateTime, network);
                BUSINESS_LOGS.info("Imported network for timestamp: {}", offsetDateTime);
            }
        }
        BUSINESS_LOGS.info("----- Importing networks [end]");

        // 3. Import CRACs
        BUSINESS_LOGS.info("----- Importing CRACs [start]");
        TemporalData<Crac> cracs = new TemporalDataImpl<>();
        File[] cracFiles = new File(importPath.concat("cracs/")).listFiles((dir, name) -> name.endsWith(".json"));

        if (cracFiles != null) {
            for (File cracFile : cracFiles) {
                String timestamp = cracFile.getName().replace(".json", "");
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(timestamp);
                try (FileInputStream cracInputStream = new FileInputStream(cracFile)) {
                    Crac crac = Crac.read(cracFile.getName(), cracInputStream, networks.getData(offsetDateTime).orElseThrow());
                    cracs.put(offsetDateTime, crac);
                    BUSINESS_LOGS.info("Imported CRAC for timestamp: {}", offsetDateTime);
                }
            }
        }
        BUSINESS_LOGS.info("----- Importing CRACs [end]");

        // 4. Import RAO Results
        BUSINESS_LOGS.info("----- Importing RAO Results [start]");
        TemporalData<RaoResult> raoResults = new TemporalDataImpl<>();
        File[] raoResultFiles = new File(importPath.concat("intermediate-rao-results/")).listFiles((dir, name) -> name.endsWith(".json"));

        if (raoResultFiles != null) {
            for (File raoResultFile : raoResultFiles) {
                String timestamp = raoResultFile.getName().replace(".json", "");
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(timestamp);
                try (FileInputStream raoResultInputStream = new FileInputStream(raoResultFile)) {
                    RaoResult raoResult = RaoResult.read(raoResultInputStream, cracs.getData(offsetDateTime).orElseThrow());
                    raoResults.put(offsetDateTime, raoResult);
                    BUSINESS_LOGS.info("Imported RAO Result for timestamp: {}", offsetDateTime);
                }
            }
        }
        BUSINESS_LOGS.info("----- Importing RAO Results [end]");

        List<OffsetDateTime> timestamps = networks.getTimestamps();
        TemporalData<RaoInput> raoInputs = new TemporalDataImpl<>();

        for (OffsetDateTime timestamp : timestamps) {
            RaoInput raoInput = RaoInput.build(networks.getData(timestamp).orElseThrow(), cracs.getData(timestamp).orElseThrow()).build();
            raoInputs.put(timestamp, raoInput);
            BUSINESS_LOGS.info("Imported RAO Input for timestamp: {}", timestamp);
            MarmotUtils.releaseAllWithoutOverwrite(networks);
            MarmotUtils.releaseAllWithoutOverwrite(raoInputs.map(RaoInput::getNetwork));
        }

        CommonTestData.setTimeCoupledRaoInput(new TimeCoupledRaoInput(raoInputs, new HashSet<>(timestamps), timeCoupledConstraints, raoResults));

        timeCoupledRaoResult = TimeCoupledRao.run(CommonTestData.getTimeCoupledRaoInput(), getRaoParameters());
    }
}
