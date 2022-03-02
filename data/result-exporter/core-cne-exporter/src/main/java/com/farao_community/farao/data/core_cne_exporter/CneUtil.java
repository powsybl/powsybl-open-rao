/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.core_cne_exporter;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.core_cne_exporter.xsd.AreaIDString;
import com.farao_community.farao.data.core_cne_exporter.xsd.ESMPDateTimeInterval;
import com.farao_community.farao.data.core_cne_exporter.xsd.PartyIDString;
import com.farao_community.farao.data.core_cne_exporter.xsd.ResourceIDString;
import org.threeten.extra.Interval;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Auxiliary methods
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class CneUtil {
    private static final double FLOAT_LIMIT = 999999;
    private static Set<String> usedUniqueIds;

    private CneUtil() {
    }

    public static void initUniqueIds() {
        usedUniqueIds = new HashSet<>();
    }

    // Creation of time interval
    public static ESMPDateTimeInterval createEsmpDateTimeInterval(OffsetDateTime offsetDateTime) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");

        ESMPDateTimeInterval timeInterval = new ESMPDateTimeInterval();

        OffsetDateTime utcDateTime = offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC);
        timeInterval.setStart(dateFormat.format(utcDateTime));
        timeInterval.setEnd(dateFormat.format(utcDateTime.plusHours(1)));
        return timeInterval;

    }

    // Creation of time interval
    public static ESMPDateTimeInterval createEsmpDateTimeIntervalForWholeDay(String intervalString) {
        Interval interval = Interval.parse(intervalString);
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'").withZone(ZoneId.from(ZoneOffset.UTC));
        ESMPDateTimeInterval timeInterval = new ESMPDateTimeInterval();
        timeInterval.setStart(dateFormat.format(interval.getStart()));
        timeInterval.setEnd(dateFormat.format(interval.getEnd()));
        return timeInterval;
    }

    // Creation of current date
    public static XMLGregorianCalendar createXMLGregorianCalendarNow() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
        try {
            XMLGregorianCalendar xmlcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(dateFormat.format(new Date()));
            xmlcal.setTimezone(0);

            return xmlcal;
        } catch (DatatypeConfigurationException e) {
            throw new FaraoException();
        }
    }

    // Creation of ID with code scheme
    public static ResourceIDString createResourceIDString(String codingScheme, String value) {
        ResourceIDString resourceMRID = new ResourceIDString();
        resourceMRID.setCodingScheme(codingScheme);
        resourceMRID.setValue(cutString(value, 60));
        return resourceMRID;
    }

    // Creation of ID with code scheme
    public static PartyIDString createPartyIDString(String codingScheme, String value) {
        PartyIDString marketParticipantMRID = new PartyIDString();
        marketParticipantMRID.setCodingScheme(codingScheme);
        marketParticipantMRID.setValue(cutString(value, 16));
        return marketParticipantMRID;
    }

    // Creation of area ID with code scheme
    public static AreaIDString createAreaIDString(String codingScheme, String value) {
        AreaIDString areaIDString = new AreaIDString();
        areaIDString.setCodingScheme(codingScheme);
        areaIDString.setValue(cutString(value, 16));
        return areaIDString;
    }

    public static String cutString(String string, int maxChar) {
        return string.substring(0, Math.min(string.length(), maxChar));
    }

    public static String randomizeString(String string, int maxChar) {
        int nbRandomChars = 5;
        Random random = new SecureRandom();
        String randomString;
        do {
            String newString = cutString(string, maxChar - nbRandomChars - 1);
            randomString = newString + "_" + cutString(Integer.toHexString(random.nextInt()), nbRandomChars);
        } while (usedUniqueIds.contains(randomString));
        usedUniqueIds.add(randomString);
        return randomString;
    }

    public static float limitFloatInterval(double value) {
        return (float) Math.min(Math.round(Math.abs(value)), FLOAT_LIMIT);
    }
}
