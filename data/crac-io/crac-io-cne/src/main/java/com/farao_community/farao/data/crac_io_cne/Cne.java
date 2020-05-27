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
import com.powsybl.iidm.network.Network;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.*;

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
    private CneHelper cneHelper;

    private static final Logger LOGGER = LoggerFactory.getLogger(Cne.class);

    public Cne() {
        marketDocument = new CriticalNetworkElementMarketDocument();
        cneHelper = new CneHelper();
    }

    public CriticalNetworkElementMarketDocument getMarketDocument() {
        return marketDocument;
    }

    /*****************
     GENERAL METHODS
     *****************/
    // Main method
    public void generate(Crac crac, Network network) {

        if (!crac.isSynchronized()) {
            crac.synchronize(network);
        }

        fillHeader(crac.getNetworkDate());
        addTimeSeriesToCne(crac.getNetworkDate());
        Point point = marketDocument.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

        if (crac.getExtension(CracResultExtension.class) != null && crac.getExtension(ResultVariantManager.class).getVariants() != null) { // Computation ended

            CracResultExtension cracExtension = crac.getExtension(CracResultExtension.class);

            // define preOptimVariant and postOptimVariant
            List<String> variants = new ArrayList<>(crac.getExtension(ResultVariantManager.class).getVariants());

            if (!variants.isEmpty()) {
                cneHelper.initializeAttributes(crac, cracExtension, variants);

                // fill CNE
                createAllConstraintSeries(point, crac, network);
                addSuccessReasonToPoint(point, cracExtension.getVariant(cneHelper.getPostOptimVariantId()).getNetworkSecurityStatus());
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
            marketDocument.timeSeries = Collections.singletonList(newTimeSeries(B54_BUSINESS_TYPE_TS, A01_CURVE_TYPE, period));
        } catch (DatatypeConfigurationException e) {
            throw new FaraoException("Failure in TimeSeries creation");
        }
    }

    /*****************
     CONSTRAINT_SERIES
     *****************/
    // Creates and fills all ConstraintSeries
    private void createAllConstraintSeries(Point point, Crac crac, Network network) {

        List<ConstraintSeries> constraintSeriesList = new ArrayList<>();

        /* Contingencies */
        // PREVENTIVE STATE
        cneHelper.addBasecaseConstraintsToMap();

        // AFTER CONTINGENCY
        crac.getContingencies().forEach(
            contingency -> cneHelper.addConstraintsToMap(contingency));

        /* Monitored Elements*/
        //List<ConstraintSeries> constraintSeriesListB57 = new ArrayList<>(constraintSeriesMapB57.values(), OPTIMIZED_MARKET_STATUS);
        //crac.getCnecs().forEach(cnec -> addCnecToConstraintSeries(cnec, constraintSeriesListB57, network));

        /* Remedial Actions*/
        //crac.getNetworkActions().forEach(this::addRemedialActionsToConstraintSeries);
        //crac.getRangeActions().forEach(this::addRemedialActionsToConstraintSeries);

        constraintSeriesList.addAll(cneHelper.getConstraintSeriesMapB54().values());
        constraintSeriesList.addAll(cneHelper.getConstraintSeriesMapB56().values());
        constraintSeriesList.addAll(cneHelper.getConstraintSeriesMapB57().values());
        constraintSeriesList.addAll(cneHelper.getConstraintSeriesMapB88().values());
        point.constraintSeries = constraintSeriesList;
    }

    /*****************
     MONITORED ELEMENTS
     *****************/
    // Adds to the ConstraintSeries all elements relative to a cnec
    /*private void addCnecToConstraintSeries(Cnec cnec, List<ConstraintSeries> constraintSeriesList, Network network) {

        List<MonitoredSeries> monitoredSeriesList = new ArrayList<>();
        monitoredSeriesList.add(createMonitoredSeriesFromCnec(cnec, network));

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
    private MonitoredSeries createMonitoredSeriesFromCnec(Cnec cnec, Network network) {

        // create measurements
        List<Analog> measurementsList = createMeasurements(cnec);
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



    // Creates all Measurements (flow and thresholds)
    private List<Analog> createMeasurements(Cnec cnec) {
        List<Analog> measurementsList = new ArrayList<>();

        if (cnec.getExtension(CnecResultExtension.class) != null) {
            CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);
            CnecResult cnecResult = cnecResultExtension.getVariant(cneHelper.getPostOptimVariantId());

            // Flows

            // Thresholds
            //handleThresholds(cnecResult, measurementsList, instantToCodeConverter(cnec.getState().getInstant()));
        }

        return measurementsList;
    }



    /*****************
     REMEDIAL ACTIONS
     *****************/
    // Adds to the ConstraintSeries all RemedialActionSeries needed
    /*private void addRemedialActionsToConstraintSeries(RemedialAction remedialAction) {

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
            RangeActionResult preOptimRangeActionResult = rangeAction.getExtension(RangeActionResultExtension.class).getVariant(cneHelper.getPreOptimVariantId());
            RangeActionResult postOptimRangeActionResult = rangeAction.getExtension(RangeActionResultExtension.class).getVariant(cneHelper.getPostOptimVariantId());
            if (preOptimRangeActionResult != null && postOptimRangeActionResult != null
                && CneUtil.isActivated(state.getId(), preOptimRangeActionResult, postOptimRangeActionResult)) {
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
            if (networkAction.getExtension(NetworkActionResultExtension.class).getVariant(cneHelper.getPreOptimVariantId()) != null
                && networkAction.getExtension(NetworkActionResultExtension.class).getVariant(cneHelper.getPostOptimVariantId()) != null
                && CneUtil.isActivated(state.getId(),
                networkAction.getExtension(NetworkActionResultExtension.class).getVariant(cneHelper.getPreOptimVariantId()),
                networkAction.getExtension(NetworkActionResultExtension.class).getVariant(cneHelper.getPostOptimVariantId()))) {
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

     */
}
