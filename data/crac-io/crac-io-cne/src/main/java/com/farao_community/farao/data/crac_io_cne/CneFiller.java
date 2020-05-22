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
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_io_cne.CneClassCreator.*;
import static com.farao_community.farao.data.crac_io_cne.CneUtil.*;
import static com.farao_community.farao.data.crac_io_cne.CneConstants.*;

/**
 * Fills the classes that constitute the CNE file structure
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CneFiller {

    private static CriticalNetworkElementMarketDocument cne = new CriticalNetworkElementMarketDocument();
    private static List<Instant> instants;
    private static Map<State, ConstraintSeries> constraintSeriesMapB57 = new HashMap<>();
    private static Map<State, ConstraintSeries> constraintSeriesMapB56 = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(CneFiller.class);

    private CneFiller() { }

    public static CriticalNetworkElementMarketDocument getCne() {
        return cne;
    }

    /*****************
     GENERAL METHODS
     *****************/
    // Main method
    public static void generate(Crac crac, Network network, Unit chosenExportUnit) {

        if (!crac.isSynchronized()) {
            crac.synchronize(network);
        }

        // sort the instants in order to determine which one is preventive, after outage, after auto RA and after CRA
        instants = crac.getInstants().stream().sorted(Comparator.comparing(Instant::getSeconds)).collect(Collectors.toList());

        fillHeader(crac.getNetworkDate());
        addTimeSeriesToCne(crac.getNetworkDate());
        Point point = cne.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

        if (crac.getExtension(CracResultExtension.class) != null && crac.getExtension(ResultVariantManager.class).getVariants() != null) { // Computation ended

            CracResultExtension cracExtension = crac.getExtension(CracResultExtension.class);

            String preOptimVariantId;
            String postOptimVariantId;

            // define preOptimVariant and postOptimVariant
            List<String> variants = new ArrayList<>(crac.getExtension(ResultVariantManager.class).getVariants());

            if (!variants.isEmpty()) {

                preOptimVariantId = variants.get(0);
                postOptimVariantId = variants.get(0);

                double minCost = cracExtension.getVariant(variants.get(0)).getCost();
                double maxCost = cracExtension.getVariant(variants.get(0)).getCost();
                for (String variant : variants) {
                    if (cracExtension.getVariant(variants.get(0)).getCost() < minCost) {
                        minCost = cracExtension.getVariant(variant).getCost();
                        postOptimVariantId = variant;
                    } else if (cracExtension.getVariant(variants.get(0)).getCost() > maxCost) {
                        maxCost = cracExtension.getVariant(variant).getCost();
                        preOptimVariantId = variant;
                    }
                }

                // fill CNE
                createAllConstraintSeries(point, crac, preOptimVariantId, postOptimVariantId, chosenExportUnit, network);
                addSuccessReasonToPoint(point, cracExtension.getVariant(postOptimVariantId).getNetworkSecurityStatus());
            } else {
                addFailureReasonToPoint(point);
                throw new FaraoException(String.format("Number of variants is %s (different from 2).", variants.size()));
            }
        } else { // Failure of computation
            addFailureReasonToPoint(point);
        }
    }

    /*****************
     HEADER
     *****************/
    // fills the header of the CNE
    private static void fillHeader(DateTime networkDate) {
        cne.setMRID(generateRandomMRID());
        cne.setRevisionNumber("1");
        cne.setType(CNE_TYPE);
        cne.setProcessProcessType(CNE_PROCESS_TYPE);
        cne.setSenderMarketParticipantMRID(createPartyIDString(A01_CODING_SCHEME, CNE_SENDER_MRID));
        cne.setSenderMarketParticipantMarketRoleType(CNE_SENDER_MARKET_ROLE_TYPE);
        cne.setReceiverMarketParticipantMRID(createPartyIDString(A01_CODING_SCHEME, CNE_RECEIVER_MRID));
        cne.setReceiverMarketParticipantMarketRoleType(CNE_RECEIVER_MARKET_ROLE_TYPE);
        cne.setCreatedDateTime(createXMLGregorianCalendarNow());
        cne.setTimePeriodTimeInterval(createEsmpDateTimeInterval(networkDate));
        cne.setDomainMRID(createAreaIDString(A01_CODING_SCHEME, DOMAIN_MRID));
    }

    /*****************
     TIME_SERIES and REASON
     *****************/
    // creates the Reason of a Point after the end of the rao
    private static void addSuccessReasonToPoint(Point point, CracResult.NetworkSecurityStatus status) {
        if (status.equals(CracResult.NetworkSecurityStatus.SECURED)) {
            point.reason = Collections.singletonList(newReason(SECURE_REASON_CODE, SECURE_REASON_TEXT));
        } else if (status.equals(CracResult.NetworkSecurityStatus.UNSECURED)) {
            point.reason = Collections.singletonList(newReason(UNSECURE_REASON_CODE, UNSECURE_REASON_TEXT));
        } else {
            throw new FaraoException(String.format("Unexpected status %s.", status));
        }
    }

    // creates the Reason of a Point after a failure
    private static void addFailureReasonToPoint(Point point) {
        point.reason = Collections.singletonList(newReason(OTHER_FAILURE_REASON_CODE, OTHER_FAILURE_REASON_TEXT));
    }

    // creates and adds the TimeSeries to the CNE
    private static void addTimeSeriesToCne(DateTime networkDate) {
        try {
            SeriesPeriod period = newPeriod(networkDate, SIXTY_MINUTES_DURATION, newPoint(1));
            cne.timeSeries = Collections.singletonList(newTimeSeries(B54_BUSINESS_TYPE, A01_CURVE_TYPE, period));
        } catch (DatatypeConfigurationException e) {
            throw new FaraoException("Failure in TimeSeries creation");
        }
    }

    /*****************
     CONSTRAINT_SERIES
     *****************/
    // Creates and fills all ConstraintSeries
    private static void createAllConstraintSeries(Point point, Crac crac, String preOptimVariantId, String postOptimVariantId, Unit chosenExportUnit, Network network) {

        List<ConstraintSeries> constraintSeriesList = new ArrayList<>();

        /* Contingencies */
        // PREVENTIVE STATE
        addConstraintToMapAndCne(newConstraintSeries(B57_BUSINESS_TYPE), constraintSeriesList, Collections.singleton(crac.getPreventiveState()));
        if (getNumberOfPra(crac, preOptimVariantId, postOptimVariantId) != 0) {
            addConstraintToMapAndCne(newConstraintSeries(B56_BUSINESS_TYPE), constraintSeriesList, Collections.singleton(crac.getPreventiveState()));
        }

        // AFTER CONTINGENCY
        crac.getContingencies().forEach(
            contingency -> createAllConstraintSeriesOfAContingency(contingency, constraintSeriesList, crac.getStates(contingency)));

        /* Monitored Elements*/
        List<ConstraintSeries> constraintSeriesListB57 = constraintSeriesList.stream().filter(constraintSeries -> constraintSeries.getBusinessType().equals(B57_BUSINESS_TYPE)).collect(Collectors.toList());
        crac.getCnecs().forEach(cnec -> addCnecToConstraintSeries(cnec, constraintSeriesListB57, postOptimVariantId, chosenExportUnit, network));

        /* Remedial Actions*/
        crac.getNetworkActions().forEach(networkAction -> addRemedialActionsToConstraintSeries(networkAction, preOptimVariantId, postOptimVariantId));
        crac.getRangeActions().forEach(rangeAction -> addRemedialActionsToConstraintSeries(rangeAction, preOptimVariantId, postOptimVariantId));

        point.constraintSeries = constraintSeriesList;
    }

    // Helper: fills maps (B56/B57) containing the constraint series corresponding to a state
    private static void addConstraintToMapAndCne(ConstraintSeries constraintSeries, List<ConstraintSeries> constraintSeriesList, Set<State> states) {

        // add to map
        if (constraintSeries.getBusinessType().equals(B56_BUSINESS_TYPE)) {
            states.forEach(state -> constraintSeriesMapB56.put(state, constraintSeries));
        } else if (constraintSeries.getBusinessType().equals(B57_BUSINESS_TYPE)) {
            states.forEach(state -> constraintSeriesMapB57.put(state, constraintSeries));
        } else {
            throw new FaraoException(String.format("Unhandled businessType %s", constraintSeries.getBusinessType()));
        }

        // add to CNE
        constraintSeriesList.add(constraintSeries);
    }

    /*****************
     CONTINGENCIES
     *****************/
    // Create  one B56 and one B57 ConstraintSeries relative to a contingency
    private static void createAllConstraintSeriesOfAContingency(Contingency contingency, List<ConstraintSeries> constraintSeriesList, SortedSet<State> states) {
        addConstraintToMapAndCne(newConstraintSeries(B57_BUSINESS_TYPE, newContingencySeries(contingency.getId(), contingency.getName())), constraintSeriesList, states);
        addConstraintToMapAndCne(newConstraintSeries(B56_BUSINESS_TYPE, newContingencySeries(contingency.getId(), contingency.getName())), constraintSeriesList, states);
    }

    /*****************
     MONITORED ELEMENTS
     *****************/
    // Adds to the ConstraintSeries all elements relative to a cnec
    private static void addCnecToConstraintSeries(Cnec cnec, List<ConstraintSeries> constraintSeriesList, String postOptimVariantId, Unit chosenExportUnit, Network network) {

        List<MonitoredSeries> monitoredSeriesList = new ArrayList<>();
        createMonitoredSeriesFromCnec(cnec, monitoredSeriesList, postOptimVariantId, chosenExportUnit, network);

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

    // Creates a MonitoredSeries from a given cnec
    private static void createMonitoredSeriesFromCnec(Cnec cnec, List<MonitoredSeries> monitoredSeriesList, String postOptimVariantId, Unit chosenExportUnit, Network network) {

        // create measurements
        List<Analog> measurementsList = createMeasurements(cnec, postOptimVariantId, chosenExportUnit);
        // add measurements to monitoredRegisteredResource
        MonitoredRegisteredResource monitoredRegisteredResource = createMonitoredRegisteredResource(cnec.getNetworkElement(), measurementsList, network);
        // add monitoredRegisteredResource to monitoredSeries
        MonitoredSeries monitoredSeries = newMonitoredSeries(cnec.getId(), cnec.getName(), monitoredRegisteredResource);
        monitoredSeriesList.add(monitoredSeries);
    }

    private static MonitoredRegisteredResource createMonitoredRegisteredResource(NetworkElement networkElement, List<Analog> measurementsList, Network network) {
        return newMonitoredRegisteredResource(networkElement.getId(),
            networkElement.getName(),
            findNodeInNetwork(networkElement.getId(), network, Branch.Side.ONE),
            findNodeInNetwork(networkElement.getId(), network, Branch.Side.TWO),
            measurementsList);
    }

    private static String findNodeInNetwork(String id, Network network, Branch.Side side) {
        try {
            return network.getBranch(id).getTerminal(side).getBusView().getBus().getId();
        } catch (NullPointerException e) {
            LOGGER.warn(e.toString());
            return network.getBranch(id).getTerminal(side).getBusView().getConnectableBus().getId();
        }
    }

    // Creates all Measurements (flow and thresholds)
    private static List<Analog> createMeasurements(Cnec cnec, String postOptimVariantId, Unit chosenExportUnit) {
        List<Analog> measurementsList = new ArrayList<>();

        Unit finalUnit = chosenExportUnit;

        // Flows
        if (cnec.getExtension(CnecResultExtension.class) != null) {
            CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);
            CnecResult cnecResult = cnecResultExtension.getVariant(postOptimVariantId);

            finalUnit = handleFlow(cnecResult, chosenExportUnit, measurementsList);
        }

        // Thresholds
        handleThresholds(cnec, finalUnit, measurementsList);

        return measurementsList;
    }

    // TODO: is this done correctly?
    // Creates a Measurement from flow
    private static Unit handleFlow(CnecResult cnecResult, Unit chosenExportUnit, List<Analog> measurementsList) {
        Unit finalUnit = chosenExportUnit;
        if (chosenExportUnit.equals(Unit.AMPERE)) {
            if (Double.isNaN(cnecResult.getFlowInA()) && !Double.isNaN(cnecResult.getFlowInMW())) { // if the expected value is not defined, but another is defined
                finalUnit = Unit.MEGAWATT;
                measurementsList.add(newMeasurement(FLOW_MEASUREMENT_TYPE, chosenExportUnit, cnecResult.getFlowInMW()));
            } else { // normal case or case when nothing is defined
                measurementsList.add(newMeasurement(FLOW_MEASUREMENT_TYPE, chosenExportUnit, cnecResult.getFlowInA()));
            }
        } else if (chosenExportUnit.equals(Unit.MEGAWATT)) {
            if (Double.isNaN(cnecResult.getFlowInMW()) && !Double.isNaN(cnecResult.getFlowInA())) { // if the expected value is not defined, but another is defined
                finalUnit = Unit.AMPERE;
                measurementsList.add(newMeasurement(FLOW_MEASUREMENT_TYPE, chosenExportUnit, cnecResult.getFlowInA()));
            } else { // normal case or case when nothing is defined
                measurementsList.add(newMeasurement(FLOW_MEASUREMENT_TYPE, chosenExportUnit, cnecResult.getFlowInMW()));
            }
        } else {
            throw new FaraoException(String.format("Unhandled unit %s", chosenExportUnit.toString()));
        }
        return finalUnit;
    }

    // Creates Measurements from thresholds
    private static void handleThresholds(Cnec cnec, Unit unit, List<Analog> measurementsList) {
        if (cnec.getState().getInstant().equals(instants.get(0))) { // Before contingency
            cnec.getMaxThreshold(unit).ifPresent(threshold -> measurementsList.add(newMeasurement(PATL_MEASUREMENT_TYPE, unit, threshold)));
        } else if (cnec.getState().getInstant().equals(instants.get(1))) { // After contingency, before any post-contingency RA
            cnec.getMaxThreshold(unit).ifPresent(threshold -> measurementsList.add(newMeasurement(TATL_MEASUREMENT_TYPE, unit, threshold)));
        }  else if (cnec.getState().getInstant().equals(instants.get(2))) { // After contingency and automatic RA, before curative RA
            cnec.getMaxThreshold(unit).ifPresent(threshold -> measurementsList.add(newMeasurement(TATL_AFTER_AUTO_MEASUREMENT_TYPE, unit, threshold)));
        } else { // After CRA
            cnec.getMaxThreshold(unit).ifPresent(threshold -> measurementsList.add(newMeasurement(TATL_AFTER_CRA_MEASUREMENT_TYPE, unit, threshold)));
        }
    }

    /*****************
     REMEDIAL ACTIONS
     *****************/
    // Adds to the ConstraintSeries all RemedialActionSeries needed
    private static void addRemedialActionsToConstraintSeries(RemedialAction remedialAction, String preOptimVariantId, String postOptimVariantId) {

        if (remedialAction.getExtension(NetworkActionResultExtension.class) != null) {
            addActivatedRemedialActionSeries(remedialAction, preOptimVariantId, postOptimVariantId, constraintSeriesMapB56, false);
            addActivatedRemedialActionSeries(remedialAction, preOptimVariantId, postOptimVariantId, constraintSeriesMapB57, false);
        }
        if (remedialAction.getExtension(RangeActionResultExtension.class) != null) {
            addActivatedRemedialActionSeries(remedialAction, preOptimVariantId, postOptimVariantId, constraintSeriesMapB56, false);
            addActivatedRemedialActionSeries(remedialAction, preOptimVariantId, postOptimVariantId, constraintSeriesMapB57, true);
        }
    }

    // Adds to the ConstraintSeries a complete RemedialActionSeries created after any RemedialAction
    private static void addActivatedRemedialActionSeries(RemedialAction<?> remedialAction, String preOptimVariantId, String postOptimVariantId, Map<State, ConstraintSeries> constraintSeriesList, boolean createResource) {
        if (remedialAction instanceof NetworkAction) {
            addActivatedNetworkAction(remedialAction, constraintSeriesList, postOptimVariantId);
        } else if (remedialAction instanceof RangeAction) {
            addActivatedRangeAction(remedialAction, constraintSeriesList, preOptimVariantId, postOptimVariantId, createResource);
        } else {
            throw new FaraoException(String.format("Remedial action %s of incorrect type", remedialAction.getId()));
        }
    }

    // Adds to the ConstraintSeries a complete RemedialActionSeries created after a RangeAction
    private static void addActivatedRangeAction(RemedialAction<?> remedialAction, Map<State, ConstraintSeries> constraintSeriesList, String preOptimVariantId, String postOptimVariantId, boolean createResource) {
        RangeAction rangeAction = (RangeAction) remedialAction;
        constraintSeriesList.forEach((state, constraintSeries) -> {
            RangeActionResultExtension rangeActionResultExtension = rangeAction.getExtension(RangeActionResultExtension.class);
            RangeActionResult postOptimRangeActionResult = rangeAction.getExtension(RangeActionResultExtension.class).getVariant(postOptimVariantId);
            if (postOptimRangeActionResult.isActivated(state.getId()) &&
                rangeActionResultExtension.getVariant(postOptimVariantId).getSetPoint(state.getId()) != rangeActionResultExtension.getVariant(preOptimVariantId).getSetPoint(state.getId())) {
                if (constraintSeries.remedialActionSeries == null) {
                    constraintSeries.remedialActionSeries = new ArrayList<>();
                }
                if (postOptimRangeActionResult instanceof PstRangeResult) {
                    int setpoint = ((PstRangeResult) postOptimRangeActionResult).getTap(state.getId());
                    String rangeActionId = createRangeActionId(rangeAction.getId(), setpoint);
                    RemedialActionSeries remedialActionSeries = createRemedialActionSeries(rangeAction, rangeActionId);
                    if (createResource) {
                        remedialActionSeries.registeredResource = Collections.singletonList(createPstRangeActionRegisteredResource(rangeAction, setpoint));
                    }
                    constraintSeries.remedialActionSeries.add(remedialActionSeries);
                } else {
                    throw new FaraoException(String.format("Range action is not PST range: %s not handled.", remedialAction.getId()));
                }
            }
        });
    }

    // Adds to the ConstraintSeries a complete RemedialActionSeries created after a NetworkAction
    private static void addActivatedNetworkAction(RemedialAction<?> remedialAction, Map<State, ConstraintSeries> constraintSeriesList, String postOptimVariantId) {
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

    // Creates a RegisteredResource from a PST range remedial action
    private static RemedialActionRegisteredResource createPstRangeActionRegisteredResource(RangeAction rangeAction, int tap) {
        if (rangeAction.getNetworkElements().size() == 1) {
            NetworkElement networkElement = rangeAction.getNetworkElements().stream().findFirst().orElseThrow(FaraoException::new);
            return newRemedialActionRegisteredResource(networkElement.getId(), networkElement.getName(), PST_RANGE_PSR_TYPE, tap, WITHOUT_UNIT_SYMBOL, ABSOLUTE_MARKET_OBJECT_STATUS);
        } else {
            throw new FaraoException(String.format("Number of network elements is not 1 for range action %s", rangeAction.getId()));
        }
    }

    // Creates a RemedialActionSeries from a remedial action
    private static RemedialActionSeries createRemedialActionSeries(RemedialAction<?> remedialAction, String id) {
        return newRemedialActionSeries(id, remedialAction.getName(), PREVENTIVE_MARKET_OBJECT_STATUS);
        // deal with automatic RA (A20) and curative RA (A19) once developed
    }
}
