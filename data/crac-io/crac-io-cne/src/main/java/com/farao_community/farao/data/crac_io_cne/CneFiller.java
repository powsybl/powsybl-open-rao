/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_result_extensions.CracResult;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import org.joda.time.DateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CneFiller {

    private static CriticalNetworkElementMarketDocument cne = new CriticalNetworkElementMarketDocument();
    private static List<Instant> instants;

    private CneFiller() { }

    public static CriticalNetworkElementMarketDocument getCne() {
        return cne;
    }

    public static void generate(Crac crac) {
        SimpleDateFormat dateFormat = setDateTimeFormat();
        if (crac.isSynchronized()) {

            instants = crac.getInstants().stream().sorted(Comparator.comparing(Instant::getSeconds)).collect(Collectors.toList());

            fillHeader(crac.getNetworkDate(), dateFormat);
            createTimeSeries(crac.getNetworkDate(), dateFormat);
            Point point = cne.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

            if (crac.getExtension(CracResultExtension.class) != null) { // Computation ended
                CracResultExtension cracExtension = crac.getExtension(CracResultExtension.class);

                // TODO: Don't hardcode it, once it can be read from crac
                String preOptimVariantId = "variant2";
                String postOptimVariantId = "variant1";

                addSuccessReasonToPoint(point, cracExtension.getVariant(postOptimVariantId).getNetworkSecurityStatus());
                createAllConstraintSeries(point, crac);

            } else { // Failure of computation
                addFailureReasonToPoint(point);
            }
        } else {
            throw new FaraoException("Crac should be synchronized!");
        }
    }

    private static void createAllConstraintSeries(Point point, Crac crac) {

        List<ConstraintSeries> constraintSeriesList = new ArrayList<>();

        /* Contingencies */
        // PREVENTIVE STATE
        constraintSeriesList.add(createAConstraintSeries("B57"));
        //TODO: delete this if no PRA
        constraintSeriesList.add(createAConstraintSeries("B56"));

        // AFTER CONTINGENCY
        crac.getContingencies().forEach(
            contingency ->
                createAllConstraintSeriesOfAContingency(contingency, constraintSeriesList));

        /* Monitored Elements*/
        List<ConstraintSeries> constraintSeriesListB57 = constraintSeriesList.stream().filter(constraintSeries -> constraintSeries.getBusinessType().equals("B57")).collect(Collectors.toList());
        List<ConstraintSeries> constraintSeriesListB56 = constraintSeriesList.stream().filter(constraintSeries -> constraintSeries.getBusinessType().equals("B56")).collect(Collectors.toList());

        crac.getCnecs().forEach(cnec -> findAndAddConstraintSeries(cnec, constraintSeriesListB57));

        point.constraintSeries = constraintSeriesList;
    }

    private static void findAndAddConstraintSeries(Cnec cnec, List<ConstraintSeries> constraintSeriesList) {

        List<MonitoredSeries> monitoredSeriesList = new ArrayList<>();
        // TODO: create MonitoredSeries
        createMonitoredSeriesFromCnec(cnec, monitoredSeriesList);

        Optional<Contingency> optionalContingency = cnec.getState().getContingency();
        if (optionalContingency.isPresent()) { // after a contingency
            Contingency contingency = optionalContingency.get();
            constraintSeriesList.stream().filter(
                constraintSeries ->
                    constraintSeries.getContingencySeries().stream().anyMatch(
                        contingencySeries -> contingencySeries.getMRID().equals(contingency.getId()))
            ).findFirst().orElseThrow(FaraoException::new).monitoredSeries = monitoredSeriesList;
        } else { // preventive
            constraintSeriesList.stream().filter(constraintSeries -> constraintSeries.getContingencySeries().isEmpty()).findFirst().orElseThrow(FaraoException::new).monitoredSeries = monitoredSeriesList;
        }
    }

    /*****************
     MONITORED ELEMENTS
     *****************/
    private static void createMonitoredSeriesFromCnec(Cnec cnec, List<MonitoredSeries> monitoredSeriesList) {
        // TODO: handle other units
        Unit unit = Unit.MEGAWATT;

        MonitoredSeries monitoredSeries = new MonitoredSeries();
        monitoredSeries.setMRID(cnec.getId());
        monitoredSeries.setName(cnec.getName());

        MonitoredRegisteredResource monitoredRegisteredResource = new MonitoredRegisteredResource();
        monitoredRegisteredResource.setMRID(createResourceIDString("A02", cnec.getNetworkElement().getId()));
        monitoredRegisteredResource.setName(cnec.getNetworkElement().getName());
        // TODO: origin and extremity from network?
        monitoredRegisteredResource.setInAggregateNodeMRID(createResourceIDString("A02", "in"));
        monitoredRegisteredResource.setOutAggregateNodeMRID(createResourceIDString("A02", "out"));

        List<Analog> measurementsList = new ArrayList<>();

        // TODO: handle flows
        // Thresholds
        if (cnec.getState().getInstant().equals(instants.get(0))) { // Before contingency
            cnec.getMaxThreshold(unit).ifPresent(threshold -> measurementsList.add(createMeasurement("A02", unit, threshold)));
        } else if (cnec.getState().getInstant().equals(instants.get(1))) { // After contingency, before any post-contingency RA
            cnec.getMaxThreshold(unit).ifPresent(threshold -> measurementsList.add(createMeasurement("A07", unit, threshold)));
        }  else if (cnec.getState().getInstant().equals(instants.get(2))) { // After contingency and automatic RA, before curative RA
            cnec.getMaxThreshold(unit).ifPresent(threshold -> measurementsList.add(createMeasurement("A12", unit, threshold)));
        } else { // After CRA
            cnec.getMaxThreshold(unit).ifPresent(threshold -> measurementsList.add(createMeasurement("A13", unit, threshold)));
        }

        monitoredRegisteredResource.measurements = measurementsList;
        monitoredSeries.registeredResource = Collections.singletonList(monitoredRegisteredResource);
        monitoredSeriesList.add(monitoredSeries);
    }

    private static Analog createMeasurement(String measurementType, Unit unit, double flow) {
        Analog measurement = new Analog();
        measurement.setMeasurementType(measurementType);

        if (unit.equals(Unit.MEGAWATT)) {
            measurement.setUnitSymbol("MAW");
        } else if (unit.equals(Unit.AMPERE)) {
            measurement.setUnitSymbol("AMP");
        } else {
            throw new FaraoException(String.format("Unhandled unit %s", unit.toString()));
        }

        if (flow < 0) {
            measurement.setPositiveFlowIn("A02");
        } else {
            measurement.setPositiveFlowIn("A01");
        }
        measurement.setAnalogValuesValue(Math.round(Math.abs(flow)));

        return measurement;
    }

    // Creation of ID with code scheme
    private static ResourceIDString createResourceIDString(String codingScheme, String value) {
        ResourceIDString resourceMRID = new ResourceIDString();
        resourceMRID.setCodingScheme(codingScheme);
        resourceMRID.setValue(value);
        return resourceMRID;
    }

    /*****************
     CONTINGENCIES
     *****************/
    private static void createAllConstraintSeriesOfAContingency(Contingency contingency, List<ConstraintSeries> constraintSeriesList) {
        constraintSeriesList.add(createAConstraintSeriesWithContingency("B57", contingency));
        constraintSeriesList.add(createAConstraintSeriesWithContingency("B56", contingency));
    }

    private static ConstraintSeries createAConstraintSeries(String businessType) {
        ConstraintSeries constraintSeries = new ConstraintSeries();
        constraintSeries.setMRID(generateRandomMRID());
        constraintSeries.setBusinessType(businessType);

        return constraintSeries;
    }

    private static ConstraintSeries createAConstraintSeriesWithContingency(String businessType, Contingency contingency) {
        ConstraintSeries constraintSeries = createAConstraintSeries(businessType);

        ContingencySeries contingencySeries = new ContingencySeries();
        contingencySeries.setMRID(contingency.getId());
        contingencySeries.setName(contingency.getName());

        constraintSeries.contingencySeries = Collections.singletonList(contingencySeries);
        return constraintSeries;
    }

    /*****************
     TIME_SERIES and REASON
     *****************/
    // TODO: get the correct network status (from CracResultExtension)
    private static void addSuccessReasonToPoint(Point point, CracResult.NetworkSecurityStatus status) {
        Reason reason = new Reason();
        if (status.equals(CracResult.NetworkSecurityStatus.SECURED)) {
            reason.setCode("Z13");
            reason.setText("Situation is secure");
        } else if (status.equals(CracResult.NetworkSecurityStatus.UNSECURED)) {
            reason.setCode("Z03");
            reason.setText("Situation is unsecure");
        } else {
            throw new FaraoException(String.format("Unexpected status %s.", status));
        }
        point.reason = Collections.singletonList(reason);
    }

    private static void addFailureReasonToPoint(Point point) {
        Reason reason = new Reason();
        reason.setCode("999");
        reason.setText("Other failure");
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
