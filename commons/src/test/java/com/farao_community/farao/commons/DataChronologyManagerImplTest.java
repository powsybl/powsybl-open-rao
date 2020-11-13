/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons;

import com.farao_community.farao.commons.chronology.DataChronologyManager;
import com.farao_community.farao.commons.chronology.DataChronologyManagerImpl;
import org.junit.Test;
import org.threeten.extra.Interval;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class DataChronologyManagerImplTest {
    @Test
    public void testStoringByInstant() {
        DataChronologyManager<Integer> dataChronologyManager = DataChronologyManagerImpl.create();
        String instantAsString = "2007-12-03T10:15:30.00Z";
        String instantInHourAsString = "2007-12-03T10:45:30.00Z";
        String otherInstantAsString = "2008-12-03T10:15:30.00Z";
        dataChronologyManager.storeDataAtInstant(10, Instant.parse(instantAsString));
        assertNotNull(dataChronologyManager.getDataForInstant(Instant.parse(instantAsString)));
        assertEquals(10, dataChronologyManager.getDataForInstant(Instant.parse(instantAsString)).intValue());
        assertNotNull(dataChronologyManager.getDataForInstant(Instant.parse(instantInHourAsString)));
        assertEquals(10, dataChronologyManager.getDataForInstant(Instant.parse(instantInHourAsString)).intValue());
        assertNull(dataChronologyManager.getDataForInstant(Instant.parse(otherInstantAsString)));
    }

    @Test
    public void testStoringByInstantAndPeriod() {
        DataChronologyManager<Integer> dataChronologyManager = DataChronologyManagerImpl.create();
        String instantAsString = "2007-12-03T10:15:30.00Z";
        String otherInstantWithinPeriodAsString = "2008-12-03T10:15:30.00Z";
        String otherInstantOutsidePeriodAsString = "2010-12-03T10:15:30.00Z";
        dataChronologyManager.storeDataAtInstant(10, Instant.parse(instantAsString), Period.ofYears(2));
        assertNotNull(dataChronologyManager.getDataForInstant(Instant.parse(instantAsString)));
        assertEquals(10, dataChronologyManager.getDataForInstant(Instant.parse(instantAsString)).intValue());
        assertNotNull(dataChronologyManager.getDataForInstant(Instant.parse(otherInstantWithinPeriodAsString)));
        assertEquals(10, dataChronologyManager.getDataForInstant(Instant.parse(otherInstantWithinPeriodAsString)).intValue());
        assertNull(dataChronologyManager.getDataForInstant(Instant.parse(otherInstantOutsidePeriodAsString)));
    }

    @Test
    public void testStoringByInterval() {
        DataChronologyManager<Integer> dataChronologyManager = DataChronologyManagerImpl.create();
        String intervalAsString = "2009-12-03T10:15:30.00Z/2010-12-03T10:15:30.00Z";
        String instantInsidePeriod = "2010-03-03T10:15:30.00Z";
        String instantOutsidePeriod = "2011-03-03T10:15:30.00Z";
        dataChronologyManager.storeDataOnInterval(0, Interval.parse(intervalAsString));
        assertNotNull(dataChronologyManager.getDataForInstant(Instant.parse(instantInsidePeriod)));
        assertEquals(0, dataChronologyManager.getDataForInstant(Instant.parse(instantInsidePeriod)).intValue());
        assertNull(dataChronologyManager.getDataForInstant(Instant.parse(instantOutsidePeriod)));
    }

    @Test
    public void testStoringByBeginningAndEndInstants() {
        DataChronologyManager<Integer> dataChronologyManager = DataChronologyManagerImpl.create();
        String beginningInstantAsString = "2009-12-03T10:15:30.00Z";
        String endInstantAsString = "2010-12-03T10:15:30.00Z";
        String instantInsidePeriod = "2010-03-03T10:15:30.00Z";
        String instantOutsidePeriod = "2011-03-03T10:15:30.00Z";
        dataChronologyManager.storeDataBetweenInstants(-10, Instant.parse(beginningInstantAsString), Instant.parse(endInstantAsString));
        assertNotNull(dataChronologyManager.getDataForInstant(Instant.parse(instantInsidePeriod)));
        assertEquals(-10, dataChronologyManager.getDataForInstant(Instant.parse(instantInsidePeriod)).intValue());
        assertNull(dataChronologyManager.getDataForInstant(Instant.parse(instantOutsidePeriod)));
    }

    @Test
    public void testWithReplacementStrategyGet() {
        DataChronologyManager<Integer> dataChronologyManager = DataChronologyManagerImpl.create();
        String instant1 = "2010-01-01T10:15:30.00Z";
        String instant2 = "2011-01-01T10:15:30.00Z";
        String instantInside = "2010-03-03T10:15:30.00Z";
        String instantBefore = "2009-03-03T10:15:30.00Z";
        String instantAfter = "2012-03-03T10:15:30.00Z";
        dataChronologyManager.storeDataAtInstant(1, Instant.parse(instant1));
        dataChronologyManager.storeDataAtInstant(2, Instant.parse(instant2));

        // If instant inside interval [instant1, instant2]
        assertNull(dataChronologyManager.getDataForInstant(Instant.parse(instantInside)));
        assertNotNull(dataChronologyManager.getDataForInstant(Instant.parse(instantInside), DataChronologyManager.ReplacementStrategy.DATA_AT_PREVIOUS_INSTANT));
        assertEquals(1, dataChronologyManager.getDataForInstant(Instant.parse(instantInside), DataChronologyManager.ReplacementStrategy.DATA_AT_PREVIOUS_INSTANT).intValue());
        assertNotNull(dataChronologyManager.getDataForInstant(Instant.parse(instantInside), DataChronologyManager.ReplacementStrategy.DATA_AT_NEXT_INSTANT));
        assertEquals(2, dataChronologyManager.getDataForInstant(Instant.parse(instantInside), DataChronologyManager.ReplacementStrategy.DATA_AT_NEXT_INSTANT).intValue());

        // If instant before interval [instant1, instant2]
        assertNull(dataChronologyManager.getDataForInstant(Instant.parse(instantBefore)));
        assertNull(dataChronologyManager.getDataForInstant(Instant.parse(instantBefore), DataChronologyManager.ReplacementStrategy.DATA_AT_PREVIOUS_INSTANT));
        assertNotNull(dataChronologyManager.getDataForInstant(Instant.parse(instantBefore), DataChronologyManager.ReplacementStrategy.DATA_AT_NEXT_INSTANT));
        assertEquals(1, dataChronologyManager.getDataForInstant(Instant.parse(instantBefore), DataChronologyManager.ReplacementStrategy.DATA_AT_NEXT_INSTANT).intValue());

        // If instant after interval [instant1, instant2]
        assertNull(dataChronologyManager.getDataForInstant(Instant.parse(instantAfter)));
        assertNotNull(dataChronologyManager.getDataForInstant(Instant.parse(instantAfter), DataChronologyManager.ReplacementStrategy.DATA_AT_PREVIOUS_INSTANT));
        assertEquals(2, dataChronologyManager.getDataForInstant(Instant.parse(instantAfter), DataChronologyManager.ReplacementStrategy.DATA_AT_PREVIOUS_INSTANT).intValue());
        assertNull(dataChronologyManager.getDataForInstant(Instant.parse(instantAfter), DataChronologyManager.ReplacementStrategy.DATA_AT_NEXT_INSTANT));
    }

    @Test(expected = FaraoException.class)
    public void testErrorWhenAddingInstantInAlreadyCreatedInstant() {
        DataChronologyManager<Integer> dataChronologyManager = DataChronologyManagerImpl.create();
        String instant = "2010-03-03T10:15:30.00Z";
        dataChronologyManager.storeDataAtInstant(2, Instant.parse(instant));
        dataChronologyManager.storeDataAtInstant(2, Instant.parse(instant));
    }

    @Test(expected = FaraoException.class)
    public void testErrorWhenAddingInstantInValidityPeriodOfAnotherInstant() {
        DataChronologyManager<Integer> dataChronologyManager = DataChronologyManagerImpl.create();
        String instant = "2010-03-03T10:15:30.00Z";
        String instantPlus59Min = "2010-03-03T10:16:29.00Z";
        dataChronologyManager.storeDataAtInstant(2, Instant.parse(instant));
        dataChronologyManager.storeDataAtInstant(2, Instant.parse(instantPlus59Min));
    }

    @Test(expected = FaraoException.class)
    public void testErrorWhenAddingInstantInAlreadyCreatedPeriod() {
        DataChronologyManager<Integer> dataChronologyManager = DataChronologyManagerImpl.create();
        String interval = "2010-01-01T10:15:30.00Z/2011-01-01T10:15:30.00Z";
        String instantInside = "2010-03-03T10:15:30.00Z";
        dataChronologyManager.storeDataOnInterval(1, Interval.parse(interval));
        dataChronologyManager.storeDataAtInstant(2, Instant.parse(instantInside));
    }

    @Test(expected = FaraoException.class)
    public void testErrorWhenIntervalsOverlaps() {
        DataChronologyManager<Integer> dataChronologyManager = DataChronologyManagerImpl.create();
        String interval1 = "2010-01-01T10:15:30.00Z/2011-01-01T10:15:30.00Z";
        String interval2 = "2010-05-01T10:15:30.00Z/2011-05-01T10:15:30.00Z";
        dataChronologyManager.storeDataOnInterval(1, Interval.parse(interval1));
        dataChronologyManager.storeDataOnInterval(2, Interval.parse(interval2));
    }

    @Test
    public void testLimitsOfIntervals() {
        DataChronologyManager<Integer> dataChronologyManager = DataChronologyManagerImpl.create();
        String instant = "2010-03-03T10:15:30.00Z";
        String instantPlus1Hour = "2010-03-03T11:15:30.00Z";
        String instantPlus1Hour1Day = "2010-03-04T11:15:30.00Z";
        dataChronologyManager.storeDataAtInstant(2, Instant.parse(instant));
        dataChronologyManager.storeDataAtInstant(2, Instant.parse(instantPlus1Hour), Duration.ofDays(1));
        dataChronologyManager.storeDataAtInstant(2, Instant.parse(instantPlus1Hour1Day));
    }
}

