/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons;

import com.farao_community.farao.commons.chronology.DataChronology;
import com.farao_community.farao.commons.chronology.DataChronologyImpl;
import org.junit.Test;
import org.threeten.extra.Interval;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class DataChronologyImplTest {
    @Test
    public void testStoringByInstant() {
        DataChronology<Integer> dataChronology = DataChronologyImpl.create();
        String instantAsString = "2007-12-03T10:15:30.00Z";
        String instantInHourAsString = "2007-12-03T10:45:30.00Z";
        String otherInstantAsString = "2008-12-03T10:15:30.00Z";
        dataChronology.storeDataAtInstant(10, Instant.parse(instantAsString));
        assertTrue(dataChronology.getDataForInstant(Instant.parse(instantAsString)).isPresent());
        assertEquals(10, dataChronology.getDataForInstant(Instant.parse(instantAsString)).get().intValue());
        assertTrue(dataChronology.getDataForInstant(Instant.parse(instantInHourAsString)).isPresent());
        assertEquals(10, dataChronology.getDataForInstant(Instant.parse(instantInHourAsString)).get().intValue());
        assertFalse(dataChronology.getDataForInstant(Instant.parse(otherInstantAsString)).isPresent());
    }

    @Test
    public void testStoringByInstantAndPeriod() {
        DataChronology<Integer> dataChronology = DataChronologyImpl.create();
        String instantAsString = "2007-12-03T10:15:30.00Z";
        String otherInstantWithinPeriodAsString = "2008-12-03T10:15:30.00Z";
        String otherInstantOutsidePeriodAsString = "2010-12-03T10:15:30.00Z";
        dataChronology.storeDataAtInstant(10, Instant.parse(instantAsString), Period.ofYears(2));
        assertTrue(dataChronology.getDataForInstant(Instant.parse(instantAsString)).isPresent());
        assertEquals(10, dataChronology.getDataForInstant(Instant.parse(instantAsString)).get().intValue());
        assertTrue(dataChronology.getDataForInstant(Instant.parse(otherInstantWithinPeriodAsString)).isPresent());
        assertEquals(10, dataChronology.getDataForInstant(Instant.parse(otherInstantWithinPeriodAsString)).get().intValue());
        assertFalse(dataChronology.getDataForInstant(Instant.parse(otherInstantOutsidePeriodAsString)).isPresent());
    }

    @Test
    public void testStoringByInterval() {
        DataChronology<Integer> dataChronology = DataChronologyImpl.create();
        String intervalAsString = "2009-12-03T10:15:30.00Z/2010-12-03T10:15:30.00Z";
        String instantInsidePeriod = "2010-03-03T10:15:30.00Z";
        String instantOutsidePeriod = "2011-03-03T10:15:30.00Z";
        dataChronology.storeDataOnInterval(0, Interval.parse(intervalAsString));
        assertTrue(dataChronology.getDataForInstant(Instant.parse(instantInsidePeriod)).isPresent());
        assertEquals(0, dataChronology.getDataForInstant(Instant.parse(instantInsidePeriod)).get().intValue());
        assertFalse(dataChronology.getDataForInstant(Instant.parse(instantOutsidePeriod)).isPresent());
    }

    @Test
    public void testStoringByBeginningAndEndInstants() {
        DataChronology<Integer> dataChronology = DataChronologyImpl.create();
        String beginningInstantAsString = "2009-12-03T10:15:30.00Z";
        String endInstantAsString = "2010-12-03T10:15:30.00Z";
        String instantInsidePeriod = "2010-03-03T10:15:30.00Z";
        String instantOutsidePeriod = "2011-03-03T10:15:30.00Z";
        dataChronology.storeDataBetweenInstants(-10, Instant.parse(beginningInstantAsString), Instant.parse(endInstantAsString));
        assertTrue(dataChronology.getDataForInstant(Instant.parse(instantInsidePeriod)).isPresent());
        assertEquals(-10, dataChronology.getDataForInstant(Instant.parse(instantInsidePeriod)).get().intValue());
        assertFalse(dataChronology.getDataForInstant(Instant.parse(instantOutsidePeriod)).isPresent());
    }

    @Test
    public void testWithReplacementStrategyGet() {
        DataChronology<Integer> dataChronology = DataChronologyImpl.create();
        String instant1 = "2010-01-01T10:15:30.00Z";
        String instant2 = "2011-01-01T10:15:30.00Z";
        String instantInside = "2010-03-03T10:15:30.00Z";
        String instantBefore = "2009-03-03T10:15:30.00Z";
        String instantAfter = "2012-03-03T10:15:30.00Z";
        dataChronology.storeDataAtInstant(1, Instant.parse(instant1));
        dataChronology.storeDataAtInstant(2, Instant.parse(instant2));

        // If instant inside interval [instant1, instant2]
        assertFalse(dataChronology.getDataForInstant(Instant.parse(instantInside)).isPresent());
        assertTrue(dataChronology.getDataForInstant(Instant.parse(instantInside), DataChronology.ReplacementStrategy.DATA_AT_PREVIOUS_INSTANT).isPresent());
        assertEquals(1, dataChronology.getDataForInstant(Instant.parse(instantInside), DataChronology.ReplacementStrategy.DATA_AT_PREVIOUS_INSTANT).get().intValue());
        assertTrue(dataChronology.getDataForInstant(Instant.parse(instantInside), DataChronology.ReplacementStrategy.DATA_AT_NEXT_INSTANT).isPresent());
        assertEquals(2, dataChronology.getDataForInstant(Instant.parse(instantInside), DataChronology.ReplacementStrategy.DATA_AT_NEXT_INSTANT).get().intValue());

        // If instant before interval [instant1, instant2]
        assertFalse(dataChronology.getDataForInstant(Instant.parse(instantBefore)).isPresent());
        assertFalse(dataChronology.getDataForInstant(Instant.parse(instantBefore), DataChronology.ReplacementStrategy.DATA_AT_PREVIOUS_INSTANT).isPresent());
        assertTrue(dataChronology.getDataForInstant(Instant.parse(instantBefore), DataChronology.ReplacementStrategy.DATA_AT_NEXT_INSTANT).isPresent());
        assertEquals(1, dataChronology.getDataForInstant(Instant.parse(instantBefore), DataChronology.ReplacementStrategy.DATA_AT_NEXT_INSTANT).get().intValue());

        // If instant after interval [instant1, instant2]
        assertFalse(dataChronology.getDataForInstant(Instant.parse(instantAfter)).isPresent());
        assertTrue(dataChronology.getDataForInstant(Instant.parse(instantAfter), DataChronology.ReplacementStrategy.DATA_AT_PREVIOUS_INSTANT).isPresent());
        assertEquals(2, dataChronology.getDataForInstant(Instant.parse(instantAfter), DataChronology.ReplacementStrategy.DATA_AT_PREVIOUS_INSTANT).get().intValue());
        assertFalse(dataChronology.getDataForInstant(Instant.parse(instantAfter), DataChronology.ReplacementStrategy.DATA_AT_NEXT_INSTANT).isPresent());
    }

    @Test(expected = FaraoException.class)
    public void testErrorWhenAddingInstantInAlreadyCreatedInstant() {
        DataChronology<Integer> dataChronology = DataChronologyImpl.create();
        String instant = "2010-03-03T10:15:30.00Z";
        dataChronology.storeDataAtInstant(2, Instant.parse(instant));
        dataChronology.storeDataAtInstant(2, Instant.parse(instant));
    }

    @Test(expected = FaraoException.class)
    public void testErrorWhenAddingInstantInValidityPeriodOfAnotherInstant() {
        DataChronology<Integer> dataChronology = DataChronologyImpl.create();
        String instant = "2010-03-03T10:15:30.00Z";
        String instantPlus59Min = "2010-03-03T10:16:29.00Z";
        dataChronology.storeDataAtInstant(2, Instant.parse(instant));
        dataChronology.storeDataAtInstant(2, Instant.parse(instantPlus59Min));
    }

    @Test(expected = FaraoException.class)
    public void testErrorWhenAddingInstantInAlreadyCreatedPeriod() {
        DataChronology<Integer> dataChronology = DataChronologyImpl.create();
        String interval = "2010-01-01T10:15:30.00Z/2011-01-01T10:15:30.00Z";
        String instantInside = "2010-03-03T10:15:30.00Z";
        dataChronology.storeDataOnInterval(1, Interval.parse(interval));
        dataChronology.storeDataAtInstant(2, Instant.parse(instantInside));
    }

    @Test(expected = FaraoException.class)
    public void testErrorWhenIntervalsOverlaps() {
        DataChronology<Integer> dataChronology = DataChronologyImpl.create();
        String interval1 = "2010-01-01T10:15:30.00Z/2011-01-01T10:15:30.00Z";
        String interval2 = "2010-05-01T10:15:30.00Z/2011-05-01T10:15:30.00Z";
        dataChronology.storeDataOnInterval(1, Interval.parse(interval1));
        dataChronology.storeDataOnInterval(2, Interval.parse(interval2));
    }

    @Test
    public void testLimitsOfIntervals() {
        DataChronology<Integer> dataChronology = DataChronologyImpl.create();
        String instant = "2010-03-03T10:15:30.00Z";
        String instantPlus1Hour = "2010-03-03T11:15:30.00Z";
        String instantPlus1Hour1Day = "2010-03-04T11:15:30.00Z";
        dataChronology.storeDataAtInstant(2, Instant.parse(instant));
        dataChronology.storeDataAtInstant(2, Instant.parse(instantPlus1Hour), Duration.ofDays(1));
        dataChronology.storeDataAtInstant(2, Instant.parse(instantPlus1Hour1Day));
    }
}

