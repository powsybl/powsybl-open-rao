/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class FlowResult extends AbstractExtension<RaoResult> {
    private static final String EXTENSION_NAME = "flow-results";

    private final Map<FlowCnec, FlowCnecResult> results;

    public FlowResult() {
        this.results = new HashMap<>();
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    /**
     * Provides the flow on a specific {@link FlowCnec} for a given {@link Instant}, {@link TwoSides}, and {@link Unit}.
     * If the information is unavailable, it returns {@code Double.NaN}.
     *
     * @param optimizedInstant The optimized instant to be analyzed (set to null to access initial results).
     * @param flowCnec         The flow CNEC whose flow is to be retrieved.
     * @param side             The side of the flow CNEC to be analyzed.
     * @param unit             The unit in which the flow is queried. Allowed units are {@link Unit#MEGAWATT} and {@link Unit#AMPERE}.
     * @return The flow value in the specified unit or {@code Double.NaN} if the information is not available.
     * @throws OpenRaoException If the unit is not supported (only {@link Unit#MEGAWATT} and {@link Unit#AMPERE} are allowed).
     */
    public double getFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        checkUnit(unit);
        return results.getOrDefault(flowCnec, FlowCnecResult.of(flowCnec)).getFlow(optimizedInstant, side, unit).orElse(Double.NaN);
    }

    /**
     * Provides the loop flow on a specific {@link FlowCnec} for a given {@link Instant}, {@link TwoSides}, and
     * {@link Unit}. If the information is unavailable, it returns {@code Double.NaN}.
     *
     * @param optimizedInstant The optimized instant to be analyzed (set to null to access initial results).
     * @param flowCnec         The flow CNEC whose loop flow is to be retrieved.
     * @param side             The side of the flow CNEC to be analyzed.
     * @param unit             The unit in which the loop flow is queried. Allowed units are {@link Unit#MEGAWATT} and {@link Unit#AMPERE}.
     * @return The loop flow value in the specified unit or {@code Double.NaN} if the information is not available.
     * @throws OpenRaoException If the unit is not supported (only {@link Unit#MEGAWATT} and {@link Unit#AMPERE} are allowed).
     */
    public double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        checkUnit(unit);
        return results.getOrDefault(flowCnec, FlowCnecResult.of(flowCnec)).getLoopFlow(optimizedInstant, side, unit).orElse(Double.NaN);
    }

    /**
     * Provides the commercial flow on a specific {@link FlowCnec} for a given {@link Instant}, {@link TwoSides}, and
     * {@link Unit}. If the information is unavailable, it returns {@code Double.NaN}.
     *
     * @param optimizedInstant The optimized instant to be analyzed (set to null to access initial results).
     * @param flowCnec         The flow CNEC whose commercial flow is to be retrieved.
     * @param side             The side of the flow CNEC to be analyzed.
     * @param unit             The unit in which the commercial flow is queried. Allowed units are {@link Unit#MEGAWATT} and {@link Unit#AMPERE}.
     * @return The commercial flow value in the specified unit or {@code Double.NaN} if the information is not available.
     * @throws OpenRaoException If the unit is not supported (only {@link Unit#MEGAWATT} and {@link Unit#AMPERE} are allowed).
     */
    public double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        checkUnit(unit);
        return results.getOrDefault(flowCnec, FlowCnecResult.of(flowCnec)).getCommercialFlow(optimizedInstant, side, unit).orElse(Double.NaN);
    }

    /**
     * Computes the zonal PTDF (Power Transfer Distribution Factor) sum for a given {@link FlowCnec} for a given
     * {@link Instant}, {@link TwoSides}, and {@link Unit}. If the information is unavailable, it returns {@code Double.NaN}.
     *
     * @param optimizedInstant The optimized instant to be analyzed (set to null to access initial results).
     * @param flowCnec         The flow CNEC whose zonal PTDF sum is to be retrieved.
     * @param side             The side of the flow CNEC to be analyzed.
     * @return The zonal PTDF sum or {@code Double.NaN} if the information is not available.
     * @throws OpenRaoException If the unit is not supported (only {@link Unit#MEGAWATT} and {@link Unit#AMPERE} are allowed).
     */
    public double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side) {
        return results.getOrDefault(flowCnec, FlowCnecResult.of(flowCnec)).getPtdfZonalSum(optimizedInstant, side).orElse(Double.NaN);
    }

    /**
     * Provides the margin on a specific {@link FlowCnec} for a given {@link Instant} and {@link Unit}. If the
     * information is unavailable, it returns {@code Double.NaN}.
     *
     * @param optimizedInstant The optimized instant to be analyzed (set to null to access initial results).
     * @param flowCnec         The flow CNEC whose margin is to be retrieved.
     * @param unit             The unit in which the margin is queried. Allowed units are {@link Unit#MEGAWATT} and {@link Unit#AMPERE}.
     * @return The margin value in the specified unit or {@code Double.NaN} if the information is not available.
     * @throws OpenRaoException If the unit is not supported (only {@link Unit#MEGAWATT} and {@link Unit#AMPERE} are allowed).
     */
    public double getMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        checkUnit(unit);
        return results.getOrDefault(flowCnec, FlowCnecResult.of(flowCnec)).getMargin(optimizedInstant, unit).orElse(Double.NaN);
    }

    /**
     * Provides the relative margin on a specific {@link FlowCnec} for a given {@link Instant} and {@link Unit}. If the
     * information is unavailable, it returns {@code Double.NaN}.
     *
     * @param optimizedInstant The optimized instant to be analyzed (set to null to access initial results).
     * @param flowCnec         The flow CNEC whose relative margin is to be retrieved.
     * @param unit             The unit in which the relative margin is queried. Allowed units are {@link Unit#MEGAWATT} and {@link Unit#AMPERE}.
     * @return The relative margin value in the specified unit or {@code Double.NaN} if the information is not available.
     * @throws OpenRaoException If the unit is not supported (only {@link Unit#MEGAWATT} and {@link Unit#AMPERE} are allowed).
     */
    public double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        checkUnit(unit);
        return results.getOrDefault(flowCnec, FlowCnecResult.of(flowCnec)).getRelativeMargin(optimizedInstant, unit).orElse(Double.NaN);
    }

    public void addMeasurement(double flow, Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        addMeasurement(flow, Double.NaN, Double.NaN, optimizedInstant, flowCnec, side, unit);
    }

    public void addMeasurement(double flow, double commercialFlow, double ptdfZonalSum, Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        checkUnit(unit);
        results.computeIfAbsent(flowCnec, k -> FlowCnecResult.of(flowCnec)).addFlowMeasurement(optimizedInstant, side, unit, flow, commercialFlow, ptdfZonalSum);
    }

    private static void checkUnit(Unit unit) {
        if (!Unit.MEGAWATT.equals(unit) || !Unit.AMPERE.equals(unit)) {
            throw new OpenRaoException("FlowCNEC results are only allowed for megawatts and amperes.");
        }
    }

    // serialization

    public void serialize(JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartArray();
        for (FlowCnec flowCnec : results.keySet().stream().sorted(Comparator.comparing(Identifiable::getId)).toList()) {
            results.get(flowCnec).serialize(jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    // result data model

    // TODO: think of a more efficient data storage / serialization mode
    private static class FlowCnecResult {
        private final FlowCnec flowCnec;
        private final Map<SidedFlowData, Double> flows = new HashMap<>();
        private final Map<SidedFlowData, Double> loopFlows = new HashMap<>();
        private final Map<SidedFlowData, Double> commercialFlows = new HashMap<>();
        private final Map<SidedFlowData, Double> ptdfZonalSums = new HashMap<>();
        private final Map<UnsidedFlowData, Double> margins = new HashMap<>();
        private final Map<UnsidedFlowData, Double> relativeMargins = new HashMap<>();

        public FlowCnecResult(FlowCnec flowCnec) {
            this.flowCnec = flowCnec;
        }

        public FlowCnec getFlowCnec() {
            return flowCnec;
        }

        public Optional<Double> getFlow(Instant instant, TwoSides side, Unit unit) {
            return getAdequateSidedResult(instant, side, unit, flows);
        }

        public Optional<Double> getLoopFlow(Instant instant, TwoSides side, Unit unit) {
            return getAdequateSidedResult(instant, side, unit, loopFlows);
        }

        public Optional<Double> getCommercialFlow(Instant instant, TwoSides side, Unit unit) {
            return getAdequateSidedResult(instant, side, unit, commercialFlows);
        }

        public Optional<Double> getPtdfZonalSum(Instant instant, TwoSides side) {
            SidedFlowData sidedFlowData = new SidedFlowData(instant, side, null);
            return ptdfZonalSums.containsKey(sidedFlowData) ? Optional.of(ptdfZonalSums.get(sidedFlowData)) : Optional.empty();
        }

        public Optional<Double> getMargin(Instant instant, Unit unit) {
            return getAdequateUnsidedResult(instant, unit, margins);
        }

        public Optional<Double> getRelativeMargin(Instant instant, Unit unit) {
            return getAdequateUnsidedResult(instant, unit, relativeMargins);
        }

        public void addFlowMeasurement(Instant instant, TwoSides side, Unit unit, double flow, double commercialFlow, double ptdfZonalSum) {
            checkUnit(unit);
            SidedFlowData sidedFlowData = new SidedFlowData(instant, side, unit);
            UnsidedFlowData unsidedFlowData = new UnsidedFlowData(instant, unit);
            boolean isFlowProvided = Double.isNaN(flow);
            boolean isCommercialFlowProvided = Double.isNaN(commercialFlow);
            boolean isPtdfZonalSumProvided = Double.isNaN(ptdfZonalSum);

            if (isFlowProvided) {
                flows.put(sidedFlowData, flow);
                double currentMargin = margins.getOrDefault(unsidedFlowData, Double.MAX_VALUE);
                margins.put(unsidedFlowData, Math.min(currentMargin, computeMargin(flow, unit)));
            }
            if (isCommercialFlowProvided) {
                commercialFlows.put(sidedFlowData, commercialFlow);
            }
            if (isFlowProvided && isCommercialFlowProvided) {
                loopFlows.put(sidedFlowData, flow - commercialFlow);
            }
            if (!isPtdfZonalSumProvided) {
                ptdfZonalSums.put(new SidedFlowData(instant, side, null), ptdfZonalSum);
                double currentRelativeMargin = relativeMargins.getOrDefault(unsidedFlowData, Double.MAX_VALUE);
                Optional<Double> currentMargin = Optional.ofNullable(margins.get(unsidedFlowData));
                if (currentMargin.isPresent()) {
                    relativeMargins.put(unsidedFlowData, Math.min(currentRelativeMargin, computeRelativeMargin(flow, ptdfZonalSum)));
                }
            }
        }

        private double computeMargin(double flow, Unit unit) {
            double marginOne = Math.min(
                flow - flowCnec.getLowerBound(TwoSides.ONE, unit).orElse(-Double.MAX_VALUE),
                flowCnec.getUpperBound(TwoSides.ONE, unit).orElse(Double.MAX_VALUE) - flow
            );
            double marginTwo = Math.min(
                flow - flowCnec.getLowerBound(TwoSides.TWO, unit).orElse(-Double.MAX_VALUE),
                flowCnec.getUpperBound(TwoSides.TWO, unit).orElse(Double.MAX_VALUE) - flow
            );
            return Math.min(marginOne, marginTwo);
        }

        private double computeRelativeMargin(double margin, double ptdfZonalSum) {
            return margin <= 0 ? margin : margin / ptdfZonalSum;
        }

        public static FlowCnecResult of(FlowCnec flowCnec) {
            return new FlowCnecResult(flowCnec);
        }

        private static Optional<Double> getAdequateSidedResult(Instant instant, TwoSides side, Unit unit, Map<SidedFlowData, Double> map) {
            checkUnit(unit);
            SidedFlowData sidedFlowData = new SidedFlowData(instant, side, unit);
            return map.containsKey(sidedFlowData) ? Optional.of(map.get(sidedFlowData)) : Optional.empty();
        }

        private static Optional<Double> getAdequateUnsidedResult(Instant instant, Unit unit, Map<UnsidedFlowData, Double> map) {
            checkUnit(unit);
            UnsidedFlowData unsidedFlowData = new UnsidedFlowData(instant, unit);
            return map.containsKey(unsidedFlowData) ? Optional.of(map.get(unsidedFlowData)) : Optional.empty();
        }

        public void serialize(JsonGenerator jsonGenerator) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("flowCnecId", flowCnec.getId());
            jsonGenerator.writeObjectFieldStart("measurements");
            serializeResultsForUnit(jsonGenerator, Unit.MEGAWATT);
            serializeResultsForUnit(jsonGenerator, Unit.AMPERE);
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndObject();
        }

        private void serializeResultsForUnit(JsonGenerator jsonGenerator, Unit unit) throws IOException {
            if (hasResultsForUnit(unit)) {
                jsonGenerator.writeArrayFieldStart(unit.name().toLowerCase());
                if (hasResultsForUnitAndInstant(unit, null)) {
                    serializeMeasurementsForInstant(jsonGenerator, null, unit);
                }
                for (Instant instant : getInstantsWithResults(unit)) {
                    serializeMeasurementsForInstant(jsonGenerator, instant, unit);
                }
                jsonGenerator.writeEndArray();
            }
        }

        private boolean hasResultsForUnit(Unit unit) {
            return flows.keySet().stream().anyMatch(sidedFlowData -> sidedFlowData.unit() == unit) ||
                commercialFlows.keySet().stream().anyMatch(sidedFlowData -> sidedFlowData.unit() == unit) ||
                ptdfZonalSums.keySet().stream().anyMatch(sidedFlowData -> sidedFlowData.unit() == unit);
        }

        private boolean hasResultsForUnitAndInstant(Unit unit, Instant instant) {
            return flows.keySet().stream().anyMatch(sidedFlowData -> sidedFlowData.unit() == unit && sidedFlowData.instant() == instant) ||
                commercialFlows.keySet().stream().anyMatch(sidedFlowData -> sidedFlowData.unit() == unit && sidedFlowData.instant() == instant) ||
                ptdfZonalSums.keySet().stream().anyMatch(sidedFlowData -> sidedFlowData.unit() == unit && sidedFlowData.instant() == instant);
        }

        private List<Instant> getInstantsWithResults(Unit unit) {
            Set<Instant> instants = new HashSet<>();
            instants.addAll(flows.keySet().stream().filter(sidedFlowData -> sidedFlowData.unit() == unit).map(SidedFlowData::instant).collect(Collectors.toSet()));
            instants.addAll(commercialFlows.keySet().stream().filter(sidedFlowData -> sidedFlowData.unit() == unit).map(SidedFlowData::instant).collect(Collectors.toSet()));
            instants.addAll(ptdfZonalSums.keySet().stream().filter(sidedFlowData -> sidedFlowData.unit() == unit).map(SidedFlowData::instant).collect(Collectors.toSet()));
            return instants.stream().sorted().toList();
        }

        private void serializeMeasurementsForInstant(JsonGenerator jsonGenerator, Instant instant, Unit unit) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("instant", instant == null ? "initial" : instant.getId());
            double margin = margins.getOrDefault(new UnsidedFlowData(instant, unit), Double.NaN);
            if (!Double.isNaN(margin)) {
                jsonGenerator.writeNumberField("margin", margin);
            }
            double relativeMargin = relativeMargins.getOrDefault(new UnsidedFlowData(instant, unit), Double.NaN);
            if (!Double.isNaN(relativeMargin)) {
                jsonGenerator.writeNumberField("margin", relativeMargin);
            }
            jsonGenerator.writeObjectFieldStart("sides");
            serializeResultsOnSide(jsonGenerator, instant, TwoSides.ONE, unit);
            serializeResultsOnSide(jsonGenerator, instant, TwoSides.TWO, unit);
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndObject();
        }

        private void serializeResultsOnSide(JsonGenerator jsonGenerator, Instant instant, TwoSides side, Unit unit) throws IOException {
            SidedFlowData sidedFlowData = new SidedFlowData(instant, side, unit);
            if (!flows.containsKey(sidedFlowData) && !commercialFlows.containsKey(sidedFlowData) && !ptdfZonalSums.containsKey(sidedFlowData)) {
                return;
            }
            jsonGenerator.writeObjectFieldStart(side.name().toLowerCase());
            double flow = flows.getOrDefault(sidedFlowData, Double.NaN);
            if (!Double.isNaN(flow)) {
                jsonGenerator.writeNumberField("flow", flow);
            }
            double commercialFlow = commercialFlows.getOrDefault(sidedFlowData, Double.NaN);
            if (!Double.isNaN(commercialFlow)) {
                jsonGenerator.writeNumberField("commercialFlow", commercialFlow);
            }
            double loopFlow = loopFlows.getOrDefault(sidedFlowData, Double.NaN);
            if (!Double.isNaN(loopFlow)) {
                jsonGenerator.writeNumberField("loopFlow", loopFlow);
            }
            double ptdfZonalSum = ptdfZonalSums.getOrDefault(sidedFlowData, Double.NaN);
            if (!Double.isNaN(ptdfZonalSum)) {
                jsonGenerator.writeNumberField("ptdfZonalSum", ptdfZonalSum);
            }
            jsonGenerator.writeEndObject();
        }
    }

    private record UnsidedFlowData(Instant instant, Unit unit) {
    }

    private record SidedFlowData(Instant instant, TwoSides side, Unit unit) {
    }
}
