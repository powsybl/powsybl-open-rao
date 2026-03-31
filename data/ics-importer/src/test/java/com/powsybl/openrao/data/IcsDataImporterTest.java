/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.powsybl.openrao.commons.logs.RaoBusinessWarns;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class IcsDataImporterTest {

    List<ILoggingEvent> logsList;

    public static List<OffsetDateTime> generateOffsetDateTimeList(int numberOfHours) {
        List<OffsetDateTime> dateTimes = new ArrayList<>();

        OffsetDateTime start = OffsetDateTime.of(2025, 2, 13, 0, 30, 0, 0, ZoneOffset.UTC);

        int current = 0;
        while (current < numberOfHours) {
            dateTimes.add(start.plusHours(current));
            current++;
        }

        return dateTimes;
    }

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(RaoBusinessWarns.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logsList = listAppender.list;
    }

    @Test
    void testParseSeriesCsv() throws IOException {
        Map<String, Map<String, CSVRecord>> result = IcsDataImporter.parseSeriesCsv(getClass().getResourceAsStream("/ics/series.csv"));

        assertEquals(1, result.size());
        assertTrue(result.containsKey("Redispatching_RA"));

        Map<String, CSVRecord> redispatchingRaSeries = result.get("Redispatching_RA");
        assertEquals(4, redispatchingRaSeries.size());
        assertTrue(redispatchingRaSeries.containsKey("RDP-"));
        assertTrue(redispatchingRaSeries.containsKey("RDP+"));
        assertTrue(redispatchingRaSeries.containsKey("P0"));
        assertTrue(redispatchingRaSeries.containsKey("Pmin_RD"));

        assertEquals("35", redispatchingRaSeries.get("RDP-").get("00:30"));
        assertEquals("43", redispatchingRaSeries.get("RDP+").get("00:30"));
        assertEquals("116", redispatchingRaSeries.get("P0").get("00:30"));
        assertEquals("10", redispatchingRaSeries.get("Pmin_RD").get("00:30"));

        assertEquals("39", redispatchingRaSeries.get("RDP-").get("23:30"));
        assertEquals("39", redispatchingRaSeries.get("RDP+").get("23:30"));
        assertEquals("120", redispatchingRaSeries.get("P0").get("23:30"));
        assertEquals("30", redispatchingRaSeries.get("Pmin_RD").get("23:30"));
    }

    @Test
    void testParseStaticCsv() throws IOException {
        Map<String, CSVRecord> result = IcsDataImporter.parseStaticCsv(getClass().getResourceAsStream("/ics/static.csv"));

        assertEquals(1, result.size());
        assertTrue(result.containsKey("Redispatching_RA"));

        CSVRecord record = result.get("Redispatching_RA");
        assertEquals("FR", record.get("TSO"));
        assertEquals("TRUE", record.get("Preventive"));
        assertEquals("FALSE", record.get("Curative"));
        assertEquals("Node", record.get("RD description mode"));
        assertEquals("BBE1AA1", record.get("UCT Node or GSK ID"));
        assertEquals("50", record.get("Minimum Redispatch [MW]"));
        assertEquals("FALSE", record.get("Startup allowed"));
        assertEquals("FALSE", record.get("Shutdown allowed"));
    }

    @Test
    void testParseGskCsv() throws IOException {
        Map<String, Map<String, Double>> result = IcsDataImporter.parseGskCsv(getClass().getResourceAsStream("/glsk/gsk.csv"));

        assertEquals(1, result.size());
        assertTrue(result.containsKey("GSK_NAME"));

        Map<String, Double> gskWeights = result.get("GSK_NAME");
        assertEquals(2, gskWeights.size());
        assertEquals(0.6, gskWeights.get("BBE1AA1"), 1e-6);
        assertEquals(0.4, gskWeights.get("FFR1AA1"), 1e-6);
    }

    @Test
    void testStandardIcsDataImporterRead() throws IOException {
        IcsData icsData = IcsDataImporter.read(
            getClass().getResourceAsStream("/ics/static.csv"),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList(24));
        assertEquals(1, icsData.getRedispatchingActions().size());
        assertTrue(icsData.getRedispatchingActions().contains("Redispatching_RA"));
    }

    @Test
    void testRaNotAvailableInPreventive() throws IOException {
        String staticCsv = """
            RA RD ID;TSO;Preventive;Curative;Time From;Time To;Generator Name;RD description mode;UCT Node or GSK ID;Minimum Redispatch [MW];Fuel type;Minimum up-time [h];Minimum down-time [h];Maximum positive power gradient [MW/h];Maximum negative power gradient [MW/h];Lead time [h];Lag time [h];Startup allowed;Shutdown allowed
            Redispatching_RA;FR;FALSE;FALSE;00:00;24:00:00;Generator_Name;Node;BBE1AA1;50;Coal;2;2;20;20;1;1;FALSE;FALSE
            """;
        IcsData icsData = IcsDataImporter.read(
            new ByteArrayInputStream(staticCsv.getBytes(StandardCharsets.UTF_8)),
            getClass().getResourceAsStream("/ics/series.csv"),
            null,
            generateOffsetDateTimeList(24));
        assertEquals(0, icsData.getRedispatchingActions().size());
        assertEquals("Redispatching action Redispatching_RA is not defined on preventive instant", logsList.get(0).getFormattedMessage());
    }

    @Test
    void testRaDefinedInStaticButNotInSeries() throws IOException {
        String staticCsv = """
            RA RD ID;TSO;Preventive;Curative;Time From;Time To;Generator Name;RD description mode;UCT Node or GSK ID;Minimum Redispatch [MW];Fuel type;Minimum up-time [h];Minimum down-time [h];Maximum positive power gradient [MW/h];Maximum negative power gradient [MW/h];Lead time [h];Lag time [h];Startup allowed;Shutdown allowed
            Redispatching_RA;FR;TRUE;FALSE;00:00;24:00:00;Generator_Name;Node;BBE1AA1;50;Coal;2;2;20;20;1;1;FALSE;FALSE
            Redispatching_RA_not_defined_in_series_csv;FR;FALSE;FALSE;00:00;24:00:00;Generator_Name;Node;BBE1AA1;50;Coal;2;2;20;20;1;1;FALSE;FALSE
            """;
        IcsData icsData = IcsDataImporter.read(
            new ByteArrayInputStream(staticCsv.getBytes(StandardCharsets.UTF_8)),
            getClass().getResourceAsStream("/ics/series.csv"),
            null,
            generateOffsetDateTimeList(24));
        assertEquals(1, icsData.getRedispatchingActions().size());
        assertTrue(icsData.getRedispatchingActions().contains("Redispatching_RA"));
        assertFalse(icsData.getRedispatchingActions().contains("Redispatching_RA_not_defined_in_series_csv"));
        assertEquals("Redispatching action Redispatching_RA_not_defined_in_series_csv is not defined in the time series csv", logsList.get(0).getFormattedMessage());
    }

    @Test
    void testRaDefinedOnGskButNotInGskCsv() throws IOException {
        String staticCsv = """
            RA RD ID;TSO;Preventive;Curative;Time From;Time To;Generator Name;RD description mode;UCT Node or GSK ID;Minimum Redispatch [MW];Fuel type;Minimum up-time [h];Minimum down-time [h];Maximum positive power gradient [MW/h];Maximum negative power gradient [MW/h];Lead time [h];Lag time [h];Startup allowed;Shutdown allowed
            Redispatching_RA;FR;TRUE;FALSE;00:00;24:00:00;Generator_Name;GSK;GSK_NAME_NOT_IN_CSV;50;Coal;2;2;20;20;1;1;FALSE;FALSE
            """;
        IcsData icsData = IcsDataImporter.read(
            new ByteArrayInputStream(staticCsv.getBytes(StandardCharsets.UTF_8)),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList(24));
        assertEquals(0, icsData.getRedispatchingActions().size());
        assertFalse(icsData.getRedispatchingActions().contains("Redispatching_RA"));
        assertEquals("Redispatching action Redispatching_RA is defined on a gsk GSK_NAME_NOT_IN_CSV but the gsk is not defined in the gsk csv", logsList.get(0).getFormattedMessage());
    }

    @Test
    void testRaNotDefinedOnANodeOrAGsk() throws IOException {
        String staticCsv = """
            RA RD ID;TSO;Preventive;Curative;Time From;Time To;Generator Name;RD description mode;UCT Node or GSK ID;Minimum Redispatch [MW];Fuel type;Minimum up-time [h];Minimum down-time [h];Maximum positive power gradient [MW/h];Maximum negative power gradient [MW/h];Lead time [h];Lag time [h];Startup allowed;Shutdown allowed
            Redispatching_RA;FR;TRUE;FALSE;00:00;24:00:00;Generator_Name;OTHER;GSK_NAME_NOT_IN_CSV;50;Coal;2;2;20;20;1;1;FALSE;FALSE
            """;
        IcsData icsData = IcsDataImporter.read(
            new ByteArrayInputStream(staticCsv.getBytes(StandardCharsets.UTF_8)),
            getClass().getResourceAsStream("/ics/series.csv"),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList(24));
        assertEquals(0, icsData.getRedispatchingActions().size());
        assertEquals("Redispatching action Redispatching_RA is not defined on a node or a gsk but on a OTHER", logsList.get(0).getFormattedMessage());
    }

    private static Stream<Arguments> seriesCsvWithMissingSeriesTypeCases() {
        String header = """
            RA RD ID;Type of timeseries;00:30;01:30
            """;

        String missingP0Csv = header + """
            Redispatching_RA;RDP-;35;35
            Redispatching_RA;RDP+;43;43
            Redispatching_RA;Pmin_RD;10;15
            """;

        String missingRdpDownCsv = header + """
            Redispatching_RA;RDP+;43;43
            Redispatching_RA;P0;116;120
            Redispatching_RA;Pmin_RD;10;15
            """;

        String missingRdpUpCsv = header + """
            Redispatching_RA;RDP-;35;35
            Redispatching_RA;P0;116;120
            Redispatching_RA;Pmin_RD;10;15
            """;

        return Stream.of(
            Arguments.of(missingP0Csv, "Redispatching action Redispatching_RA is not defined in the time series csv. Missing one or several timeseries type (P0, RDP_DOWN, RDP_UP or P_MIN_RD)."),
            Arguments.of(missingRdpDownCsv, "Redispatching action Redispatching_RA is not defined in the time series csv. Missing one or several timeseries type (P0, RDP_DOWN, RDP_UP or P_MIN_RD)."),
            Arguments.of(missingRdpUpCsv, "Redispatching action Redispatching_RA is not defined in the time series csv. Missing one or several timeseries type (P0, RDP_DOWN, RDP_UP or P_MIN_RD).")
        );
    }

    private static Stream<Arguments> rangeIsNotOkayCases() {
        String baseHeader = """
            RA RD ID;Type of timeseries;00:30;01:30;02:30
            """;

        String negativeRdpPlusCsv = baseHeader + """
            Redispatching_RA;RDP-;35;35;35
            Redispatching_RA;RDP+;43;-1;43
            Redispatching_RA;P0;116;120;117
            Redispatching_RA;Pmin_RD;10;15;20
            """;
        String negativeRdpMinusCsv = baseHeader + """
            Redispatching_RA;RDP-;35;-35;35
            Redispatching_RA;RDP+;43;1;43
            Redispatching_RA;P0;116;120;117
            Redispatching_RA;Pmin_RD;10;15;20
            """;

        String tooSmallRangeCsv = baseHeader + """
            Redispatching_RA;RDP-;0;0;0
            Redispatching_RA;RDP+;0;0;0
            Redispatching_RA;P0;116;120;117
            Redispatching_RA;Pmin_RD;10;15;20
            """;

        return Stream.of(
            Arguments.of(negativeRdpPlusCsv,
                "Redispatching action Redispatching_RA will not be imported because of RDP+ -1.0 or RDP- 35.0 is negative for datetime 2025-02-13T01:30Z"),
            Arguments.of(negativeRdpMinusCsv,
                "Redispatching action Redispatching_RA will not be imported because of RDP+ 1.0 or RDP- -35.0 is negative for datetime 2025-02-13T01:30Z"),
            Arguments.of(tooSmallRangeCsv,
                "Redispatching action Redispatching_RA will not be imported because max range in the day 0.0 MW is too small")
        );
    }

    private static Stream<Arguments> gradientNotRespectCsvCases() {
        String header = """
            RA RD ID;Type of timeseries;00:30;01:30;02:30
            """;

        // diff = 150 - 116 = 34 > 20 => rejected
        String tooHighGradientCsv = header + """
            Redispatching_RA;RDP-;35;35;35
            Redispatching_RA;RDP+;43;43;43
            Redispatching_RA;P0;116;150;117
            Redispatching_RA;Pmin_RD;10;15;20
            """;

        // diff = 80 - 116 = -36 < -20 => rejected
        String tooLowGradientCsv = header + """
            Redispatching_RA;RDP-;35;35;35
            Redispatching_RA;RDP+;43;43;43
            Redispatching_RA;P0;116;80;117
            Redispatching_RA;Pmin_RD;10;15;20
            """;

        return Stream.of(
            Arguments.of(
                tooHighGradientCsv,
                "Redispatching action Redispatching_RA will not be imported because it does not respect power gradients : min/max/diff = -20.0 / 20.0 / 34.0"
            ),
            Arguments.of(
                tooLowGradientCsv,
                "Redispatching action Redispatching_RA will not be imported because it does not respect power gradients : min/max/diff = -20.0 / 20.0 / -36.0"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("gradientNotRespectCsvCases")
    @MethodSource("rangeIsNotOkayCases")
    @MethodSource("seriesCsvWithMissingSeriesTypeCases")
    void testP0RespectsGradients(String seriesCsv, String expectedLogMessage) throws IOException {
        IcsData icsData = IcsDataImporter.read(
            getClass().getResourceAsStream("/ics/static.csv"),
            new ByteArrayInputStream(seriesCsv.getBytes(StandardCharsets.UTF_8)),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList(3));

        assertEquals(0, icsData.getRedispatchingActions().size());
        assertEquals(expectedLogMessage, logsList.get(0).getFormattedMessage());
    }

    @Test
    void testMissingGradientInStaticCsv() throws IOException {
        // Missing gradient in static csv -> we should use the default MAX_GRADIENT
        String staticCsv = """
            RA RD ID;TSO;Preventive;Curative;Time From;Time To;Generator Name;RD description mode;UCT Node or GSK ID;Minimum Redispatch [MW];Fuel type;Minimum up-time [h];Minimum down-time [h];Maximum positive power gradient [MW/h];Maximum negative power gradient [MW/h];Lead time [h];Lag time [h];Startup allowed;Shutdown allowed
            Redispatching_RA;FR;TRUE;FALSE;00:00;24:00:00;Generator_Name;Node;BBE1AA1;50;Coal;2;2;;;1;1;FALSE;FALSE
            """;
        String seriesCsv = """
            RA RD ID;Type of timeseries;00:30;01:30;02:30
            Redispatching_RA;RDP-;35;35;35
            Redispatching_RA;RDP+;43;43;43
            Redispatching_RA;P0;116;2080;117
            Redispatching_RA;Pmin_RD;10;15;20
            """;
        IcsData icsData = IcsDataImporter.read(
            new ByteArrayInputStream(staticCsv.getBytes(StandardCharsets.UTF_8)),
            new ByteArrayInputStream(seriesCsv.getBytes(StandardCharsets.UTF_8)),
            getClass().getResourceAsStream("/glsk/gsk.csv"),
            generateOffsetDateTimeList(3));

        assertEquals(0, icsData.getRedispatchingActions().size());
        assertEquals("Redispatching action Redispatching_RA will not be imported because it does not respect power gradients : min/max/diff = -1000.0 / 1000.0 / 1964.0", logsList.get(0).getFormattedMessage());
    }

    @Test
    void testGskWeightSumNotEqualToOne() throws IOException {
        String gsk = """
            GSK ID;Node;Weight
            GSK_NAME;BBE1AA1;0.6
            GSK_NAME;FFR1AA1;0.5
            """;
        IcsData icsData = IcsDataImporter.read(
            getClass().getResourceAsStream("/ics/static_with_gsk.csv"),
            getClass().getResourceAsStream("/ics/series.csv"),
            new ByteArrayInputStream(gsk.getBytes(StandardCharsets.UTF_8)),
            generateOffsetDateTimeList(24));

        assertEquals(0, icsData.getRedispatchingActions().size());
        assertEquals("Redispatching action Redispatching_RA is ignored but it is defined on a GSK but sum of weights is not equal to 1", logsList.get(0).getFormattedMessage());
    }
}
