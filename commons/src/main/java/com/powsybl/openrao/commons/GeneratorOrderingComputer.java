/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.function.ToDoubleFunction;

import static java.lang.Math.max;

/**
 * @author Enrico Bettiol {@literal <enrico.bettiol at artelys.com>}
 */
public final class GeneratorOrderingComputer {

    private GeneratorOrderingComputer() { }

    /**
     * Builds the structure containing the different fields
     * used to handle proportional counter trading:
     * given the glsk weights, compute ordered lists for the upward and downward directions
     * having all the generators, their available capacity in the corresponding direction,
     * sorted by the saturation ratio capacity/weight;
     * computes also the cumulative allocation corresponding to "m_k":
     *  it is the upper bound of the variable for the corresponding layer
     *  it is the maximal CT value that can be dispatched to generators k, ..., n
     *  (after all previous generators are saturated)
     *  proportionally and without overloading any other generators.
     *
     *  It also computes the last layer that could saturate generators, if a maximal CT values was applied
     *  (useful for the linear model implementation).
     *
     * @param glskWeights               the proportional glsk weights
     * @param network                   the network
     * @param tolerance                 a tolerance (negligible elements could be discarded)
     * @param maxDownwardMagnitude      the maximal admissible positive setpoint decrease
     * @param maxUpwardMagnitude        the maximal admissible positive setpoint increase
     */
    public static CounterTradeGeneratorOrdering computeFromProportional(
            Map<String, Double> glskWeights,
            Network network,
            double tolerance,
            double maxDownwardMagnitude,
            double maxUpwardMagnitude) {
        Set<String> zeroWeightIds = new HashSet<>();
        Map<String, Double> nonZeroWeights = new HashMap<>();
        List<RawAllocation> upwardRaw = new ArrayList<>();
        List<RawAllocation> downwardRaw = new ArrayList<>();
        Map<String, Double> initialTargetP = new HashMap<>();

        glskWeights.forEach((generatorId, weight) -> {
            if (weight <= tolerance ||
                    network.getGenerator(generatorId) == null ||
                    Double.isNaN(network.getGenerator(generatorId).getTargetP()) ||
                    !hasUsableActivePowerRange(network.getGenerator(generatorId))
            ) {
                zeroWeightIds.add(generatorId);
            } else {
                nonZeroWeights.put(generatorId, weight);
            }
        });
        double keptTotal = nonZeroWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        nonZeroWeights.replaceAll((id, w) -> w / keptTotal);

        nonZeroWeights.forEach((generatorId, weight) -> {
            Generator generator = network.getGenerator(generatorId);
            double targetP = generator.getTargetP();
            initialTargetP.put(generatorId, targetP);
            double upCapacity = Math.clamp(generator.getMaxP() - targetP, 0, maxUpwardMagnitude);
            double downCapacity = Math.clamp(targetP - generator.getMinP(), 0, -maxDownwardMagnitude);
            upwardRaw.add(new RawAllocation(generatorId, weight, upCapacity, upCapacity / weight));
            downwardRaw.add(new RawAllocation(generatorId, weight, downCapacity, downCapacity / weight));
        });

        Comparator<RawAllocation> bySaturationThenId =
                Comparator.comparingDouble(RawAllocation::saturationRatio)
                        .thenComparing(RawAllocation::generatorId);

        upwardRaw.sort(bySaturationThenId);
        downwardRaw.sort(bySaturationThenId);
        List<GlskGeneratorAllocation> upward = withCumulativeAllocation(upwardRaw);
        List<GlskGeneratorAllocation> downward = withCumulativeAllocation(downwardRaw);
        int upwardLastStage = lastReachableLayer(upward, max(0, maxUpwardMagnitude));
        int downwardLastStage = lastReachableLayer(downward, max(0, -maxDownwardMagnitude));

        return new CounterTradeGeneratorOrdering(
                upward,
                downward,
                upwardLastStage,
                max(0, maxUpwardMagnitude),
                downwardLastStage,
                max(0, -maxDownwardMagnitude),
                initialTargetP,
                zeroWeightIds,
                false);
    }

    public static CounterTradeGeneratorOrdering computeFromMeritOrder(
            MeritOrderGlsk meritOrderGlsk,
            Network network,
            double maxDownwardMagnitude,
            double maxUpwardMagnitude) {
        Map<String, Double> initialTargetP = new HashMap<>();
        Set<String> ignoredIds = new HashSet<>();
        List<GlskGeneratorAllocation> upward = meritOrderLayers(meritOrderGlsk.upward(), true, network, initialTargetP, ignoredIds);
        List<GlskGeneratorAllocation> downward = meritOrderLayers(meritOrderGlsk.downward(), false, network, initialTargetP, ignoredIds);
        int upwardLastStage = lastReachableLayer(upward, max(0, maxUpwardMagnitude));
        int downwardLastStage = lastReachableLayer(downward, max(0, -maxDownwardMagnitude));

        return new CounterTradeGeneratorOrdering(
                upward,
                downward,
                upwardLastStage,
                max(0, maxUpwardMagnitude),
                downwardLastStage,
                max(0, -maxDownwardMagnitude),
                initialTargetP,
                ignoredIds,
                true);
    }

