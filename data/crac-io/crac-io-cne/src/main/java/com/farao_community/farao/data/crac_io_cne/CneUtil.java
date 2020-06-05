/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.commons.FaraoException;
import org.joda.time.DateTime;

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
 */
public final class CneUtil {

    private CneUtil() { }

    // Creation of time interval
    public static ESMPDateTimeInterval createEsmpDateTimeInterval(DateTime networkDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));

        ESMPDateTimeInterval timeInterval = new ESMPDateTimeInterval();

        timeInterval.setStart(dateFormat.format(networkDate.toDate()));
        timeInterval.setEnd(dateFormat.format(networkDate.plusHours(1).toDate()));
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
    public static PartyIDString createPartyIDString(String codingScheme, String value) {
        PartyIDString marketParticipantMRID = new PartyIDString();
        marketParticipantMRID.setCodingScheme(codingScheme);
        marketParticipantMRID.setValue(cutString(value, 16));
        return marketParticipantMRID;
    }

    // Generates a random code with 35 characters
    public static String generateRandomMRID() {
        Random random = new SecureRandom();
        return String.format("%s-%s-%s-%s", Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()));
    }

    public static String cutString(String string, int maxChar) {
        return string.substring(0, Math.min(string.length(), maxChar));
    }

    public static ConstraintSeries duplicateConstraintSeries(ConstraintSeries constraintSeries) {
        ConstraintSeries newConstraintSeries = new ConstraintSeries();
        if (constraintSeries.contingencySeries != null) {
            newConstraintSeries.contingencySeries = duplicateContingencySeriesList(constraintSeries.contingencySeries);
        }
        return newConstraintSeries;
    }

    public static List<ContingencySeries> duplicateContingencySeriesList(List<ContingencySeries> contingencySeriesList) {
        List<ContingencySeries> newContingencySeriesList = new ArrayList<>();
        contingencySeriesList.forEach(contingencySeries -> newContingencySeriesList.add(duplicateContingencySeries(contingencySeries)));
        return newContingencySeriesList;
    }

    public static ContingencySeries duplicateContingencySeries(ContingencySeries contingencySeries) {
        ContingencySeries newContingencySeries = new ContingencySeries();
        newContingencySeries.setMRID(contingencySeries.getMRID());
        if (contingencySeries.getName() != null) {
            newContingencySeries.setName(contingencySeries.getName());
        }
        return newContingencySeries;
    }
}
