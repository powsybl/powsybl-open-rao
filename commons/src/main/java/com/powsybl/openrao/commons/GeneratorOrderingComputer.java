package com.powsybl.openrao.commons;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;

import java.util.*;

public final class GeneratorOrderingComputer {

    private GeneratorOrderingComputer() {}

    public static CounterTradeGeneratorOrdering compute(Map<String, Double> glskWeights, Network network) {
        Set<String> zeroWeightIds = new HashSet<>();
        List<RawAllocation> upwardRaw = new ArrayList<>();
        List<RawAllocation> downwardRaw = new ArrayList<>();

        glskWeights.forEach((generatorId, weight) -> {
            if (weight <= 0) { // use a tolerance instead..
                zeroWeightIds.add(generatorId);
                return;
            }
            Generator generator = network.getGenerator(generatorId);
            double upCapacity = generator.getMaxP() - generator.getTargetP();
            double downCapacity = generator.getTargetP() - generator.getMinP();
            upwardRaw.add(new RawAllocation(generatorId, weight, upCapacity, upCapacity / weight));
            downwardRaw.add(new RawAllocation(generatorId, weight, downCapacity, downCapacity / weight));
        });

        Comparator<RawAllocation> bySaturationThenId =
                Comparator.comparingDouble(RawAllocation::saturationRatio)
                        .thenComparing(RawAllocation::generatorId);

        upwardRaw.sort(bySaturationThenId);
        downwardRaw.sort(bySaturationThenId);

        return new CounterTradeGeneratorOrdering(
                withCumulativeAllocation(upwardRaw),
                withCumulativeAllocation(downwardRaw),
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

    private record RawAllocation(String generatorId, double weight, double availableCapacity, double saturationRatio) {}
}