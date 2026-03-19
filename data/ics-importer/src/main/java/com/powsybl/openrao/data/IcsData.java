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
import com.powsybl.openrao.raoapi.TimeCoupledRaoInputWithNetworkPaths;
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
        return staticConstraintPerId.get(raId).get(UCT_NODE_OR_GSK_ID);
    }

    /**
     * Generates a set of generator constraints based on the provided remedial action ID.
     *
     * @param raId The identifier of the remedial action for which the generator constraints are being created.
     * @param weightPerNode A map linking node identifiers to their respective generation shift key weights.
     * @param networkElementIdPerNodeId A map linking nodeId to their respective network elements id.
     * @return A set of {@code GeneratorConstraints} generated for the specified parameters.
     * @throws OpenRaoException if data related to shutdown or startup allowances cannot be parsed.
     */
    public Set<GeneratorConstraints> createGeneratorConstraints(String raId, Map<String, Double> weightPerNode, Map<String, String> networkElementIdPerNodeId) {
        Set<GeneratorConstraints> generatorConstraintsSet = new HashSet<>();
        for (Map.Entry<String, Double> entry : weightPerNode.entrySet()) {
            String nodeId = entry.getKey();
            Double shiftKey = entry.getValue();
            CSVRecord staticRecord = staticConstraintPerId.get(raId);
            GeneratorConstraints.GeneratorConstraintsBuilder builder = GeneratorConstraints.create().withGeneratorId(networkElementIdPerNodeId.get(nodeId));
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
                throw new OpenRaoException("Could not parse shutDownAllowed value for raId " + raId + ": " + staticRecord.get(SHUTDOWN_ALLOWED));
            } else {
                builder.withShutDownAllowed(Boolean.parseBoolean(staticRecord.get(SHUTDOWN_ALLOWED)));
            }
            if (staticRecord.get(STARTUP_ALLOWED).isEmpty() ||
                !staticRecord.get(STARTUP_ALLOWED).equalsIgnoreCase(TRUE) && !staticRecord.get(STARTUP_ALLOWED).equalsIgnoreCase(FALSE)) {
                throw new OpenRaoException("Could not parse startUpAllowed value for raId " + raId + ": " + staticRecord.get(STARTUP_ALLOWED) );
            } else {
                builder.withStartUpAllowed(Boolean.parseBoolean(staticRecord.get(STARTUP_ALLOWED)));
            }
            GeneratorConstraints generatorConstraints = builder.build();
            generatorConstraintsSet.add(generatorConstraints);
        }
        return generatorConstraintsSet;
    }

    /**
     * Creates generator/load elements and updates provided networks given the remedial action's ID and its shift key mapping
     *
     * @param initialNetworksToModify Temporal data representing the networks that will be modified.
     *                                Contains network configurations per timestamp.
     * @param raId The identifier of the remedial action for which generators and network modifications are being applied.
     * @param weightPerNode A map linking node identifiers to their corresponding generation shift key weights.
     * @return A map associating each node identifier to its corresponding generator identifier.
     *         Returns an empty map if the process is aborted due to missing network components.
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

    /**
     * Creates injection range actions and updates CRACs for all timestamps.
     *
     * @param cracToModify Temporal data containing CRACs to be modified and timestamps to consider.
     * @param raId The identifier of the remedial action for which injection range actions are created.
     * @param weightPerNode A map linking node identifiers to their associated generation shift key weights.
     * @param networkElementPerNode A map linking each node identifier to its corresponding network element/generator id.
     * @param costUp The cost associated with increasing the generation (VariationDirection.UP).
     * @param costDown The cost associated with decreasing the generation (VariationDirection.DOWN).
     */
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


    // READER //

    /**
     * Reads and processes inputs to generate Ics Data Object
     *
     * @param staticInputStream the input stream for static constraints data, defining generator constraints
     *                          associated with remedial actions.
     * @param seriesInputStream the input stream for time series data, mapped per RA_ID and series type
     *                          (e.g., RDP-, RDP+, Pmin_RD, or P0).
     * @param gskInputStream the input stream for GSK data, mapping nodes to their generation shift key weights.
     * @param sortedTimestampToRun the list of timestamps to consider
     * @return an {@code IcsData} instance
     * @throws IOException if an issue occurs while reading or processing the input streams.
     */
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

    private static Map<String, Map<String, Double>> parseGskCsv(CSVFormat csvFormat, InputStream gskInputStream) throws IOException {
        Iterable<CSVRecord> gskCsvRecords = csvFormat.parse(new InputStreamReader(gskInputStream));
        Map<String, Map<String, Double>> weightPerNodePerGsk = new HashMap<>();
        gskCsvRecords.forEach(record -> {
            weightPerNodePerGsk.putIfAbsent(record.get(GSK_ID), new HashMap<>());
            weightPerNodePerGsk.get(record.get(GSK_ID)).put(record.get("Node"), parseDoubleWithPossibleCommas(record.get("Weight")));
        });

        return weightPerNodePerGsk;
    }

    // Consistency check functions
    private static boolean shouldBeImported(CSVRecord staticRecord,  List<OffsetDateTime> sortedTimestampToRun, Map<String, Map<String, Double>> weightPerNodePerGsk, Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType) {
        //TODO: check that sum of GSK if defined on one equal to 1

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

    /**
     * Determines whether the P0 record values respect the specified power gradients for each time interval.
     * It checks the difference in values between consecutive timestamps and ensures that the differences
     * fall within the acceptable gradient range defined by the static record.
     *
     * @param staticRecord The static record containing gradient constraints, including the maximum positive
     *                     and minimum negative power gradients.
     * @param p0record The P0 record containing time-series data representing power values at specific timestamps.
     * @param dateTimes A list of timestamps to evaluate the gradient between consecutive entries in the P0 record.
     * @return {@code true} if the P0 record respects the specified power gradients for all timestamps;
     *         {@code false} otherwise.
     */
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

    /**
     * Verifies whether the range of redispatching parameters is valid for the input time series,
     * ensuring that redispatching values are non-negative and exceed a minimum threshold.
     *
     * @param seriesPerType A map where keys are series types (e.g., RDP+ or RDP-) and values are time-series data (CSVRecord)
     *                      corresponding to these types.
     * @param dateTimes A list of timestamps to evaluate the redispatching parameters at specific hours within a day.
     * @return {@code true} if the range of redispatching values is valid and meets the defined constraints;
     *         {@code false} otherwise.
     */
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
}