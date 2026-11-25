/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.cnecfiltering;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.FillersUtil;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.RangeActionImpacts;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.RangeActionSetpointBounds;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.raoimpl.result.api.FlowResult;
import com.powsybl.openrao.raoimpl.result.api.RangeActionActivationResult;
import com.powsybl.openrao.raoimpl.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.raoimpl.result.api.SensitivityResult;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * Removes the optimized flow CNECs that can never be the most limiting one in the linear problem,
 * so that omitting their margin constraints does not change the optimal objective value.
 * <p>
 * Around the linearization point, the margin of each CNEC side can only move by
 * {@code Δ = Σ |sensitivity| · max distance between the linearization set-point and the set-point variable bounds},
 * so it lies in {@code [m - Δ, m + Δ]}. The candidate with the smallest upper bound (the witness) gives an upper
 * bound {@code UB} of the optimal minimum margin; any CNEC whose lower bound is greater than {@code UB} is useless.
 * <p>
 * When in doubt (NaN or infinite values), CNECs are kept.
 *
 * @author Guillaume Verger {@literal <guillaume.verger at artelys.com>}
 */
public class SafeFlowCnecFilter implements FlowCnecFilter {
    private final OptimizationPerimeter perimeter;
    private final RangeActionSetpointResult prePerimeterSetpoints;
    private final RangeActionImpacts impacts;
    private final MarginCriterion criterion;
    private final Set<FlowCnec> protectedCnecs;
    private final Set<FlowCnec> notWitnessCnecs;
    private final Unit unit;
    private final Map<RangeActionState, RangeActionSetpointBounds.Interval> boundsCache = new HashMap<>();

    // last computation, cached so that all the fillers of one fill see the same CNECs
    private FlowResult lastFlowResult;
    private SensitivityResult lastSensitivityResult;
    private RangeActionActivationResult lastLinearizationPoint;
    private Set<FlowCnec> lastRemovableCnecs;

    private record RangeActionState(RangeAction<?> rangeAction, State state) {
    }

    private record MarginInterval(FlowCnec cnec, double absoluteLow, double low, double high) {
    }

    public SafeFlowCnecFilter(OptimizationPerimeter perimeter,
                              RangeActionSetpointResult prePerimeterSetpoints,
                              RangeActionImpacts impacts,
                              MarginCriterion criterion,
                              Set<FlowCnec> protectedCnecs,
                              Set<FlowCnec> notWitnessCnecs,
                              Unit unit) {
        this.perimeter = perimeter;
        this.prePerimeterSetpoints = prePerimeterSetpoints;
        this.impacts = impacts;
        this.criterion = criterion;
        this.protectedCnecs = new HashSet<>(protectedCnecs);
        this.notWitnessCnecs = new HashSet<>(notWitnessCnecs);
        this.unit = unit;
    }

