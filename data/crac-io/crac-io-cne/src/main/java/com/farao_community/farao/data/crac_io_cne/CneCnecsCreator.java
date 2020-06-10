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
import com.farao_community.farao.data.crac_api.Unit;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.farao_community.farao.data.crac_io_cne.CneClassCreator.*;
import static com.farao_community.farao.data.crac_io_cne.CneClassCreator.newMonitoredSeries;
import static com.farao_community.farao.data.crac_io_cne.CneConstants.*;
import static com.farao_community.farao.data.crac_io_cne.CneConstants.ABS_MARG_TATL_MEASUREMENT_TYPE;
import static com.farao_community.farao.data.crac_io_cne.CneUtil.findNodeInNetwork;

/**
 * Creates the measurements, monitored registered resources and monitored series
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CneCnecsCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CneCnecsCreator.class);

    private CneCnecsCreator() {

    }

    static void createConstraintSeriesOfACnec(Cnec cnec, Network network, List<ConstraintSeries> constraintSeriesList, String measurementType, String preOptimVariantId, String postOptimVariantId) {

        /* Country of cnecs */
        Set<Country> countries = createCountries(network, cnec.getNetworkElement().getId());

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
            CneCnecsCreator.createB54B57Measurements(cnec, measurementType, postOptimVariantId, measurementsB54, measurementsB57);
            CneCnecsCreator.createB88Measurements(cnec, measurementType, preOptimVariantId, measurementsB88);

            MonitoredRegisteredResource monitoredRegisteredResourceB54 = createMonitoredRegisteredResource(cnec, network, measurementsB54);
            constraintSeriesB54.monitoredSeries.add(newMonitoredSeries(cnec.getId(), cnec.getName(), monitoredRegisteredResourceB54));

            MonitoredRegisteredResource monitoredRegisteredResourceB57 = createMonitoredRegisteredResource(cnec, network, measurementsB57);
            constraintSeriesB57.monitoredSeries.add(newMonitoredSeries(cnec.getId(), cnec.getName(), monitoredRegisteredResourceB57));

            MonitoredRegisteredResource monitoredRegisteredResourceB88 = createMonitoredRegisteredResource(cnec, network, measurementsB88);
            constraintSeriesB88.monitoredSeries.add(newMonitoredSeries(cnec.getId(), cnec.getName(), monitoredRegisteredResourceB88));

        } else {
            LOGGER.warn(String.format("Results of CNEC %s are not exported.", cnec.getName()));
        }

        /* Add constraint series to the list */
        constraintSeriesList.add(constraintSeriesB54);
        constraintSeriesList.add(constraintSeriesB57);
        constraintSeriesList.add(constraintSeriesB88);
    }

    private static Set<Country> createCountries(Network network, String networkElementId) {
        Line cnecLine = network.getLine(networkElementId);
        Set<Country> countries = new HashSet<>();

        if (cnecLine != null) {
            cnecLine.getTerminal1().getVoltageLevel().getSubstation().getCountry().ifPresent(countries::add);
            cnecLine.getTerminal2().getVoltageLevel().getSubstation().getCountry().ifPresent(countries::add);
        }

        return countries;
    }

    // B54 & B57
    private static void createB54B57Measurements(Cnec cnec, String measurementType, String postOptimVariantId, List<Analog> measurementsB54, List<Analog> measurementsB57) {
        // The check of the existence of the CnecResultExtension was done in another method
        CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);
        assert cnecResultExtension != null;

        CnecResult cnecResultPost = cnecResultExtension.getVariant(postOptimVariantId);
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
    private static void createB88Measurements(Cnec cnec, String measurementType, String preOptimVariantId, List<Analog> measurementsB88) {
        CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);
        // The check of the existence of the CnecResultExtension was done in another method
        assert cnecResultExtension != null;

        CnecResult cnecResultPre = cnecResultExtension.getVariant(preOptimVariantId);
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
    private static void addFlowThreshold(CnecResult cnecResult, Unit unit, String measurementType, List<Analog> measurements) {
        addFlowThreshold(cnecResult, unit, measurementType, measurements, measurements, true);
    }

    private static void addFlowThreshold(CnecResult cnecResult, Unit unit, String measurementType, List<Analog> measurementsPatl, List<Analog> measurementsTatl) {
        addFlowThreshold(cnecResult, unit, measurementType, measurementsPatl, measurementsTatl, false);
    }

    private static void addFlowThreshold(CnecResult cnecResult, Unit unit, String measurementType, List<Analog> measurementsPatl, List<Analog> measurementsTatl, boolean b88) {

        double flow;
        double threshold;
        // cnecResult is not null, checked before
        assert cnecResult != null;
        if (unit.equals(Unit.AMPERE)) {
            flow = cnecResult.getFlowInA();
            threshold = getClosestThresholdInA(cnecResult);
        } else if (unit.equals(Unit.MEGAWATT)) {
            flow = cnecResult.getFlowInMW();
            threshold = getClosestThresholdInMW(cnecResult);
        } else {
            throw new FaraoException(String.format(UNHANDLED_UNIT, unit.toString()));
        }

        if (!Double.isNaN(flow) && !Double.isNaN(threshold)) {
            measurementsPatl.add(newMeasurement(FLOW_MEASUREMENT_TYPE, unit, flow));
            if (b88) {
                measurementsTatl.add(newMeasurement(measurementType, unit, threshold));
            } else {
                measurementsTatl.add(newMeasurement(FLOW_MEASUREMENT_TYPE, unit, flow));
            }
        }

        String absMarginMeasType = computeAbsMarginMeasType(measurementType);
        addAbsMargins(absMarginMeasType, flow, threshold, unit, measurementsPatl, measurementsTatl);
    }

    private static double getClosestThresholdInA(CnecResult cnecResult) {
        double flow = Double.NaN;
        if (!Double.isNaN(cnecResult.getFlowInA())) {
            flow = cnecResult.getFlowInA();
            double maxThreshold = Double.POSITIVE_INFINITY;
            double minThreshold = Double.POSITIVE_INFINITY;
            if (!Double.isNaN(cnecResult.getMaxThresholdInA())) {
                maxThreshold = cnecResult.getMaxThresholdInA();
            }
            if (!Double.isNaN(cnecResult.getMinThresholdInA())) {
                minThreshold = cnecResult.getMinThresholdInA();
            }
            if (Math.abs(maxThreshold - flow) < Math.abs(minThreshold - flow)) {
                return maxThreshold;
            } else {
                return minThreshold;
            }
        }
        return flow;
    }

    private static double getClosestThresholdInMW(CnecResult cnecResult) {
        double flow = Double.NaN;
        if (!Double.isNaN(cnecResult.getFlowInMW())) {
            flow = cnecResult.getFlowInMW();
            double maxThreshold = Double.POSITIVE_INFINITY;
            double minThreshold = Double.POSITIVE_INFINITY;
            if (!Double.isNaN(cnecResult.getMaxThresholdInMW())) {
                maxThreshold = cnecResult.getMaxThresholdInMW();
            }
            if (!Double.isNaN(cnecResult.getMinThresholdInMW())) {
                minThreshold = cnecResult.getMinThresholdInMW();
            }
            if (Math.abs(maxThreshold - flow) < Math.abs(minThreshold - flow)) {
                return maxThreshold;
            } else {
                return minThreshold;
            }
        }
        return flow;
    }

    private static void addAbsMargins(String absMarginMeasType, double flow, double threshold, Unit unit, List<Analog> measurementsPatl, List<Analog> measurementsTatl) {
        double value = Math.abs(threshold) - Math.abs(flow);
        if (absMarginMeasType.equals(ABS_MARG_PATL_MEASUREMENT_TYPE)) {
            measurementsPatl.add(newMeasurement(absMarginMeasType, unit, value));
            measurementsPatl.add(newMeasurement(OBJ_FUNC_PATL_MEASUREMENT_TYPE, unit, -value));
        } else if (absMarginMeasType.equals(ABS_MARG_TATL_MEASUREMENT_TYPE)) {
            measurementsTatl.add(newMeasurement(absMarginMeasType, unit, value));
            measurementsTatl.add(newMeasurement(OBJ_FUNC_TATL_MEASUREMENT_TYPE, unit, -value));
        }
    }

    private static void addFrm(Cnec cnec, List<Analog> measurements) {
        if (cnec instanceof SimpleCnec && !Double.isNaN(((SimpleCnec) cnec).getFrm())) {
            measurements.add(newMeasurement(FRM_MEASUREMENT_TYPE, Unit.MEGAWATT, ((SimpleCnec) cnec).getFrm()));
        }
    }

    private static void addSumPtdf() {
        // TODO: develop this
    }

    private static void addLoopflow(CnecResult cnecResult, List<Analog> measurements) {
        if (!Double.isNaN(cnecResult.getLoopflowInMW()) && !Double.isNaN(cnecResult.getLoopflowThresholdInMW())) {
            measurements.add(newMeasurement(LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResult.getLoopflowInMW()));
            double threshold = Math.signum(cnecResult.getLoopflowInMW()) * cnecResult.getLoopflowThresholdInMW();
            measurements.add(newMeasurement(MAX_LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, threshold));
        }
    }

    public static String computeAbsMarginMeasType(String measurementType) {
        String absMarginMeasType = "";
        if (measurementType.equals(PATL_MEASUREMENT_TYPE)) {
            absMarginMeasType = ABS_MARG_PATL_MEASUREMENT_TYPE;
        } else if (measurementType.equals(TATL_MEASUREMENT_TYPE)) {
            absMarginMeasType = ABS_MARG_TATL_MEASUREMENT_TYPE;
        }
        return absMarginMeasType;
    }

    public static MonitoredRegisteredResource createMonitoredRegisteredResource(Cnec cnec, Network network, List<Analog> measurements) {
        String nodeOr = findNodeInNetwork(cnec.getNetworkElement().getId(), network, Branch.Side.ONE);
        String nodeEx = findNodeInNetwork(cnec.getNetworkElement().getId(), network, Branch.Side.TWO);

        return newMonitoredRegisteredResource(cnec.getId(), cnec.getName(), nodeOr, nodeEx, measurements);
    }
}
