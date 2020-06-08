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
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.*;

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
            /* Country of cnecs */
            Line cnecLine = network.getLine(cnec.getNetworkElement().getId());
            Set<Country> countries = new HashSet<>();
            // check if the cnec is cross zonal
            if (cnecLine != null) {
                cnecLine.getTerminal1().getVoltageLevel().getSubstation().getCountry().ifPresent(countries::add);
                cnecLine.getTerminal2().getVoltageLevel().getSubstation().getCountry().ifPresent(countries::add);
            }

            /* Create Constraint series */
            ConstraintSeries constraintSeriesB54 = newConstraintSeries(cnec.getId(), B54_BUSINESS_TYPE, countries, OPTIMIZED_MARKET_STATUS);
            ConstraintSeries constraintSeriesB57 = newConstraintSeries(cnec.getId(), B57_BUSINESS_TYPE, countries, OPTIMIZED_MARKET_STATUS);
            ConstraintSeries constraintSeriesB88 = newConstraintSeries(cnec.getId(), B88_BUSINESS_TYPE, countries, OPTIMIZED_MARKET_STATUS);

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
                createB54B57Measurements(cnec, measurementsB54, measurementsB57);
                createB88Measurements(cnec, measurementsB88);

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
    private void createB54B57Measurements(Cnec cnec, List<Analog> measurementsB54, List<Analog> measurementsB57) {

        // The check of the existence of the CnecResultExtension was done in another method
        CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);
        String measurementType = cneHelper.instantToCodeConverter(cnec.getState().getInstant());

        CnecResult cnecResultPost = cnecResultExtension.getVariant(cneHelper.getPostOptimVariantId());
        if (cnecResultPost != null) {
            // Flow and threshold in A
            addFlowThreshold(cnecResultPost, Unit.AMPERE, measurementType, measurementsB54, measurementsB57);
            // Flow and threshold in MW
            addFlowThreshold(cnecResultPost, Unit.MEGAWATT, measurementType, measurementsB54, measurementsB57);

            // sumPTDF
            addSumPtdf();

            // loopflow
            addLoopflow(cnecResultPost, measurementsB54);
            addLoopflow(cnecResultPost, measurementsB57);
        }
    }

    // B88
    private void createB88Measurements(Cnec cnec, List<Analog> measurementsB88) {
        // The check of the existence of the CnecResultExtension was done in another method
        CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);
        String measurementType = cneHelper.instantToCodeConverter(cnec.getState().getInstant());

        CnecResult cnecResultPre = cnecResultExtension.getVariant(cneHelper.getPreOptimVariantId());
        if (cnecResultPre != null) {
            // Flow and threshold in A
            addFlowThreshold(cnecResultPre, Unit.AMPERE, measurementType, measurementsB88);
            // Flow and threshold in MW
            addFlowThreshold(cnecResultPre, Unit.MEGAWATT, measurementType, measurementsB88);
            // FRM
            addFrm(cnec, measurementsB88);

            // sumPTDF
            addSumPtdf();

            // loopflow
            addLoopflow(cnecResultPre, measurementsB88);
        }
    }

    // Flow and threshold
    private void addFlowThreshold(CnecResult cnecResult, Unit unit, String measurementType, List<Analog> measurements) {
        addFlowThreshold(cnecResult, unit, measurementType, measurements, measurements, true);
    }

    private void addFlowThreshold(CnecResult cnecResult, Unit unit, String measurementType, List<Analog> measurementsPatl, List<Analog> measurementsTatl) {
        addFlowThreshold(cnecResult, unit, measurementType, measurementsPatl, measurementsTatl, false);
    }

    private void addFlowThreshold(CnecResult cnecResult, Unit unit, String measurementType, List<Analog> measurementsPatl, List<Analog> measurementsTatl, boolean b88) {

        double flow;
        double threshold;
        // cnecResult is not null, checked before
        if (unit.equals(Unit.AMPERE)) {
            flow = cnecResult.getFlowInA();
            threshold = cnecResult.getMaxThresholdInA();
        } else if (unit.equals(Unit.MEGAWATT)) {
            flow = cnecResult.getFlowInMW();
            threshold = cnecResult.getLoopflowThresholdInMW();
        } else {
            throw new FaraoException(String.format(UNHANDLED_UNIT, unit.toString()));
        }

        String absMarginMeasType = computeAbsMarginMeasType(measurementType);
        if (!Double.isNaN(flow) && !Double.isNaN(threshold)) {
            measurementsPatl.add(newMeasurement(FLOW_MEASUREMENT_TYPE, unit, flow));
            if (b88) {
                measurementsTatl.add(newMeasurement(measurementType, unit, threshold));
            } else {
                measurementsTatl.add(newMeasurement(FLOW_MEASUREMENT_TYPE, unit, flow));
            }

            double value = threshold - flow;
            if (absMarginMeasType.equals(ABS_MARG_PATL_MEASUREMENT_TYPE)) {
                measurementsPatl.add(newMeasurement(absMarginMeasType, unit, value));
                measurementsPatl.add(newMeasurement(OBJ_FUNC_PATL_MEASUREMENT_TYPE, unit, -value));
            } else if (absMarginMeasType.equals(ABS_MARG_TATL_MEASUREMENT_TYPE)) {
                measurementsTatl.add(newMeasurement(absMarginMeasType, unit, value));
                measurementsTatl.add(newMeasurement(OBJ_FUNC_TATL_MEASUREMENT_TYPE, unit, -value));
            }
        }
    }

    private void addFrm(Cnec cnec, List<Analog> measurements) {
        if (cnec instanceof SimpleCnec && Double.isNaN(((SimpleCnec) cnec).getFrm())) {
            measurements.add(newMeasurement(FRM_MEASUREMENT_TYPE, Unit.MEGAWATT, ((SimpleCnec) cnec).getFrm()));
        }
    }

    private void addSumPtdf() {
        // TODO: develop this
    }

    private void addLoopflow(CnecResult cnecResult, List<Analog> measurements) {
        if (!Double.isNaN(cnecResult.getLoopflowInMW()) && !Double.isNaN(cnecResult.getLoopflowThresholdInMW())) {
            measurements.add(newMeasurement(LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResult.getLoopflowInMW()));
            measurements.add(newMeasurement(MAX_LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResult.getLoopflowThresholdInMW()));
        }
    }
}