    private static List<GlskGeneratorAllocation> meritOrderLayers(
            List<MeritOrderTranche> tranches,
            boolean upward,
            Network network,
            Map<String, Double> initialTargetP,
            Set<String> ignoredIds) {
        Map<String, Double> positions = new HashMap<>();
        List<GlskGeneratorAllocation> layers = new ArrayList<>();
        for (MeritOrderTranche tranche : tranches) {
            String generatorId = tranche.generatorId();
            Generator generator = network.getGenerator(generatorId);
            if (generator == null || !hasUsableActivePowerRange(generator)) {
                ignoredIds.add(generatorId);
                continue;
            }
            double initialP = generator.getTargetP();
            initialTargetP.put(generatorId, initialP);
            double lower = max(tranche.minP(), generator.getMinP());
            double upper = Math.min(tranche.maxP(), generator.getMaxP());
            double position = positions.getOrDefault(generatorId, initialP);
            if (position < lower || position > upper) {
                continue;
            }
            double capacity;
            if (upward) {
                capacity = upper - position;
                positions.put(generatorId, upper);
            } else {
                capacity = position - lower;
                positions.put(generatorId, lower);
            }
            if (capacity > 0) {
                layers.add(new GlskGeneratorAllocation(generatorId, 1.0, capacity, capacity, capacity));
            }
        }
        return layers;
    }

    private static boolean hasUsableActivePowerRange(Generator generator) {
        return generator.getTargetP() >= generator.getMinP()
                && generator.getMaxP() >= generator.getTargetP();
    }

    /** m_k = S_k * (v_k / lambda_k) - S_k * T_k, with T_k = sum_{h=0}^{k-1} m_h / S_h accumulated in one pass. */
    private static List<GlskGeneratorAllocation> withCumulativeAllocation(List<RawAllocation> sorted) {
        int n = sorted.size();
        double[] suffixSum = new double[n + 1];
        for (int i = n - 1; i >= 0; i--) {
            suffixSum[i] = suffixSum[i + 1] + sorted.get(i).weight();
        }

        List<GlskGeneratorAllocation> result = new ArrayList<>(n);
        double runningT = 0.0;

        for (int k = 0; k < n; k++) {
            RawAllocation entry = sorted.get(k);
            double sk = suffixSum[k];
            double mk = max(0, sk * (entry.availableCapacity() / entry.weight()) - sk * runningT);

            result.add(new GlskGeneratorAllocation(
                    entry.generatorId(), entry.weight(), entry.availableCapacity(),
                    entry.saturationRatio(), mk));

            runningT += mk / sk;
        }
        return result;
    }

    /**
     * Computes, for every generator i in the given (already sorted) allocation list,
     * sum_{h=0}^{i} lambda_i^h * x_h, where x_h is the solved CT level corresponding the
     * generator h, solution of the MIP - can be recalculated with layerAllocation() below.
     * It returns the quantity to dispatch to the generators.
     */
    public static Map<String, Double> computeDispatchQuantity(
            List<GlskGeneratorAllocation> sortedAllocations,
            double[] ctXValue) {

        int n = sortedAllocations.size();
        double[] suffixSum = new double[n + 1];
        for (int i = n - 1; i >= 0; i--) {
            suffixSum[i] = suffixSum[i + 1] + sortedAllocations.get(i).weight();
        }

        double[] ratio = new double[n + 1];
        for (int h = 0; h < n; h++) {
            ratio[h + 1] = ratio[h] + ctXValue[h] / suffixSum[h];
        }

        Map<String, Double> result = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            GlskGeneratorAllocation allocation = sortedAllocations.get(i);
            result.put(allocation.generatorId(), allocation.weight() * ratio[i + 1]);
        }
        return result;
    }

    public static Map<String, Double> meritOrderDispatchQuantity(List<GlskGeneratorAllocation> order, double[] ctXValue) {
        Map<String, Double> result = new HashMap<>();
        for (int h = 0; h < order.size(); h++) {
            result.merge(order.get(h).generatorId(), ctXValue[h], Double::sum);
        }
        return result;
    }

    public static double[] layerAllocation(List<GlskGeneratorAllocation> order, double magnitude) {
        double[] x = new double[order.size()];
        double remaining = magnitude;
        for (int h = 0; h < order.size(); h++) {
            double xh = Math.min(remaining, order.get(h).cumulativeAllocation());
            x[h] = xh;
            remaining -= xh;
        }
        return x; // any leftover means the setpoint exceeds total capacity
    }

    public static double[] stageSensitivities(List<GlskGeneratorAllocation> order, int lastStage,
                                              ToDoubleFunction<String> individualSensitivity,
                                              double tailWeightedSensitivity) {
        double[] sigma = new double[lastStage + 1];
        double suffixWeight = 0.0;
        for (int i = lastStage; i < order.size(); i++) {
            suffixWeight += order.get(i).weight();
        }
        double weightedSum = tailWeightedSensitivity; // sum_{i>=K} lambda_i * s_i
        sigma[lastStage] = suffixWeight == 0 ? weightedSum : weightedSum / suffixWeight;
        for (int h = lastStage - 1; h >= 0; h--) {
            GlskGeneratorAllocation a = order.get(h);
            weightedSum += a.weight() * individualSensitivity.applyAsDouble(a.generatorId());
            suffixWeight += a.weight();
            sigma[h] = weightedSum / suffixWeight;
        }
        return sigma;
    }

    public static double[] meritOrderSensitivities(List<GlskGeneratorAllocation> order, int lastStage,
                                                   ToDoubleFunction<String> sensitivityOfGenerator) {
        double[] sigma = new double[lastStage + 1];
        for (int h = 0; h <= lastStage; h++) {
            sigma[h] = sensitivityOfGenerator.applyAsDouble(order.get(h).generatorId());
        }
        return sigma;
    }

    public static int lastReachableLayer(List<GlskGeneratorAllocation> order, double maxMagnitude) {
        if (order.isEmpty()) {
            return 0;
        }
        double cumulative = 0.0;
        for (int k = 0; k < order.size(); k++) {
            cumulative += order.get(k).cumulativeAllocation();
            if (cumulative >= maxMagnitude) {
                return k;
            }
        }
        return order.size() - 1;
    }

    private record RawAllocation(String generatorId, double weight, double availableCapacity, double saturationRatio) { }
}
