
/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.steps;

import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.nad.NadParameters;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.nad.build.iidm.VoltageLevelFilter;
import com.powsybl.nad.layout.LayoutParameters;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.dc.equations.DcApproximationType;
import com.powsybl.openloadflow.network.LinePerUnitMode;
import com.powsybl.openloadflow.network.ReferenceBusSelectionMode;
import com.powsybl.openloadflow.network.SlackBusSelectionMode;
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
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.data.refprog.refprogxmlimporter.InterTemporalRefProg;
import com.powsybl.openrao.data.refprog.refprogxmlimporter.RefProgImporter;
import com.powsybl.openrao.raoapi.*;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.tests.utils.CoreCcPreprocessor;
import io.cucumber.datatable.DataTable;
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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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
    private static String refProgPath;
    private static ReferenceProgram referenceProgram;
    private static InterTemporalRaoInputWithNetworkPaths interTemporalRaoInput;
    private static InterTemporalRaoResult interTemporalRaoResult;

    private static final List<String> DE_TSOS = List.of("D2", "D4", "D7", "D8");

    public InterTemporalRaoSteps() {
        // should not be instantiated
    }

    @Given("intertemporal RefProg file is {string}")
    public static void refProgFileIs(String path) {
        refProgPath = getResourcesPath().concat("refprogs/").concat(path);
    }

    // TODO : add after to run after all @intertemporal scenarios
    private static void cleanModifiedNetworks() {
        interTemporalRaoInput.getTimestampsToRun().forEach(offsetDateTime -> {
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
            CoreCcPreprocessor.applyCoreCcNetworkPreprocessing(network, false);
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
        IcsImporter.populateInputWithICS(interTemporalRaoInput, new FileInputStream(getFile(icsStaticPath)), new FileInputStream(getFile(icsSeriesPath)), gskInputStream, raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getIcsImporterParameters().orElseThrow());

    }

    @When("I launch marmot")
    public static void iLaunchMarmot() {
        interTemporalRaoResult = InterTemporalRao.run(interTemporalRaoInput, getRaoParameters());
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

    @When("I export RefProg after redispatching to {string} based on raoResults folder {string}")
    public static void generateRefProg(String outputPath, String raoResultsPath) throws IOException {
        // Load networks in networkTemporalData
        // Load redispatchingVolume per timestamp per operator
        Map<OffsetDateTime, Map<String, Double>> rdVolumes = new HashMap<>();
        for (OffsetDateTime offsetDateTime : interTemporalRaoInput.getTimestampsToRun()) {
            rdVolumes.put(offsetDateTime, new HashMap<>());

            FileInputStream raoResultInputStream = new FileInputStream(getFile(getResourcesPath().concat(raoResultsPath) + "/RAO_RESULT_" + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + ".json"));
            RaoResult raoResult = RaoResult.read(raoResultInputStream, interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac());

            // Load redispatching volumes
            Crac crac = interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).get().getCrac();
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
            //OffsetDateTime firstTimestamp = interTemporalRaoInput.getTimestampsToRun().stream().findFirst().get();
            //ReferenceProgram generatedRefProg = RefProgImporter.importRefProg(refProgInputStream, firstTimestamp);
            InterTemporalRefProg.updateRefProg(refProgInputStream, new TemporalDataImpl<>(rdVolumes), becValues, getResourcesPath().concat(outputPath));
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

    @When("I export networks with PRAs to {string} from raoresult folder {string}")
    public static void generateNetworksWithPraFromResults(String outputPath, String resultsPath) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(getFile(getResourcesPath().concat(outputPath)));
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

        for (OffsetDateTime offsetDateTime : interTemporalRaoInput.getTimestampsToRun()) {
            FileInputStream raoResultInputStream = new FileInputStream(getFile(getResourcesPath().concat(resultsPath) + "/RAO_RESULT_" + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + ".json"));
            RaoResult raoResult = RaoResult.read(raoResultInputStream, interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).orElseThrow().getCrac());

            Set<NetworkAction> preventiveNetworkActions = raoResult.getActivatedNetworkActionsDuringState(interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).get().getCrac().getPreventiveState());
            Set<RangeAction<?>> preventiveRangeActions = raoResult.getActivatedRangeActionsDuringState(interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).get().getCrac().getPreventiveState());
            Network modifiedNetwork = Network.read(interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).orElseThrow().getPostIcsImportNetworkPath());
            Network initialNetwork = Network.read(interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).orElseThrow().getInitialNetworkPath());

            // Apply PRAs on modified network
            preventiveNetworkActions.forEach(networkAction -> networkAction.apply(initialNetwork));
            preventiveRangeActions.forEach(rangeAction -> {
                double optimizedSetpoint = raoResult.getOptimizedSetPointOnState(interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).get().getCrac().getPreventiveState(), rangeAction);
                if (rangeAction instanceof InjectionRangeAction) {
                    applyRedispatchingAction((InjectionRangeAction) rangeAction, optimizedSetpoint, modifiedNetwork, initialNetwork);
                } else {
                    rangeAction.apply(initialNetwork, optimizedSetpoint);
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
            preventiveNetworkActions.forEach(networkAction -> networkAction.apply(initialNetwork));
            preventiveRangeActions.forEach(rangeAction -> {
                double optimizedSetpoint = interTemporalRaoResult.getIndividualRaoResult(offsetDateTime).getOptimizedSetPointOnState(interTemporalRaoInput.getRaoInputs().getData(offsetDateTime).get().getCrac().getPreventiveState(), rangeAction);
                if (rangeAction instanceof InjectionRangeAction) {
                    applyRedispatchingAction((InjectionRangeAction) rangeAction, optimizedSetpoint, modifiedNetwork, initialNetwork);
                } else {
                    rangeAction.apply(initialNetwork, optimizedSetpoint);
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

    @When("I check flows")
    public void iCheckFlows() {
        Network network = Network.read(getResourcesPath().concat("cases/idcc/20240718-FID2-734-v1-10V1001C--00264T-to-10V1001C--00085T/20240718_0030_2D4_UX2_FINIT_EXPORTGRIDMODEL_DC_CGM_10V1001C--00264T.uct"));
        //Network network = Network.read(getResourcesPath().concat("cases/idcc/20240718-FID2-620-v3-10V1001C--00264T-to-10V1001C--00085T/20240718_0030_FO4_UX0.uct"));
        CoreCcPreprocessor.applyCoreCcNetworkPreprocessing(network, false);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(true);
        loadFlowParameters.setBalanceType(LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_P);


        LoadFlow.find("OpenLoadFlow").run(network, loadFlowParameters);
        System.out.printf("flow before contingency %.2f\n", network.getLine("DHE_WO11 D8WOL_11 1").getTerminal1().getP());

        network.getLine("DHE_WO12 D8WOL_11 1").getTerminal1().disconnect();
        network.getLine("DHE_WO12 D8WOL_11 1").getTerminal2().disconnect();
        network.getLine("DHE_WO12 D2HELM11 1").getTerminal1().disconnect();
        network.getLine("DHE_WO12 D2HELM11 1").getTerminal2().disconnect();

        LoadFlow.find("OpenLoadFlow").run(network, loadFlowParameters);
        System.out.printf("flow after contingency %.2f\n", network.getLine("DHE_WO11 D8WOL_11 1").getTerminal1().getP());

        /*SvgParameters svgParameters = new SvgParameters().setFixedHeight(3000);
        LayoutParameters layoutParameters = new LayoutParameters().setSpringRepulsionFactorForceLayout(0.7);
        NadParameters nadParameters = new NadParameters().setSvgParameters(svgParameters).setLayoutParameters(layoutParameters);
        VoltageLevelFilter vlFilter = VoltageLevelFilter.createVoltageLevelDepthFilter(network, "DHE_WO1", 5);
        NetworkAreaDiagram.draw(network, Path.of("/tmp/diagram2.svg"), nadParameters, vlFilter);*/
    }

    /*private void rebalance(Network network) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(getResourcesPath().concat("refprogs/idcc/refprog_initial.xml"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        ReferenceProgram referenceProgram = RefProgImporter.importRefProg(inputStream, getOffsetDateTimeFromBrusselsTimestamp("2024-07-18 00:30"));

        Map<Country, Double> totalGenerationPerCountry = new HashMap<>();
        Map<Country, Double> totalLoadPerCountry = new HashMap<>();

        network.getGeneratorStream().forEach(generator -> {
            Optional<Substation> substationOptional = generator.getTerminal().getVoltageLevel().getSubstation();
            Country country = substationOptional.get().getNullableCountry();
            totalGenerationPerCountry.putIfAbsent(country, 0.);
            totalGenerationPerCountry.put(country, totalGenerationPerCountry.get(country) + generator.getTargetP());
        });

        network.getLoadStream().forEach(load -> {
            Optional<Substation> substationOptional = load.getTerminal().getVoltageLevel().getSubstation();
            Country country = substationOptional.get().getNullableCountry();
            totalLoadPerCountry.putIfAbsent(country, 0.);
            totalLoadPerCountry.put(country, totalLoadPerCountry.get(country) + load.getP0());
        });

        Map<Country, Double> netPositionPerCountry = new HashMap<>();
        totalGenerationPerCountry.keySet().forEach(country -> {
            netPositionPerCountry.put(country, totalGenerationPerCountry.get(country) - totalLoadPerCountry.get(country));
        });

        netPositionPerCountry.forEach((country, netPosition) -> {
            if (country == Country.XK) {
                return;
            }
            double refProgNP = referenceProgram.getGlobalNetPosition(new CountryEICode(country).getCode());
            double delta = refProgNP - netPosition;
            double totalInjection = totalGenerationPerCountry.get(country) + totalLoadPerCountry.get(country);
            System.out.printf("delta of %.2f for country %s\n", delta, country);
            network.getGeneratorStream().forEach(generator -> {
                Optional<Substation> substationOptional = generator.getTerminal().getVoltageLevel().getSubstation();
                if (country == substationOptional.get().getNullableCountry()) {
                    generator.setTargetP(generator.getTargetP() + delta * generator.getTargetP() / totalInjection);
                }
            });

            network.getLoadStream().forEach(load -> {
                Optional<Substation> substationOptional = load.getTerminal().getVoltageLevel().getSubstation();
                if (country == substationOptional.get().getNullableCountry()) {
                    load.setP0(load.getP0() - delta * load.getP0() / totalInjection);
                }
            });
        });
        Map<Country, Double> newTotalGenerationPerCountry = new HashMap<>();
        Map<Country, Double> newTotalLoadPerCountry = new HashMap<>();

        network.getGeneratorStream().forEach(generator -> {
            Optional<Substation> substationOptional = generator.getTerminal().getVoltageLevel().getSubstation();
            Country country = substationOptional.get().getNullableCountry();
            newTotalGenerationPerCountry.putIfAbsent(country, 0.);
            newTotalGenerationPerCountry.put(country, newTotalGenerationPerCountry.get(country) + generator.getTargetP());
        });

        network.getLoadStream().forEach(load -> {
            Optional<Substation> substationOptional = load.getTerminal().getVoltageLevel().getSubstation();
            Country country = substationOptional.get().getNullableCountry();
            newTotalLoadPerCountry.putIfAbsent(country, 0.);
            newTotalLoadPerCountry.put(country, newTotalLoadPerCountry.get(country) + load.getP0());
        });

        Map<Country, Double> newNetPositionPerCountry = new HashMap<>();
        totalGenerationPerCountry.keySet().forEach(country -> {
            newNetPositionPerCountry.put(country, newTotalGenerationPerCountry.get(country) - newTotalLoadPerCountry.get(country));
        });
    }*/
}
