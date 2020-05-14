/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import org.joda.time.DateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CneFiller {

    private static CriticalNetworkElementMarketDocument cne = new CriticalNetworkElementMarketDocument();
    private static SimpleDateFormat dateFormat = null;

    private CneFiller() { }

    public static CriticalNetworkElementMarketDocument getCne() {
        return cne;
    }

    private static void setDateTimeFormat() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    public static void generate(Crac crac) {
        setDateTimeFormat();

        /*if (crac.isSynchronized()) {
            fillHeader(crac.getNetworkDate());
        } else {
            throw new FaraoException("Crac should be synchronized!");
        }*/
        fillHeader(DateTime.now());
    }

    private static void fillHeader(DateTime networkDate) {
        cne.setMRID(generateRandomMRID());
        cne.setRevisionNumber("1");
        cne.setType("B06");
        cne.setProcessProcessType("A43");
        cne.setSenderMarketParticipantMRID(createPartyIDString("A01", "22XCORESO------S"));
        cne.setSenderMarketParticipantMarketRoleType("A44");
        cne.setReceiverMarketParticipantMRID(createPartyIDString("A01", "17XTSO-CS------W"));
        cne.setReceiverMarketParticipantMarketRoleType("A36");
        cne.setCreatedDateTime(createXMLGregorianCalendarNow());
        cne.setTimePeriodTimeInterval(createEsmpDateTimeInterval(networkDate));
        cne.setDomainMRID(createAreaIDString("A01", "10YDOM-REGION-1V"));
    }

    private static ESMPDateTimeInterval createEsmpDateTimeInterval(DateTime networkDate) {
        ESMPDateTimeInterval timeInterval = new ESMPDateTimeInterval();

        timeInterval.setStart(dateFormat.format(networkDate.toDate()));
        timeInterval.setEnd(dateFormat.format(networkDate.plusHours(1).toDate()));
        return timeInterval;
    }

    private static XMLGregorianCalendar createXMLGregorianCalendarNow() {
        try {
            XMLGregorianCalendar xmlcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(dateFormat.format(new Date()));
            xmlcal.setTimezone(0);

            return xmlcal;
        } catch (DatatypeConfigurationException e) {
            throw new FaraoException();
        }
    }

    private static AreaIDString createAreaIDString(String codingScheme, String value) {
        AreaIDString domainMRID = new AreaIDString();
        domainMRID.setCodingScheme(codingScheme);
        domainMRID.setValue(value);
        return domainMRID;
    }

    private static PartyIDString createPartyIDString(String codingScheme, String value) {
        PartyIDString marketParticipantMRID = new PartyIDString();
        marketParticipantMRID.setCodingScheme(codingScheme);
        marketParticipantMRID.setValue(value);
        return marketParticipantMRID;
    }

    /*
    *  Generates a random code with 35 characters
    * */
    private static String generateRandomMRID() {
        Random random = new Random(System.nanoTime());
        return String.format("%s-%s-%s-%s", Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()));
    }
}
