/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_result_extensions.*;
import org.joda.time.DateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.security.SecureRandom;
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
    private static Map<State, ConstraintSeries> constraintSeriesMapB57 = new HashMap<>();
    private static Map<State, ConstraintSeries> constraintSeriesMapB56 = new HashMap<>();

    private CneFiller() { }

    public static CriticalNetworkElementMarketDocument getCne() {
        return cne;
    }

    public static void generate(Crac crac) {
        if (crac.isSynchronized()) {

            instants = crac.getInstants().stream().sorted(Comparator.comparing(Instant::getSeconds)).collect(Collectors.toList());

            fillHeader(crac.getNetworkDate());
            createTimeSeries(crac.getNetworkDate());
            Point point = cne.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

            if (crac.getExtension(CracResultExtension.class) != null) { // Computation ended
                CracResultExtension cracExtension = crac.getExtension(CracResultExtension.class);

                // TODO: Don't hardcode it, once it can be read from crac
                //String preOptimVariantId = "preOptimisationResults-2ad0d908-b660-48cf-9f1a-f3add0d6f005";
                String postOptimVariantId = "preOptimisationResults-eea6969d-0a58-4723-8320-0e06eafbed8e";

                addSuccessReasonToPoint(point, cracExtension.getVariant(postOptimVariantId).getNetworkSecurityStatus());
                createAllConstraintSeries(point, crac, postOptimVariantId);

            } else { // Failure of computation
                addFailureReasonToPoint(point);
            }
        } else {
            throw new FaraoException("Crac should be synchronized!");
        }
    }

    private static void createAllConstraintSeries(Point point, Crac crac, String postOptimVariantId) {

        List<ConstraintSeries> constraintSeriesList = new ArrayList<>();

        /* Contingencies */
        // PREVENTIVE STATE
        addConstraintToMapAndCne(createAConstraintSeries("B57"), constraintSeriesList, Collections.singleton(crac.getPreventiveState()));
        //TODO: delete this if no PRA
        addConstraintToMapAndCne(createAConstraintSeries("B56"), constraintSeriesList, Collections.singleton(crac.getPreventiveState()));

        // AFTER CONTINGENCY
        crac.getContingencies().forEach(
            contingency ->
                createAllConstraintSeriesOfAContingency(contingency, constraintSeriesList, crac.getStates(contingency)));

        /* Monitored Elements*/
        List<ConstraintSeries> constraintSeriesListB57 = constraintSeriesList.stream().filter(constraintSeries -> constraintSeries.getBusinessType().equals("B57")).collect(Collectors.toList());
        crac.getCnecs().forEach(cnec -> findAndAddConstraintSeries(cnec, constraintSeriesListB57, postOptimVariantId));

        /* Remedial Actions*/
        crac.getNetworkActions().forEach(networkAction -> findAndAddRemedialActions(networkAction, postOptimVariantId));
        crac.getRangeActions().forEach(rangeAction -> findAndAddRemedialActions(rangeAction, postOptimVariantId));

        point.constraintSeries = constraintSeriesList;
    }

    private static void findAndAddRemedialActions(RemedialAction remedialAction, String postOptimVariantId) {
        if (remedialAction.getExtension(NetworkActionResultExtension.class) != null) {
            addActivatedRemedialActionSeries(remedialAction, postOptimVariantId, constraintSeriesMapB56, false);
            addActivatedRemedialActionSeries(remedialAction, postOptimVariantId, constraintSeriesMapB57, false);
        }
        if (remedialAction.getExtension(RangeActionResultExtension.class) != null) {
            addActivatedRemedialActionSeries(remedialAction, postOptimVariantId, constraintSeriesMapB56, false);
            addActivatedRemedialActionSeries(remedialAction, postOptimVariantId, constraintSeriesMapB57, true);
        }
    }

    private static void addActivatedRemedialActionSeries(RemedialAction<?> remedialAction, String postOptimVariantId, Map<State, ConstraintSeries> constraintSeriesList, boolean createResource) {
        if (remedialAction instanceof NetworkAction) {
            addNetworkAction(remedialAction, constraintSeriesList, postOptimVariantId);
        } else if (remedialAction instanceof RangeAction) {
            addRangeAction(remedialAction, constraintSeriesList, postOptimVariantId, createResource);
        } else {
            throw new FaraoException(String.format("Remedial action %s of incorrect type", remedialAction.getId()));
        }
    }

    private static void addRangeAction(RemedialAction<?> remedialAction, Map<State, ConstraintSeries> constraintSeriesList, String postOptimVariantId, boolean createResource) {
        RangeAction rangeAction = (RangeAction) remedialAction;
        constraintSeriesList.forEach((state, constraintSeries) -> {
            RangeActionResult rangeActionResult = rangeAction.getExtension(RangeActionResultExtension.class).getVariant(postOptimVariantId);
            if (rangeActionResult.isActivated(state.getId())) {
                if (constraintSeries.remedialActionSeries == null) {
                    constraintSeries.remedialActionSeries = new ArrayList<>();
                }
                // TODO: check if setpoint has good value (angle VS tap)
                double setpoint = Math.ceil(rangeActionResult.getSetPoint(state.getId()));
                String rangeActionId = createRangeActionId(rangeAction.getId(), setpoint);
                RemedialActionSeries remedialActionSeries = createRemedialActionSeries(rangeAction, rangeActionId);
                if (createResource) {
                    remedialActionSeries.registeredResource = Collections.singletonList(createRemedialActionRegisteredResource(rangeAction, setpoint));
                }
                constraintSeries.remedialActionSeries.add(remedialActionSeries);
            }
        });
    }

    private static void addNetworkAction(RemedialAction<?> remedialAction, Map<State, ConstraintSeries> constraintSeriesList, String postOptimVariantId) {
        NetworkAction networkAction = (NetworkAction) remedialAction;
        constraintSeriesList.forEach((state, constraintSeries) -> {
            if (networkAction.getExtension(NetworkActionResultExtension.class).getVariant(postOptimVariantId).isActivated(state.getId())) {
                if (constraintSeries.remedialActionSeries == null) {
                    constraintSeries.remedialActionSeries = new ArrayList<>();
                }
                constraintSeries.remedialActionSeries.add(createRemedialActionSeries(networkAction, networkAction.getId()));
            }
        });
    }

    private static RemedialActionRegisteredResource createRemedialActionRegisteredResource(RangeAction rangeAction, double setpoint) {
        RemedialActionRegisteredResource remedialActionRegisteredResource = new RemedialActionRegisteredResource();
        if (rangeAction.getNetworkElements().size() == 1) {
            NetworkElement networkElement = rangeAction.getNetworkElements().stream().findFirst().orElseThrow(FaraoException::new);
            remedialActionRegisteredResource.setMRID(createResourceIDString("A01", networkElement.getId()));
            remedialActionRegisteredResource.setName(networkElement.getName());
            remedialActionRegisteredResource.setPSRTypePsrType("A06"); // PST range
            remedialActionRegisteredResource.setResourceCapacityDefaultCapacity(BigDecimal.valueOf(setpoint));
            remedialActionRegisteredResource.setResourceCapacityUnitSymbol("C62"); // without unit
            remedialActionRegisteredResource.setMarketObjectStatusStatus("A26"); // absolute
            return remedialActionRegisteredResource;
        } else {
            throw new FaraoException(String.format("Number of network elements is not 1 for range action %s", rangeAction.getId()));
        }
    }

    private static String createRangeActionId(String id, double setpoint) {
        return String.format("%s@%s@", id, setpoint);
    }

    private static RemedialActionSeries createRemedialActionSeries(RemedialAction<?> remedialAction, String id) {
        RemedialActionSeries remedialActionSeries = new RemedialActionSeries();
        remedialActionSeries.setMRID(id);
        remedialActionSeries.setName(remedialAction.getName());
        // TODO: deal with automatic RA (A20) and curative RA (A19)
        remedialActionSeries.setApplicationModeMarketObjectStatusStatus("A18");

        return remedialActionSeries;
    }

    private static void addConstraintToMapAndCne(ConstraintSeries constraintSeries, List<ConstraintSeries> constraintSeriesList, Set<State> states) {

        // add to map
        if (constraintSeries.getBusinessType().equals("B56")) {
            states.forEach(state -> constraintSeriesMapB56.put(state, constraintSeries));
        } else if (constraintSeries.getBusinessType().equals("B57")) {
            states.forEach(state -> constraintSeriesMapB57.put(state, constraintSeries));
        } else {
            throw new FaraoException(String.format("Unhandled businessType %s", constraintSeries.getBusinessType()));
        }

        // add to CNE
        constraintSeriesList.add(constraintSeries);
    }

    /*****************
     MONITORED ELEMENTS
     *****************/
    private static void findAndAddConstraintSeries(Cnec cnec, List<ConstraintSeries> constraintSeriesList, String postOptimVariantId) {

        List<MonitoredSeries> monitoredSeriesList = new ArrayList<>();
        createMonitoredSeriesFromCnec(cnec, monitoredSeriesList, postOptimVariantId);

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

    private static void createMonitoredSeriesFromCnec(Cnec cnec, List<MonitoredSeries> monitoredSeriesList, String postOptimVariantId) {
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
        Unit finalUnit = unit;

        // Flows
        if (cnec.getExtension(CnecResultExtension.class) != null) {
            CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);
            CnecResult cnecResult = cnecResultExtension.getVariant(postOptimVariantId);

            finalUnit = handleFlow(cnecResult, unit, measurementsList);
        }

        // Thresholds
        handleThresholds(cnec, finalUnit, measurementsList);

        monitoredRegisteredResource.measurements = measurementsList;
        monitoredSeries.registeredResource = Collections.singletonList(monitoredRegisteredResource);
        monitoredSeriesList.add(monitoredSeries);
    }

    // TODO: check how to do this correctly
    private static Unit handleFlow(CnecResult cnecResult, Unit unit, List<Analog> measurementsList) {
        Unit finalUnit = unit;
        if (unit.equals(Unit.AMPERE)) {
            if (Double.isNaN(cnecResult.getFlowInA()) && !Double.isNaN(cnecResult.getFlowInMW())) { // if the expected value is not defined, but another is defined
                finalUnit = Unit.MEGAWATT;
                measurementsList.add(createMeasurement("A01", unit, cnecResult.getFlowInMW()));
            } else { // normal case or case when nothing is defined
                measurementsList.add(createMeasurement("A01", unit, cnecResult.getFlowInA()));
            }
        } else if (unit.equals(Unit.MEGAWATT)) {
            if (Double.isNaN(cnecResult.getFlowInMW()) && !Double.isNaN(cnecResult.getFlowInA())) { // if the expected value is not defined, but another is defined
                finalUnit = Unit.AMPERE;
                measurementsList.add(createMeasurement("A01", unit, cnecResult.getFlowInA()));
            } else { // normal case or case when nothing is defined
                measurementsList.add(createMeasurement("A01", unit, cnecResult.getFlowInMW()));
            }
        } else {
            throw new FaraoException(String.format("Unhandled unit %s", unit.toString()));
        }
        return finalUnit;
    }

    private static void handleThresholds(Cnec cnec, Unit unit, List<Analog> measurementsList) {
        if (cnec.getState().getInstant().equals(instants.get(0))) { // Before contingency
            cnec.getMaxThreshold(unit).ifPresent(threshold -> measurementsList.add(createMeasurement("A02", unit, threshold)));
        } else if (cnec.getState().getInstant().equals(instants.get(1))) { // After contingency, before any post-contingency RA
            cnec.getMaxThreshold(unit).ifPresent(threshold -> measurementsList.add(createMeasurement("A07", unit, threshold)));
        }  else if (cnec.getState().getInstant().equals(instants.get(2))) { // After contingency and automatic RA, before curative RA
            cnec.getMaxThreshold(unit).ifPresent(threshold -> measurementsList.add(createMeasurement("A12", unit, threshold)));
        } else { // After CRA
            cnec.getMaxThreshold(unit).ifPresent(threshold -> measurementsList.add(createMeasurement("A13", unit, threshold)));
        }
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
    private static void createAllConstraintSeriesOfAContingency(Contingency contingency, List<ConstraintSeries> constraintSeriesList, SortedSet<State> states) {
        addConstraintToMapAndCne(createAConstraintSeriesWithContingency("B57", contingency), constraintSeriesList, states);
        addConstraintToMapAndCne(createAConstraintSeriesWithContingency("B56", contingency), constraintSeriesList, states);
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

    private static void createTimeSeries(DateTime networkDate) {

        try {
            Point point = new Point();
            point.setPosition(1);

            SeriesPeriod period = new SeriesPeriod();
            period.setTimeInterval(createEsmpDateTimeInterval(networkDate));
            period.setResolution(DatatypeFactory.newInstance().newDuration("PT60M"));
            period.point = Collections.singletonList(point);

            TimeSeries timeSeries = new TimeSeries();
            timeSeries.setMRID(generateRandomMRID());
            timeSeries.setBusinessType("B54");
            timeSeries.setCurveType("A01");
            timeSeries.period = Collections.singletonList(period);

            cne.timeSeries = Collections.singletonList(timeSeries);
        } catch (DatatypeConfigurationException e) {
            throw new FaraoException("Failure in TimeSeries creation");
        }
    }

    /*****************
     HEADER
     *****************/
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

    // Creation of time interval
    private static ESMPDateTimeInterval createEsmpDateTimeInterval(DateTime networkDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));

        ESMPDateTimeInterval timeInterval = new ESMPDateTimeInterval();

        timeInterval.setStart(dateFormat.format(networkDate.toDate()));
        timeInterval.setEnd(dateFormat.format(networkDate.plusHours(1).toDate()));
        return timeInterval;
    }

    // Creation of current date
    private static XMLGregorianCalendar createXMLGregorianCalendarNow() {
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
        Random random = new SecureRandom();
        return String.format("%s-%s-%s-%s", Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()), Integer.toHexString(random.nextInt()));
    }
}
