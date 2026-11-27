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
     * computes also the capacity of each layer "m_k": the increase of the CT value
     *  that can be dispatched to generators k, ..., n (after all previous generators are saturated)
     *  proportionally and without overloading any other generators.
     *
     *  It also computes the last layer that could saturate generators, if a maximal CT values was applied
     *  (useful for the linear model implementation).
     *
     * @param glskWeights               the proportional glsk weights
     * @param network                   the network
     * @param tolerance                 a tolerance (negligible elements could be discarded)
     * @param maxDownwardMagnitude      the maximal admissible setpoint decrease, as a negative value
     * @param maxUpwardMagnitude        the maximal admissible positive setpoint increase
     */
    public static CounterTradeGeneratorOrdering compute(
            Map<String, Double> glskWeights,
            Network network,
            double tolerance,
            double maxDownwardMagnitude,
            double maxUpwardMagnitude) {
        // Notation: lambda_i = normalised glsk weight, v+_i / v-_i = up / down available capacity, P0_i = initial targetP,
        // M+ = maxUpwardMagnitude, M- = -maxDownwardMagnitude (both positive), eps = tolerance.
        Set<String> zeroWeightIds = new HashSet<>();
        Map<String, Double> nonZeroWeights = new HashMap<>();
        List<RawAllocation> upwardRaw = new ArrayList<>();
        List<RawAllocation> downwardRaw = new ArrayList<>();
        Map<String, Double> initialTargetP = new HashMap<>();

        if (tolerance < 0) {
            throw new OpenRaoException("tolerance must be non-negative");
        }

        glskWeights.forEach((generatorId, weight) -> {
            if (!(weight > tolerance) ||
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
        // non-null: missing generators were filtered into zeroWeightIds above.
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
        List<GlskGeneratorAllocation> upward = withLayerCapacity(upwardRaw);
        List<GlskGeneratorAllocation> downward = withLayerCapacity(downwardRaw);
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
                zeroWeightIds);
    }

    private static boolean hasUsableActivePowerRange(Generator generator) {
        return generator.getTargetP() >= generator.getMinP()
                && generator.getMaxP() >= generator.getTargetP();
    }

    /**
     * Generators are sorted by v_k / lambda_k; layer k is the CT range where generators k..n-1 move together, until k saturates.
     * Notation: v_k = available capacity, lambda_k = weight, S_k = sum of weights of generators k..n-1, T_k = sum of m_h / S_h over h before k.
     * Computes the layer capacity {@code m_k = S_k * (v_k / lambda_k - T_k)}, clamped at 0.
     */
    private static List<GlskGeneratorAllocation> withLayerCapacity(List<RawAllocation> sorted) {
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
     * Notation: x_h = CT magnitude in layer h, lambda_i = weight, S_h = sum of weights of generators h..n-1, {@code R_i = sum_{h<=i} x_h / S_h}.
     * Returns the MW dispatch {@code lambda_i * R_i} of each generator i, for the x_h given by the MIP or by layerAllocation.
     */
    public static Map<String, Double> computeDispatchQuantity(
            List<GlskGeneratorAllocation> sortedAllocations,
            Map<String, Double> ctXValue) {

        int n = sortedAllocations.size();
        double[] suffixSum = new double[n + 1];
        for (int i = n - 1; i >= 0; i--) {
            suffixSum[i] = suffixSum[i + 1] + sortedAllocations.get(i).weight();
        }

        double[] ratio = new double[n + 1];
        for (int h = 0; h < n; h++) {
            String generatorId = sortedAllocations.get(h).generatorId();
            double xh = ctXValue.getOrDefault(generatorId, 0.0);
            ratio[h + 1] = ratio[h] + xh / suffixSum[h];
        }

        Map<String, Double> result = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            GlskGeneratorAllocation allocation = sortedAllocations.get(i);
            result.put(allocation.generatorId(), allocation.weight() * ratio[i + 1]);
        }
        return result;
    }

    /**
     * Notation: M = requested CT magnitude, m_k = layer capacity, x_k = CT magnitude in layer k.
     * Fills layers in order: {@code x_k = min(remaining, m_k)}.
     */
    public static Map<String, Double> layerAllocation(List<GlskGeneratorAllocation> order, double magnitude) {
        Map<String, Double> x = new LinkedHashMap<>();
        double remaining = magnitude;
        for (GlskGeneratorAllocation allocation : order) {
            double xh = Math.min(remaining, allocation.layerCapacity());
            x.put(allocation.generatorId(), xh);
            remaining -= xh;
        }
        return x; // any leftover means the setpoint exceeds total capacity
    }

    /**
     * Notation: K = lastStage, lambda_i = weight, s_i = sensitivity of generator i, S_h = sum of weights of generators h..n-1.
     * Returns sigma_h, the sensitivity of one MW of CT in layer h: {@code sigma_h = sum_{i>=h} lambda_i * s_i / S_h},
     * where the sum over {@code i >= K} is passed as tailWeightedSensitivity.
     */
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

    /**
     * Notation: C_k = m_0 + ... + m_k (cumulative layer capacity), M = maxMagnitude.
     * Returns the first k with {@code C_k >= M}, or the last layer index if M is never reached.
     */
    public static int lastReachableLayer(List<GlskGeneratorAllocation> order, double maxMagnitude) {
        if (order.isEmpty()) {
            return 0;
        }
        double cumulative = 0.0;
        for (int k = 0; k < order.size(); k++) {
            cumulative += order.get(k).layerCapacity();
            if (cumulative >= maxMagnitude) {
                return k;
            }
        }
        return order.size() - 1;
    }

    private record RawAllocation(String generatorId, double weight, double availableCapacity, double saturationRatio) { }
}
