package com.powsybl.openrao.raoapi;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.LoadType;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.intertemporalconstraint.PowerGradient;
import com.powsybl.openrao.raoapi.parameters.extensions.IcsImporterParameters;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

public final class IcsImporter {
    private static final int OFFSET = 2;
    private static final String MAX_GRADIENT = "1000";

    private static double costUp;
    private static double costDown;
    // TODO:QUALITY CHECK: do PO respect constraints?
    // TODO:QUALITY CHECK: consistency between gsks defined in static file + gsk file

    // INFOS
    // Gradient constraints are defined for gsks à la maille parade, et pas par générateur : on applique donc le shift key au Pmax/Pmin

    private IcsImporter() {
        //should only be used statically
    }

    public static void populateInputWithICS(InterTemporalRaoInputWithNetworkPaths interTemporalRaoInput, InputStream staticInputStream, InputStream seriesInputStream, InputStream gskInputStream, IcsImporterParameters icsImporterParameters) throws IOException {
        costUp = icsImporterParameters.getCostUp();
        costDown = icsImporterParameters.getCostDown();

        TemporalData<Network> initialNetworks = new TemporalDataImpl<>();
        interTemporalRaoInput.getRaoInputs().getDataPerTimestamp().forEach((dateTime, raoInput) ->
            initialNetworks.add(dateTime, Network.read(raoInput.getInitialNetworkPath()))
        );
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
            .setDelimiter(";")
            .setHeader()
            .setSkipHeaderRecord(true)
            .get();
        Iterable<CSVRecord> staticCsvRecords = csvFormat.parse(new InputStreamReader(staticInputStream));
        Iterable<CSVRecord> seriesCsvRecords = csvFormat.parse(new InputStreamReader(seriesInputStream));

        Map<String, Map<String, CSVRecord>> seriesPerIdAndType = new HashMap<>();
        seriesCsvRecords.forEach(record -> {
            seriesPerIdAndType.putIfAbsent(record.get("RA RD ID"), new HashMap<>());
            seriesPerIdAndType.get(record.get("RA RD ID")).put(record.get("Type of timeseries"), record);
        });

        Map<String, Map<String, Double>> weightPerNodePerGsk = new HashMap<>();
        if (gskInputStream != null) {
            Iterable<CSVRecord> gskCsvRecords = csvFormat.parse(new InputStreamReader(gskInputStream));
            gskCsvRecords.forEach(record -> {
                weightPerNodePerGsk.putIfAbsent(record.get("GSK ID"), new HashMap<>());
                weightPerNodePerGsk.get(record.get("GSK ID")).put(record.get("Node"), parseDoubleWithPossibleCommas(record.get("Weight")));
            });
        }

        staticCsvRecords.forEach(staticRecord -> {
            if (shouldBeImported(staticRecord, weightPerNodePerGsk)) {
                String raId = staticRecord.get("RA RD ID");
                Map<String, CSVRecord> seriesPerType = seriesPerIdAndType.get(raId);
                if (seriesPerType != null && seriesPerType.containsKey("P0") && seriesPerType.containsKey("RDP-") && seriesPerType.containsKey("RDP+") && p0RespectsGradients(staticRecord, seriesPerType.get("P0"), interTemporalRaoInput.getTimestampsToRun().stream().sorted().toList())) {
                    if (staticRecord.get("RD description mode").equalsIgnoreCase("NODE")) {
                        importNodeRedispatchingAction(interTemporalRaoInput, staticRecord, initialNetworks, seriesPerType, raId);
                    } else {
                        importGskRedispatchingAction(interTemporalRaoInput, staticRecord, initialNetworks, seriesPerType, raId, weightPerNodePerGsk.get(staticRecord.get("UCT Node or GSK ID")));
                    }
                }
            }
        });

        initialNetworks.getDataPerTimestamp().forEach((dateTime, initialNetwork) ->
            initialNetwork.write("JIIDM", new Properties(), Path.of(interTemporalRaoInput.getRaoInputs().getData(dateTime).orElseThrow().getPostIcsImportNetworkPath())));
    }

