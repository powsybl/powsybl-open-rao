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
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
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
            }
        } else { // Failure of computation
            LOGGER.warn("Failure of computation");
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
        // post contingency
        crac.getContingencies().forEach(contingency -> {
            if (!cneHelper.getConstraintSeriesMap().containsKey(contingency)) {
                ConstraintSeries constraintSeries = newConstraintSeries(newContingencySeries(contingency.getId(), contingency.getName()));
                cneHelper.addToConstraintSeriesMap(contingency, constraintSeries);
            }
        });
        // basecase
        cneHelper.addToConstraintSeriesMap(cneHelper.getBasecase(), new ConstraintSeries());

        /* Monitored Elements*/
        Network network = cneHelper.getNetwork();
        crac.getCnecs().forEach(cnec -> {
            // find the corresponding constraint series
            Optional<Contingency> optionalContingency = cnec.getState().getContingency();
            ConstraintSeries constraintSeries;
            if (optionalContingency.isPresent()) {
                constraintSeries = cneHelper.getConstraintSeriesMap().get(optionalContingency.get());
            } else {
                constraintSeries = cneHelper.getConstraintSeriesMap().get(cneHelper.getBasecase());
            }

            // initialize monitoredSeries if not done already
            if (constraintSeries.monitoredSeries == null) {
                constraintSeries.monitoredSeries = new ArrayList<>();
            }

            Optional<MonitoredSeries> optionalMonitoredSeries = constraintSeries.monitoredSeries.stream().filter(monitoredSeries -> monitoredSeriesContainsACnec(monitoredSeries, cnec)).findFirst();
            if (optionalMonitoredSeries.isPresent()) {
                if (optionalMonitoredSeries.get().getMRID().equals(cnec.getNetworkElement().getId())) {
                    // TODO: complete the monitoredSeries && check if it really worked

                }
            } else {
                List<Analog> measurements = new ArrayList<>();

                CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);
                if (cnecResultExtension != null) {
                    CnecResult cnecResultPost = cnecResultExtension.getVariant(cneHelper.getPostOptimVariantId());
                    if (cnecResultPost != null) {
                        // Flow and threshold in A
                        if (!Double.isNaN(cnecResultPost.getFlowInA()) && !Double.isNaN(cnecResultPost.getMaxThresholdInA())) {
                            measurements.add(newMeasurement(FLOW_MEASUREMENT_TYPE, Unit.AMPERE, cnecResultPost.getFlowInA()));
                            String measurementType = cneHelper.instantToCodeConverter(cnec.getState().getInstant());
                            measurements.add(newMeasurement(measurementType, Unit.AMPERE, cnecResultPost.getMaxThresholdInA()));

                            String absMarginMeasType = CneUtil.computeAbsMarginMeasType(measurementType);
                            double value = cnecResultPost.getMaxThresholdInA() - cnecResultPost.getFlowInA();
                            if (absMarginMeasType.equals(ABS_MARG_PATL_MEASUREMENT_TYPE)) {
                                measurements.add(newMeasurement(absMarginMeasType, Unit.AMPERE, value));
                                measurements.add(newMeasurement(OBJ_FUNC_PATL_MEASUREMENT_TYPE, Unit.AMPERE, -value));
                            } else if (absMarginMeasType.equals(ABS_MARG_TATL_MEASUREMENT_TYPE)) {
                                measurements.add(newMeasurement(absMarginMeasType, Unit.AMPERE, value));
                                measurements.add(newMeasurement(OBJ_FUNC_TATL_MEASUREMENT_TYPE, Unit.AMPERE, -value));
                            }
                        }
                        // Flow and threshold in MW
                        if (!Double.isNaN(cnecResultPost.getFlowInMW()) && !Double.isNaN(cnecResultPost.getMaxThresholdInMW())) {
                            measurements.add(newMeasurement(FLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResultPost.getFlowInMW()));
                            String measurementType = cneHelper.instantToCodeConverter(cnec.getState().getInstant());
                            measurements.add(newMeasurement(cneHelper.instantToCodeConverter(cnec.getState().getInstant()), Unit.MEGAWATT, cnecResultPost.getMaxThresholdInMW()));

                            String absMarginMeasType = CneUtil.computeAbsMarginMeasType(measurementType);
                            double value = cnecResultPost.getMaxThresholdInMW() - cnecResultPost.getFlowInMW();
                            if (absMarginMeasType.equals(ABS_MARG_PATL_MEASUREMENT_TYPE)) {
                                measurements.add(newMeasurement(absMarginMeasType, Unit.MEGAWATT, value));
                                measurements.add(newMeasurement(OBJ_FUNC_PATL_MEASUREMENT_TYPE, Unit.MEGAWATT, -value));
                            } else if (absMarginMeasType.equals(ABS_MARG_TATL_MEASUREMENT_TYPE)) {
                                measurements.add(newMeasurement(absMarginMeasType, Unit.MEGAWATT, value));
                                measurements.add(newMeasurement(OBJ_FUNC_TATL_MEASUREMENT_TYPE, Unit.MEGAWATT, -value));
                            }
                        }
                        // FRM
                        if (cnec instanceof SimpleCnec && Double.isNaN(((SimpleCnec) cnec).getFrm())) {
                            measurements.add(newMeasurement(FRM_MEASUREMENT_TYPE, Unit.MEGAWATT, ((SimpleCnec) cnec).getFrm()));
                        }

                        // TODO: sumPTDF

                        // loopflow
                        if (!Double.isNaN(cnecResultPost.getLoopflowInMW()) && !Double.isNaN(cnecResultPost.getLoopflowThresholdInMW())) {
                            measurements.add(newMeasurement(LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResultPost.getLoopflowInMW()));
                            measurements.add(newMeasurement(MAX_LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResultPost.getLoopflowThresholdInMW()));
                        }
                    }
                }

                MonitoredRegisteredResource monitoredRegisteredResource = newMonitoredRegisteredResource(cnec.getId(), cnec.getName(), findNodeInNetwork(cnec.getNetworkElement().getId(), network, Branch.Side.ONE), findNodeInNetwork(cnec.getNetworkElement().getId(), network, Branch.Side.TWO), measurements);
                constraintSeries.monitoredSeries.add(newMonitoredSeries(cnec.getId(), cnec.getName(), monitoredRegisteredResource));
            }
        });

        /* Remedial Actions*/

        point.constraintSeries = constraintSeriesList;
    }

    private boolean monitoredSeriesContainsACnec(MonitoredSeries monitoredSeries, Cnec cnec) {
        return monitoredSeries.getMRID().equals(cnec.getId());
    }
}
