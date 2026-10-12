package com.powsybl.openrao.commons;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.function.ToDoubleFunction;

import static java.lang.Math.max;

public final class GeneratorOrderingComputer {

    private GeneratorOrderingComputer() { }

    public static CounterTradeGeneratorOrdering compute(
            Map<String, Double> glskWeights,
            Network network,
            double tolerance,
            double maxDownwardMagnitude,
            double maxUpwardMagnitude) {
        Set<String> zeroWeightIds = new HashSet<>();
        Map<String, Double> nonZeroWeights = new HashMap<>();
        List<RawAllocation> upwardRaw = new ArrayList<>();
        List<RawAllocation> downwardRaw = new ArrayList<>();

        glskWeights.forEach((generatorId, weight) -> {
            if (weight <= tolerance) {
                zeroWeightIds.add(generatorId);
            } else {
                nonZeroWeights.put(generatorId, weight);
            }
        });
        double keptTotal = nonZeroWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        nonZeroWeights.replaceAll((id, w) -> w / keptTotal);

        nonZeroWeights.forEach((generatorId, weight) -> {
            Generator generator = network.getGenerator(generatorId);
            double upCapacity = max(generator.getMaxP() - generator.getTargetP(), 0);
            double downCapacity = max(generator.getTargetP() - generator.getMinP(), 0);
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
        int upwardLastStage = lastReachableStage(upward, max(0, maxUpwardMagnitude));
        int downwardLastStage = lastReachableStage(downward, max(0, -maxDownwardMagnitude));

        return new CounterTradeGeneratorOrdering(
                withCumulativeAllocation(upwardRaw),
                withCumulativeAllocation(downwardRaw),
                upwardLastStage,
                downwardLastStage,
                zeroWeightIds);
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
            double mk = sk * (entry.availableCapacity() / entry.weight()) - sk * runningT;

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
     * generator h, solution of the MIP - can be recalculated with stageAllocation() below.
     * It returns the quantity to dispatch to the generators.
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

    public static Map<String, Double> stageAllocation(List<GlskGeneratorAllocation> order, double magnitude) {
        Map<String, Double> x = new LinkedHashMap<>();
        double remaining = magnitude;
        for (GlskGeneratorAllocation allocation : order) {
            double xh = Math.min(remaining, allocation.cumulativeAllocation());
            x.put(allocation.generatorId(), xh);
            remaining -= xh;
        }
        return x; // any leftover means the setpoint exceeds total capacity
    }

    public static double[] stageSensitivities(List<GlskGeneratorAllocation> order,
                                              ToDoubleFunction<String> sensitivityOfGenerator) {
        int n = order.size();
        double[] sigma = new double[n];
        double weightedSum = 0.0; // sum_{i>=h} lambda_i * s_i
        double suffixWeight = 0.0; // S_h
        for (int h = n - 1; h >= 0; h--) {
            GlskGeneratorAllocation a = order.get(h);
            weightedSum += a.weight() * sensitivityOfGenerator.applyAsDouble(a.generatorId());
            suffixWeight += a.weight();
            sigma[h] = weightedSum / suffixWeight;
        }
        return sigma;
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
        if (suffixWeight == 0) { // todo: verify if this happens
            suffixWeight = 1;
        }
        sigma[lastStage] = weightedSum / suffixWeight;
        for (int h = lastStage - 1; h >= 0; h--) {
            GlskGeneratorAllocation a = order.get(h);
            weightedSum += a.weight() * individualSensitivity.applyAsDouble(a.generatorId());
            suffixWeight += a.weight();
            sigma[h] = weightedSum / suffixWeight;
        }
        return sigma;
    }

    public static int lastReachableStage(List<GlskGeneratorAllocation> order, double maxMagnitude) {
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