    private static void importGskRedispatchingAction(InterTemporalRaoInputWithNetworkPaths interTemporalRaoInput, CSVRecord staticRecord, TemporalData<Network> initialNetworks, Map<String, CSVRecord> seriesPerType, String raId, Map<String, Double> weightPerNode) {
        Map<String, String> networkElementPerGskElement = new HashMap<>();
        for (String nodeId : weightPerNode.keySet()) {
            String networkElementId = processNetworks(nodeId, initialNetworks, seriesPerType, weightPerNode.get(nodeId));
            if (networkElementId == null) {
                return;
            }
            networkElementPerGskElement.put(nodeId, networkElementId);
        }

        interTemporalRaoInput.getRaoInputs().getDataPerTimestamp().forEach((dateTime, raoInput) -> {
            Crac crac = raoInput.getCrac();
            double p0 = parseDoubleWithPossibleCommas(seriesPerType.get("P0").get(dateTime.getHour() + OFFSET));
            InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction()
                .withId(raId + "_RD")
                .withName(staticRecord.get("Generator Name"))
                .withInitialSetpoint(p0)
                .withVariationCost(costUp, VariationDirection.UP)
                .withVariationCost(costDown, VariationDirection.DOWN)
                //.withActivationCost(ACTIVATION_COST)
                .newRange()
                .withMin(p0 - parseDoubleWithPossibleCommas(seriesPerType.get("RDP-").get(dateTime.getHour() + OFFSET)))
                .withMax(p0 + parseDoubleWithPossibleCommas(seriesPerType.get("RDP+").get(dateTime.getHour() + OFFSET)))
                .add();

            weightPerNode.forEach((nodeId, shiftKey) -> {
                injectionRangeActionAdder.withNetworkElementAndKey(shiftKey, networkElementPerGskElement.get(nodeId));
            });

            if (staticRecord.get("Preventive").equalsIgnoreCase("TRUE")) {
                injectionRangeActionAdder.newOnInstantUsageRule()
                    .withInstant(crac.getPreventiveInstant().getId())
                    .withUsageMethod(UsageMethod.AVAILABLE)
                    .add();
            }
        /*if (staticRecord.get("Curative").equalsIgnoreCase("TRUE")) {
            injectionRangeActionAdder.newOnInstantUsageRule()
                .withInstant(crac.getLastInstant().getId())
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add();
        }*/
            injectionRangeActionAdder.add();

        });

        weightPerNode.forEach((nodeId, shiftKey) -> {
            PowerGradient powerGradient = PowerGradient.builder()
                .withNetworkElementId(networkElementPerGskElement.get(nodeId))
                .withMaxValue(shiftKey * parseDoubleWithPossibleCommas(
                    staticRecord.get("Maximum positive power gradient [MW/h]").isEmpty() ? MAX_GRADIENT : staticRecord.get("Maximum positive power gradient [MW/h]")
                )).withMinValue(-shiftKey * parseDoubleWithPossibleCommas(
                    staticRecord.get("Maximum negative power gradient [MW/h]").isEmpty() ? MAX_GRADIENT : staticRecord.get("Maximum negative power gradient [MW/h]")
                )).build();
            interTemporalRaoInput.getPowerGradients().add(powerGradient);
        });
    }

    private static void importNodeRedispatchingAction(InterTemporalRaoInputWithNetworkPaths interTemporalRaoInput, CSVRecord staticRecord, TemporalData<Network> initialNetworks, Map<String, CSVRecord> seriesPerType, String raId) {
        String networkElementId = processNetworks(staticRecord.get("UCT Node or GSK ID"), initialNetworks, seriesPerType, 1.);
        if (networkElementId == null) {
            return;
        }
        interTemporalRaoInput.getRaoInputs().getDataPerTimestamp().forEach((dateTime, raoInput) -> {
            Crac crac = raoInput.getCrac();
            double p0 = parseDoubleWithPossibleCommas(seriesPerType.get("P0").get(dateTime.getHour() + OFFSET));
            InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction()
                .withId(raId + "_RD")
                .withName(staticRecord.get("Generator Name"))
                .withNetworkElement(networkElementId)
                .withInitialSetpoint(p0)
                .withVariationCost(costUp, VariationDirection.UP)
                .withVariationCost(costDown, VariationDirection.DOWN)
                //.withActivationCost(ACTIVATION_COST)
                .newRange()
                .withMin(p0 - parseDoubleWithPossibleCommas(seriesPerType.get("RDP-").get(dateTime.getHour() + OFFSET)))
                .withMax(p0 + parseDoubleWithPossibleCommas(seriesPerType.get("RDP+").get(dateTime.getHour() + OFFSET)))
                .add();
            if (staticRecord.get("Preventive").equalsIgnoreCase("TRUE")) {
                injectionRangeActionAdder.newOnInstantUsageRule()
                    .withInstant(crac.getPreventiveInstant().getId())
                    .withUsageMethod(UsageMethod.AVAILABLE)
                    .add();
            }
        /*if (staticRecord.get("Curative").equalsIgnoreCase("TRUE")) {
            injectionRangeActionAdder.newOnInstantUsageRule()
                .withInstant(crac.getLastInstant().getId())
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add();
        }*/
            injectionRangeActionAdder.add();

        });

        PowerGradient powerGradient = PowerGradient.builder()
            .withNetworkElementId(networkElementId)
            .withMaxValue(parseDoubleWithPossibleCommas(
                staticRecord.get("Maximum positive power gradient [MW/h]").isEmpty() ? MAX_GRADIENT : staticRecord.get("Maximum positive power gradient [MW/h]")
            )).withMinValue(-parseDoubleWithPossibleCommas(
                staticRecord.get("Maximum negative power gradient [MW/h]").isEmpty() ? MAX_GRADIENT : staticRecord.get("Maximum negative power gradient [MW/h]")
            )).build();
        interTemporalRaoInput.getPowerGradients().add(powerGradient);
    }

