/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
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
import static com.farao_community.farao.data.crac_io_cne.CneConstants.*;
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

    static void createConstraintSeriesOfACnec(BranchCnec cnec, CneHelper cneHelper, List<ConstraintSeries> constraintSeriesList, boolean relativePositiveMargins) {

        Network network = cneHelper.getNetwork();
        String preOptimVariantId = cneHelper.getPreOptimVariantId();
        String postOptimVariantId = cneHelper.getPostOptimVariantId();

        /* Country of cnecs */
        Set<Country> countries = createCountries(network, cnec.getNetworkElement().getId());

        /* Create Constraint series */
        String marketStatus = cnec.isOptimized() ? OPTIMIZED_MARKET_STATUS : MONITORED_MARKET_STATUS; // TO DO : separate CNECs from MNECs
        ConstraintSeries constraintSeriesB88 = newConstraintSeries(cnec.getId(), B88_BUSINESS_TYPE, countries, marketStatus);
        ConstraintSeries constraintSeriesB57 = newConstraintSeries(cnec.getId(), B57_BUSINESS_TYPE, countries, marketStatus);
        ConstraintSeries constraintSeriesB54 = newConstraintSeries(cnec.getId(), B54_BUSINESS_TYPE, countries, marketStatus);

        /* Add contingency if exists */
        Optional<Contingency> optionalContingency = cnec.getState().getContingency();
        if (optionalContingency.isPresent()) {
            ContingencySeries contingencySeries = newContingencySeries(optionalContingency.get().getId(), optionalContingency.get().getName());
            constraintSeriesB88.contingencySeries.add(contingencySeries);
            constraintSeriesB57.contingencySeries.add(contingencySeries);
            constraintSeriesB54.contingencySeries.add(contingencySeries);
        }

        /* Add critical network element */
        List<Analog> measurementsB88 = new ArrayList<>();
        List<Analog> measurementsB57 = new ArrayList<>();
        List<Analog> measurementsB54 = new ArrayList<>();

        CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);
        if (cnecResultExtension != null) {
            CneCnecsCreator.createB88Measurements(cnec, preOptimVariantId, measurementsB88, relativePositiveMargins);
            CneCnecsCreator.createB57Measurements(cnec, preOptimVariantId, postOptimVariantId, measurementsB57, relativePositiveMargins);
            CneCnecsCreator.createB54Measurements(cnec, preOptimVariantId, postOptimVariantId, measurementsB54, relativePositiveMargins);

            MonitoredRegisteredResource monitoredRegisteredResourceB88 = createMonitoredRegisteredResource(cnec, network, measurementsB88);
            constraintSeriesB88.monitoredSeries.add(newMonitoredSeries(cnec.getId(), cnec.getName(), monitoredRegisteredResourceB88));

            MonitoredRegisteredResource monitoredRegisteredResourceB57 = createMonitoredRegisteredResource(cnec, network, measurementsB57);
            constraintSeriesB57.monitoredSeries.add(newMonitoredSeries(cnec.getId(), cnec.getName(), monitoredRegisteredResourceB57));

            MonitoredRegisteredResource monitoredRegisteredResourceB54 = createMonitoredRegisteredResource(cnec, network, measurementsB54);
            constraintSeriesB54.monitoredSeries.add(newMonitoredSeries(cnec.getId(), cnec.getName(), monitoredRegisteredResourceB54));
        } else {
            String warningMsg = String.format("Results of CNEC %s are not exported.", cnec.getName());
            LOGGER.warn(warningMsg);
        }

        // Sort the measurements
        Collections.sort(measurementsB88, new AnalogComparator());
        Collections.sort(measurementsB57, new AnalogComparator());
        Collections.sort(measurementsB54, new AnalogComparator());

        /* Add constraint series to the list */
        constraintSeriesList.add(constraintSeriesB88);
        constraintSeriesList.add(constraintSeriesB57);
        constraintSeriesList.add(constraintSeriesB54);
    }

    private static class AnalogComparator implements Comparator<Analog> {
        @Override
        public int compare(Analog o1, Analog o2) {
            if (o1.getMeasurementType().equals(o2.getMeasurementType())) {
                return o1.getUnitSymbol().compareTo(o2.getUnitSymbol());
            } else {
                return o1.getMeasurementType().compareTo(o2.getMeasurementType());
            }
        }
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

    private static void createB88Measurements(BranchCnec cnec, String preOptimVariantId, List<Analog> measurements, boolean relativePositiveMargins) {
        createB88B57B54Measurements(cnec, preOptimVariantId, preOptimVariantId, measurements, relativePositiveMargins, true, true);
    }

    private static void createB57Measurements(BranchCnec cnec, String preOptimVariantId, String postOptimVariantId, List<Analog> measurements, boolean relativePositiveMargins) {
        createB88B57B54Measurements(cnec, preOptimVariantId, postOptimVariantId, measurements, relativePositiveMargins, false, true);
    }

    private static void createB54Measurements(BranchCnec cnec, String preOptimVariantId, String postOptimVariantId, List<Analog> measurements, boolean relativePositiveMargins) {
        createB88B57B54Measurements(cnec, preOptimVariantId, postOptimVariantId, measurements, relativePositiveMargins, true, false);
    }

    private static void createB88B57B54Measurements(BranchCnec cnec, String preOptimVariantId, String variantId, List<Analog> measurements, boolean relativePositiveMargins, boolean withPatl, boolean withTatl) {
        CnecResultExtension cnecResultExtension = cnec.getExtension(CnecResultExtension.class);
        // The check of the existence of the CnecResultExtension was done in another method
        assert cnecResultExtension != null;

        CnecResult initialCnecResult = cnecResultExtension.getVariant(preOptimVariantId);
        CnecResult cnecResult = cnecResultExtension.getVariant(variantId);
        if (cnecResult == null) {
            return;
        }
        // Z11
        double absPtdfSum = addSumPtdf(initialCnecResult, measurements);
        for (Unit unit : List.of(Unit.AMPERE, Unit.MEGAWATT)) {
            // A01
            double flow = addFlow(cnecResult, unit, measurements);
            if (withPatl) {
                // A02
                double patlThreshold = addThreshold(cnecResult, unit, PATL_MEASUREMENT_TYPE, measurements);
                // Z12
                double patlMargin = addMargin(flow, patlThreshold, unit, ABS_MARG_PATL_MEASUREMENT_TYPE, measurements);
                // Z13
                addObjectiveFunction(patlMargin, absPtdfSum, relativePositiveMargins, unit, OBJ_FUNC_PATL_MEASUREMENT_TYPE, measurements);
            }
            if (withTatl) {
                // A07
                double tatlThreshold = addThreshold(cnecResult, unit, TATL_MEASUREMENT_TYPE, measurements);
                // Z14
                double tatlMargin = addMargin(flow, tatlThreshold, unit, ABS_MARG_TATL_MEASUREMENT_TYPE, measurements);
                // Z15
                addObjectiveFunction(tatlMargin, absPtdfSum, relativePositiveMargins, unit, OBJ_FUNC_TATL_MEASUREMENT_TYPE, measurements);
            }
        }
        // A03
        addFrm(cnec, measurements);
        // Z16 & Z17
        addLoopflow(cnecResult, measurements);
    }

    private static double addFlow(CnecResult cnecResult, Unit unit, List<Analog> measurements) {
        double flow;
        if (unit.equals(Unit.AMPERE)) {
            flow = cnecResult.getFlowInA();
        } else if (unit.equals(Unit.MEGAWATT)) {
            flow = cnecResult.getFlowInMW();
        } else {
            throw new FaraoException(String.format(UNHANDLED_UNIT, unit.toString()));
        }
        if (!Double.isNaN(flow)) {
            measurements.add(newFlowMeasurement(FLOW_MEASUREMENT_TYPE, unit, flow));
        }
        return flow;
    }

    private static double addThreshold(CnecResult cnecResult, Unit unit, String measurementType, List<Analog> measurements) {
        double threshold = getClosestThreshold(cnecResult, unit);
        if (!Double.isNaN(threshold)) {
            measurements.add(newFlowMeasurement(measurementType, unit, threshold));
        }
        return threshold;
    }

    private static double addMargin(double flow, double threshold, Unit unit, String measurementType, List<Analog> measurements) {
        double margin = Math.signum(threshold) * (threshold - flow);
        measurements.add(newFlowMeasurement(measurementType, unit, margin));
        return margin;
    }

    private static double addObjectiveFunction(double margin, double absPtdfSum, boolean relativePositiveMargins, Unit unit, String measurementType, List<Analog> measurements) {
        double objValue = relativePositiveMargins && margin > 0 ? -margin / absPtdfSum : -margin;
        measurements.add(newFlowMeasurement(measurementType, unit, objValue));
        return objValue;
    }

    /**
     * Select the threshold closest to the flow, that will be added in the measurement.
     * This is useful when a cnec has both a Max and a Min threshold.
     * @param cnecResult CnecResult associated to the cnec
     * @param unit unit required in the measurement (can be AMPERE or MEGAWATT)
     * @return the value of the Threshold in the requested unit
     */
    private static double getClosestThreshold(CnecResult cnecResult, Unit unit) {
        double flow = unit.equals(Unit.MEGAWATT) ? cnecResult.getFlowInMW() : cnecResult.getFlowInA();
        if (!Double.isNaN(flow)) {
            double maxThreshold = unit.equals(Unit.MEGAWATT) ? cnecResult.getMaxThresholdInMW() : cnecResult.getMaxThresholdInA();
            maxThreshold = Double.isNaN(maxThreshold) ? Double.POSITIVE_INFINITY : maxThreshold;
            double minThreshold = unit.equals(Unit.MEGAWATT) ? cnecResult.getMinThresholdInMW() : cnecResult.getMinThresholdInA();
            minThreshold = Double.isNaN(minThreshold) ? Double.POSITIVE_INFINITY : minThreshold;
            if (Math.abs(maxThreshold - flow) < Math.abs(minThreshold - flow)) {
                return maxThreshold;
            } else {
                return minThreshold;
            }
        }
        return flow;

    }

    private static void addFrm(BranchCnec cnec, List<Analog> measurements) {
        if (!Double.isNaN(cnec.getReliabilityMargin())) {
            measurements.add(newFlowMeasurement(FRM_MEASUREMENT_TYPE, Unit.MEGAWATT, cnec.getReliabilityMargin()));
        }
    }

    private static double addSumPtdf(CnecResult cnecResult, List<Analog> measurements) {
        double absPtdfSum = cnecResult.getAbsolutePtdfSum();
        if (!Double.isNaN(absPtdfSum)) {
            measurements.add(newPtdfMeasurement(SUM_PTDF_MEASUREMENT_TYPE, absPtdfSum));
        }
        return absPtdfSum;
    }

    private static void addLoopflow(CnecResult cnecResult, List<Analog> measurements) {
        if (!Double.isNaN(cnecResult.getLoopflowInMW()) && !Double.isNaN(cnecResult.getLoopflowThresholdInMW())) {
            measurements.add(newFlowMeasurement(LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, cnecResult.getLoopflowInMW()));
            double threshold = Math.signum(cnecResult.getLoopflowInMW()) * cnecResult.getLoopflowThresholdInMW();
            measurements.add(newFlowMeasurement(MAX_LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, threshold));
        }
    }

    public static MonitoredRegisteredResource createMonitoredRegisteredResource(BranchCnec cnec, Network network, List<Analog> measurements) {
        String nodeOr = findNodeInNetwork(cnec.getNetworkElement().getId(), network, Branch.Side.ONE);
        String nodeEx = findNodeInNetwork(cnec.getNetworkElement().getId(), network, Branch.Side.TWO);

        return newMonitoredRegisteredResource(cnec.getId(), cnec.getName(), nodeOr, nodeEx, measurements);
    }
}
