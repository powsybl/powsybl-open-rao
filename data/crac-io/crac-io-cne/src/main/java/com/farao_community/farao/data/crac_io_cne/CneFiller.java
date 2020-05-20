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
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_io_cne.CneUtil.*;
import static com.farao_community.farao.data.crac_io_cne.CneConstants.*;

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

    /*****************
     GENERAL METHODS
     *****************/
    // Main method
    public static void generate(Crac crac, Unit chosenExportUnit) {
        if (crac.isSynchronized()) {

            instants = crac.getInstants().stream().sorted(Comparator.comparing(Instant::getSeconds)).collect(Collectors.toList());

            fillHeader(crac.getNetworkDate());
            addTimeSeriesToCne(crac.getNetworkDate());
            Point point = cne.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

            if (crac.getExtension(CracResultExtension.class) != null) { // Computation ended

                Set<String> variants = crac.getExtension(ResultVariantManager.class).getVariants();

                if (variants != null && variants.size() != 2) {
                    throw new FaraoException(String.format("Number of variants is %s (different from 2).", variants.size()));
                }

                assert variants != null;
                List<String> variantList = new ArrayList<>(variants);
                CracResultExtension cracExtension = crac.getExtension(CracResultExtension.class);

                String preOptimVariantId;
                String postOptimVariantId;
                if (cracExtension.getVariant(variantList.get(0)).getCost() > cracExtension.getVariant(variantList.get(1)).getCost()) {
                    preOptimVariantId = variantList.get(0);
                    postOptimVariantId = variantList.get(1);
                } else {
                    preOptimVariantId = variantList.get(1);
                    postOptimVariantId = variantList.get(0);
                }

                addSuccessReasonToPoint(point, cracExtension.getVariant(postOptimVariantId).getNetworkSecurityStatus());
                createAllConstraintSeries(point, crac, preOptimVariantId, postOptimVariantId, chosenExportUnit);

            } else { // Failure of computation
                addFailureReasonToPoint(point);
            }
        } else {
            throw new FaraoException("Crac should be synchronized!");
        }
    }

    // Creates all ConstraintSeries
    private static void createAllConstraintSeries(Point point, Crac crac, String preOptimVariantId, String postOptimVariantId, Unit chosenExportUnit) {

        List<ConstraintSeries> constraintSeriesList = new ArrayList<>();

        /* Contingencies */
        // PREVENTIVE STATE
        addConstraintToMapAndCne(createAConstraintSeries(B57_BUSINESS_TYPE), constraintSeriesList, Collections.singleton(crac.getPreventiveState()));
        if (getNumberOfPra(crac, preOptimVariantId, postOptimVariantId) != 0) {
            addConstraintToMapAndCne(createAConstraintSeries(B56_BUSINESS_TYPE), constraintSeriesList, Collections.singleton(crac.getPreventiveState()));
        }

        // AFTER CONTINGENCY
        crac.getContingencies().forEach(
            contingency ->
                createAllConstraintSeriesOfAContingency(contingency, constraintSeriesList, crac.getStates(contingency)));

        /* Monitored Elements*/
        List<ConstraintSeries> constraintSeriesListB57 = constraintSeriesList.stream().filter(constraintSeries -> constraintSeries.getBusinessType().equals(B57_BUSINESS_TYPE)).collect(Collectors.toList());
        crac.getCnecs().forEach(cnec -> addCnecToConstraintSeries(cnec, constraintSeriesListB57, postOptimVariantId, chosenExportUnit));

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
        Reason reason = new Reason();
        if (status.equals(CracResult.NetworkSecurityStatus.SECURED)) {
            reason.setCode(SECURE_REASON_CODE);
            reason.setText(SECURE_REASON_TEXT);
        } else if (status.equals(CracResult.NetworkSecurityStatus.UNSECURED)) {
            reason.setCode(UNSECURE_REASON_CODE);
            reason.setText(UNSECURE_REASON_TEXT);
        } else {
            throw new FaraoException(String.format("Unexpected status %s.", status));
        }
        point.reason = Collections.singletonList(reason);
    }

    // creates the Reason of a Point after a failure
    private static void addFailureReasonToPoint(Point point) {
        Reason reason = new Reason();
        reason.setCode(OTHER_FAILURE_REASON_CODE);
        reason.setText(OTHER_FAILURE_REASON_TEXT);
        point.reason = Collections.singletonList(reason);
    }

    // creates and adds the TimeSeries to the CNE
    private static void addTimeSeriesToCne(DateTime networkDate) {
        try {
            Point point = new Point();
            point.setPosition(1);

            SeriesPeriod period = new SeriesPeriod();
            period.setTimeInterval(createEsmpDateTimeInterval(networkDate));
            period.setResolution(DatatypeFactory.newInstance().newDuration(SIXTY_MINUTES_DURATION));
            period.point = Collections.singletonList(point);

            TimeSeries timeSeries = new TimeSeries();
            timeSeries.setMRID(generateRandomMRID());
            timeSeries.setBusinessType(B54_BUSINESS_TYPE);
            timeSeries.setCurveType(A01_CURVE_TYPE);
            timeSeries.period = Collections.singletonList(period);

            cne.timeSeries = Collections.singletonList(timeSeries);
        } catch (DatatypeConfigurationException e) {
            throw new FaraoException("Failure in TimeSeries creation");
        }
    }

    /*****************
     CONTINGENCIES
     *****************/
    // Create  one B56 and one B57 ConstraintSeries relative to a contingency
    private static void createAllConstraintSeriesOfAContingency(Contingency contingency, List<ConstraintSeries> constraintSeriesList, SortedSet<State> states) {
        addConstraintToMapAndCne(createAConstraintSeriesWithContingency(B57_BUSINESS_TYPE, contingency), constraintSeriesList, states);
        addConstraintToMapAndCne(createAConstraintSeriesWithContingency(B56_BUSINESS_TYPE, contingency), constraintSeriesList, states);
    }

    // Create an empty ConstraintSeries
    private static ConstraintSeries createAConstraintSeries(String businessType) {
        ConstraintSeries constraintSeries = new ConstraintSeries();
        constraintSeries.setMRID(generateRandomMRID());
        constraintSeries.setBusinessType(businessType);

        return constraintSeries;
    }

    // Create a ConstraintSeries containing a ContingencySeries
    private static ConstraintSeries createAConstraintSeriesWithContingency(String businessType, Contingency contingency) {
        ConstraintSeries constraintSeries = createAConstraintSeries(businessType);

        ContingencySeries contingencySeries = new ContingencySeries();
        contingencySeries.setMRID(contingency.getId());
        contingencySeries.setName(contingency.getName());

        constraintSeries.contingencySeries = Collections.singletonList(contingencySeries);
        return constraintSeries;
    }

    /*****************
     MONITORED ELEMENTS
     *****************/
    // Adds to the ConstraintSeries all elements relative to a cnec
    private static void addCnecToConstraintSeries(Cnec cnec, List<ConstraintSeries> constraintSeriesList, String postOptimVariantId, Unit chosenExportUnit) {

        List<MonitoredSeries> monitoredSeriesList = new ArrayList<>();
        createMonitoredSeriesFromCnec(cnec, monitoredSeriesList, postOptimVariantId, chosenExportUnit);

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
    private static void createMonitoredSeriesFromCnec(Cnec cnec, List<MonitoredSeries> monitoredSeriesList, String postOptimVariantId, Unit chosenExportUnit) {
        MonitoredSeries monitoredSeries = new MonitoredSeries();
        monitoredSeries.setMRID(cnec.getId());
        monitoredSeries.setName(cnec.getName());

        MonitoredRegisteredResource monitoredRegisteredResource = new MonitoredRegisteredResource();
        monitoredRegisteredResource.setMRID(createResourceIDString(A02_CODING_SCHEME, cnec.getNetworkElement().getId()));
        monitoredRegisteredResource.setName(cnec.getNetworkElement().getName());
        // TODO: origin and extremity from network?
        monitoredRegisteredResource.setInAggregateNodeMRID(createResourceIDString(A02_CODING_SCHEME, "in"));
        monitoredRegisteredResource.setOutAggregateNodeMRID(createResourceIDString(A02_CODING_SCHEME, "out"));

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

        monitoredRegisteredResource.measurements = measurementsList;
        monitoredSeries.registeredResource = Collections.singletonList(monitoredRegisteredResource);
        monitoredSeriesList.add(monitoredSeries);
    }

    // TODO: is this done correctly?
    // Creates a Measurement from flow
    private static Unit handleFlow(CnecResult cnecResult, Unit chosenExportUnit, List<Analog> measurementsList) {
        Unit finalUnit = chosenExportUnit;
        if (chosenExportUnit.equals(Unit.AMPERE)) {
            if (Double.isNaN(cnecResult.getFlowInA()) && !Double.isNaN(cnecResult.getFlowInMW())) { // if the expected value is not defined, but another is defined
                finalUnit = Unit.MEGAWATT;
                measurementsList.add(createMeasurement(FLOW_MEASUREMENT_TYPE, chosenExportUnit, cnecResult.getFlowInMW()));
            } else { // normal case or case when nothing is defined
                measurementsList.add(createMeasurement(FLOW_MEASUREMENT_TYPE, chosenExportUnit, cnecResult.getFlowInA()));
            }
        } else if (chosenExportUnit.equals(Unit.MEGAWATT)) {
            if (Double.isNaN(cnecResult.getFlowInMW()) && !Double.isNaN(cnecResult.getFlowInA())) { // if the expected value is not defined, but another is defined
                finalUnit = Unit.AMPERE;
                measurementsList.add(createMeasurement(FLOW_MEASUREMENT_TYPE, chosenExportUnit, cnecResult.getFlowInA()));
            } else { // normal case or case when nothing is defined
                measurementsList.add(createMeasurement(FLOW_MEASUREMENT_TYPE, chosenExportUnit, cnecResult.getFlowInMW()));
            }
        } else {
            throw new FaraoException(String.format("Unhandled unit %s", chosenExportUnit.toString()));
        }
        return finalUnit;
    }

    // Creates Measurements from thresholds
    private static void handleThresholds(Cnec cnec, Unit unit, List<Analog> measurementsList) {
        if (cnec.getState().getInstant().equals(instants.get(0))) { // Before contingency
            cnec.getMaxThreshold(unit).ifPresent(threshold -> measurementsList.add(createMeasurement(PATL_MEASUREMENT_TYPE, unit, threshold)));
        } else if (cnec.getState().getInstant().equals(instants.get(1))) { // After contingency, before any post-contingency RA
            cnec.getMaxThreshold(unit).ifPresent(threshold -> measurementsList.add(createMeasurement(TATL_MEASUREMENT_TYPE, unit, threshold)));
        }  else if (cnec.getState().getInstant().equals(instants.get(2))) { // After contingency and automatic RA, before curative RA
            cnec.getMaxThreshold(unit).ifPresent(threshold -> measurementsList.add(createMeasurement(TATL_AFTER_AUTO_MEASUREMENT_TYPE, unit, threshold)));
        } else { // After CRA
            cnec.getMaxThreshold(unit).ifPresent(threshold -> measurementsList.add(createMeasurement(TATL_AFTER_CRA_MEASUREMENT_TYPE, unit, threshold)));
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
        RemedialActionRegisteredResource remedialActionRegisteredResource = new RemedialActionRegisteredResource();
        if (rangeAction.getNetworkElements().size() == 1) {
            NetworkElement networkElement = rangeAction.getNetworkElements().stream().findFirst().orElseThrow(FaraoException::new);
            remedialActionRegisteredResource.setMRID(createResourceIDString(A01_CODING_SCHEME, networkElement.getId()));
            remedialActionRegisteredResource.setName(networkElement.getName());
            remedialActionRegisteredResource.setPSRTypePsrType(PST_RANGE_PSR_TYPE);
            remedialActionRegisteredResource.setResourceCapacityDefaultCapacity(BigDecimal.valueOf(tap));
            remedialActionRegisteredResource.setResourceCapacityUnitSymbol(WITHOUT_UNIT_SYMBOL);
            remedialActionRegisteredResource.setMarketObjectStatusStatus(ABSOLUTE_MARKET_OBJECT_STATUS);
            return remedialActionRegisteredResource;
        } else {
            throw new FaraoException(String.format("Number of network elements is not 1 for range action %s", rangeAction.getId()));
        }
    }

    // Creates a RemedialActionSeries from a remedial action
    private static RemedialActionSeries createRemedialActionSeries(RemedialAction<?> remedialAction, String id) {
        RemedialActionSeries remedialActionSeries = new RemedialActionSeries();
        remedialActionSeries.setMRID(id);
        remedialActionSeries.setName(remedialAction.getName());
        remedialActionSeries.setApplicationModeMarketObjectStatusStatus(PREVENTIVE_MARKET_OBJECT_STATUS);
        // deal with automatic RA (A20) and curative RA (A19) once developed

        return remedialActionSeries;
    }
}
