/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresult.io.idcc.core;

import org.threeten.extra.Interval;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class IntervalUtil {
    private IntervalUtil() {
        throw new AssertionError("Utility class should not be constructed");
    }

    // key is the position (start position is 1, end position is 24, but could be  23 or 25 in daylight saving)
    public static Map<Integer, Interval> getPositionsMap(String intervalString) {

        Interval interval = Interval.parse(intervalString);
        Instant startDate = interval.getStart();
        Instant endDate = interval.getEnd();
        Map<Integer, Interval> positionsMap = new TreeMap<>();
        int position = 1;
        while (startDate.isBefore(endDate)) {
            positionsMap.put(position, Interval.of(startDate, startDate.plusSeconds(3600)));
            startDate = startDate.plusSeconds(3600);
            position++;
        }
        return positionsMap;
    }

    public static String getCurrentTimeInterval(OffsetDateTime timestamp) {
        OffsetDateTime startTime = timestamp.minusMinutes(timestamp.getMinute());
        OffsetDateTime endTime = startTime.plusHours(1L);
        String start = startTime.toString();
        return start + "/" + endTime;
    }

    public static boolean isInTimeInterval(OffsetDateTime offsetDateTime, String timeInterval) {
        String[] timeIntervals = timeInterval.split("/");
        OffsetDateTime startTimeBranch = OffsetDateTime.parse(timeIntervals[0]);
        OffsetDateTime endTimeBranch = OffsetDateTime.parse(timeIntervals[1]);
        return !offsetDateTime.isBefore(startTimeBranch) && offsetDateTime.isBefore(endTimeBranch);
    }

    public static String getFormattedBusinessDay(LocalDate localDate, String format) {
        return DateTimeFormatter.ofPattern(format).format(localDate);
    }

    public static String getFormattedBusinessDay(LocalDate localDate) {
        return getFormattedBusinessDay(localDate, "yyyyMMdd");
    }

    public static String formatIntervalInUtc(Interval interval) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy'-'MM'-'dd'T'HH':'mm'Z'").withZone(ZoneId.from(ZoneOffset.UTC));
        return String.format("%s/%s", formatter.format(interval.getStart()), formatter.format(interval.getEnd()));
    }

}
