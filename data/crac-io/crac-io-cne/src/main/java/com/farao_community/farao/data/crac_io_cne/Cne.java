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
public class Cne {

    private CriticalNetworkElementMarketDocument marketDocument;
    private List<Instant> instants;
    private Map<State, ConstraintSeries> constraintSeriesMapB57;
    private Map<State, ConstraintSeries> constraintSeriesMapB56;
    private String preOptimVariantId;
    private String postOptimVariantId;

    private static final Logger LOGGER = LoggerFactory.getLogger(Cne.class);

    public Cne() {
        marketDocument = new CriticalNetworkElementMarketDocument();
        constraintSeriesMapB57 = new HashMap<>();
        constraintSeriesMapB56 = new HashMap<>();
        preOptimVariantId = "";
        postOptimVariantId = "";
    }

    public CriticalNetworkElementMarketDocument getMarketDocument() {
        return marketDocument;
    }

    /*****************
     GENERAL METHODS
     *****************/
    // Main method
    public void generate(Crac crac, Network network, Unit chosenExportUnit) {

        if (!crac.isSynchronized()) {
            crac.synchronize(network);
        }

        // sort the instants in order to determine which one is preventive, after outage, after auto RA and after CRA
        instants = crac.getInstants().stream().sorted(Comparator.comparing(Instant::getSeconds)).collect(Collectors.toList());

        fillHeader(crac.getNetworkDate());
        addTimeSeriesToCne(crac.getNetworkDate());
        Point point = marketDocument.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

        if (crac.getExtension(CracResultExtension.class) != null && crac.getExtension(ResultVariantManager.class).getVariants() != null) { // Computation ended

            CracResultExtension cracExtension = crac.getExtension(CracResultExtension.class);

            // TODO: store the information on preOptim/postOptim Variant in the ResultVariantManager
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
                createAllConstraintSeries(point, crac, chosenExportUnit, network);
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
    private void fillHeader(DateTime networkDate) {
        marketDocument.setMRID(generateRandomMRID());
        marketDocument.setRevisionNumber("1");
        marketDocument.setType(CNE_TYPE);
        marketDocument.setProcessProcessType(CNE_PROCESS_TYPE);
        marketDocument.setSenderMarketParticipantMRID(createPartyIDString(A01_CODING_SCHEME, CNE_SENDER_MRID));
        marketDocument.setSenderMarketParticipantMarketRoleType(CNE_SENDER_MARKET_ROLE_TYPE);
        marketDocument.setReceiverMarketParticipantMRID(createPartyIDString(A01_CODING_SCHEME, CNE_RECEIVER_MRID));
        marketDocument.setReceiverMarketParticipantMarketRoleType(CNE_RECEIVER_MARKET_ROLE_TYPE);
        marketDocument.setCreatedDateTime(createXMLGregorianCalendarNow());
        marketDocument.setTimePeriodTimeInterval(createEsmpDateTimeInterval(networkDate));
        marketDocument.setDomainMRID(createAreaIDString(A01_CODING_SCHEME, DOMAIN_MRID));
    }

    /*****************
     TIME_SERIES and REASON
     *****************/
    // creates the Reason of a Point after the end of the rao
    private void addSuccessReasonToPoint(Point point, CracResult.NetworkSecurityStatus status) {
        if (status.equals(CracResult.NetworkSecurityStatus.SECURED)) {
            point.reason = Collections.singletonList(newReason(SECURE_REASON_CODE, SECURE_REASON_TEXT));
        } else if (status.equals(CracResult.NetworkSecurityStatus.UNSECURED)) {
            point.reason = Collections.singletonList(newReason(UNSECURE_REASON_CODE, UNSECURE_REASON_TEXT));
        } else {
            throw new FaraoException(String.format("Unexpected status %s.", status));
        }
    }

    // creates the Reason of a Point after a failure
    private void addFailureReasonToPoint(Point point) {
        point.reason = Collections.singletonList(newReason(OTHER_FAILURE_REASON_CODE, OTHER_FAILURE_REASON_TEXT));
    }

    // creates and adds the TimeSeries to the CNE
    private void addTimeSeriesToCne(DateTime networkDate) {
        try {
            SeriesPeriod period = newPeriod(networkDate, SIXTY_MINUTES_DURATION, newPoint(1));
            marketDocument.timeSeries = Collections.singletonList(newTimeSeries(B54_BUSINESS_TYPE, A01_CURVE_TYPE, period));
        } catch (DatatypeConfigurationException e) {
            throw new FaraoException("Failure in TimeSeries creation");
        }
    }

    /*****************
     CONSTRAINT_SERIES
     *****************/
    // Creates and fills all ConstraintSeries
    private void createAllConstraintSeries(Point point, Crac crac, Unit chosenExportUnit, Network network) {

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
        List<ConstraintSeries> constraintSeriesListB57 = new ArrayList<>(constraintSeriesMapB57.values());
        crac.getCnecs().forEach(cnec -> addCnecToConstraintSeries(cnec, constraintSeriesListB57, chosenExportUnit, network));

        /* Remedial Actions*/
        crac.getNetworkActions().forEach(this::addRemedialActionsToConstraintSeries);
        crac.getRangeActions().forEach(this::addRemedialActionsToConstraintSeries);

        point.constraintSeries = constraintSeriesList;
    }

    // Helper: fills maps (B56/B57) containing the constraint series corresponding to a state
    private void addConstraintToMapAndCne(ConstraintSeries constraintSeries, List<ConstraintSeries> constraintSeriesList, Set<State> states) {

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
    private void createAllConstraintSeriesOfAContingency(Contingency contingency, List<ConstraintSeries> constraintSeriesList, SortedSet<State> states) {
        addConstraintToMapAndCne(newConstraintSeries(B57_BUSINESS_TYPE, newContingencySeries(contingency.getId(), contingency.getName())), constraintSeriesList, states);
        addConstraintToMapAndCne(newConstraintSeries(B56_BUSINESS_TYPE, newContingencySeries(contingency.getId(), contingency.getName())), constraintSeriesList, states);
    }

    /*****************
     MONITORED ELEMENTS
     *****************/
    // Adds to the ConstraintSeries all elements relative to a cnec
    private void addCnecToConstraintSeries(Cnec cnec, List<ConstraintSeries> constraintSeriesList, Unit chosenExportUnit, Network network) {

        List<MonitoredSeries> monitoredSeriesList = new ArrayList<>();
        monitoredSeriesList.add(createMonitoredSeriesFromCnec(cnec, chosenExportUnit, network));

        Optional<Contingency> optionalContingency = cnec.getState().getContingency();
        if (optionalContingency.isPresent()) { // after a contingency
            Contingency contingency = optionalContingency.get();

            List<MonitoredSeries> monitoredSeriesOfConstraintSeries =
                constraintSeriesList.stream().filter(
                    constraintSeries -> constraintSeries.getContingencySeries().stream().anyMatch(
                        contingencySeries -> contingencySeries.getMRID().equals(contingency.getId()))
            ).findFirst().orElseThrow(FaraoException::new).monitoredSeries;

            if (monitoredSeriesOfConstraintSeries == null) {
                constraintSeriesList.stream().filter(
                    constraintSeries -> constraintSeries.getContingencySeries().stream().anyMatch(
                        contingencySeries -> contingencySeries.getMRID().equals(contingency.getId()))
                ).findFirst().orElseThrow(FaraoException::new).monitoredSeries = monitoredSeriesList;
            } else {
                constraintSeriesList.stream().filter(
                    constraintSeries -> constraintSeries.getContingencySeries().stream().anyMatch(
                        contingencySeries -> contingencySeries.getMRID().equals(contingency.getId()))
                ).findFirst().orElseThrow(FaraoException::new).monitoredSeries.addAll(monitoredSeriesList);
            }
        } else { // preventive
            constraintSeriesList.stream().filter(constraintSeries -> constraintSeries.getContingencySeries().isEmpty()).findFirst().orElseThrow(FaraoException::new).monitoredSeries = monitoredSeriesList;
        }
    }

    // Creates a MonitoredSeries from a given cnec
    private MonitoredSeries createMonitoredSeriesFromCnec(Cnec cnec, Unit chosenExportUnit, Network network) {

        // create measurements
        List<Analog> measurementsList = createMeasurements(cnec, chosenExportUnit);
        // add measurements to monitoredRegisteredResource
        MonitoredRegisteredResource monitoredRegisteredResource = createMonitoredRegisteredResource(cnec.getNetworkElement(), measurementsList, network);
        // add monitoredRegisteredResource to monitoredSeries
        return newMonitoredSeries(cnec.getId(), cnec.getName(), monitoredRegisteredResource);
    }

    private MonitoredRegisteredResource createMonitoredRegisteredResource(NetworkElement networkElement, List<Analog> measurementsList, Network network) {
        return newMonitoredRegisteredResource(networkElement.getId(),
            networkElement.getName(),
            findNodeInNetwork(networkElement.getId(), network, Branch.Side.ONE),
            findNodeInNetwork(networkElement.getId(), network, Branch.Side.TWO),
            measurementsList);
    }

    private String findNodeInNetwork(String id, Network network, Branch.Side side) {
        try {
            return network.getBranch(id).getTerminal(side).getBusView().getBus().getId();
        } catch (NullPointerException e) {
            LOGGER.warn(e.toString());
            return network.getBranch(id).getTerminal(side).getBusView().getConnectableBus().getId();
        }
    }

    // Creates all Measurements (flow and thresholds)
    private List<Analog> createMeasurements(Cnec cnec, Unit chosenExportUnit) {
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
    private Unit handleFlow(CnecResult cnecResult, Unit chosenExportUnit, List<Analog> measurementsList) {
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
    private void handleThresholds(Cnec cnec, Unit unit, List<Analog> measurementsList) {
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
    private void addRemedialActionsToConstraintSeries(RemedialAction remedialAction) {

        if (remedialAction.getExtension(NetworkActionResultExtension.class) != null) {
            addActivatedNetworkAction(remedialAction, constraintSeriesMapB56);
            addActivatedNetworkAction(remedialAction, constraintSeriesMapB57);
        }
        if (remedialAction.getExtension(RangeActionResultExtension.class) != null) {
            addActivatedRangeAction(remedialAction, constraintSeriesMapB56, false);
            addActivatedRangeAction(remedialAction, constraintSeriesMapB57, true);
        }
    }

    // Adds to the ConstraintSeries a complete RemedialActionSeries created after a RangeAction
    private void addActivatedRangeAction(RemedialAction<?> remedialAction, Map<State, ConstraintSeries> constraintSeriesList, boolean createResource) {
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
    private void addActivatedNetworkAction(RemedialAction<?> remedialAction, Map<State, ConstraintSeries> constraintSeriesList) {
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
    private RemedialActionRegisteredResource createPstRangeActionRegisteredResource(RangeAction rangeAction, int tap) {
        if (rangeAction.getNetworkElements().size() == 1) {
            NetworkElement networkElement = rangeAction.getNetworkElements().stream().findFirst().orElseThrow(FaraoException::new);
            return newRemedialActionRegisteredResource(networkElement.getId(), networkElement.getName(), PST_RANGE_PSR_TYPE, tap, WITHOUT_UNIT_SYMBOL, ABSOLUTE_MARKET_OBJECT_STATUS);
        } else {
            throw new FaraoException(String.format("Number of network elements is not 1 for range action %s", rangeAction.getId()));
        }
    }

    // Creates a RemedialActionSeries from a remedial action
    private RemedialActionSeries createRemedialActionSeries(RemedialAction<?> remedialAction, String id) {
        return newRemedialActionSeries(id, remedialAction.getName(), PREVENTIVE_MARKET_OBJECT_STATUS);
        // deal with automatic RA (A20) and curative RA (A19) once developed
    }
}
