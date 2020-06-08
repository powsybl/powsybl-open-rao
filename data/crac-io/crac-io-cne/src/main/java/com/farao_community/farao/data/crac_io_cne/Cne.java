/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Unit;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.data.crac_io_cne.CneClassCreator.*;
import static com.farao_community.farao.data.crac_io_cne.CneConstants.*;
import static com.farao_community.farao.data.crac_io_cne.CneUtil.*;

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

        DateTime networkDate = cneHelper.getCrac().getNetworkDate();
        fillHeader(networkDate);
        addTimeSeriesToCne(networkDate);
        Point point = marketDocument.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);
        cneHelper.initializeAttributes();

        // fill CNE
        createAllConstraintSeries(point);
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
     TIME_SERIES
     *****************/
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
    private void createAllConstraintSeries(Point point) {

        Crac crac = cneHelper.getCrac();
        Network network = cneHelper.getNetwork();

        List<ConstraintSeries> constraintSeriesList = new ArrayList<>();

        crac.getCnecs().forEach(cnec -> {
            // TODO: replace HU by the country of the cnec
            ConstraintSeries constraintSeriesB54 = newConstraintSeries(cnec.getId(), B54_BUSINESS_TYPE, "HU", OPTIMIZED_MARKET_STATUS);
            ConstraintSeries constraintSeriesB57 = newConstraintSeries(cnec.getId(), B57_BUSINESS_TYPE, "HU", OPTIMIZED_MARKET_STATUS);
            ConstraintSeries constraintSeriesB88 = newConstraintSeries(cnec.getId(), B88_BUSINESS_TYPE, "HU", OPTIMIZED_MARKET_STATUS);

            /* Add contingency if exists */
            Optional<Contingency> optionalContingency = cnec.getState().getContingency();
            if (optionalContingency.isPresent()) {
                ContingencySeries contingencySeries = newContingencySeries(optionalContingency.get().getId(), optionalContingency.get().getName());
                constraintSeriesB54.contingencySeries.add(contingencySeries);
                constraintSeriesB57.contingencySeries.add(contingencySeries);
                constraintSeriesB88.contingencySeries.add(contingencySeries);
            }

            /* Add critical network element */
            List<Analog> measurementsB54 = new ArrayList<>();
            List<Analog> measurementsB57 = new ArrayList<>();
            List<Analog> measurementsB88 = new ArrayList<>();

            CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);
            if (cnecResultExtension != null) {
                addB54B57(cnec, measurementsB54, measurementsB57);
                addB88(cnec, measurementsB88);

                MonitoredRegisteredResource monitoredRegisteredResourceB54 = newMonitoredRegisteredResource(cnec.getId(), cnec.getName(), findNodeInNetwork(cnec.getNetworkElement().getId(), network, Branch.Side.ONE), findNodeInNetwork(cnec.getNetworkElement().getId(), network, Branch.Side.TWO), measurementsB54);
                constraintSeriesB54.monitoredSeries.add(newMonitoredSeries(cnec.getId(), cnec.getName(), monitoredRegisteredResourceB54));

                MonitoredRegisteredResource monitoredRegisteredResourceB57 = newMonitoredRegisteredResource(cnec.getId(), cnec.getName(), findNodeInNetwork(cnec.getNetworkElement().getId(), network, Branch.Side.ONE), findNodeInNetwork(cnec.getNetworkElement().getId(), network, Branch.Side.TWO), measurementsB57);
                constraintSeriesB57.monitoredSeries.add(newMonitoredSeries(cnec.getId(), cnec.getName(), monitoredRegisteredResourceB57));

                MonitoredRegisteredResource monitoredRegisteredResourceB88 = newMonitoredRegisteredResource(cnec.getId(), cnec.getName(), findNodeInNetwork(cnec.getNetworkElement().getId(), network, Branch.Side.ONE), findNodeInNetwork(cnec.getNetworkElement().getId(), network, Branch.Side.TWO), measurementsB88);
                constraintSeriesB88.monitoredSeries.add(newMonitoredSeries(cnec.getId(), cnec.getName(), monitoredRegisteredResourceB88));

            }

            /* Add constraint series to the list */
            constraintSeriesList.add(constraintSeriesB54);
            constraintSeriesList.add(constraintSeriesB57);
            constraintSeriesList.add(constraintSeriesB88);
        });

        /* Add all constraint series to the CNE */
        point.constraintSeries = constraintSeriesList;
    }

    // B54 & B57
    private void addB54B57(Cnec cnec, List<Analog> measurementsB54, List<Analog> measurementsB57) {

        // The check of the existence of the CnecResultExtension was done in another method
        CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);

        CnecResult cnecResultPost = cnecResultExtension.getVariant(cneHelper.getPostOptimVariantId());
        if (cnecResultPost != null) {
            // Flow and threshold in A
            if (!Double.isNaN(cnecResultPost.getFlowInA()) && !Double.isNaN(cnecResultPost.getMaxThresholdInA())) {
                measurementsB54.add(newMeasurement(FLOW_MEASUREMENT_TYPE, Unit.AMPERE, cnecResultPost.getFlowInA()));
                measurementsB57.add(newMeasurement(FLOW_MEASUREMENT_TYPE, Unit.AMPERE, cnecResultPost.getFlowInA()));
                String measurementType = cneHelper.instantToCodeConverter(cnec.getState().getInstant());

                String absMarginMeasType = computeAbsMarginMeasType(measurementType);
                double value = cnecResultPost.getMaxThresholdInA() - cnecResultPost.getFlowInA();
                if (absMarginMeasType.equals(ABS_MARG_PATL_MEASUREMENT_TYPE)) {
                    measurementsB54.add(newMeasurement(absMarginMeasType, Unit.AMPERE, value));
                    measurementsB54.add(newMeasurement(OBJ_FUNC_PATL_MEASUREMENT_TYPE, Unit.AMPERE, -value));
                } else if (absMarginMeasType.equals(ABS_MARG_TATL_MEASUREMENT_TYPE)) {
                    measurementsB57.add(newMeasurement(absMarginMeasType, Unit.AMPERE, value));
                    measurementsB57.add(newMeasurement(OBJ_FUNC_TATL_MEASUREMENT_TYPE, Unit.AMPERE, -value));
                }
            }
            // Flow and threshold in MW
            if (!Double.isNaN(cnecResultPost.getFlowInMW()) && !Double.isNaN(cnecResultPost.getMaxThresholdInMW())) {
                measurementsB54.add(newMeasurement(FLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResultPost.getFlowInMW()));
                measurementsB57.add(newMeasurement(FLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResultPost.getFlowInMW()));
                String measurementType = cneHelper.instantToCodeConverter(cnec.getState().getInstant());

                String absMarginMeasType = computeAbsMarginMeasType(measurementType);
                double value = cnecResultPost.getMaxThresholdInMW() - cnecResultPost.getFlowInMW();
                if (absMarginMeasType.equals(ABS_MARG_PATL_MEASUREMENT_TYPE)) {
                    measurementsB54.add(newMeasurement(absMarginMeasType, Unit.MEGAWATT, value));
                    measurementsB54.add(newMeasurement(OBJ_FUNC_PATL_MEASUREMENT_TYPE, Unit.MEGAWATT, -value));
                } else if (absMarginMeasType.equals(ABS_MARG_TATL_MEASUREMENT_TYPE)) {
                    measurementsB57.add(newMeasurement(absMarginMeasType, Unit.MEGAWATT, value));
                    measurementsB57.add(newMeasurement(OBJ_FUNC_TATL_MEASUREMENT_TYPE, Unit.MEGAWATT, -value));
                }
            }
            // TODO: sumPTDF

            // loopflow
            if (!Double.isNaN(cnecResultPost.getLoopflowInMW()) && !Double.isNaN(cnecResultPost.getLoopflowThresholdInMW())) {
                measurementsB54.add(newMeasurement(LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResultPost.getLoopflowInMW()));
                measurementsB54.add(newMeasurement(MAX_LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResultPost.getLoopflowThresholdInMW()));
                measurementsB57.add(newMeasurement(LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResultPost.getLoopflowInMW()));
                measurementsB57.add(newMeasurement(MAX_LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResultPost.getLoopflowThresholdInMW()));
            }
        }
    }

    // B88
    private void addB88(Cnec cnec, List<Analog> measurementsB88) {
        // The check of the existence of the CnecResultExtension was done in another method
        CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);

        CnecResult cnecResultPre = cnecResultExtension.getVariant(cneHelper.getPreOptimVariantId());
        if (cnecResultPre != null) {
            // Flow and threshold in A
            if (!Double.isNaN(cnecResultPre.getFlowInA()) && !Double.isNaN(cnecResultPre.getMaxThresholdInA())) {
                measurementsB88.add(newMeasurement(FLOW_MEASUREMENT_TYPE, Unit.AMPERE, cnecResultPre.getFlowInA()));
                String measurementType = cneHelper.instantToCodeConverter(cnec.getState().getInstant());
                measurementsB88.add(newMeasurement(measurementType, Unit.AMPERE, cnecResultPre.getMaxThresholdInA()));

                String absMarginMeasType = computeAbsMarginMeasType(measurementType);
                double value = cnecResultPre.getMaxThresholdInA() - cnecResultPre.getFlowInA();
                if (absMarginMeasType.equals(ABS_MARG_PATL_MEASUREMENT_TYPE)) {
                    measurementsB88.add(newMeasurement(absMarginMeasType, Unit.AMPERE, value));
                    measurementsB88.add(newMeasurement(OBJ_FUNC_PATL_MEASUREMENT_TYPE, Unit.AMPERE, -value));
                } else if (absMarginMeasType.equals(ABS_MARG_TATL_MEASUREMENT_TYPE)) {
                    measurementsB88.add(newMeasurement(absMarginMeasType, Unit.AMPERE, value));
                    measurementsB88.add(newMeasurement(OBJ_FUNC_TATL_MEASUREMENT_TYPE, Unit.AMPERE, -value));
                }
            }
            // Flow and threshold in MW
            if (!Double.isNaN(cnecResultPre.getFlowInMW()) && !Double.isNaN(cnecResultPre.getMaxThresholdInMW())) {
                measurementsB88.add(newMeasurement(FLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResultPre.getFlowInMW()));
                String measurementType = cneHelper.instantToCodeConverter(cnec.getState().getInstant());
                measurementsB88.add(newMeasurement(cneHelper.instantToCodeConverter(cnec.getState().getInstant()), Unit.MEGAWATT, cnecResultPre.getMaxThresholdInMW()));

                String absMarginMeasType = computeAbsMarginMeasType(measurementType);
                double value = cnecResultPre.getMaxThresholdInMW() - cnecResultPre.getFlowInMW();
                if (absMarginMeasType.equals(ABS_MARG_PATL_MEASUREMENT_TYPE)) {
                    measurementsB88.add(newMeasurement(absMarginMeasType, Unit.MEGAWATT, value));
                    measurementsB88.add(newMeasurement(OBJ_FUNC_PATL_MEASUREMENT_TYPE, Unit.MEGAWATT, -value));
                } else if (absMarginMeasType.equals(ABS_MARG_TATL_MEASUREMENT_TYPE)) {
                    measurementsB88.add(newMeasurement(absMarginMeasType, Unit.MEGAWATT, value));
                    measurementsB88.add(newMeasurement(OBJ_FUNC_TATL_MEASUREMENT_TYPE, Unit.MEGAWATT, -value));
                }
            }
            // FRM
            if (cnec instanceof SimpleCnec && Double.isNaN(((SimpleCnec) cnec).getFrm())) {
                measurementsB88.add(newMeasurement(FRM_MEASUREMENT_TYPE, Unit.MEGAWATT, ((SimpleCnec) cnec).getFrm()));
            }

            // TODO: sumPTDF

            // loopflow
            if (!Double.isNaN(cnecResultPre.getLoopflowInMW()) && !Double.isNaN(cnecResultPre.getLoopflowThresholdInMW())) {
                measurementsB88.add(newMeasurement(LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResultPre.getLoopflowInMW()));
                measurementsB88.add(newMeasurement(MAX_LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResultPre.getLoopflowThresholdInMW()));
            }
        }
    }
}
