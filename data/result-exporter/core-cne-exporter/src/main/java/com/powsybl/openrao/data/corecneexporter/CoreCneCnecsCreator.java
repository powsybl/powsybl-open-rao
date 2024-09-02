/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.corecneexporter;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.cneexportercommons.CneHelper;
import com.powsybl.openrao.data.corecneexporter.xsd.Analog;
import com.powsybl.openrao.data.corecneexporter.xsd.ConstraintSeries;
import com.powsybl.openrao.data.corecneexporter.xsd.ContingencySeries;
import com.powsybl.openrao.data.corecneexporter.xsd.MonitoredRegisteredResource;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.BranchCnecCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.UcteCracCreationContext;
import com.powsybl.openrao.data.cracloopflowextension.LoopFlowThreshold;

import java.util.*;

import static com.powsybl.openrao.data.cneexportercommons.CneConstants.*;
import static com.powsybl.openrao.data.corecneexporter.CoreCneClassCreator.*;

/**
 * Creates the measurements, monitored registered resources and monitored series
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class CoreCneCnecsCreator {

    private CneHelper cneHelper;
    private UcteCracCreationContext cracCreationContext;

    public CoreCneCnecsCreator(CneHelper cneHelper, UcteCracCreationContext cracCreationContext) {
        this.cneHelper = cneHelper;
        this.cracCreationContext = cracCreationContext;
    }

    private CoreCneCnecsCreator() {

    }

    public List<ConstraintSeries> generate() {
        List<ConstraintSeries> constraintSeries = new ArrayList<>();
        cracCreationContext.getBranchCnecCreationContexts().stream()
            .sorted(Comparator.comparing(BranchCnecCreationContext::getNativeObjectId))
            .forEach(cnec -> constraintSeries.addAll(createConstraintSeriesOfACnec(cnec, cneHelper)));
        return constraintSeries;
    }

    private List<ConstraintSeries> createConstraintSeriesOfACnec(BranchCnecCreationContext branchCnecCreationContext, CneHelper cneHelper) {
        if (!branchCnecCreationContext.isImported()) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.warn("Cnec {} was not imported into the RAO, its results will be absent from the CNE file", branchCnecCreationContext.getNativeObjectId());
            return new ArrayList<>();
        }
        List<ConstraintSeries> constraintSeries = new ArrayList<>();
        String outageBranchCnecId;
        String curativeBranchCnecId;
        if (branchCnecCreationContext.isBaseCase()) {
            outageBranchCnecId = branchCnecCreationContext.getCreatedCnecsIds().get(cneHelper.getCrac().getPreventiveInstant().getId());
            curativeBranchCnecId = outageBranchCnecId;
        } else {
            outageBranchCnecId = branchCnecCreationContext.getCreatedCnecsIds().get(cneHelper.getCrac().getOutageInstant().getId());
            curativeBranchCnecId = branchCnecCreationContext.getCreatedCnecsIds().get(cneHelper.getCrac().getInstant(InstantKind.CURATIVE).getId());
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
        String nativeCnecId = branchCnecCreationContext.getNativeObjectId();
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
            String contingencyName = optionalContingency.get().getName().orElse(optionalContingency.get().getId());
            ContingencySeries contingencySeries = newContingencySeries(optionalContingency.get().getId(), contingencyName);
            constraintSeriesB88.getContingencySeries().add(contingencySeries);
            constraintSeriesB57.getContingencySeries().add(contingencySeries);
            constraintSeriesB54.getContingencySeries().add(contingencySeries);
            contingencySuffix = contingencyName;
        }

        contingencySuffix = "|" + contingencySuffix;

        // B88
        List<Analog> measurementsB88 = createB88MeasurementsOfCnec(curativeCnec, outageCnec, shouldInvertBranchDirection);
        MonitoredRegisteredResource monitoredRegisteredResourceB88 = newMonitoredRegisteredResource(nativeCnecId, outageCnec.getName(), measurementsB88);
        constraintSeriesB88.getMonitoredSeries().add(newMonitoredSeries(nativeCnecId, outageCnec.getName() + contingencySuffix, monitoredRegisteredResourceB88));
        constraintSeriesOfCnec.add(constraintSeriesB88);

        // B57
        List<Analog> measurementsB57 = createB57MeasurementsOfCnec(outageCnec, shouldInvertBranchDirection);
        MonitoredRegisteredResource monitoredRegisteredResourceB57 = newMonitoredRegisteredResource(nativeCnecId, outageCnec.getName(), measurementsB57);
        constraintSeriesB57.getMonitoredSeries().add(newMonitoredSeries(nativeCnecId, outageCnec.getName() + contingencySuffix, monitoredRegisteredResourceB57));
        constraintSeriesOfCnec.add(constraintSeriesB57);

        Instant curativeInstant = cneHelper.getCrac().getInstant(InstantKind.CURATIVE);
        if (optionalContingency.isPresent() &&
            (!cneHelper.getRaoResult().getActivatedNetworkActionsDuringState(cneHelper.getCrac().getState(optionalContingency.get(), curativeInstant)).isEmpty()
                || !cneHelper.getRaoResult().getActivatedRangeActionsDuringState(cneHelper.getCrac().getState(optionalContingency.get(), curativeInstant)).isEmpty())) {
            // B54
            // TODO : remove the 'if' condition when we go back to exporting B54 series even if no CRAs are applied
            List<Analog> measurementsB54 = createB54MeasurementsOfCnec(curativeCnec, shouldInvertBranchDirection);
            MonitoredRegisteredResource monitoredRegisteredResourceB54 = newMonitoredRegisteredResource(nativeCnecId, curativeCnec.getName(), measurementsB54);
            constraintSeriesB54.getMonitoredSeries().add(newMonitoredSeries(nativeCnecId, curativeCnec.getName() + contingencySuffix, monitoredRegisteredResourceB54));
            constraintSeriesOfCnec.add(constraintSeriesB54);
        }

        return constraintSeriesOfCnec;
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

    private List<Analog> createB88MeasurementsOfCnec(FlowCnec permanentCnec, FlowCnec temporaryCnec, boolean shouldInvertBranchDirection) {
        List<Analog> measurements = new ArrayList<>();
        measurements.addAll(createFlowMeasurementsOfFlowCnec(permanentCnec, null, true, shouldInvertBranchDirection));
        measurements.addAll(createMarginMeasurementsOfFlowCnec(permanentCnec, null, false, shouldInvertBranchDirection));
        measurements.addAll(createMarginMeasurementsOfFlowCnec(temporaryCnec, null, true, shouldInvertBranchDirection));
        measurements.sort(new AnalogComparator());
        return measurements;
    }

    private List<Analog> createB57MeasurementsOfCnec(FlowCnec cnec, boolean shouldInvertBranchDirection) {
        List<Analog> measurements = new ArrayList<>();
        measurements.addAll(createFlowMeasurementsOfFlowCnec(cnec, cracCreationContext.getCrac().getPreventiveInstant(), true, shouldInvertBranchDirection));
        measurements.addAll(createMarginMeasurementsOfFlowCnec(cnec, cracCreationContext.getCrac().getPreventiveInstant(), true, shouldInvertBranchDirection));
        measurements.sort(new AnalogComparator());
        return measurements;
    }

    private List<Analog> createB54MeasurementsOfCnec(FlowCnec cnec, boolean shouldInvertBranchDirection) {
        List<Analog> measurements = new ArrayList<>();
        measurements.addAll(createFlowMeasurementsOfFlowCnec(cnec, cracCreationContext.getCrac().getInstant(InstantKind.CURATIVE), true, shouldInvertBranchDirection));
        measurements.addAll(createMarginMeasurementsOfFlowCnec(cnec, cracCreationContext.getCrac().getInstant(InstantKind.CURATIVE), false, shouldInvertBranchDirection));
        measurements.sort(new AnalogComparator());
        return measurements;
    }

    private List<Analog> createFlowMeasurementsOfFlowCnec(FlowCnec cnec, Instant optimizedInstant, boolean withSumPtdf, boolean shouldInvertBranchDirection) {
        List<Analog> measurements = new ArrayList<>();
        // A01
        measurements.add(createFlowMeasurement(cnec, optimizedInstant, Unit.MEGAWATT, shouldInvertBranchDirection));
        // Z11
        if (withSumPtdf && cneHelper.isRelativePositiveMargins()) {
            measurements.add(createPtdfZonalSumMeasurement(cnec));
        }
        // A03
        measurements.add(createFrmMeasurement(cnec));
        if (cneHelper.isWithLoopflows()) {
            // Z16 & Z17
            measurements.addAll(createLoopflowMeasurements(cnec, optimizedInstant, shouldInvertBranchDirection));
        }
        return measurements;
    }

    private List<Analog> createMarginMeasurementsOfFlowCnec(FlowCnec cnec, Instant optimizedInstant, boolean isTemporary, boolean shouldInvertBranchDirection) {
        List<Analog> measurements = new ArrayList<>();
        String measurementType;
        for (Unit unit : List.of(Unit.AMPERE, Unit.MEGAWATT)) {
            // A02 / A07
            measurementType = isTemporary ? TATL_MEASUREMENT_TYPE : PATL_MEASUREMENT_TYPE;
            measurements.add(createThresholdMeasurement(cnec, optimizedInstant, unit, measurementType, shouldInvertBranchDirection));
        }
        // Z12 / Z14
        measurementType = isTemporary ? ABS_MARG_TATL_MEASUREMENT_TYPE : ABS_MARG_PATL_MEASUREMENT_TYPE;
        measurements.add(createMarginMeasurement(cnec, optimizedInstant, Unit.MEGAWATT, measurementType));
        // Z13 / Z15
        measurementType = isTemporary ? OBJ_FUNC_TATL_MEASUREMENT_TYPE : OBJ_FUNC_PATL_MEASUREMENT_TYPE;
        measurements.add(createObjectiveValueMeasurement(cnec, optimizedInstant, Unit.MEGAWATT, measurementType));
        return measurements;
    }

    private double getCnecFlow(FlowCnec cnec, TwoSides side, Instant optimizedInstant) {
        Instant resultState = optimizedInstant;
        if (resultState != null && resultState.isCurative() && cnec.getState().getInstant().isPreventive()) {
            resultState = cracCreationContext.getCrac().getPreventiveInstant();
        }
        return cneHelper.getRaoResult().getFlow(resultState, cnec, side, Unit.MEGAWATT);
    }

    private double getCnecMargin(FlowCnec cnec, Instant optimizedInstant, Unit unit, boolean deductFrmFromThreshold) {
        Instant resultState = optimizedInstant;
        if (resultState != null && resultState.isCurative() && cnec.getState().getInstant().isPreventive()) {
            resultState = cracCreationContext.getCrac().getPreventiveInstant();
        }
        return getThresholdToMarginMap(cnec, resultState, unit, deductFrmFromThreshold).values().stream().min(Double::compareTo).orElseThrow();
    }

    private double getCnecRelativeMargin(FlowCnec cnec, Instant optimizedInstant, Unit unit) {
        double absoluteMargin = getCnecMargin(cnec, optimizedInstant, unit, true);
        Instant resultState = optimizedInstant;
        if (resultState != null && resultState.isCurative() && cnec.getState().getInstant().isPreventive()) {
            resultState = cracCreationContext.getCrac().getPreventiveInstant();
        }
        return absoluteMargin > 0 ? absoluteMargin / cneHelper.getRaoResult().getPtdfZonalSum(resultState, cnec, getMonitoredSide(cnec)) : absoluteMargin;
    }

    private Analog createFlowMeasurement(FlowCnec cnec, Instant optimizedInstant, Unit unit, boolean shouldInvertBranchDirection) {
        double invert = shouldInvertBranchDirection ? -1 : 1;
        return newFlowMeasurement(FLOW_MEASUREMENT_TYPE, unit, invert * getCnecFlow(cnec, getMonitoredSide(cnec), optimizedInstant));
    }

    private Analog createThresholdMeasurement(FlowCnec cnec, Instant optimizedInstant, Unit unit, String measurementType, boolean shouldInvertBranchDirection) {
        double threshold = getClosestThreshold(cnec, optimizedInstant, unit);
        double invert = shouldInvertBranchDirection ? -1 : 1;
        return newFlowMeasurement(measurementType, unit, invert * threshold);
    }

    private Analog createMarginMeasurement(FlowCnec cnec, Instant optimizedInstant, Unit unit, String measurementType) {
        boolean deductFrmFromMargin = Unit.MEGAWATT.equals(unit);
        return newFlowMeasurement(measurementType, unit, getCnecMargin(cnec, optimizedInstant, unit, deductFrmFromMargin));
    }

    private Analog createObjectiveValueMeasurement(FlowCnec cnec, Instant optimizedInstant, Unit unit, String measurementType) {
        double margin = getCnecMargin(cnec, optimizedInstant, unit, true);
        if (cneHelper.isRelativePositiveMargins() && margin > 0) {
            margin = getCnecRelativeMargin(cnec, optimizedInstant, unit);
        }
        return newFlowMeasurement(measurementType, unit, margin);
    }

    /**
     * Select the threshold closest to the flow, that will be added in the measurement.
     * This is useful when a cnec has both a Max and a Min threshold.
     */
    private double getClosestThreshold(FlowCnec cnec, Instant optimizedInstant, Unit unit) {
        Map<Double, Double> thresholdToMarginMap = getThresholdToMarginMap(cnec, optimizedInstant, unit, false);
        if (thresholdToMarginMap.isEmpty()) {
            return 0;
        }
        return thresholdToMarginMap.entrySet().stream().min(Map.Entry.comparingByValue()).orElseThrow().getKey();
    }

    /**
     * Returns a map containing all threshold for a given cnec and the associated margins
     *
     * @param cnec              the FlowCnec
     * @param optimizedInstant the Instant for computing margins
     * @param unit              the unit of the threshold and margin
     */
    private Map<Double, Double> getThresholdToMarginMap(FlowCnec cnec, Instant optimizedInstant, Unit unit, boolean deductFrmFromThreshold) {
        Map<Double, Double> thresholdToMarginMap = new HashMap<>();
        TwoSides side = getMonitoredSide(cnec);
        double flow = getCnecFlow(cnec, side, optimizedInstant);
        if (!Double.isNaN(flow)) {
            getThresholdToMarginMapAsCnec(cnec, unit, deductFrmFromThreshold, thresholdToMarginMap, flow, side);
        }
        return thresholdToMarginMap;
    }

    private void getThresholdToMarginMapAsCnec(FlowCnec cnec, Unit unit, boolean deductFrmFromThreshold, Map<Double, Double> thresholdToMarginMap, double flow, TwoSides side) {
        // TODO : remove this when we go back to considering FRM in the exported threshold
        double flowUnitMultiplier = getFlowUnitMultiplier(cnec, side, Unit.MEGAWATT, unit);
        double frm = deductFrmFromThreshold ? 0 : cnec.getReliabilityMargin() * flowUnitMultiplier;
        // Only look at fixed thresholds
        double maxThreshold = cnec.getUpperBound(side, unit).orElse(Double.MAX_VALUE) + frm;
        maxThreshold = Double.isNaN(maxThreshold) ? Double.POSITIVE_INFINITY : maxThreshold;
        thresholdToMarginMap.putIfAbsent(maxThreshold, maxThreshold - flow * flowUnitMultiplier);

        double minThreshold = cnec.getLowerBound(side, unit).orElse(-Double.MAX_VALUE) - frm;
        minThreshold = Double.isNaN(minThreshold) ? Double.POSITIVE_INFINITY : minThreshold;
        thresholdToMarginMap.putIfAbsent(minThreshold, flow * flowUnitMultiplier - minThreshold);
    }

    private Analog createFrmMeasurement(FlowCnec cnec) {
        return newFlowMeasurement(FRM_MEASUREMENT_TYPE, Unit.MEGAWATT, cnec.getReliabilityMargin());
    }

    private Analog createPtdfZonalSumMeasurement(FlowCnec cnec) {
        double absPtdfSum = cneHelper.getRaoResult().getPtdfZonalSum(null, cnec, getMonitoredSide(cnec));
        return newPtdfMeasurement(SUM_PTDF_MEASUREMENT_TYPE, absPtdfSum);
    }

    private List<Analog> createLoopflowMeasurements(FlowCnec cnec, Instant optimizedInstant, boolean shouldInvertBranchDirection) {
        Instant resultOptimState = optimizedInstant;
        if (resultOptimState != null && optimizedInstant.isCurative() && cnec.getState().isPreventive()) {
            resultOptimState = cracCreationContext.getCrac().getPreventiveInstant();
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
        } catch (OpenRaoException e) {
            // no commercial flow
        }
        return measurements;
    }

    public static double getFlowUnitMultiplier(FlowCnec cnec, TwoSides voltageSide, Unit unitFrom, Unit unitTo) {
        if (unitFrom == unitTo) {
            return 1;
        }
        double nominalVoltage = cnec.getNominalVoltage(voltageSide);
        if (unitFrom == Unit.MEGAWATT && unitTo == Unit.AMPERE) {
            return 1000 / (nominalVoltage * Math.sqrt(3));
        } else if (unitFrom == Unit.AMPERE && unitTo == Unit.MEGAWATT) {
            return nominalVoltage * Math.sqrt(3) / 1000;
        } else {
            throw new OpenRaoException("Only conversions between MW and A are supported.");
        }
    }

    private TwoSides getMonitoredSide(FlowCnec cnec) {
        return cnec.getMonitoredSides().contains(TwoSides.ONE) ? TwoSides.ONE : TwoSides.TWO;
    }
}
