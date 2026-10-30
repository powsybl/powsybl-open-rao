/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class FlowResult extends AbstractExtension<RaoResult> {
    private static final String EXTENSION_NAME = "flow-results";
    private static final List<Unit> UNITS = List.of(Unit.AMPERE, Unit.MEGAWATT);

    private final Map<FlowCnec, FlowCnecResult> results;

    private FlowResult(Map<FlowCnec, FlowCnecResult> results) {
        this.results = new HashMap<>(results);
    }

    public FlowResult(FlowResult flowResultToCopy) {
        this(flowResultToCopy.results);
    }

    /**
     * Returns the flow on a {@link FlowCnec} after a given {@link Instant} and in a
     * given {@link Unit}.
     *
     * @param instant  The optimized instant to be studied (set to null to access initial results)
     * @param flowCnec The branch to be studied.
     * @param side     The side of the branch to be queried.
     * @param unit     The unit in which the flow is queried. The only accepted values are MEGAWATT or AMPERE.
     * @return The flow on the branch at the optimization state in the given unit.
     */
    public double getFlow(Instant instant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return safeGetResult(flowCnec).flatMap(flowCnecResult -> flowCnecResult.getFlow(instant, side, unit)).orElse(Double.NaN);
    }

    /**
     * Returns the margin on a {@link FlowCnec} at a given {@link Instant} and in a
     * given {@link Unit}. It is basically the difference between the flow and the most constraining threshold in the
     * flow direction of the given branch. If it is negative, the branch is under constraint.
     *
     * @param instant  The optimized instant to be studied (set to null to access initial results)
     * @param flowCnec The branch to be studied.
     * @param unit     The unit in which the margin is queried. The only accepted values are MEGAWATT or AMPERE.
     * @return The margin on the branch at the optimization state in the given unit.
     */
    public double getMargin(Instant instant, FlowCnec flowCnec, Unit unit) {
        return safeGetResult(flowCnec).flatMap(flowCnecResult -> flowCnecResult.getMargin(instant, unit)).orElse(Double.NaN);
    }

    /**
     * Returns the relative margin (according to CORE D-2 CC methodology) on a {@link FlowCnec} at a given
     * {@link Instant} and in a given {@link Unit}. If the margin is negative, returns it directly (same
     * value as {@code getMargin} method. If the margin is positive Returns this value divided by the sum of the zonal
     * PTDFs on this branch of the studied zone. Zones to include in this computation are defined in the
     * RAO. If it is negative the branch is under constraint. If the PTDFs are not defined in the
     * computation or the sum of them is null, this method could return {@code Double.NaN} values.
     *
     * @param instant  The optimized instant to be studied (set to null to access initial results)
     * @param flowCnec The branch to be studied.
     * @param unit     The unit in which the relative margin is queried. Only accepted values are MEGAWATT or AMPERE.
     * @return The relative margin on the branch at the optimization state in the given unit.
     */
    public double getRelativeMargin(Instant instant, FlowCnec flowCnec, Unit unit) {
        return safeGetResult(flowCnec).flatMap(flowCnecResult -> flowCnecResult.getRelativeMargin(instant, unit)).orElse(Double.NaN);
    }

    /**
     * Returns the value of commercial flow (according to CORE D-2 CC methodology) on a {@link FlowCnec} at a given
     * {@link Instant} and in a given {@link Unit}. If the branch is not considered as a branch on which the
     * loop flows are monitored, this method could return {@code Double.NaN} values.
     *
     * @param instant  The optimized instant to be studied (set to null to access initial results)
     * @param flowCnec The branch to be studied.
     * @param unit     The unit in which the commercial flow is queried. Only accepted values are MEGAWATT or AMPERE.
     * @return The commercial flow on the branch at the optimization state in the given unit.
     */
    public double getCommercialFlow(Instant instant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return safeGetResult(flowCnec).flatMap(flowCnecResult -> flowCnecResult.getCommercialFlow(instant, side, unit)).orElse(Double.NaN);
    }

    /**
     * Returns the value of loop flow (according to CORE D-2 CC methodology) on a {@link FlowCnec} at a given
     * {@link Instant} and in a given {@link Unit}. If the branch is not considered as a branch on which the
     * loop flows are monitored, this method could return {@code Double.NaN} values.
     *
     * @param instant  The optimized instant to be studied (set to null to access initial results)
     * @param flowCnec The branch to be studied.
     * @param unit     The unit in which the loop flow is queried. Only accepted values are MEGAWATT or AMPERE.
     * @return The loop flow on the branch at the optimization state in the given unit.
     */
    public double getLoopFlow(Instant instant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return safeGetResult(flowCnec).flatMap(flowCnecResult -> flowCnecResult.getLoopFlow(instant, side, unit)).orElse(Double.NaN);
    }

    /**
     * Returns the sum of the computation areas' zonal PTDFs on a {@link FlowCnec} at a given
     * {@link Instant}. If the computation does not consider PTDF values or if the RAO does
     * not define any list of considered areas, this method could return {@code Double.NaN} values.
     *
     * @param instant  The optimized instant to be studied (set to null to access initial results)
     * @param flowCnec The branch to be studied.
     * @return The sum of the computation areas' zonal PTDFs on the branch at the optimization state.
     */
    public double getPtdfZonalSum(Instant instant, FlowCnec flowCnec, TwoSides side) {
        return safeGetResult(flowCnec).flatMap(flowCnecResult -> flowCnecResult.getPtdfZonalSum(instant, side)).orElse(Double.NaN);
    }

    private Optional<FlowCnecResult> safeGetResult(FlowCnec flowCnec) {
        return Optional.ofNullable(results.get(flowCnec));
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    // result data model

    private static class FlowCnecResult {
        private final FlowCnec flowCnec;
        private final ElementaryFlowCnecMeasurement initialMeasurement;
        private final Map<Instant, ElementaryFlowCnecMeasurement> measurementsPerInstant;

        FlowCnecResult(FlowCnec flowCnec) {
            this.flowCnec = flowCnec;
            this.initialMeasurement = new ElementaryFlowCnecMeasurement(flowCnec);
            this.measurementsPerInstant = new HashMap<>();
        }

        void addFlowMeasurement(double flow, Instant instant, TwoSides side, Unit unit) {
            getOrCreateMeasurement(instant).addFlowMeasurement(flow, side, unit);
        }

        void addCommercialFlowMeasurement(double commercialFlow, Instant instant, TwoSides side, Unit unit) {
            getOrCreateMeasurement(instant).addCommercialFlowMeasurement(commercialFlow, side, unit);
        }

        void addPtdfZonalSumMeasurement(double ptdfZonalSum, Instant instant, TwoSides side) {
            getOrCreateMeasurement(instant).addPtdfZonalSumMeasurement(ptdfZonalSum, side);
        }

        Optional<Double> getFlow(Instant instant, TwoSides side, Unit unit) {
            return getMeasurement(instant).getFlow(side, unit);
        }

        Optional<Double> getMargin(Instant instant, Unit unit) {
            return getMeasurement(instant).getMargin(unit);
        }

        Optional<Double> getRelativeMargin(Instant instant, Unit unit) {
            return getMeasurement(instant).getRelativeMargin(unit);
        }

        Optional<Double> getCommercialFlow(Instant instant, TwoSides side, Unit unit) {
            return getMeasurement(instant).getCommercialFlow(side, unit);
        }

        Optional<Double> getLoopFlow(Instant instant, TwoSides side, Unit unit) {
            return getMeasurement(instant).getLoopFlow(side, unit);
        }

        Optional<Double> getPtdfZonalSum(Instant instant, TwoSides side) {
            return getMeasurement(instant).getPtdfZonalSum(side);
        }

        private ElementaryFlowCnecMeasurement getOrCreateMeasurement(Instant instant) {
            return instant == null ? initialMeasurement : measurementsPerInstant.computeIfAbsent(instant, k -> new ElementaryFlowCnecMeasurement(flowCnec));
        }

        private ElementaryFlowCnecMeasurement getMeasurement(Instant instant) {
            Instant actualInstant = getPreviousInstantWithResult(instant);
            return actualInstant == null ? initialMeasurement : measurementsPerInstant.get(actualInstant);
        }

        private Instant getPreviousInstantWithResult(Instant instant) {
            if (instant == null) {
                return null;
            } else if (measurementsPerInstant.containsKey(instant)) {
                return instant;
            } else {
                // TODO: might be wrong if previous results are only available for the other unit
                return measurementsPerInstant.keySet()
                    .stream()
                    .filter(otherInstant -> otherInstant.comesBefore(instant))
                    .max(Comparator.comparing(Instant::getOrder))
                    .orElse(null);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FlowCnecResult that = (FlowCnecResult) o;
            return flowCnec.equals(that.flowCnec);
        }

        @Override
        public int hashCode() {
            return Objects.hash(flowCnec);
        }
    }

    private static class ElementaryFlowCnecMeasurement {
        private final FlowCnec flowCnec;
        private final Map<SidedUnit, Double> flows;
        private final Map<Unit, Double> margins;
        private final Map<Unit, Double> relativeMargins;
        private final Map<SidedUnit, Double> commercialFlows;
        private final Map<SidedUnit, Double> loopFlows;
        private final Map<TwoSides, Double> ptdfZonalSums;

        ElementaryFlowCnecMeasurement(FlowCnec flowCnec) {
            this.flowCnec = flowCnec;
            this.flows = new HashMap<>();
            this.margins = new HashMap<>();
            this.relativeMargins = new HashMap<>();
            this.commercialFlows = new HashMap<>();
            this.loopFlows = new HashMap<>();
            this.ptdfZonalSums = new HashMap<>();
        }

        void addFlowMeasurement(double flow, TwoSides side, Unit unit) {
            SidedUnit sidedUnit = new SidedUnit(side, unit);
            flows.put(sidedUnit, flow);

            // update loop flow, if applicable
            if (commercialFlows.containsKey(sidedUnit)) {
                loopFlows.put(sidedUnit, flow - commercialFlows.get(sidedUnit));
            }

            // update margin
            double currentMargin = margins.getOrDefault(unit, Double.POSITIVE_INFINITY);
            double newMargin = computeMargin(flowCnec, flow, side, unit);
            margins.put(unit, Math.min(currentMargin, newMargin));

            // update relative margin, if applicable
            if (ptdfZonalSums.containsKey(side)) {
                relativeMargins.put(unit, computeRelativeMargin(margins.get(unit), ptdfZonalSums.get(side)));
            }
        }

        void addCommercialFlowMeasurement(double commercialFlow, TwoSides side, Unit unit) {
            SidedUnit sidedUnit = new SidedUnit(side, unit);
            commercialFlows.put(sidedUnit, commercialFlow);

            // update loop flow, if applicable
            if (flows.containsKey(sidedUnit)) {
                loopFlows.put(sidedUnit, flows.get(sidedUnit) - commercialFlow);
            }
        }

        void addPtdfZonalSumMeasurement(double ptdfZonalSum, TwoSides side) {
            ptdfZonalSums.put(side, ptdfZonalSum);

            // update relative margin, if applicable
            for (Unit unit : UNITS) {
                if (margins.containsKey(unit)) {
                    relativeMargins.put(unit, computeRelativeMargin(margins.get(unit), ptdfZonalSum));
                }
            }
        }

        Optional<Double> getFlow(TwoSides side, Unit unit) {
            return Optional.ofNullable(flows.get(new SidedUnit(side, unit)));
        }

        Optional<Double> getMargin(Unit unit) {
            return Optional.ofNullable(margins.get(unit));
        }

        Optional<Double> getRelativeMargin(Unit unit) {
            return Optional.ofNullable(relativeMargins.get(unit));
        }

        Optional<Double> getCommercialFlow(TwoSides side, Unit unit) {
            return Optional.ofNullable(commercialFlows.get(new SidedUnit(side, unit)));
        }

        Optional<Double> getLoopFlow(TwoSides side, Unit unit) {
            return Optional.ofNullable(loopFlows.get(new SidedUnit(side, unit)));
        }

        Optional<Double> getPtdfZonalSum(TwoSides side) {
            return Optional.ofNullable(ptdfZonalSums.get(side));
        }
    }

    private record SidedUnit(TwoSides side, Unit unit) {
    }

    private static double computeMargin(FlowCnec flowCnec, double flow, TwoSides side, Unit unit) {
        return Math.min(
            flow - flowCnec.getLowerBound(side, unit).orElse(Double.NEGATIVE_INFINITY),
            flowCnec.getUpperBound(side, unit).orElse(Double.POSITIVE_INFINITY) - flow
        );
    }

    private static double computeRelativeMargin(double margin, double ptdfZonalSum) {
        return margin <= 0 ? margin : Math.min(Double.POSITIVE_INFINITY, margin / ptdfZonalSum);
    }

    // builder

    public static FlowResultBuilder build() {
        return new FlowResultBuilder();
    }

    public static final class FlowResultBuilder {
        private final Map<FlowCnec, FlowCnecResult> results;

        private FlowResultBuilder() {
            results = new HashMap<>();
        }

        public FlowResultBuilder addFlowMeasurement(double flow, Instant instant, FlowCnec flowCnec, TwoSides side, Unit unit) {
            results.computeIfAbsent(flowCnec, FlowCnecResult::new).addFlowMeasurement(flow, instant, side, unit);
            return this;
        }

        public FlowResultBuilder addCommercialFlowMeasurement(double commercialFlow, Instant instant, FlowCnec flowCnec, TwoSides side, Unit unit) {
            results.computeIfAbsent(flowCnec, FlowCnecResult::new).addCommercialFlowMeasurement(commercialFlow, instant, side, unit);
            return this;
        }

        public FlowResultBuilder addPtdfZonalSumMeasurement(double ptdfZonalSum, Instant instant, FlowCnec flowCnec, TwoSides side) {
            results.computeIfAbsent(flowCnec, FlowCnecResult::new).addPtdfZonalSumMeasurement(ptdfZonalSum, instant, side);
            return this;
        }

        public FlowResult build() {
            return new FlowResult(results);
        }
    }
}
