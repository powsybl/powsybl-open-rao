/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.icsimporter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.data.icsimporter.IcsUtil.*;
import static com.powsybl.openrao.data.icsimporter.IcsUtil.MAXIMUM_NEGATIVE_POWER_GRADIENT;
import static com.powsybl.openrao.data.icsimporter.IcsUtil.MAXIMUM_POSITIVE_POWER_GRADIENT;
import static com.powsybl.openrao.data.icsimporter.IcsUtil.MAX_GRADIENT;
import static com.powsybl.openrao.data.icsimporter.IcsUtil.NODE;
import static com.powsybl.openrao.data.icsimporter.IcsUtil.OFFSET;
import static com.powsybl.openrao.data.icsimporter.IcsUtil.P0;
import static com.powsybl.openrao.data.icsimporter.IcsUtil.PREVENTIVE;
import static com.powsybl.openrao.data.icsimporter.IcsUtil.RA_RD_ID;
import static com.powsybl.openrao.data.icsimporter.IcsUtil.RDP_DOWN;
import static com.powsybl.openrao.data.icsimporter.IcsUtil.RDP_UP;
import static com.powsybl.openrao.data.icsimporter.IcsUtil.RD_DESCRIPTION_MODE;
import static com.powsybl.openrao.data.icsimporter.IcsUtil.TRUE;
import static com.powsybl.openrao.data.icsimporter.IcsUtil.UCT_NODE_OR_GSK_ID;
import static com.powsybl.openrao.data.icsimporter.IcsUtil.parseDoubleWithPossibleCommas;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class IcsDataImporter {

    private IcsDataImporter() {

    }

    private static CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
        .setDelimiter(";")
        .setHeader()
        .setSkipHeaderRecord(true)
        .get();

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

        // Parse and sort  per serie type (RDP-, RDP+, Pmin_RD or P0) and per RA_ID
        Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType = parseSeriesCsv(seriesInputStream);
        // Parse GSK and get weight Per Node Per Gsk
        Map<String, Map<String, Double>> weightPerNodePerGsk = new HashMap<>();
        if (gskInputStream != null) {
            weightPerNodePerGsk = parseGskCsv(gskInputStream);
        }
        // Parse static CSV: remedial action’s generator’s static constraints. one line per RA_ID
        Map<String, CSVRecord> staticConstraintPerId = parseStaticCsv(staticInputStream);

        Set<String> consistentRAs = filterRedispatchingActions(staticConstraintPerId, timeseriesPerIdAndType, weightPerNodePerGsk, sortedTimestampToRun);

        return new IcsData(consistentRAs, timeseriesPerIdAndType, weightPerNodePerGsk, staticConstraintPerId);

    }

    static Set<String> filterRedispatchingActions(Map<String, CSVRecord> staticConstraintPerId,
                                                  Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType,
                                                  Map<String, Map<String, Double>> weightPerNodePerGsk,
                                                  List<OffsetDateTime> sortedTimestampToRun) {
        // Get a set of consistent redispatching action ID.
        Set<String> raToImport = new HashSet<>();
        staticConstraintPerId.forEach((raId, record) -> {
            if (shouldBeImported(record, sortedTimestampToRun, weightPerNodePerGsk, timeseriesPerIdAndType)) {
                raToImport.add(raId);
            }
        });
        // Remove inconsistent RAs from the data structures
        staticConstraintPerId.entrySet().removeIf(entry -> !raToImport.contains(entry.getKey()));
        timeseriesPerIdAndType.entrySet().removeIf(entry -> !raToImport.contains(entry.getKey()));
        return raToImport;
    }

    static Map<String, CSVRecord> parseStaticCsv(InputStream staticInputStream) throws IOException {
        Iterable<CSVRecord> staticCsvRecords = csvFormat.parse(new InputStreamReader(staticInputStream));
        Map<String, CSVRecord> filteredStaticCsvRecords = new HashMap<>();
        staticCsvRecords.forEach(record -> filteredStaticCsvRecords.put(record.get(RA_RD_ID), record));
        return filteredStaticCsvRecords;
    }

    static Map<String, Map<String, CSVRecord>> parseSeriesCsv(InputStream seriesInputStream) throws IOException {
        Iterable<CSVRecord> seriesCsvRecords = csvFormat.parse(new InputStreamReader(seriesInputStream));

        Map<String, Map<String, CSVRecord>> seriesPerIdAndType = new HashMap<>();
        seriesCsvRecords.forEach(csvRecord -> {
            seriesPerIdAndType.putIfAbsent(csvRecord.get(RA_RD_ID), new HashMap<>());
            seriesPerIdAndType.get(csvRecord.get(RA_RD_ID)).put(csvRecord.get("Type of timeseries"), csvRecord);
        });

        return seriesPerIdAndType;
    }

    static Map<String, Map<String, Double>> parseGskCsv(InputStream gskInputStream) throws IOException {
        Iterable<CSVRecord> gskCsvRecords = csvFormat.parse(new InputStreamReader(gskInputStream));
        Map<String, Map<String, Double>> weightPerNodePerGsk = new HashMap<>();
        gskCsvRecords.forEach(record -> {
            weightPerNodePerGsk.putIfAbsent(record.get(GSK_ID), new HashMap<>());
            weightPerNodePerGsk.get(record.get(GSK_ID)).put(record.get("Node"), parseDoubleWithPossibleCommas(record.get("Weight")));
        });

        return weightPerNodePerGsk;
    }

    private static boolean isValidBooleanValue(String value) {
        return value.equalsIgnoreCase(TRUE) || value.equalsIgnoreCase(FALSE);
    }

    // Consistency check functions
    private static boolean shouldBeImported(CSVRecord staticRecord, List<OffsetDateTime> sortedTimestampToRun, Map<String, Map<String, Double>> weightPerNodePerGsk, Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType) {
        String raId = staticRecord.get(RA_RD_ID);

        // Check static record mandatory fields : Preventive, curative, Generator Name, RD Description mode, UCT Node or GSK ID, Startup allowed and Shutdown allowed
        if (staticRecord.get(PREVENTIVE).isEmpty() || staticRecord.get(CURATIVE).isEmpty() ||
            staticRecord.get(GENERATOR_NAME).isEmpty() || staticRecord.get(RD_DESCRIPTION_MODE).isEmpty() ||
            staticRecord.get(UCT_NODE_OR_GSK_ID).isEmpty() || staticRecord.get(STARTUP_ALLOWED).isEmpty() ||
            staticRecord.get(SHUTDOWN_ALLOWED).isEmpty()) {
            BUSINESS_WARNS.warn("Redispatching action {} is not imported: missing mandatory static data", raId);
            return false;
        }

        // Check that boolean fields are either "TRUE" or "FALSE"
        List<String> booleanFields = List.of(PREVENTIVE, CURATIVE, STARTUP_ALLOWED, SHUTDOWN_ALLOWED);
        for (String field : booleanFields) {
            String value = staticRecord.get(field);
            if (!isValidBooleanValue(value)) {
                BUSINESS_WARNS.warn("Redispatching action {} is not imported: invalid '{}' value '{}' (expected TRUE or FALSE)", raId, field, value);
                return false;
            }
        }

        // Check that remedial action is defined in series csv and gsk (if defined on a gsk)
        if (!timeseriesPerIdAndType.containsKey(raId)) {
            BUSINESS_WARNS.warn("Redispatching action {} is not imported: not defined in the time series csv", raId);
            return false;
        }

        // Check that mandatory timeseries type (P0, RDP_DOWN, RDP_UP) are defined in the time series csv
        Map<String, CSVRecord> seriesPerType = timeseriesPerIdAndType.get(raId);
        boolean isDefinedInSeriesCsv = seriesPerType.containsKey(P0) &&
            seriesPerType.containsKey(RDP_DOWN) &&
            seriesPerType.containsKey(RDP_UP);

        if (!isDefinedInSeriesCsv) {
            BUSINESS_WARNS.warn("Redispatching action {} is not imported: missing one or several mandatory timeseries type (P0, RDP_DOWN, RDP_UP).", raId);
            return false;
        }

        // Check that data exists for all timestamps to run
        List<String> mandatorySeriesTypes = List.of(P0, RDP_DOWN, RDP_UP);
        for (OffsetDateTime timestamp : sortedTimestampToRun) {
            int columnName = timestamp.getHour() + OFFSET;
            for (String seriesType : mandatorySeriesTypes) {
                String value = seriesPerType.get(seriesType).get(columnName);
                if (value == null || value.isEmpty()) {
                    BUSINESS_WARNS.warn("Redispatching action {} is not imported: missing {} data for timestamp {}", raId, seriesType, timestamp);
                    return false;
                }
            }
        }

        // If remedial action is defined on a gsk
        if (staticRecord.get(RD_DESCRIPTION_MODE).equalsIgnoreCase(GSK)) {
            // Check that the gsk is defined in the gsk csv
            if (!weightPerNodePerGsk.containsKey(staticRecord.get(UCT_NODE_OR_GSK_ID))) {
                BUSINESS_WARNS.warn("Redispatching action {} is not imported: defined on a gsk {} but the gsk is not defined in the gsk csv", raId, staticRecord.get(UCT_NODE_OR_GSK_ID));
                return false;
            }

            // Check that the sum of weight if RA is defined on GSK equals to 1
            if (!sumOfGskEqualsOne(staticRecord.get(UCT_NODE_OR_GSK_ID), weightPerNodePerGsk)) {
                BUSINESS_WARNS.warn("Redispatching action {} is not imported: defined on a GSK but sum of weights is not equal to 1", raId);
                return false;
            }
        }

        // Check that remedial action should at least be defined on preventive instant
        if (!staticRecord.get(PREVENTIVE).equalsIgnoreCase(TRUE)) {
            BUSINESS_WARNS.warn("Redispatching action {} is not imported: not defined on preventive instant", raId);
            return false;
        }

        // Check that the remedial action is defined on a node or a gsk
        if (!staticRecord.get(RD_DESCRIPTION_MODE).equalsIgnoreCase(NODE) && !staticRecord.get(RD_DESCRIPTION_MODE).equalsIgnoreCase(GSK)) {
            BUSINESS_WARNS.warn("Redispatching action {} is not imported: not defined on a node or a gsk but on a {}", raId, staticRecord.get(RD_DESCRIPTION_MODE));
            return false;
        }

        // Check that the range of redispatching parameters is valid
        if (!rangeIsOkay(seriesPerType, sortedTimestampToRun)) {
            return false;
        }

        // Check that P0 respects generator constraints
        if (!p0RespectsConstraints(staticRecord, seriesPerType, sortedTimestampToRun)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the sum of the generation shift key (GSK) weights associated with a specific GSK ID equals 1
     *
     * @param gskId
     * @param weightPerNodePerGsk
     * @return {@code true} if the sum of the GSK weights equals 1 within a small tolerance;
     *         {@code false} otherwise.
     */
    private static boolean sumOfGskEqualsOne(String gskId, Map<String, Map<String, Double>> weightPerNodePerGsk) {
        double sumOfGsk = 0.;
        for (Map.Entry<String, Double> entry : weightPerNodePerGsk.get(gskId).entrySet()) {
            sumOfGsk += entry.getValue();
        }
        return Math.abs(sumOfGsk - 1.) < 1e-6;
    }

    /**
     * Determines whether the P0 record values respect the specified constraints for each time interval.
     * Constraints are not respected when:
     * - Gradients are not respected, i.e the difference in values between consecutive timestamps
     *   fall within the acceptable gradient range defined by the static record.
     * - P0 is strictly above ON_THRESHOLD and strictly below Pmin
     * - P0 falls from above Pmin to below Pmin even though shut down is not allowed
     * - P0 rises from below Pmin to above Pmin even though start up is not allowed
     * - Lead time is defined but not respected, i.e before start up, P0 is below Pmin for less than Lead time.
     * - Lag time is defined but not respected, i.e after shut down, P0 is below Pmin for less than Lag time + Lead time.
     *
     * @param staticRecord The static record containing gradient constraints, including the maximum positive
     *                     and minimum negative power gradients.
     * @param seriesRecord The series record containing time series data, mapped per RA_ID and series type
     *                     (e.g., RDP-, RDP+, Pmin_RD, or P0).
     * @param dateTimes A list of timestamps to evaluate the gradient between consecutive entries in the P0 record.
     * @return {@code true} if the P0 record respects the specified constraints for all timestamps;
     *         {@code false} otherwise.
     */
    private static boolean p0RespectsConstraints(CSVRecord staticRecord, Map<String, CSVRecord> seriesRecord, List<OffsetDateTime> dateTimes) {
        // Generatcr constraints varaibles
        double timestampDuration = IcsUtil.computeTimestampDuration(dateTimes);
        Boolean shutDownAllowed = Boolean.parseBoolean(staticRecord.get(SHUTDOWN_ALLOWED));
        Boolean startUpAllowed = Boolean.parseBoolean(staticRecord.get(STARTUP_ALLOWED));
        Optional<Integer> lead = Optional.empty();
        Optional<Integer> lagAndLead = Optional.empty();
        Optional<Double> parsedLead = Optional.empty();
        if (!staticRecord.get(LEAD_TIME).isEmpty()) {
            parsedLead = Optional.of(parseDoubleWithPossibleCommas(staticRecord.get(LEAD_TIME)));
            lead = Optional.of((int) Math.ceil(parsedLead.get() / timestampDuration));
        }
        if (!staticRecord.get(LAG_TIME).isEmpty()) {
            double parsedLag = parseDoubleWithPossibleCommas(staticRecord.get(LAG_TIME));
            double parsedLagAndLead = parsedLead.map(aDouble -> aDouble + parsedLag).orElse(parsedLag);
            lagAndLead = Optional.of((int) Math.ceil(parsedLagAndLead / timestampDuration));
        }
        double maxGradient = staticRecord.get(MAXIMUM_POSITIVE_POWER_GRADIENT).isEmpty() ?
                MAX_GRADIENT : parseDoubleWithPossibleCommas(staticRecord.get(MAXIMUM_POSITIVE_POWER_GRADIENT));
        double minGradient = staticRecord.get(MAXIMUM_NEGATIVE_POWER_GRADIENT).isEmpty() ?
                -MAX_GRADIENT : -parseDoubleWithPossibleCommas(staticRecord.get(MAXIMUM_NEGATIVE_POWER_GRADIENT));

        // Intermediate variables
        boolean countLag = false;
        int countConsecutiveNullValues = 0;

        Iterator<OffsetDateTime> dateTimeIterator = dateTimes.iterator();
        OffsetDateTime currentDateTime = dateTimeIterator.next();
        while (dateTimeIterator.hasNext()) {
            OffsetDateTime nextDateTime = dateTimeIterator.next();
            double nextP0 = parseDoubleWithPossibleCommas(seriesRecord.get(P0).get(nextDateTime.getHour() + OFFSET));
            double currentP0 = parseDoubleWithPossibleCommas(seriesRecord.get(P0).get(currentDateTime.getHour() + OFFSET));
            Optional<Double> pMinRD = parseValue(seriesRecord, P_MIN_RD, currentDateTime, 1);
            double pMin = pMinRD.orElse(ON_POWER_THRESHOLD);

            // 1- Check gradients
            if (!areGradientsRespected(staticRecord, nextP0, currentP0, maxGradient, minGradient, currentDateTime)) {
                return false;
            }

            // 2 - Check Pmin is respected
            if (!isPminRespected(staticRecord, currentP0, pMin, currentDateTime)) {
                return false;
            }

            // 3 - Starting up next timestamp
            // a) Check start up is allowed
            // b) Check lead time is respected
            // c) Check lag time + lead time is respected
            if (currentP0 < pMin) {
                countConsecutiveNullValues += 1;
            }
            if (currentP0 < pMin && nextP0 >= pMin) {
                if (!isStartUpRespected(staticRecord, startUpAllowed, currentDateTime)) {
                    return false;
                }
                if (!isLeadTimeRespected(staticRecord, lead, countConsecutiveNullValues, currentDateTime)) {
                    return false;
                }
                if (countLag) {
                    if (!isLeadTimeAndLagTimeRespected(staticRecord, countConsecutiveNullValues, lagAndLead, currentDateTime)) {
                        return false;
                    }
                    // Re-initialize
                    countLag = false;
                }
            }
            // 4 - Shutting down next timestamp
            // a) Check shut down is allowed
            // activate lag time + lead time checking
            if (currentP0 >= pMin && nextP0 < pMin) {
                if (!isShutDownRespected(staticRecord, shutDownAllowed, currentDateTime)) {
                    return false;
                }
                if (lagAndLead.isPresent()) {
                    countLag = true;
                }
            }

            // Re-init countConsecutiveNullValues
            if (currentP0 >= pMin) {
                countConsecutiveNullValues = 0;
            }
            currentDateTime = nextDateTime;
        }

        // Last timestamp
        double currentP0 = parseDoubleWithPossibleCommas(seriesRecord.get(P0).get(currentDateTime.getHour() + OFFSET));
        Optional<Double> pMinRD = parseValue(seriesRecord, P_MIN_RD, currentDateTime, 1);
        double pMin = pMinRD.orElse(ON_POWER_THRESHOLD);
        return isPminRespected(staticRecord, currentP0, pMin, currentDateTime);
    }

    private static boolean isShutDownRespected(CSVRecord staticRecord, Boolean shutDownAllowed, OffsetDateTime currentDateTime) {
        if (!shutDownAllowed) {
            BUSINESS_WARNS.warn("Redispatching action {} is not imported (hour {}): shut down prohibited",
                    staticRecord.get(0), currentDateTime.getHour());
            return false;
        }
        return true;
    }

    private static boolean isLeadTimeAndLagTimeRespected(CSVRecord staticRecord, int countConsecutiveNullValues, Optional<Integer> lagAndLead, OffsetDateTime currentDateTime) {
        if (countConsecutiveNullValues < lagAndLead.get()) {
            BUSINESS_WARNS.warn("Redispatching action {} is not imported (hour {}): lagTime + leadTime ({}) not respected. RA was OFF after shut down for only {} timestamps",
                    staticRecord.get(0), currentDateTime.getHour(), lagAndLead.get(), countConsecutiveNullValues);
            return false;
        }
        return true;
    }

    private static boolean isLeadTimeRespected(CSVRecord staticRecord, Optional<Integer> lead, int countConsecutiveNullValues, OffsetDateTime currentDateTime) {
        if (lead.isPresent() && countConsecutiveNullValues < lead.get()) {
            BUSINESS_WARNS.warn("Redispatching action {} is not imported (hour {}): leadTime ({}) not respected. RA was OFF before start up for only {} timestamps",
                    staticRecord.get(0), currentDateTime.getHour(), lead.get(), countConsecutiveNullValues);
            return false;
        }
        return true;
    }

    private static boolean isStartUpRespected(CSVRecord staticRecord, Boolean startUpAllowed, OffsetDateTime currentDateTime) {
        if (!startUpAllowed) {
            BUSINESS_WARNS.warn("Redispatching action {} is not imported (hour {}): start up prohibited",
                    staticRecord.get(0), currentDateTime.getHour());
            return false;
        }
        return true;
    }

    private static boolean areGradientsRespected(CSVRecord staticRecord, double nextP0, double currentP0, double maxGradient, double minGradient, OffsetDateTime currentDateTime) {
        double diff = nextP0 - currentP0;
        if (diff > maxGradient || diff < minGradient) {
            BUSINESS_WARNS.warn(
                    "Redispatching action {} is not imported (hour {}): does not respect power gradients : min/max/diff = {} / {} / {}",
                    staticRecord.get(0), currentDateTime.getHour(), minGradient, maxGradient, diff
            );
            return false;
        }
        return true;
    }

    private static boolean isPminRespected(CSVRecord staticRecord, double currentP0, double pMin, OffsetDateTime currentDateTime) {
        if (currentP0 < pMin && currentP0 > ON_POWER_THRESHOLD) {
            BUSINESS_WARNS.warn("Redispatching action {} is not imported (hour {}): does not respect Pmin : P0 is {} and Pmin at {}",
                    staticRecord.get(0), currentDateTime.getHour(), currentP0, pMin);
            return false;
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
                BUSINESS_WARNS.warn("Redispatching action {} is not imported: RDP+ {} or RDP- {} is negative for datetime {}", seriesPerType.get(P0).get(RA_RD_ID), rdpPlus, rdpMinus, dateTime);
                return false;
            }
        }
        if (maxRange < 1) {
            BUSINESS_WARNS.warn("Redispatching action {} is not imported: max range in the day {} MW is too small", seriesPerType.get(P0).get(RA_RD_ID), maxRange);
            return false;
        }
        return true;
    }
}