    @Override
    public Set<FlowCnec> retain(Set<FlowCnec> cnecs, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult linearizationPoint) {
        Set<FlowCnec> removableCnecs = getRemovableCnecs(flowResult, sensitivityResult, linearizationPoint);
        if (removableCnecs.isEmpty()) {
            return cnecs;
        }
        return cnecs.stream()
            .filter(cnec -> !removableCnecs.contains(cnec))
            .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Identifiable::getId))));
    }

    private Set<FlowCnec> getRemovableCnecs(FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult linearizationPoint) {
        if (lastRemovableCnecs == null
            || flowResult != lastFlowResult
            || sensitivityResult != lastSensitivityResult
            || linearizationPoint != lastLinearizationPoint) {
            lastRemovableCnecs = computeRemovableCnecs(flowResult, sensitivityResult, linearizationPoint);
            lastFlowResult = flowResult;
            lastSensitivityResult = sensitivityResult;
            lastLinearizationPoint = linearizationPoint;
        }
        return lastRemovableCnecs;
    }

    private Set<FlowCnec> computeRemovableCnecs(FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult linearizationPoint) {
        Set<FlowCnec> candidates = FillersUtil.getFlowCnecsComputationStatusOk(perimeter.getOptimizedFlowCnecs(), sensitivityResult);
        Set<MarginInterval> intervals = candidates.stream()
            .map(cnec -> computeMarginInterval(cnec, flowResult, sensitivityResult, linearizationPoint))
            .collect(Collectors.toSet());

        // NaN upper bounds are sorted last by Double.compare, and then give a NaN UB which keeps every CNEC
        Optional<MarginInterval> witness = intervals.stream()
            .filter(interval -> !notWitnessCnecs.contains(interval.cnec()))
            .min(Comparator.comparingDouble(MarginInterval::high).thenComparing(interval -> interval.cnec().getId()));
        if (witness.isEmpty()) {
            // without a witness, nothing guarantees that the minimum margin stays bounded by a remaining CNEC
            TECHNICAL_LOGS.debug("Linear problem CNEC filtering: kept {} of {} optimized flow CNECs", candidates.size(), candidates.size());
            return Set.of();
        }
        double upperBound = witness.get().high();
        FlowCnec witnessCnec = witness.get().cnec();

        Set<FlowCnec> removableCnecs = intervals.stream()
            .filter(interval -> !protectedCnecs.contains(interval.cnec()))
            .filter(interval -> !interval.cnec().equals(witnessCnec))
            .filter(interval -> isRemovable(interval, upperBound))
            .map(MarginInterval::cnec)
            .collect(Collectors.toSet());

        TECHNICAL_LOGS.debug("Linear problem CNEC filtering: kept {} of {} optimized flow CNECs", candidates.size() - removableCnecs.size(), candidates.size());
        return removableCnecs;
    }

    // Comparisons involving NaN are false, so a NaN bound always keeps the CNEC
    private boolean isRemovable(MarginInterval interval, double upperBound) {
        return switch (criterion) {
            case MarginCriterion.Absolute absolute -> interval.low() > upperBound
                || absolute.secureThreshold().isPresent() && interval.low() >= absolute.secureThreshold().getAsDouble();
            case MarginCriterion.Relative ignored -> interval.absoluteLow() >= 0 && interval.low() > upperBound;
        };
    }

    private MarginInterval computeMarginInterval(FlowCnec cnec, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult linearizationPoint) {
        double absoluteLow = Double.POSITIVE_INFINITY;
        double low = Double.POSITIVE_INFINITY;
        double high = Double.POSITIVE_INFINITY;
        for (TwoSides side : cnec.getMonitoredSides()) {
            // same reference flow and thresholds as the linear problem
            double margin = cnec.computeMargin(flowResult.getFlow(cnec, side, unit), side, unit);
            double delta = computeMaxMarginVariation(cnec, side, sensitivityResult, linearizationPoint);
            double scale = getRelativeMarginCoefficient(cnec, side, flowResult);
            // Math.min propagates NaN
            absoluteLow = Math.min(absoluteLow, margin - delta);
            low = Math.min(low, (margin - delta) / scale);
            high = Math.min(high, (margin + delta) / scale);
        }
        return new MarginInterval(cnec, absoluteLow, low, high);
    }

    private double computeMaxMarginVariation(FlowCnec cnec, TwoSides side, SensitivityResult sensitivityResult, RangeActionActivationResult linearizationPoint) {
        double delta = 0;
        for (RangeActionImpacts.Impact impact : impacts.getImpacts(cnec, side, sensitivityResult)) {
            RangeActionSetpointBounds.Interval bounds = boundsCache.computeIfAbsent(new RangeActionState(impact.rangeAction(), impact.state()),
                key -> RangeActionSetpointBounds.getSetpointVariableBounds(perimeter, key.rangeAction(), key.state(), prePerimeterSetpoints, Double.POSITIVE_INFINITY));
            double setpoint = linearizationPoint.getOptimizedSetpoint(impact.rangeAction(), impact.state());
            // an infinite bound gives an infinite variation; NaN propagates
            double maxDistance = Math.max(bounds.max() - setpoint, setpoint - bounds.min());
            delta += Math.abs(impact.sensitivity()) * maxDistance;
        }
        return delta;
    }

    /**
     * Same coefficient as in MaxMinRelativeMarginFiller; 1 for absolute margins.
     * A non-positive coefficient would invert the comparisons, so it yields NaN, which keeps the CNEC.
     */
    private double getRelativeMarginCoefficient(FlowCnec cnec, TwoSides side, FlowResult flowResult) {
        if (!(criterion instanceof MarginCriterion.Relative relative)) {
            return 1.;
        }
        FlowResult ptdfSource = relative.ptdfSource() == null ? flowResult : relative.ptdfSource();
        double ptdfZonalSum = ptdfSource.getPtdfZonalSum(cnec, side);
        double coefficient = Double.isNaN(ptdfZonalSum) ? relative.ptdfSumLowerBound() : Math.max(ptdfZonalSum, relative.ptdfSumLowerBound());
        return coefficient > 0 ? coefficient : Double.NaN;
    }
}
