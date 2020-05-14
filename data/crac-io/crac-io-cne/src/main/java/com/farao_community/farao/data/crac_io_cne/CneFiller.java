/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_result_extensions.CracResult;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import org.joda.time.DateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CneFiller {

    private static CriticalNetworkElementMarketDocument cne = new CriticalNetworkElementMarketDocument();

    private CneFiller() { }

    public static CriticalNetworkElementMarketDocument getCne() {
        return cne;
    }

    public static void generate(Crac crac) {
        SimpleDateFormat dateFormat = setDateTimeFormat();
        if (crac.isSynchronized()) {
            fillHeader(crac.getNetworkDate(), dateFormat);
            createTimeSeries(crac.getNetworkDate(), dateFormat);
            Point point = cne.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

            if (crac.getExtension(CracResultExtension.class) != null) { // Computation ended
                CracResultExtension cracExtension = crac.getExtension(CracResultExtension.class);
                addReasonToPoint(point, Optional.of(cracExtension.getVariant("variant1").getNetworkSecurityStatus()));
                addContingencies(point, crac);

            } else { // Failure of computation
                addReasonToPoint(cne.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0), Optional.empty());
            }

        } else {
            throw new FaraoException("Crac should be synchronized!");
        }
    }

    private static void addContingencies(Point point, Crac crac) {

        List<ConstraintSeries> constraintSeriesList = new ArrayList<>();

        crac.getCnecs().forEach(
            cnec -> {
                if (cnec.getState().getContingency().isPresent()) {
                    Contingency contingency = cnec.getState().getContingency().get();

                    constraintSeriesList.add(createConstraintSeriesWithContingency("B57", contingency));
                    constraintSeriesList.add(createConstraintSeriesWithContingency("B56", contingency));

                } else {
                    constraintSeriesList.add(createConstraintSeries("B57"));
                    //TODO: do this only if there is a PRA
                    constraintSeriesList.add(createConstraintSeries("B56"));
                }
            }
        );

        point.constraintSeries = constraintSeriesList;
    }

    private static ConstraintSeries createConstraintSeries(String businessType) {
        ConstraintSeries constraintSeries = new ConstraintSeries();
        constraintSeries.setMRID(generateRandomMRID());
        constraintSeries.setBusinessType(businessType);

        return constraintSeries;
    }

    private static ConstraintSeries createConstraintSeriesWithContingency(String businessType, Contingency contingency) {
        ConstraintSeries constraintSeries = createConstraintSeries(businessType);
        ContingencySeries contingencySeries = new ContingencySeries();
        contingencySeries.setMRID(contingency.getId());
        contingencySeries.setName(contingency.getName());
        constraintSeries.contingencySeries = Collections.singletonList(contingencySeries);

        return constraintSeries;
    }


    // TODO: clean it and get the correct network status (from CracResultExtension)
    private static void addReasonToPoint(Point point, Optional<CracResult.NetworkSecurityStatus> status) {
        Reason reason = new Reason();
        if (status.isPresent()) {
            if (status.get().equals(CracResult.NetworkSecurityStatus.SECURED)) {
                reason.setCode("Z13");
                reason.setText("Situation is secure");
            } else if (status.get().equals(CracResult.NetworkSecurityStatus.UNSECURED)) {
                reason.setCode("Z03");
                reason.setText("Situation is unsecure");
            } else {
                throw new FaraoException(String.format("Unexpected status %s.", status));
            }
        } else {
            reason.setCode("999");
            reason.setText("Other failure");
        }
        point.reason = Collections.singletonList(reason);
    }

    private static void createTimeSeries(DateTime networkDate, SimpleDateFormat dateFormat) {

        Point point = new Point();
        point.setPosition(1);

        SeriesPeriod period = new SeriesPeriod();
        period.setTimeInterval(createEsmpDateTimeInterval(networkDate, dateFormat));
        // TODO: resolution
        period.point = Collections.singletonList(point);

        TimeSeries timeSeries = new TimeSeries();
        timeSeries.setMRID(generateRandomMRID());
        timeSeries.setBusinessType("B54");
        timeSeries.setCurveType("A01");
        timeSeries.period = Collections.singletonList(period);

        cne.timeSeries = Collections.singletonList(timeSeries);
    }

    /*****************
     HEADER
     *****************/
    private static void fillHeader(DateTime networkDate, SimpleDateFormat dateFormat) {
        cne.setMRID(generateRandomMRID());
        cne.setRevisionNumber("1");
        cne.setType("B06");
        cne.setProcessProcessType("A43");
        cne.setSenderMarketParticipantMRID(createPartyIDString("A01", "22XCORESO------S"));
        cne.setSenderMarketParticipantMarketRoleType("A44");
        cne.setReceiverMarketParticipantMRID(createPartyIDString("A01", "17XTSO-CS------W"));
        cne.setReceiverMarketParticipantMarketRoleType("A36");
        cne.setCreatedDateTime(createXMLGregorianCalendarNow(dateFormat));
        cne.setTimePeriodTimeInterval(createEsmpDateTimeInterval(networkDate, dateFormat));
        cne.setDomainMRID(createAreaIDString("A01", "10YDOM-REGION-1V"));
    }

    // Helper for date format
    private static SimpleDateFormat setDateTimeFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
        return dateFormat;
    }

    // Creation of time interval
    private static ESMPDateTimeInterval createEsmpDateTimeInterval(DateTime networkDate, SimpleDateFormat dateFormat) {
        ESMPDateTimeInterval timeInterval = new ESMPDateTimeInterval();

        timeInterval.setStart(dateFormat.format(networkDate.toDate()));
        timeInterval.setEnd(dateFormat.format(networkDate.plusHours(1).toDate()));
        return timeInterval;
    }

    // Creation of current date
    private static XMLGregorianCalendar createXMLGregorianCalendarNow(SimpleDateFormat dateFormat) {
        try {
            XMLGregorianCalendar xmlcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(dateFormat.format(new Date()));
            xmlcal.setTimezone(0);

            return xmlcal;
        } catch (DatatypeConfigurationException e) {
            throw new FaraoException();
        }
    }

    // Creation of ID with code scheme
    private static AreaIDString createAreaIDString(String codingScheme, String value) {
        AreaIDString domainMRID = new AreaIDString();
        domainMRID.setCodingScheme(codingScheme);
        domainMRID.setValue(value);
        return domainMRID;
    }

    // Creation of ID with code scheme
    private static PartyIDString createPartyIDString(String codingScheme, String value) {
        PartyIDString marketParticipantMRID = new PartyIDString();
        marketParticipantMRID.setCodingScheme(codingScheme);
        marketParticipantMRID.setValue(value);
        return marketParticipantMRID;
    }

    // Generates a random code with 35 characters
    private static String generateRandomMRID() {
        Random random = new Random(System.nanoTime());
        return String.format("%s-%s-%s-%s", Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()));
    }
}
