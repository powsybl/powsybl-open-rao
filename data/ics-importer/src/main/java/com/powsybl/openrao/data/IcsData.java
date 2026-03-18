/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.timecoupledconstraints.GeneratorConstraints;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.data.IcsUtil.*;


/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class IcsData {

    private static Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType;
    private static Map<String, Map<String, Double>> weightPerNodePerGsk;
    private static Map<String, CSVRecord> staticConstraintPerId;

    // TODO : either parametrize this or set it to true. May have to change the way it works to import for all curative instants instead of only the last one
    public static boolean importCurative = false;

    public IcsData(Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType,
                   Map<String, Map<String, Double>> weightPerNodePerGsk,
                   Map<String, CSVRecord> staticConstraintPerId) {
        this.staticConstraintPerId = staticConstraintPerId;
        this.timeseriesPerIdAndType = timeseriesPerIdAndType;
        this.weightPerNodePerGsk = weightPerNodePerGsk;
    }

    // Define getters
    public Map<String, CSVRecord> getStaticConstraintPerId() {
        return staticConstraintPerId;
    }

    public static Map<String, Map<String, CSVRecord>> getTimeseriesPerIdAndType() {
        return timeseriesPerIdAndType;
    }

    public Map<String, Map<String, Double>> getWeightPerNodePerGsk() {
        return weightPerNodePerGsk;
    }

    public static String getGeneratorIdFromRaIdAndNodeId(String raId, String nodeId) {
        return raId + "_" + nodeId + GENERATOR_SUFFIX;
    }

    public boolean isRaDefinedOnANode(String raId) {
        return staticConstraintPerId.get(raId).get(RD_DESCRIPTION_MODE).equalsIgnoreCase(NODE);
    }

    public String getNodeIdOrGskIdFromRaId(String raId) {
        return staticConstraintPerId.get(raId).get("UCT Node or GSK ID");
    }

    public Set<GeneratorConstraints> getAllGeneratorConstraints() {
        Set<GeneratorConstraints> generatorConstraintsSet = new HashSet<>();
        staticConstraintPerId.forEach((raId, staticRecord) -> {
            // If the remedial action is defined on a Node.
            if (staticRecord.get(RD_DESCRIPTION_MODE).equalsIgnoreCase(NODE)) {
                // create a generator constraint from staticRecord
                String networkElementId = getGeneratorIdFromRaIdAndNodeId(raId, staticRecord.get(UCT_NODE_OR_GSK_ID));
                GeneratorConstraints generatorConstraints = createGeneratorConstraintFromStaticRecord(networkElementId, staticRecord, staticRecord.get(UCT_NODE_OR_GSK_ID),1.0);
                generatorConstraintsSet.add(generatorConstraints);
            } else { // If the remedial action is defined on a GSK
                // For a given GSK, create a generator constraint for each node of the GSK according to the shiftKey
                Map<String, Double> weightPerNode = weightPerNodePerGsk.get(staticRecord.get(UCT_NODE_OR_GSK_ID));
                for (Map.Entry<String, Double> entry : weightPerNode.entrySet()) {
                    String nodeId = entry.getKey();
                    Double shiftKey = entry.getValue();
                    String networkElementId = getGeneratorIdFromRaIdAndNodeId(raId, nodeId);
                    GeneratorConstraints generatorConstraints = createGeneratorConstraintFromStaticRecord(networkElementId, staticRecord, nodeId, shiftKey);
                    generatorConstraintsSet.add(generatorConstraints);
                }
            }
        });

        return generatorConstraintsSet;
    }

    public void createGeneratorConstraints(TimeCoupledConstraints getTimeCoupledConstraints, Map<String, Double> weightPerNode, String raId, Map<String, String> networkElementPerGskElement) {
        for (Map.Entry<String, Double> entry : weightPerNode.entrySet()) {
            String nodeId = entry.getKey();
            Double shiftKey = entry.getValue();
            CSVRecord staticRecord = staticConstraintPerId.get(raId);
            GeneratorConstraints generatorConstraints = createGeneratorConstraintFromStaticRecord(networkElementPerGskElement.get(nodeId), staticRecord, nodeId, shiftKey);
            getTimeCoupledConstraints.addGeneratorConstraints(generatorConstraints);
        }
    }

    /**
     *  Find bus in network corresponding to the nodes of the GSK, create a generator and a load for each node of the GSK
     *  and return a map of the generatorId to the node id (if no bus is found, the generator is not created and not included in the returned map) .
     *
     * @param initialNetworksToModify
     * @param raId
     * @param weightPerNode
     * @return
     */
    public static Map<String, String> createGeneratorAndLoadAndUpdateNetworks(TemporalData<Network> initialNetworksToModify,
                                                                              String raId,
                                                                              Map<String, Double> weightPerNode) {

        Map<String, String> networkElementPerGskElement = new HashMap<>();
        Map<String, CSVRecord> seriesPerType = timeseriesPerIdAndType.get(raId);

        for (Map.Entry<String, Double> entry : weightPerNode.entrySet()) {

            String nodeId = entry.getKey();
            Double shiftKey = entry.getValue();
            String generatorId = getGeneratorIdFromRaIdAndNodeId(raId, nodeId);

            for (Map.Entry<OffsetDateTime, Network> networkEntry : initialNetworksToModify.getDataPerTimestamp().entrySet()) {
                OffsetDateTime dateTime = networkEntry.getKey();
                Network network = networkEntry.getValue();

                Bus bus = findBus(nodeId, network);
                if (bus == null) {
                    BUSINESS_WARNS.warn("Redispatching action {} cannot be imported because bus {} could not be found", raId, nodeId);
                    return Map.of();
                }

                int index = dateTime.getHour() + OFFSET;
                Double p0 = parseDoubleWithPossibleCommas(seriesPerType.get(P0).get(index)) * shiftKey;
                Optional<Double> pMinRd = IcsUtil.parseValue(seriesPerType, P_MIN_RD, dateTime, shiftKey);
                processBus(bus, generatorId, p0, pMinRd.orElse(ON_POWER_THRESHOLD));
            }

            networkElementPerGskElement.put(nodeId, generatorId);
        }

        return networkElementPerGskElement;
    }

    public static void createInjectionRangeActionsAndUpdateCracs(TemporalData<Crac> cracToModify,
                                                                 String raId,
                                                                 Map<String, Double> weightPerNode,
                                                                 Map<String, String> networkElementPerNode,
                                                                 double costUp,
                                                                 double costDown) {

        CSVRecord staticRecord = staticConstraintPerId.get(raId);
        Map<String, CSVRecord> seriesPerType = timeseriesPerIdAndType.get(raId);
        cracToModify.getDataPerTimestamp().forEach((dateTime, crac) -> {
            double p0 = parseDoubleWithPossibleCommas(seriesPerType.get(P0).get(dateTime.getHour() + OFFSET));
            InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction()
                .withId(raId + RD_SUFFIX)
                .withName(staticRecord.get(GENERATOR_NAME))
                .withInitialSetpoint(p0)
                .withVariationCost(costUp, VariationDirection.UP)
                .withVariationCost(costDown, VariationDirection.DOWN)
                .newRange()
                .withMin(p0 - parseDoubleWithPossibleCommas(seriesPerType.get(RDP_DOWN).get(dateTime.getHour() + OFFSET)))
                .withMax(p0 + parseDoubleWithPossibleCommas(seriesPerType.get(RDP_UP).get(dateTime.getHour() + OFFSET)))
                .add();

            weightPerNode.forEach((nodeId, shiftKey) -> {
                injectionRangeActionAdder.withNetworkElementAndKey(shiftKey, networkElementPerNode.get(nodeId));
            });

            if (staticRecord.get(PREVENTIVE).equalsIgnoreCase(TRUE)) {
                injectionRangeActionAdder.newOnInstantUsageRule()
                    .withInstant(crac.getPreventiveInstant().getId())
                    .add();
            }
            if (importCurative && staticRecord.get(CURATIVE).equalsIgnoreCase(TRUE)) {
                injectionRangeActionAdder.newOnInstantUsageRule()
                    .withInstant(crac.getLastInstant().getId())
                    .add();
            }

            injectionRangeActionAdder.add();
        });
    }


    public static GeneratorConstraints createGeneratorConstraintFromStaticRecord(String generatorId,
                                                                                 CSVRecord staticRecord,
                                                                                 String nodeId,
                                                                                 Double shiftKey) {

        GeneratorConstraints.GeneratorConstraintsBuilder builder = GeneratorConstraints.create().withGeneratorId(generatorId);
        if (!staticRecord.get(MAXIMUM_POSITIVE_POWER_GRADIENT).isEmpty()) {
            builder.withUpwardPowerGradient(shiftKey * parseDoubleWithPossibleCommas(staticRecord.get(MAXIMUM_POSITIVE_POWER_GRADIENT)));
        } else {
            builder.withUpwardPowerGradient(shiftKey * MAX_GRADIENT);
        }
        if (!staticRecord.get(MAXIMUM_NEGATIVE_POWER_GRADIENT).isEmpty()) {
            builder.withDownwardPowerGradient(-shiftKey * parseDoubleWithPossibleCommas(staticRecord.get(MAXIMUM_NEGATIVE_POWER_GRADIENT)));
        } else {
            builder.withDownwardPowerGradient(-shiftKey * MAX_GRADIENT);
        }
        if (!staticRecord.get(LEAD_TIME).isEmpty()) {
            builder.withLeadTime(parseDoubleWithPossibleCommas(staticRecord.get(LEAD_TIME)));
        }
        if (!staticRecord.get(LAG_TIME).isEmpty()) {
            builder.withLagTime(parseDoubleWithPossibleCommas(staticRecord.get(LAG_TIME)));
        }
        if (staticRecord.get(SHUTDOWN_ALLOWED).isEmpty() ||
            !staticRecord.get(SHUTDOWN_ALLOWED).equalsIgnoreCase(TRUE) && !staticRecord.get(SHUTDOWN_ALLOWED).equalsIgnoreCase(FALSE)) {
            throw new OpenRaoException("Could not parse shutDownAllowed value " + staticRecord.get(SHUTDOWN_ALLOWED) + " for nodeId " + nodeId);
        } else {
            builder.withShutDownAllowed(Boolean.parseBoolean(staticRecord.get(SHUTDOWN_ALLOWED)));
        }
        if (staticRecord.get(STARTUP_ALLOWED).isEmpty() ||
            !staticRecord.get(STARTUP_ALLOWED).equalsIgnoreCase(TRUE) && !staticRecord.get(STARTUP_ALLOWED).equalsIgnoreCase(FALSE)) {
            throw new OpenRaoException("Could not parse startUpAllowed value " + staticRecord.get(STARTUP_ALLOWED) + " for nodeId " + nodeId);
        } else {
            builder.withStartUpAllowed(Boolean.parseBoolean(staticRecord.get(STARTUP_ALLOWED)));
        }
        return builder.build();
    }

    public static IcsData read(InputStream staticInputStream,
                               InputStream seriesInputStream,
                               InputStream gskInputStream,
                               List<OffsetDateTime> sortedTimestampToRun) throws IOException {

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
            .setDelimiter(";")
            .setHeader()
            .setSkipHeaderRecord(true)
            .get();

        // Parse and sort per RA_ID and serie type (RDP-, RDP+, Pmin_RD or P0)
        Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType = parseSeriesCsv(csvFormat, seriesInputStream);
        // Parse GSK and get weight Per Node Per Gsk
        Map<String, Map<String, Double>> weightPerNodePerGsk = parseGskCsv(csvFormat, gskInputStream);
        // Parse static CSV: remedial action’s generator’s static constraints. one line per RA_ID
        Map<String, CSVRecord>  staticConstraintPerId = parseAndFilterStaticCsv(csvFormat, staticInputStream, sortedTimestampToRun, weightPerNodePerGsk, timeseriesPerIdAndType);

        return new IcsData(timeseriesPerIdAndType, weightPerNodePerGsk, staticConstraintPerId);

    }

    static Map<String, CSVRecord> parseAndFilterStaticCsv(CSVFormat csvFormat, InputStream staticInputStream, List<OffsetDateTime> sortedTimestampToRun, Map<String, Map<String, Double>> weightPerNodePerGsk, Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType) throws IOException {
        Iterable<CSVRecord> staticCsvRecords = csvFormat.parse(new InputStreamReader(staticInputStream));
        Map<String, CSVRecord> filteredStaticCsvRecords = new HashMap<>();
        staticCsvRecords.forEach(record -> {
            if (shouldBeImported(record, sortedTimestampToRun, weightPerNodePerGsk, timeseriesPerIdAndType)) {
                filteredStaticCsvRecords.put(record.get(RA_RD_ID), record);
            }
        });
        return filteredStaticCsvRecords;
    }

    private static Map<String, Map<String, CSVRecord>> parseSeriesCsv(CSVFormat csvFormat, InputStream seriesInputStream) throws IOException {
        Iterable<CSVRecord> seriesCsvRecords = csvFormat.parse(new InputStreamReader(seriesInputStream));

        Map<String, Map<String, CSVRecord>> seriesPerIdAndType = new HashMap<>();
        seriesCsvRecords.forEach(record -> {
            seriesPerIdAndType.putIfAbsent(record.get(RA_RD_ID), new HashMap<>());
            seriesPerIdAndType.get(record.get(RA_RD_ID)).put(record.get("Type of timeseries"), record);
        });

        return seriesPerIdAndType;
    }

    private static boolean shouldBeImported(CSVRecord staticRecord,  List<OffsetDateTime> sortedTimestampToRun, Map<String, Map<String, Double>> weightPerNodePerGsk, Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType) {
        //TODO: add more consistency checks ?
        //  - check that P0s respect the min/max gradients
        //  - import curative or no ?

        // remedial action should at least be defined on preventive instant
        boolean isPreventive = staticRecord.get(PREVENTIVE).equalsIgnoreCase(TRUE);

        // remedial action is defined on a node or a gsk
        boolean isDefinedOnANodeOrGsk = staticRecord.get(RD_DESCRIPTION_MODE).equalsIgnoreCase(NODE) || weightPerNodePerGsk.containsKey(staticRecord.get(UCT_NODE_OR_GSK_ID));

        String raId = staticRecord.get(RA_RD_ID);
        Map<String, CSVRecord> seriesPerType = timeseriesPerIdAndType.get(raId);

        // is correctly defined in series csv
        boolean isDefinedInSeriesCsv = seriesPerType != null &&
            seriesPerType.containsKey(P0) &&
            seriesPerType.containsKey(RDP_DOWN) &&
            seriesPerType.containsKey(RDP_UP) &&
            seriesPerType.containsKey(P_MIN_RD);

        boolean rangeIsOkay = rangeIsOkay(seriesPerType, sortedTimestampToRun);
        boolean p0RespectsGradients = p0RespectsGradients(staticRecord, seriesPerType.get(P0), sortedTimestampToRun);

        return isDefinedOnANodeOrGsk && isPreventive && isDefinedInSeriesCsv && rangeIsOkay && p0RespectsGradients;
    }

    private static boolean p0RespectsGradients(CSVRecord staticRecord, CSVRecord p0record, List<OffsetDateTime> dateTimes) {
        double maxGradient = staticRecord.get(MAXIMUM_POSITIVE_POWER_GRADIENT).isEmpty() ?
            MAX_GRADIENT : parseDoubleWithPossibleCommas(staticRecord.get(MAXIMUM_POSITIVE_POWER_GRADIENT));
        double minGradient = staticRecord.get(MAXIMUM_NEGATIVE_POWER_GRADIENT).isEmpty() ?
            -MAX_GRADIENT : -parseDoubleWithPossibleCommas(staticRecord.get(MAXIMUM_NEGATIVE_POWER_GRADIENT));

        Iterator<OffsetDateTime> dateTimeIterator = dateTimes.iterator();
        OffsetDateTime currentDateTime = dateTimeIterator.next();
        while (dateTimeIterator.hasNext()) {
            OffsetDateTime nextDateTime = dateTimeIterator.next();
            double diff = parseDoubleWithPossibleCommas(p0record.get(nextDateTime.getHour() + OFFSET)) - parseDoubleWithPossibleCommas(p0record.get(currentDateTime.getHour() + OFFSET));
            if (diff > maxGradient || diff < minGradient) {
                BUSINESS_WARNS.warn(
                    "Redispatching action {} will not be imported because it does not respect power gradients : min/max/diff {} {} {}",
                    staticRecord.get(0), minGradient, maxGradient, diff
                );
                return false;
            }
            currentDateTime = nextDateTime;
        }
        return true;
    }

    private static boolean rangeIsOkay(Map<String, CSVRecord> seriesPerType, List<OffsetDateTime> dateTimes) {
        double maxRange = 0.;
        for (OffsetDateTime dateTime : dateTimes) {
            double rdpPlus = parseDoubleWithPossibleCommas(seriesPerType.get(RDP_UP).get(dateTime.getHour() + OFFSET));
            double rdpMinus = parseDoubleWithPossibleCommas(seriesPerType.get(RDP_DOWN).get(dateTime.getHour() + OFFSET));
            maxRange = Math.max(maxRange, rdpPlus + rdpMinus);
            if (rdpPlus < -1e-6 || rdpMinus < -1e-6) {
                BUSINESS_WARNS.warn("Redispatching action {} will not be imported because of RDP+ {} or RDP- {} is negative", seriesPerType.get(P0).get(RA_RD_ID), rdpPlus, rdpMinus);
                return false;
            }
        }
        if (maxRange < 1) {
            BUSINESS_WARNS.warn("Redispatching action {} will not be imported because max range in the day {} MW is too small", seriesPerType.get(P0).get(RA_RD_ID), maxRange);
            return false;
        }
        return true;
    }

    private static Map<String, Map<String, Double>> parseGskCsv(CSVFormat csvFormat, InputStream gskInputStream) throws IOException {
        Iterable<CSVRecord> gskCsvRecords = csvFormat.parse(new InputStreamReader(gskInputStream));
        Map<String, Map<String, Double>> weightPerNodePerGsk = new HashMap<>();
        gskCsvRecords.forEach(record -> {
            weightPerNodePerGsk.putIfAbsent(record.get(GSK_ID), new HashMap<>());
            weightPerNodePerGsk.get(record.get(GSK_ID)).put(record.get("Node"), parseDoubleWithPossibleCommas(record.get("Weight")));
        });

        return weightPerNodePerGsk;
    }
}