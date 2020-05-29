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
import org.apache.commons.math3.util.Pair;
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

    public Cne(Crac crac, Network network) {
        marketDocument = new CriticalNetworkElementMarketDocument();
        cneHelper = new CneHelper(crac, network);
    }

    public CriticalNetworkElementMarketDocument getMarketDocument() {
        return marketDocument;
    }

    /*****************
     GENERAL METHODS
     *****************/
    // Main method
    public void generate() {

        cneHelper.checkSynchronize();
        Crac crac = cneHelper.getCrac();

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
                createAllConstraintSeries(point, crac);
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
    private void createAllConstraintSeries(Point point, Crac crac) {

        List<ConstraintSeries> constraintSeriesList = new ArrayList<>();

        /* Contingencies */
        crac.getStates().forEach(state -> {
            if (state.getInstant().equals(cneHelper.getInstants().get(0)) || state.getInstant().equals(cneHelper.getInstants().get(1))) {
                cneHelper.addConstraintsToMap(state);
            }
        });

        /* Monitored Elements*/
        Set< Pair<String, String>> recordedCbco = new HashSet<>();
        crac.getCnecs().forEach(cnec -> cneHelper.addB88MonitoredSeriesToConstraintSeries(cnec, recordedCbco));

        /* Remedial Actions*/
        crac.getNetworkActions().forEach(this::addRemedialActionsToConstraintSeries);
        crac.getRangeActions().forEach(this::addRemedialActionsToConstraintSeries);

        constraintSeriesList.addAll(sortConstraintSeries(cneHelper.getConstraintSeriesMapB54()));
        constraintSeriesList.addAll(sortConstraintSeries(cneHelper.getConstraintSeriesMapB56()));
        constraintSeriesList.addAll(sortConstraintSeries(cneHelper.getConstraintSeriesMapB57()));
        constraintSeriesList.addAll(sortConstraintSeries(cneHelper.getConstraintSeriesMapB88()));
        point.constraintSeries = constraintSeriesList;
    }

    /* Remove some constraint series that are duplicated because they point on the same contingency but different states */
    private List<ConstraintSeries> sortConstraintSeries(Map<Pair<Optional<Contingency>, String>, ConstraintSeries> constraintSeriesMap) {
        List<ConstraintSeries> constraintSeriesList = new ArrayList<>();
        Set<Optional<Contingency>> contingencies = new HashSet<>();

        constraintSeriesMap.forEach((pair, constraintSeries) -> {
                if (!contingencies.contains(pair.getFirst())) {
                    contingencies.add(pair.getFirst());
                    constraintSeriesList.add(constraintSeries);
                }
            }
        );
        return constraintSeriesList;
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
    }*/




    /*****************
     REMEDIAL ACTIONS
     *****************/
    // Adds to the ConstraintSeries all RemedialActionSeries needed
    private void addRemedialActionsToConstraintSeries(RemedialAction remedialAction) {

        if (remedialAction.getExtension(NetworkActionResultExtension.class) != null) {
            addActivatedNetworkAction(remedialAction, cneHelper.getConstraintSeriesMapB54(), false);
            addActivatedNetworkAction(remedialAction, cneHelper.getConstraintSeriesMapB56(), false);
            addActivatedNetworkAction(remedialAction, cneHelper.getConstraintSeriesMapB57(), true);
        }
        if (remedialAction.getExtension(RangeActionResultExtension.class) != null) {
            addActivatedRangeAction(remedialAction, cneHelper.getConstraintSeriesMapB54(), false);
            addActivatedRangeAction(remedialAction, cneHelper.getConstraintSeriesMapB56(), false);
            addActivatedRangeAction(remedialAction, cneHelper.getConstraintSeriesMapB57(), true);
        }
    }

    // Adds to the ConstraintSeries a complete RemedialActionSeries created after a RangeAction
    private void addActivatedRangeAction(RemedialAction<?> remedialAction, Map<Pair<Optional<Contingency>, String>, ConstraintSeries> constraintSeriesList, boolean createDetails) {
        RangeAction rangeAction = (RangeAction) remedialAction;
        constraintSeriesList.forEach((pair, constraintSeries) -> {
            RangeActionResult preOptimRangeActionResult = rangeAction.getExtension(RangeActionResultExtension.class).getVariant(cneHelper.getPreOptimVariantId());
            RangeActionResult postOptimRangeActionResult = rangeAction.getExtension(RangeActionResultExtension.class).getVariant(cneHelper.getPostOptimVariantId());
            if (preOptimRangeActionResult != null && postOptimRangeActionResult != null
                && CneUtil.isActivated(pair.getSecond(), preOptimRangeActionResult, postOptimRangeActionResult)) {
                if (constraintSeries.remedialActionSeries == null) {
                    constraintSeries.remedialActionSeries = new ArrayList<>();
                }
                if (postOptimRangeActionResult instanceof PstRangeResult) {
                    int setpoint = ((PstRangeResult) postOptimRangeActionResult).getTap(pair.getSecond());
                    String rangeActionId = createRangeActionId(rangeAction.getId(), setpoint);
                    RemedialActionSeries remedialActionSeries = createRemedialActionSeries(rangeAction, rangeActionId);
                    if (createDetails) {
                        remedialActionSeries.partyMarketParticipant = Collections.singletonList(newPartyMarketParticipant(remedialAction.getOperator()));
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
    private void addActivatedNetworkAction(RemedialAction<?> remedialAction, Map<Pair<Optional<Contingency>, String>, ConstraintSeries> constraintSeriesList, boolean createDetails) {
        NetworkAction networkAction = (NetworkAction) remedialAction;
        constraintSeriesList.forEach((pair, constraintSeries) -> {
            if (networkAction.getExtension(NetworkActionResultExtension.class).getVariant(cneHelper.getPreOptimVariantId()) != null
                && networkAction.getExtension(NetworkActionResultExtension.class).getVariant(cneHelper.getPostOptimVariantId()) != null
                && CneUtil.isActivated(pair.getSecond(),
                networkAction.getExtension(NetworkActionResultExtension.class).getVariant(cneHelper.getPreOptimVariantId()),
                networkAction.getExtension(NetworkActionResultExtension.class).getVariant(cneHelper.getPostOptimVariantId()))) {
                if (constraintSeries.remedialActionSeries == null) {
                    constraintSeries.remedialActionSeries = new ArrayList<>();
                }
                RemedialActionSeries remedialActionSeries = createRemedialActionSeries(networkAction, networkAction.getId());
                if (createDetails) {
                    remedialActionSeries.partyMarketParticipant = Collections.singletonList(newPartyMarketParticipant(remedialAction.getOperator()));
                }
                constraintSeries.remedialActionSeries.add(remedialActionSeries);
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
