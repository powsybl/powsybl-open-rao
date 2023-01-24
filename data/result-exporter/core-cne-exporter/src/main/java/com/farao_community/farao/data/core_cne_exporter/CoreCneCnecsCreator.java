/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.core_cne_exporter;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.data.cne_exporter_commons.CneHelper;
import com.farao_community.farao.data.core_cne_exporter.xsd.Analog;
import com.farao_community.farao.data.core_cne_exporter.xsd.ConstraintSeries;
import com.farao_community.farao.data.core_cne_exporter.xsd.ContingencySeries;
import com.farao_community.farao.data.core_cne_exporter.xsd.MonitoredRegisteredResource;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.BranchCnecCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.StandardCracCreationContext;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThreshold;
import com.farao_community.farao.data.rao_result_api.OptimizationState;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.cne_exporter_commons.CneConstants.*;
import static com.farao_community.farao.data.core_cne_exporter.CoreCneClassCreator.*;

/**
 * Creates the measurements, monitored registered resources and monitored series
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class CoreCneCnecsCreator {

    private CneHelper cneHelper;
    private StandardCracCreationContext cracCreationContext;

    public CoreCneCnecsCreator(CneHelper cneHelper, StandardCracCreationContext cracCreationContext) {
        this.cneHelper = cneHelper;
        this.cracCreationContext = cracCreationContext;
    }

    private CoreCneCnecsCreator() {

    }

    public List<ConstraintSeries> generate() {
        List<ConstraintSeries> constraintSeries = new ArrayList<>();
        List<BranchCnecCreationContext> sortedCnecs = cracCreationContext.getBranchCnecCreationContexts().stream()
            .sorted(Comparator.comparing(BranchCnecCreationContext::getNativeId)).collect(Collectors.toList());
        for (BranchCnecCreationContext cnec : sortedCnecs) {
            constraintSeries.addAll(createConstraintSeriesOfACnec(cnec, cneHelper));
        }
        return constraintSeries;
    }

    private List<ConstraintSeries> createConstraintSeriesOfACnec(BranchCnecCreationContext branchCnecCreationContext, CneHelper cneHelper) {
        if (!branchCnecCreationContext.isImported()) {
            FaraoLoggerProvider.TECHNICAL_LOGS.warn("Cnec {} was not imported into the RAO, its results will be absent from the CNE file", branchCnecCreationContext.getNativeId());
            return new ArrayList<>();
        }
        List<ConstraintSeries> constraintSeries = new ArrayList<>();
        String outageBranchCnecId;
        String curativeBranchCnecId;
        if (branchCnecCreationContext.isBaseCase()) {
            outageBranchCnecId = branchCnecCreationContext.getCreatedCnecsIds().get(Instant.PREVENTIVE);
            curativeBranchCnecId = outageBranchCnecId;
        } else {
            outageBranchCnecId = branchCnecCreationContext.getCreatedCnecsIds().get(Instant.OUTAGE);
            curativeBranchCnecId = branchCnecCreationContext.getCreatedCnecsIds().get(Instant.CURATIVE);
        }

        // A52 (CNEC)
        if (cneHelper.getCrac().getFlowCnec(outageBranchCnecId).isOptimized()) {
            constraintSeries.addAll(
                createConstraintSeriesOfCnec(branchCnecCreationContext, outageBranchCnecId, curativeBranchCnecId, false, cneHelper)
            );
        } else if (cneHelper.getCrac().getFlowCnec(outageBranchCnecId).isMonitored()) {
            // A49 (MNEC)
            // TODO : remove 'else' when we go back to exporting CNEC+MNEC branches as both a CNEC and a MNEC
            constraintSeries.addAll(
                createConstraintSeriesOfCnec(branchCnecCreationContext, outageBranchCnecId, curativeBranchCnecId, true, cneHelper)
            );
        }
        return constraintSeries;
    }

    private List<ConstraintSeries> createConstraintSeriesOfCnec(BranchCnecCreationContext branchCnecCreationContext, String outageCnecId, String curativeCnecId, boolean asMnec, CneHelper cneHelper) {
        List<ConstraintSeries> constraintSeriesOfCnec = new ArrayList<>();
        String nativeCnecId = branchCnecCreationContext.getNativeId();
        boolean shouldInvertBranchDirection = branchCnecCreationContext.isDirectionInvertedInNetwork();

        FlowCnec outageCnec = cneHelper.getCrac().getFlowCnec(outageCnecId);
        FlowCnec curativeCnec = cneHelper.getCrac().getFlowCnec(curativeCnecId);

        /* Create Constraint series */
        String marketStatus = asMnec ? MONITORED_MARKET_STATUS : OPTIMIZED_MARKET_STATUS;
        ConstraintSeries constraintSeriesB88 = newConstraintSeries(nativeCnecId, B88_BUSINESS_TYPE, outageCnec.getOperator(), marketStatus);
        ConstraintSeries constraintSeriesB57 = newConstraintSeries(nativeCnecId, B57_BUSINESS_TYPE, outageCnec.getOperator(), marketStatus);
        ConstraintSeries constraintSeriesB54 = newConstraintSeries(nativeCnecId, B54_BUSINESS_TYPE, outageCnec.getOperator(), marketStatus);

        /* Add contingency if exists */
        Optional<Contingency> optionalContingency = outageCnec.getState().getContingency();
        String contingencySuffix = "BASECASE";
        if (optionalContingency.isPresent()) {
            ContingencySeries contingencySeries = newContingencySeries(optionalContingency.get().getId(), optionalContingency.get().getName());
            constraintSeriesB88.getContingencySeries().add(contingencySeries);
            constraintSeriesB57.getContingencySeries().add(contingencySeries);
            constraintSeriesB54.getContingencySeries().add(contingencySeries);
            contingencySuffix = optionalContingency.get().getName();
        }

        contingencySuffix = "|" + contingencySuffix;

        // B88
        List<Analog> measurementsB88 = createB88MeasurementsOfCnec(curativeCnec, outageCnec, asMnec, shouldInvertBranchDirection);
        MonitoredRegisteredResource monitoredRegisteredResourceB88 = newMonitoredRegisteredResource(nativeCnecId, outageCnec.getName(), measurementsB88);
        constraintSeriesB88.getMonitoredSeries().add(newMonitoredSeries(nativeCnecId, outageCnec.getName() + contingencySuffix, monitoredRegisteredResourceB88));
        constraintSeriesOfCnec.add(constraintSeriesB88);

        // B57
        List<Analog> measurementsB57 = createB57MeasurementsOfCnec(outageCnec, asMnec, shouldInvertBranchDirection);
        MonitoredRegisteredResource monitoredRegisteredResourceB57 = newMonitoredRegisteredResource(nativeCnecId, outageCnec.getName(), measurementsB57);
        constraintSeriesB57.getMonitoredSeries().add(newMonitoredSeries(nativeCnecId, outageCnec.getName() + contingencySuffix, monitoredRegisteredResourceB57));
        constraintSeriesOfCnec.add(constraintSeriesB57);

        if (optionalContingency.isPresent() &&
            (!cneHelper.getRaoResult().getActivatedNetworkActionsDuringState(cneHelper.getCrac().getState(optionalContingency.get(), Instant.CURATIVE)).isEmpty()
                || !cneHelper.getRaoResult().getActivatedRangeActionsDuringState(cneHelper.getCrac().getState(optionalContingency.get(), Instant.CURATIVE)).isEmpty())) {
            // B54
            // TODO : remove the 'if' condition when we go back to exporting B54 series even if no CRAs are applied
            List<Analog> measurementsB54 = createB54MeasurementsOfCnec(curativeCnec, asMnec, shouldInvertBranchDirection);
            MonitoredRegisteredResource monitoredRegisteredResourceB54 = newMonitoredRegisteredResource(nativeCnecId, curativeCnec.getName(), measurementsB54);
            constraintSeriesB54.getMonitoredSeries().add(newMonitoredSeries(nativeCnecId, curativeCnec.getName() + contingencySuffix, monitoredRegisteredResourceB54));
            constraintSeriesOfCnec.add(constraintSeriesB54);
        }

        return constraintSeriesOfCnec;
    }

    private class AnalogComparator implements Comparator<Analog> {
        @Override
        public int compare(Analog o1, Analog o2) {
            if (o1.getMeasurementType().equals(o2.getMeasurementType())) {
                return o1.getUnitSymbol().compareTo(o2.getUnitSymbol());
            } else {
                return o1.getMeasurementType().compareTo(o2.getMeasurementType());
            }
        }
    }

    private List<Analog> createB88MeasurementsOfCnec(FlowCnec permanentCnec, FlowCnec temporaryCnec, boolean asMnec, boolean shouldInvertBranchDirection) {
        List<Analog> measurements = new ArrayList<>();
        measurements.addAll(createFlowMeasurementsOfFlowCnec(permanentCnec, OptimizationState.INITIAL, true, shouldInvertBranchDirection)); // TODO : replace true with !asMnec when we go back to proper implementation
        measurements.addAll(createMarginMeasurementsOfFlowCnec(permanentCnec, OptimizationState.INITIAL, asMnec, false, shouldInvertBranchDirection));
        measurements.addAll(createMarginMeasurementsOfFlowCnec(temporaryCnec, OptimizationState.INITIAL, asMnec, true, shouldInvertBranchDirection));
        measurements.sort(new AnalogComparator());
        return measurements;
    }

    private List<Analog> createB57MeasurementsOfCnec(FlowCnec cnec, boolean asMnec, boolean shouldInvertBranchDirection) {
        List<Analog> measurements = new ArrayList<>();
        measurements.addAll(createFlowMeasurementsOfFlowCnec(cnec, OptimizationState.AFTER_PRA, true, shouldInvertBranchDirection)); // TODO : replace true with !asMnec when we go back to proper implementation
        measurements.addAll(createMarginMeasurementsOfFlowCnec(cnec, OptimizationState.AFTER_PRA, asMnec, true, shouldInvertBranchDirection));
        measurements.sort(new AnalogComparator());
        return measurements;
    }

    private List<Analog> createB54MeasurementsOfCnec(FlowCnec cnec, boolean asMnec, boolean shouldInvertBranchDirection) {
        List<Analog> measurements = new ArrayList<>();
        measurements.addAll(createFlowMeasurementsOfFlowCnec(cnec, OptimizationState.AFTER_CRA, true, shouldInvertBranchDirection)); // TODO : replace true with !asMnec when we go back to proper implementation
        measurements.addAll(createMarginMeasurementsOfFlowCnec(cnec, OptimizationState.AFTER_CRA, asMnec, false, shouldInvertBranchDirection));
        measurements.sort(new AnalogComparator());
        return measurements;
    }

    private List<Analog> createFlowMeasurementsOfFlowCnec(FlowCnec cnec, OptimizationState optimizationState, boolean withSumPtdf, boolean shouldInvertBranchDirection) {
        List<Analog> measurements = new ArrayList<>();
        // A01
        measurements.add(createFlowMeasurement(cnec, optimizationState, Unit.MEGAWATT, shouldInvertBranchDirection));
        // Z11
        if (withSumPtdf && cneHelper.isRelativePositiveMargins()) {
            measurements.add(createPtdfZonalSumMeasurement(cnec));
        }
        // A03
        measurements.add(createFrmMeasurement(cnec));
        if (cneHelper.isWithLoopflows()) {
            // Z16 & Z17
            measurements.addAll(createLoopflowMeasurements(cnec, optimizationState, shouldInvertBranchDirection));
        }
        return measurements;
    }

    private List<Analog> createMarginMeasurementsOfFlowCnec(FlowCnec cnec, OptimizationState optimizationState, boolean asMnec, boolean isTemporary, boolean shouldInvertBranchDirection) {
        List<Analog> measurements = new ArrayList<>();
        String measurementType;
        for (Unit unit : List.of(Unit.AMPERE, Unit.MEGAWATT)) {
            // A02 / A07
            measurementType = isTemporary ? TATL_MEASUREMENT_TYPE : PATL_MEASUREMENT_TYPE;
            measurements.add(createThresholdMeasurement(cnec, optimizationState, asMnec, unit, measurementType, shouldInvertBranchDirection));
        }
        // Z12 / Z14
        measurementType = isTemporary ? ABS_MARG_TATL_MEASUREMENT_TYPE : ABS_MARG_PATL_MEASUREMENT_TYPE;
        measurements.add(createMarginMeasurement(cnec, optimizationState, asMnec, Unit.MEGAWATT, measurementType));
        // Z13 / Z15
        if (true) { // (!asMnec) { TODO : reactivate this when we go back to proper implementation
            measurementType = isTemporary ? OBJ_FUNC_TATL_MEASUREMENT_TYPE : OBJ_FUNC_PATL_MEASUREMENT_TYPE;
            measurements.add(createObjectiveValueMeasurement(cnec, optimizationState, asMnec, Unit.MEGAWATT, measurementType));
        }
        return measurements;
    }

    private double getCnecFlow(FlowCnec cnec, Side side, OptimizationState optimizationState, Unit unit) {
        OptimizationState resultState = optimizationState;
        if (resultState.equals(OptimizationState.AFTER_CRA) && cnec.getState().getInstant().equals(Instant.PREVENTIVE)) {
            resultState = OptimizationState.AFTER_PRA;
        }
        return cneHelper.getRaoResult().getFlow(resultState, cnec, side, unit);
    }

    private double getCnecMargin(FlowCnec cnec, OptimizationState optimizationState, boolean asMnec, Unit unit, boolean deductFrmFromThreshold) {
        OptimizationState resultState = optimizationState;
        if (resultState.equals(OptimizationState.AFTER_CRA) && cnec.getState().getInstant().equals(Instant.PREVENTIVE)) {
            resultState = OptimizationState.AFTER_PRA;
        }
        return getThresholdToMarginMap(cnec, resultState, asMnec, unit, deductFrmFromThreshold).values().stream().min(Double::compareTo).orElseThrow();
    }

    private double getCnecRelativeMargin(FlowCnec cnec, OptimizationState optimizationState, boolean asMnec, Unit unit) {
        double absoluteMargin = getCnecMargin(cnec, optimizationState, asMnec, unit, true);
        OptimizationState resultState = optimizationState;
        if (resultState.equals(OptimizationState.AFTER_CRA) && cnec.getState().getInstant().equals(Instant.PREVENTIVE)) {
            resultState = OptimizationState.AFTER_PRA;
        }
        return absoluteMargin > 0 ? absoluteMargin / cneHelper.getRaoResult().getPtdfZonalSum(resultState, cnec, getMonitoredSide(cnec)) : absoluteMargin;
    }

    private Analog createFlowMeasurement(FlowCnec cnec, OptimizationState optimizationState, Unit unit, boolean shouldInvertBranchDirection) {
        double invert = shouldInvertBranchDirection ? -1 : 1;
        return newFlowMeasurement(FLOW_MEASUREMENT_TYPE, unit, invert * getCnecFlow(cnec, getMonitoredSide(cnec), optimizationState, unit));
    }

    private Analog createThresholdMeasurement(FlowCnec cnec, OptimizationState optimizationState, boolean asMnec, Unit unit, String measurementType, boolean shouldInvertBranchDirection) {
        double threshold = getClosestThreshold(cnec, optimizationState, asMnec, unit);
        double invert = shouldInvertBranchDirection ? -1 : 1;
        return newFlowMeasurement(measurementType, unit, invert * threshold);
    }

    private Analog createMarginMeasurement(FlowCnec cnec, OptimizationState optimizationState, boolean asMnec, Unit unit, String measurementType) {
        return newFlowMeasurement(measurementType, unit, getCnecMargin(cnec, optimizationState, asMnec, unit, false));
    }

    private Analog createObjectiveValueMeasurement(FlowCnec cnec, OptimizationState optimizationState, boolean asMnec, Unit unit, String measurementType) {
        double margin = getCnecMargin(cnec, optimizationState, asMnec, unit, true);
        if (cneHelper.isRelativePositiveMargins() && margin > 0) {
            margin = getCnecRelativeMargin(cnec, optimizationState, asMnec, unit);
        }
        return newFlowMeasurement(measurementType, unit, margin);
    }

    /**
     * Select the threshold closest to the flow, that will be added in the measurement.
     * This is useful when a cnec has both a Max and a Min threshold.
     */
    private double getClosestThreshold(FlowCnec cnec, OptimizationState optimizationState, boolean asMnec, Unit unit) {
        Map<Double, Double> thresholdToMarginMap = getThresholdToMarginMap(cnec, optimizationState, asMnec, unit, false);
        if (thresholdToMarginMap.isEmpty()) {
            return 0;
        }
        return thresholdToMarginMap.entrySet().stream().min(Comparator.comparing(Map.Entry::getValue)).orElseThrow().getKey();
    }

    /**
     * Returns a map containing all threshold for a given cnec and the associated margins
     * If the CNEC is also a MNEC, extra thresholds corresponding to the MNEc constraints are returned
     *
     * @param cnec              the FlowCnec
     * @param optimizationState the OptimizationState for computing margins
     * @param asMnec            true if it should be treated as a MNEC
     * @param unit              the unit of the threshold and margin
     */
    private Map<Double, Double> getThresholdToMarginMap(FlowCnec cnec, OptimizationState optimizationState, boolean asMnec, Unit unit, boolean deductFrmFromThreshold) {
        Map<Double, Double> thresholdToMarginMap = new HashMap<>();
        Side side = getMonitoredSide(cnec);
        double flow = getCnecFlow(cnec, side, optimizationState, unit);
        if (!Double.isNaN(flow)) {
            if (false) {
                // TODO : reactivate this for MNECs (if (asMnec)) when we should go back to the full version
                getThresholdToMarginMapAsMnec(cnec, unit, thresholdToMarginMap, flow, side);
            } else {
                getThresholdToMarginMapAsCnec(cnec, unit, deductFrmFromThreshold, thresholdToMarginMap, flow, side);
            }
        }
        return thresholdToMarginMap;
    }

    private void getThresholdToMarginMapAsCnec(FlowCnec cnec, Unit unit, boolean deductFrmFromThreshold, Map<Double, Double> thresholdToMarginMap, double flow, Side side) {
        // TODO : remove this when we go back to considering FRM in the exported threshold
        double frm = deductFrmFromThreshold ? 0 : cnec.getReliabilityMargin() * getFlowUnitMultiplier(cnec, side, Unit.MEGAWATT, unit);
        // Only look at fixed thresholds
        double maxThreshold = cnec.getUpperBound(side, unit).orElse(Double.MAX_VALUE) + frm;
        maxThreshold = Double.isNaN(maxThreshold) ? Double.POSITIVE_INFINITY : maxThreshold;
        thresholdToMarginMap.putIfAbsent(maxThreshold, maxThreshold - flow);

        double minThreshold = cnec.getLowerBound(side, unit).orElse(-Double.MAX_VALUE) - frm;
        minThreshold = Double.isNaN(minThreshold) ? Double.POSITIVE_INFINITY : minThreshold;
        thresholdToMarginMap.putIfAbsent(minThreshold, flow - minThreshold);
    }

    private void getThresholdToMarginMapAsMnec(FlowCnec cnec, Unit unit, Map<Double, Double> thresholdToMarginMap, double flow, Side side) {
        // Look at thresholds computed using initial flow
        double initialFlow = getCnecFlow(cnec, getMonitoredSide(cnec), OptimizationState.INITIAL, unit);
        double tolerance = cneHelper.getMnecAcceptableMarginDiminution() * getFlowUnitMultiplier(cnec, side, Unit.MEGAWATT, unit);

        double mnecUpperThreshold = Math.max(cnec.getUpperBound(side, unit).orElse(Double.MAX_VALUE), initialFlow + tolerance);
        mnecUpperThreshold = Double.isNaN(mnecUpperThreshold) ? Double.POSITIVE_INFINITY : mnecUpperThreshold;
        thresholdToMarginMap.putIfAbsent(mnecUpperThreshold, mnecUpperThreshold - flow);

        double mnecLowerThreshold = Math.min(cnec.getLowerBound(side, unit).orElse(-Double.MAX_VALUE), initialFlow - tolerance);
        mnecLowerThreshold = Double.isNaN(mnecLowerThreshold) ? Double.POSITIVE_INFINITY : mnecLowerThreshold;
        thresholdToMarginMap.putIfAbsent(mnecLowerThreshold, flow - mnecLowerThreshold);
    }

    private Analog createFrmMeasurement(FlowCnec cnec) {
        return newFlowMeasurement(FRM_MEASUREMENT_TYPE, Unit.MEGAWATT, cnec.getReliabilityMargin());
    }

    private Analog createPtdfZonalSumMeasurement(FlowCnec cnec) {
        double absPtdfSum = cneHelper.getRaoResult().getPtdfZonalSum(OptimizationState.INITIAL, cnec, getMonitoredSide(cnec));
        return newPtdfMeasurement(SUM_PTDF_MEASUREMENT_TYPE, absPtdfSum);
    }

    private List<Analog> createLoopflowMeasurements(FlowCnec cnec, OptimizationState optimizationState, boolean shouldInvertBranchDirection) {
        OptimizationState resultOptimState = optimizationState;
        if (optimizationState.equals(OptimizationState.AFTER_CRA) && cnec.getState().isPreventive()) {
            resultOptimState = OptimizationState.AFTER_PRA;
        }
        List<Analog> measurements = new ArrayList<>();
        try {
            double loopflow = cneHelper.getRaoResult().getLoopFlow(resultOptimState, cnec, getMonitoredSide(cnec), Unit.MEGAWATT);
            LoopFlowThreshold loopFlowExtension = cnec.getExtension(LoopFlowThreshold.class);
            if (!Objects.isNull(loopFlowExtension) && !Double.isNaN(loopflow)) {
                double invert = shouldInvertBranchDirection ? -1 : 1;
                measurements.add(newFlowMeasurement(LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, invert * loopflow));
                double threshold = invert * Math.signum(loopflow) * loopFlowExtension.getThreshold(Unit.MEGAWATT);
                measurements.add(newFlowMeasurement(MAX_LOOPFLOW_MEASUREMENT_TYPE, Unit.MEGAWATT, threshold));
            }
        } catch (FaraoException e) {
            // no commercial flow
        }
        return measurements;
    }

    public static double getFlowUnitMultiplier(FlowCnec cnec, Side voltageSide, Unit unitFrom, Unit unitTo) {
        if (unitFrom == unitTo) {
            return 1;
        }
        double nominalVoltage = cnec.getNominalVoltage(voltageSide);
        if (unitFrom == Unit.MEGAWATT && unitTo == Unit.AMPERE) {
            return 1000 / (nominalVoltage * Math.sqrt(3));
        } else if (unitFrom == Unit.AMPERE && unitTo == Unit.MEGAWATT) {
            return nominalVoltage * Math.sqrt(3) / 1000;
        } else {
            throw new FaraoException("Only conversions between MW and A are supported.");
        }
    }

    private Side getMonitoredSide(FlowCnec cnec) {
        return cnec.getMonitoredSides().contains(Side.LEFT) ? Side.LEFT : Side.RIGHT;
    }
}