    private static String processNetworks(String nodeId, TemporalData<Network> initialNetworks, Map<String, CSVRecord> seriesPerType, double shiftKey) {
        String generatorId = seriesPerType.get("P0").get("RA RD ID") + "_" + nodeId + "_GENERATOR";
        for (Map.Entry<OffsetDateTime, Network> entry : initialNetworks.getDataPerTimestamp().entrySet()) {
            Bus bus = findBus(nodeId, entry.getValue());
            if (bus == null) {
                BUSINESS_WARNS.warn("Redispatching action {} cannot be imported because bus {} could not be found", seriesPerType.get("P0").get("RA RD ID"), nodeId);
                return null;
            }
            Double p0 = parseDoubleWithPossibleCommas(seriesPerType.get("P0").get(entry.getKey().getHour() + OFFSET)) * shiftKey;
            processBus(bus, generatorId, p0);
        }
        return generatorId;
    }

    //TODO: make this more robust (and less UCTE dependent)
    private static Bus findBus(String nodeId, Network network) {
        //First try get the bus in bus breaker view
        Bus bus = network.getBusBreakerView().getBus(nodeId);
        if (bus != null) {
            return bus;
        }

        //Then if last char is *, remove it
        String modifiedNodeId = nodeId;
        if (nodeId.endsWith("*")) {
            modifiedNodeId = nodeId.substring(0, nodeId.length() - 1);
        }
        //Try find the bus using bus view
        return network.getBusBreakerView().getBus(modifiedNodeId + " ");
    }

    private static void processBus(Bus bus, String generatorId, Double p0) {
        bus.getVoltageLevel().newGenerator()
            .setBus(bus.getId())
            .setEnsureIdUnicity(true)
            .setId(generatorId)
            .setMaxP(999999)
            .setMinP(0)
            .setTargetP(p0)
            .setTargetQ(0)
            .setTargetV(bus.getVoltageLevel().getNominalV())
            .setVoltageRegulatorOn(false)
            .add()
            .setFictitious(true);

        bus.getVoltageLevel().newLoad()
            .setBus(bus.getId())
            .setEnsureIdUnicity(true)
            .setId(bus.getId() + "_LOAD")
            .setP0(p0)
            .setQ0(0)
            .setLoadType(LoadType.FICTITIOUS)
            .add();
    }

    private static boolean shouldBeImported(CSVRecord staticRecord, Map<String, Map<String, Double>> weightPerNodePerGsk) {
        return (staticRecord.get("RD description mode").equalsIgnoreCase("NODE") || weightPerNodePerGsk.containsKey(staticRecord.get("UCT Node or GSK ID"))) &&
            (staticRecord.get("Preventive").equalsIgnoreCase("TRUE") /*|| staticRecord.get("Curative").equalsIgnoreCase("TRUE")*/);
    }

    private static boolean p0RespectsGradients(CSVRecord staticRecord, CSVRecord p0record, List<OffsetDateTime> dateTimes) {
        double maxGradient = parseDoubleWithPossibleCommas(staticRecord.get("Maximum positive power gradient [MW/h]").isEmpty() ?
            MAX_GRADIENT : staticRecord.get("Maximum positive power gradient [MW/h]"));
        double minGradient = -parseDoubleWithPossibleCommas(staticRecord.get("Maximum negative power gradient [MW/h]").isEmpty() ?
            MAX_GRADIENT : staticRecord.get("Maximum negative power gradient [MW/h]"));

        Iterator<OffsetDateTime> dateTimeIterator = dateTimes.iterator();
        OffsetDateTime currentDateTime = dateTimeIterator.next();
        while (dateTimeIterator.hasNext()) {
            OffsetDateTime nextDateTime = dateTimeIterator.next();
            double diff = parseDoubleWithPossibleCommas(p0record.get(nextDateTime.getHour() + OFFSET)) - parseDoubleWithPossibleCommas(p0record.get(currentDateTime.getHour() + OFFSET));
            if (diff > maxGradient || diff < minGradient) {
                System.out.printf("%s does not respect power gradients : min/max/diff %f %f %f%n", staticRecord.get(0), minGradient, maxGradient, diff);
                return false;
            }
            currentDateTime = nextDateTime;
        }
        return true;
    }

    private static double parseDoubleWithPossibleCommas(String string) {
        return Double.parseDouble(string.replaceAll(",", "."));
    }
}
