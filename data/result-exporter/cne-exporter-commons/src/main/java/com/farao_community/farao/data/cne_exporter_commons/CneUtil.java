/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.cne_exporter_commons;

import com.powsybl.open_rao.commons.FaraoException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
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

    // Creation of current date
    public static XMLGregorianCalendar createXMLGregorianCalendarNow() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
        try {
            XMLGregorianCalendar xmlcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(dateFormat.format(new Date()));
            xmlcal.setTimezone(0);

            return xmlcal;
        } catch (DatatypeConfigurationException e) {
            throw new FaraoException("Could not write current date and time.");
        }
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

    public static String generateUUID() {
        String uuidString = UUID.randomUUID().toString().substring(1);
        while (usedUniqueIds.contains(uuidString)) {
            uuidString = UUID.randomUUID().toString().substring(1);
        }
        usedUniqueIds.add(uuidString);
        return uuidString;
    }
}
