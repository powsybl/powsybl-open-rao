
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
import com.powsybl.openrao.data.crac.io.fbconstraint.FbConstraintCreationContext;
import com.powsybl.openrao.data.crac.io.fbconstraint.parameters.FbConstraintCracCreationParameters;
import com.powsybl.openrao.data.crac.util.IcsImporter;
import com.powsybl.openrao.data.intertemporalconstraints.IntertemporalConstraints;
import com.powsybl.openrao.data.intertemporalconstraints.io.JsonIntertemporalConstraints;
import com.powsybl.openrao.data.raoresult.api.InterTemporalRaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.io.idcc.core.F711Utils;
import com.powsybl.openrao.data.refprog.refprogxmlimporter.InterTemporalRefProg;
import com.powsybl.openrao.raoapi.*;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.MDC;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.tests.steps.CommonTestData.*;
import static com.powsybl.openrao.tests.utils.Helpers.*;
import static com.powsybl.openrao.tests.utils.Helpers.getFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class InterTemporalRaoSteps {
    private static String networkFolderPath;
    private static String cracFolderPath;
    private static boolean useIndividualCracs = false;
    private static String networkFolderPathPostIcsImport;
    private static String icsStaticPath;
    private static String icsSeriesPath;
    private static String icsGskPath;
    private static String refProgPath;
    private static InterTemporalRaoInputWithNetworkPaths interTemporalRaoInputWithNetworkPaths;
    private static InterTemporalRaoResult interTemporalRaoResult;
    private static Map<OffsetDateTime, CracCreationContext> cracCreationContexts;

    private static final List<String> DE_TSOS = List.of("D2", "D4", "D7", "D8");
    static final String DEFAULT_CRAC_CREATION_PARAMETERS_PATH = "cracCreationParameters/epic93/CracCreationParameters_93.json";

    public InterTemporalRaoSteps() {
        // should not be instantiated
    }

    @Given("intertemporal RefProg file is {string}")
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

    @Given("intertemporal constraints are in file {string} and rao inputs are:")
    public static void intertemporalRaoInputsAre(String intertemporalConstraintsPath, DataTable arg1) throws IOException {
        loadDataForInterTemporalRao(intertemporalConstraintsPath, arg1);
    }

    public static void loadDataForInterTemporalRao(String intertemporalConstraintsPath, DataTable arg1) throws IOException {
        raoParameters = buildConfig(getFile(raoParametersPath));

        networkFolderPath = getResourcesPath().concat("cases/");
        cracFolderPath = getResourcesPath().concat("crac/");
        String intertemporalConstraintsFolderPath = getResourcesPath().concat("intertemporalConstraints/");

        TemporalData<RaoInputWithNetworkPaths> raoInputs = new TemporalDataImpl<>();
        List<Map<String, String>> inputs = arg1.asMaps(String.class, String.class);
        for (Map<String, String> tsInput : inputs) {
            OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(tsInput.get("Timestamp"));
            Network network = importNetwork(getFile(networkFolderPath.concat(tsInput.get("Network"))), false);
            Crac crac = importCrac(getFile(cracFolderPath.concat(tsInput.get("CRAC"))), network, null).getLeft();
            raoInputs.put(offsetDateTime, RaoInputWithNetworkPaths.build(networkFolderPath.concat(tsInput.get("Network")), networkFolderPath.concat(tsInput.get("Network")), crac).build());
        }

        IntertemporalConstraints intertemporalConstraints = JsonIntertemporalConstraints.read(new FileInputStream(intertemporalConstraintsFolderPath.concat(intertemporalConstraintsPath)));
        interTemporalRaoInputWithNetworkPaths = new InterTemporalRaoInputWithNetworkPaths(raoInputs, intertemporalConstraints);
    }

    @Given("intertemporal rao inputs for CORE are:")
    public static void coreIntertemporalRaoInputsAre(DataTable arg1) throws IOException {
        loadDataForCoreInterTemporalRao(arg1);
    }

    public static void loadDataForCoreInterTemporalRao(DataTable arg1) throws IOException {
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

        TemporalData<RaoInputWithNetworkPaths> raoInputs = new TemporalDataImpl<>();
        List<Map<String, String>> inputs = arg1.asMaps(String.class, String.class);
        for (Map<String, String> tsInput : inputs) {
            OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(tsInput.get("Timestamp"));
            TECHNICAL_LOGS.info("**** Loading data for TS {} ****", offsetDateTime);
            // Network
            String initialNetworkPath = networkFolderPath.concat(tsInput.get("Network"));
            String postIcsNetworkPath = networkFolderPathPostIcsImport.concat(tsInput.get("Network")).split(".uct")[0].concat(".jiidm");
            Network network = importNetwork(getFile(networkFolderPath.concat(tsInput.get("Network"))), false);
            CoreCcPreprocessor.applyCoreCcNetworkPreprocessing(network);
            // Crac
            Pair<Crac, CracCreationContext> cracImportResult;
            if (useIndividualCracs) { // only works with json
                cracImportResult = importCrac(getFile(cracFolderPath.concat(tsInput.get("Crac"))), network, null);
            } else {
                cracCreationParameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(offsetDateTime);
                cracImportResult = importCrac(cracFile, network, cracCreationParameters);
            }
            RaoInputWithNetworkPaths raoInput = RaoInputWithNetworkPaths
                .build(initialNetworkPath, postIcsNetworkPath, cracImportResult.getLeft())
                .build();
            raoInputs.put(offsetDateTime, raoInput);
            cracCreationContexts.put(offsetDateTime, cracImportResult.getRight());
        }
        interTemporalRaoInputWithNetworkPaths = new InterTemporalRaoInputWithNetworkPaths(raoInputs, new IntertemporalConstraints());
        InputStream gskInputStream = icsGskPath == null ? null : new FileInputStream(getFile(icsGskPath));

        FbConstraintCracCreationParameters fbConstraintParameters = cracCreationParameters.getExtension(FbConstraintCracCreationParameters.class);
        if (fbConstraintParameters == null) {
            TECHNICAL_LOGS.warn("No FB Constraint CRAC creation parameters found. Default parameters will be used.");
            fbConstraintParameters = new FbConstraintCracCreationParameters();
        }
        IcsImporter.populateInputWithICS(interTemporalRaoInputWithNetworkPaths, new FileInputStream(getFile(icsStaticPath)), new FileInputStream(getFile(icsSeriesPath)), gskInputStream, fbConstraintParameters.getIcsCostUp(), fbConstraintParameters.getIcsCostDown());
    }

    @When("I launch marmot")
    public static void iLaunchMarmot() {
        interTemporalRaoResult = InterTemporalRao.run(interTemporalRaoInputWithNetworkPaths, getRaoParameters());
    }

    @When("I export marmot results to {string}")
    public static void iExportMarmotResults(String outputPath) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(getFile(getResourcesPath().concat(outputPath)));
        Properties properties = new Properties();
        properties.put("rao-result.export.json.flows-in-megawatts", "true");
        properties.put("inter-temporal-rao-result.export.filename-template", "'RAO_RESULT_'yyyy-MM-dd'T'HH:mm:ss'.json'");
        properties.put("inter-temporal-rao-result.export.summary-filename", "summary.json");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
            interTemporalRaoResult.write(zipOutputStream, interTemporalRaoInputWithNetworkPaths.getRaoInputs().map(RaoInputWithNetworkPaths::getCrac), properties);
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
            for (OffsetDateTime offsetDateTime : interTemporalRaoInputWithNetworkPaths.getTimestampsToRun()) {
                rdVolumes.put(offsetDateTime, new HashMap<>());
                String filename = "RAO_RESULT_" + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + ".json";
                FileInputStream raoResultInputStream = new FileInputStream(getFile(String.valueOf(tempDir.resolve(filename))));
                RaoResult raoResult = RaoResult.read(raoResultInputStream, interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac());

                // Load redispatching volumes
                Crac crac = interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).get().getCrac();
                Set<RangeAction<?>> preventiveRangeActions = raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState());
                Set<InjectionRangeAction> redispatchingRangeActions = preventiveRangeActions.stream().filter(InjectionRangeAction.class::isInstance).map(InjectionRangeAction.class::cast).collect(Collectors.toSet());
                redispatchingRangeActions.forEach(rangeAction -> {
                    double redispatchedVolumeForRa = raoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), rangeAction) - rangeAction.getInitialSetpoint();
                    for (String subString : rangeAction.getId().toUpperCase().split("_")) {
                        if (subString.equals("RA")) {
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
                InterTemporalRefProg.updateRefProg(refProgInputStream, new TemporalDataImpl<>(rdVolumes), becValues, getResourcesPath().concat(outputPath));
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

    @When("I export networks with PRAs to {string} based on raoResults zip {string}")
    public static void generateNetworksWithPraFromResults(String outputPath, String raoResultsZipPath) throws IOException {

        //Unzip the given zip file to a temp folder
        Path tempDir = Files.createTempDirectory("raoResults_");
        unzipZipToFolder(getResourcesPath().concat(raoResultsZipPath), tempDir);

        FileOutputStream fileOutputStream = new FileOutputStream(getFile(getResourcesPath().concat(outputPath)));
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
        try {
            for (OffsetDateTime offsetDateTime : interTemporalRaoInputWithNetworkPaths.getTimestampsToRun()) {

                String filename = "RAO_RESULT_" + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + ".json";
                FileInputStream raoResultInputStream = new FileInputStream(getFile(String.valueOf(tempDir.resolve(filename))));
                RaoResult raoResult = RaoResult.read(raoResultInputStream, interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac());

                Set<NetworkAction> preventiveNetworkActions = raoResult.getActivatedNetworkActionsDuringState(interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).get().getCrac().getPreventiveState());
                Set<RangeAction<?>> preventiveRangeActions = raoResult.getActivatedRangeActionsDuringState(interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).get().getCrac().getPreventiveState());
                Network modifiedNetwork = Network.read(interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).orElseThrow().getPostIcsImportNetworkPath());
                Network initialNetwork = Network.read(interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).orElseThrow().getInitialNetworkPath());

                // Apply PRAs on modified network
                preventiveNetworkActions.forEach(networkAction -> networkAction.apply(initialNetwork));
                preventiveRangeActions.forEach(rangeAction -> {
                    double optimizedSetpoint = raoResult.getOptimizedSetPointOnState(interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).get().getCrac().getPreventiveState(), rangeAction);
                    if (rangeAction instanceof InjectionRangeAction) {
                        applyRedispatchingAction((InjectionRangeAction) rangeAction, optimizedSetpoint, modifiedNetwork, initialNetwork);
                    } else {
                        rangeAction.apply(initialNetwork, optimizedSetpoint);
                    }
                });
                // Write network
                String path = interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).orElseThrow().getPostIcsImportNetworkPath().split(".jiidm")[0].concat(".uct");
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
        } finally {
            deleteDirectoryRecursively(tempDir);
        }
    }

    @When("I export networks with PRAs to {string}")
    public static void iExportNetworksWithPras(String outputPath) throws IOException {

        FileOutputStream fileOutputStream = new FileOutputStream(getFile(getResourcesPath().concat(outputPath)));
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

        for (OffsetDateTime offsetDateTime : interTemporalRaoResult.getTimestamps()) {
            Set<NetworkAction> preventiveNetworkActions = interTemporalRaoResult.getIndividualRaoResult(offsetDateTime).getActivatedNetworkActionsDuringState(interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).get().getCrac().getPreventiveState());
            Set<RangeAction<?>> preventiveRangeActions = interTemporalRaoResult.getIndividualRaoResult(offsetDateTime).getActivatedRangeActionsDuringState(interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).get().getCrac().getPreventiveState());
            Network modifiedNetwork = Network.read(interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).orElseThrow().getPostIcsImportNetworkPath());
            Network initialNetwork = Network.read(interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).orElseThrow().getInitialNetworkPath());

            // Apply PRAs on modified network
            preventiveNetworkActions.forEach(networkAction -> networkAction.apply(initialNetwork));
            preventiveRangeActions.forEach(rangeAction -> {
                double optimizedSetpoint = interTemporalRaoResult.getIndividualRaoResult(offsetDateTime).getOptimizedSetPointOnState(interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).get().getCrac().getPreventiveState(), rangeAction);
                if (rangeAction instanceof InjectionRangeAction) {
                    applyRedispatchingAction((InjectionRangeAction) rangeAction, optimizedSetpoint, modifiedNetwork, initialNetwork);
                } else {
                    rangeAction.apply(initialNetwork, optimizedSetpoint);
                }
            });
            // Write network
            String path = interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).orElseThrow().getPostIcsImportNetworkPath().split(".jiidm")[0].concat(".uct");
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

    @Then("the initial margin on {string} for timestamp {string} is {double} MW")
    public static void theInitialMarginOnCnecForTimestampIsMW(String cnecId, String timestamp, double margin) {
        checkMarginInMw(cnecId, timestamp, margin, null);
    }

    @Then("the optimized margin on {string} for timestamp {string} is {double} MW")
    public static void theOptimizedMarginOnCnecForTimestampIsMW(String cnecId, String timestamp, double margin) {
        Instant afterCra = interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(getOffsetDateTimeFromBrusselsTimestamp(timestamp)).orElseThrow().getCrac().getLastInstant();
        checkMarginInMw(cnecId, timestamp, margin, afterCra);
    }

    private static void checkMarginInMw(String cnecId, String timestamp, double margin, Instant optimizedInstant) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        FlowCnec flowCnec = interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac().getFlowCnec(cnecId);
        assertEquals(margin,
            interTemporalRaoResult.getIndividualRaoResult(offsetDateTime).getMargin(optimizedInstant, flowCnec, Unit.MEGAWATT),
            RaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the functional cost for timestamp {string} is {double}")
    public static void theFunctionalCostForTimestampIs(String timestamp, double functionalCost) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Instant afterCra = interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac().getLastInstant();
        assertEquals(functionalCost,
            interTemporalRaoResult.getFunctionalCost(afterCra, offsetDateTime),
            RaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the total cost for timestamp {string} is {double}")
    public static void theTotalCostForTimestampIs(String timestamp, double totalCost) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Instant afterCra = interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac().getLastInstant();
        assertEquals(totalCost,
            interTemporalRaoResult.getCost(afterCra, offsetDateTime),
            RaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the functional cost for all timestamps is {double}")
    public static void theFunctionalCostForAllTimestampsIs(double functionalCost) {
        assertEquals(functionalCost,
            interTemporalRaoResult.getGlobalFunctionalCost(cracCreationContexts.values().iterator().next().getCrac().getLastInstant()),
            RaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @Then("the total cost for all timestamps is {double}")
    public static void theTotalCostForAllTimestampsIs(double totalCost) {
        assertEquals(totalCost,
            interTemporalRaoResult.getGlobalCost(cracCreationContexts.values().iterator().next().getCrac().getLastInstant()),
            RaoSteps.TOLERANCE_FLOW_IN_MEGAWATT);
    }

    @When("I export F711 for business date {string}") // expected format yyyyMMdd
    public static void exportF711(String businessDate) {
        Map<OffsetDateTime, RaoResult> raoResults = new HashMap<>();
        interTemporalRaoInputWithNetworkPaths.getTimestampsToRun().forEach(timestamp -> raoResults.put(timestamp, interTemporalRaoResult.getIndividualRaoResult(timestamp)));
        F711Utils.write(new TemporalDataImpl<>(raoResults), new TemporalDataImpl<>(cracCreationContexts).map(FbConstraintCreationContext.class::cast), cracPath, getResourcesPath().concat("raoresults/%s-FID2-711-v1-10V1001C--00264T-to-10V1001C--00085T.xml").formatted(businessDate));
    }

    @When("I export F711 for business date {string} based on raoResults zip {string}") // expected format yyyyMMdd
    public static void exportF711(String businessDate, String raoResultsZipPath) throws IOException {
        //Unzip the given zip file to a temp folder
        Path tempDir = Files.createTempDirectory("raoResults_");
        unzipZipToFolder(getResourcesPath().concat(raoResultsZipPath), tempDir);
        try {
            Map<OffsetDateTime, RaoResult> raoResults = new HashMap<>();
            for (OffsetDateTime timestamp : interTemporalRaoInputWithNetworkPaths.getTimestampsToRun()) {
                String filename = "RAO_RESULT_" + timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + ".json";
                FileInputStream raoResultInputStream = new FileInputStream(getFile(String.valueOf(tempDir.resolve(filename))));
                RaoResult raoResult = RaoResult.read(raoResultInputStream, interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(timestamp).orElseThrow().getCrac());
                raoResults.put(timestamp, raoResult);
            }
            F711Utils.write(new TemporalDataImpl<>(raoResults), new TemporalDataImpl<>(cracCreationContexts).map(FbConstraintCreationContext.class::cast), cracPath, getResourcesPath().concat("generatedF711/%s-FID2-711-v1-10V1001C--00264T-to-10V1001C--00085T.xml").formatted(businessDate));
        } finally {
            deleteDirectoryRecursively(tempDir);
        }
    }

    @Then("the preventive power of generator {string} at state timestamp {string} is {double} MW")
    public static void getGeneratorPower(String networkElementId, String timestamp, double expectedPower) {
        assertPowerValue(networkElementId, timestamp, expectedPower);
    }

    @Then("the preventive power of load {string} at state timestamp {string} is {double} MW")
    public static void getLoadPower(String networkElementId, String timestamp, double expectedPower) {
        assertPowerValue(networkElementId, timestamp, -expectedPower);
    }

    private static void assertPowerValue(String networkElementId, String timestamp, double expectedPower) {
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        Crac crac = interTemporalRaoInputWithNetworkPaths.getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac();
        State preventiveState = crac.getPreventiveState();
        Optional<InjectionRangeAction> injectionRangeAction = crac.getRangeActions(preventiveState)
            .stream()
            .filter(InjectionRangeAction.class::isInstance)
            .map(InjectionRangeAction.class::cast)
            .filter(injection -> injection.getInjectionDistributionKeys().keySet().stream().anyMatch(networkElement -> networkElement.getId().equals(networkElementId)))
            .findFirst();
        assertTrue(injectionRangeAction.isPresent());
        NetworkElement networkElement = injectionRangeAction.get().getNetworkElements().stream().filter(ne -> ne.getId().equals(networkElementId)).findFirst().orElseThrow();
        assertEquals(expectedPower, interTemporalRaoResult.getOptimizedSetPointOnState(preventiveState, injectionRangeAction.get()) / injectionRangeAction.get().getInjectionDistributionKeys().get(networkElement), 1e-3);
    }
}
