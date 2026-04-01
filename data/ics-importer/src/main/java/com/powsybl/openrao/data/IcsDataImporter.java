/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.data.IcsUtil.*;
import static com.powsybl.openrao.data.IcsUtil.MAXIMUM_NEGATIVE_POWER_GRADIENT;
import static com.powsybl.openrao.data.IcsUtil.MAXIMUM_POSITIVE_POWER_GRADIENT;
import static com.powsybl.openrao.data.IcsUtil.MAX_GRADIENT;
import static com.powsybl.openrao.data.IcsUtil.NODE;
import static com.powsybl.openrao.data.IcsUtil.OFFSET;
import static com.powsybl.openrao.data.IcsUtil.P0;
import static com.powsybl.openrao.data.IcsUtil.PREVENTIVE;
import static com.powsybl.openrao.data.IcsUtil.RA_RD_ID;
import static com.powsybl.openrao.data.IcsUtil.RDP_DOWN;
import static com.powsybl.openrao.data.IcsUtil.RDP_UP;
import static com.powsybl.openrao.data.IcsUtil.RD_DESCRIPTION_MODE;
import static com.powsybl.openrao.data.IcsUtil.TRUE;
import static com.powsybl.openrao.data.IcsUtil.UCT_NODE_OR_GSK_ID;
import static com.powsybl.openrao.data.IcsUtil.parseDoubleWithPossibleCommas;

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

        // Parse and sort  and serie type (RDP-, RDP+, Pmin_RD or P0) and per RA_ID
        Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType = parseSeriesCsv(seriesInputStream);
        // Parse GSK and get weight Per Node Per Gsk
        Map<String, Map<String, Double>> weightPerNodePerGsk = new HashMap<>();
        if (gskInputStream != null) {
            weightPerNodePerGsk = parseGskCsv(gskInputStream);
        }
        // Parse static CSV: remedial action’s generator’s static constraints. one line per RA_ID
        Map<String, CSVRecord> staticConstraintPerId = parseStaticCsv(staticInputStream);

        Set<String> consistentRAs = filterOutInconsistentRedispatchingActions(staticConstraintPerId, timeseriesPerIdAndType, weightPerNodePerGsk, sortedTimestampToRun);

        return new IcsData(consistentRAs, timeseriesPerIdAndType, weightPerNodePerGsk, staticConstraintPerId);

    }

    static Set<String> filterOutInconsistentRedispatchingActions(Map<String, CSVRecord> staticConstraintPerId,
                                                                 Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType,
                                                                 Map<String, Map<String, Double>> weightPerNodePerGsk,
                                                                 List<OffsetDateTime> sortedTimestampToRun) {
        // Get a set of consistent redispatching action ID.
        Set<String> consistentRAs = new HashSet<>();
        staticConstraintPerId.forEach((raId, record) -> {
            if (shouldBeImported(record, sortedTimestampToRun, weightPerNodePerGsk, timeseriesPerIdAndType)) {
                consistentRAs.add(raId);
            }
        });
        // Remove inconsistent RAs from the data structures
        staticConstraintPerId.entrySet().removeIf(entry -> !consistentRAs.contains(entry.getKey()));
        timeseriesPerIdAndType.entrySet().removeIf(entry -> !consistentRAs.contains(entry.getKey()));
        return consistentRAs;
    }

    static Map<String, CSVRecord> parseStaticCsv(InputStream staticInputStream) throws IOException {
        Iterable<CSVRecord> staticCsvRecords = csvFormat.parse(new InputStreamReader(staticInputStream));
        Map<String, CSVRecord> filteredStaticCsvRecords = new HashMap<>();
        staticCsvRecords.forEach(record -> {
            filteredStaticCsvRecords.put(record.get(RA_RD_ID), record);
        });
        return filteredStaticCsvRecords;
    }

    static Map<String, Map<String, CSVRecord>> parseSeriesCsv(InputStream seriesInputStream) throws IOException {
        Iterable<CSVRecord> seriesCsvRecords = csvFormat.parse(new InputStreamReader(seriesInputStream));

        Map<String, Map<String, CSVRecord>> seriesPerIdAndType = new HashMap<>();
        seriesCsvRecords.forEach(record -> {
            seriesPerIdAndType.putIfAbsent(record.get(RA_RD_ID), new HashMap<>());
            seriesPerIdAndType.get(record.get(RA_RD_ID)).put(record.get("Type of timeseries"), record);
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

    // Consistency check functions
    private static boolean shouldBeImported(CSVRecord staticRecord, List<OffsetDateTime> sortedTimestampToRun, Map<String, Map<String, Double>> weightPerNodePerGsk, Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType) {
        String raId = staticRecord.get(RA_RD_ID);

        // TODO: checks that all the mandatory fields are defined for all timestamps

        // Check that remedial action is defined in series csv and gsk (if defined on a gsk)
        if (!timeseriesPerIdAndType.containsKey(raId)) {
            BUSINESS_WARNS.warn("Redispatching action {} is not defined in the time series csv", raId);
            return false;
        }
        // If remedial action is defined on a gsk
        if (staticRecord.get(RD_DESCRIPTION_MODE).equalsIgnoreCase(GSK)) {
            // Check that the gsk is defined in the gsk csv
            if (!weightPerNodePerGsk.containsKey(staticRecord.get(UCT_NODE_OR_GSK_ID))) {
                BUSINESS_WARNS.warn("Redispatching action {} is defined on a gsk {} but the gsk is not defined in the gsk csv", raId, staticRecord.get(UCT_NODE_OR_GSK_ID));
                return false;
            }

            // Check that the sum of weight if RA is defined on GSK equals to 1
            if (staticRecord.get(RD_DESCRIPTION_MODE).equalsIgnoreCase(GSK)) {
                if (!sumOfGskEqualsOne(staticRecord.get(UCT_NODE_OR_GSK_ID), weightPerNodePerGsk)) {
                    BUSINESS_WARNS.warn("Redispatching action {} is ignored but it is defined on a GSK but sum of weights is not equal to 1", raId);
                    return false;
                }
            }
        }

        // Check that remedial action should at least be defined on preventive instant
        if (!staticRecord.get(PREVENTIVE).equalsIgnoreCase(TRUE)) {
            BUSINESS_WARNS.warn("Redispatching action {} is not defined on preventive instant", raId);
            return false;
        }

        // Check that the remedial action is defined on a node or a gsk
        if (!staticRecord.get(RD_DESCRIPTION_MODE).equalsIgnoreCase(NODE) && !staticRecord.get(RD_DESCRIPTION_MODE).equalsIgnoreCase(GSK)) {
            BUSINESS_WARNS.warn("Redispatching action {} is not defined on a node or a gsk but on a {}", raId, staticRecord.get(RD_DESCRIPTION_MODE));
            return false;
        }

        // Check that the RA is correctly defined in series csv
        Map<String, CSVRecord> seriesPerType = timeseriesPerIdAndType.get(raId);
        boolean isDefinedInSeriesCsv = seriesPerType.containsKey(P0) &&
            seriesPerType.containsKey(RDP_DOWN) &&
            seriesPerType.containsKey(RDP_UP);

        if (!isDefinedInSeriesCsv) {
            BUSINESS_WARNS.warn("Redispatching action {} is not defined in the time series csv. Missing one or several timeseries type (P0, RDP_DOWN, RDP_UP or P_MIN_RD).", raId);
            return false;
        }

        // Check that the range of redispatching parameters is valid
        if (!rangeIsOkay(seriesPerType, sortedTimestampToRun)) {
            return false;
        }

        // Check that the P0 record respects the specified power gradients
        if (!p0RespectsGradients(staticRecord, seriesPerType.get(P0), sortedTimestampToRun)) {
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
                    "Redispatching action {} will not be imported because it does not respect power gradients : min/max/diff = {} / {} / {}",
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
                BUSINESS_WARNS.warn("Redispatching action {} will not be imported because of RDP+ {} or RDP- {} is negative for datetime {}", seriesPerType.get(P0).get(RA_RD_ID), rdpPlus, rdpMinus, dateTime);
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
