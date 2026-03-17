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

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class IcsDataImporter {

    private static final int OFFSET = 2;
    private static final double MAX_GRADIENT = 1000.0;

    public static final String MAXIMUM_POSITIVE_POWER_GRADIENT = "Maximum positive power gradient [MW/h]";
    public static final String MAXIMUM_NEGATIVE_POWER_GRADIENT = "Maximum negative power gradient [MW/h]";
    public static final String RA_RD_ID = "RA RD ID";
    public static final String RDP_UP = "RDP+";
    public static final String RDP_DOWN = "RDP-";
    public static final String P_MIN_RD = "Pmin_RD";
    public static final String P0 = "P0";
    public static final String UCT_NODE_OR_GSK_ID = "UCT Node or GSK ID";
    public static final String GSK_ID = "GSK ID";
    public static final String PREVENTIVE = "Preventive";
    public static final String TRUE = "TRUE";
    public static final String RD_DESCRIPTION_MODE = "RD description mode";
    public static final String NODE = "NODE";

    public IcsDataImporter() {
        // only static use
    }

    public IcsData read(InputStream staticInputStream,
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

    Map<String, CSVRecord> parseAndFilterStaticCsv(CSVFormat csvFormat, InputStream staticInputStream, List<OffsetDateTime> sortedTimestampToRun, Map<String, Map<String, Double>> weightPerNodePerGsk, Map<String, Map<String, CSVRecord>> timeseriesPerIdAndType) throws IOException {
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

        // remedial action is defined on preventive instant
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

    private static double parseDoubleWithPossibleCommas(String string) {
        return Double.parseDouble(string.replaceAll(",", "."));
    }
}