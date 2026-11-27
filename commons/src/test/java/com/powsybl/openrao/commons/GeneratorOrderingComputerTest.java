/*
 * Copyright (c) 2026, Artelys (http://www.artelys.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reference case used by most tests ("three generators"):
 * g1, g2, g3 at targetP 0, maxP 10 / 20 / 100, raw weights 1 / 1 / 2 (normalised 0.25 / 0.25 / 0.5).
 * Upward saturation ratios 40 / 80 / 200, layer bounds m = 40 / 30 / 60, total capacity 130.
 *
 * @author Rodolphe Griset {@literal <rodolphe.griset at artelys.com>}
 */
class GeneratorOrderingComputerTest {


    private static final double EPS = 1e-6;
    private static final double DOUBLE_TOLERANCE = 1e-3;
    // Mocks allow generator states (NaN or out-of-range targetP) that an IIDM network would reject.
    private static void addGenerator(Network network, String id, double minP, double maxP, double targetP) {
        Generator generator = Mockito.mock(Generator.class);
        Mockito.when(generator.getId()).thenReturn(id);
        Mockito.when(generator.getMinP()).thenReturn(minP);
        Mockito.when(generator.getMaxP()).thenReturn(maxP);
        Mockito.when(generator.getTargetP()).thenReturn(targetP);
        Mockito.when(network.getGenerator(id)).thenReturn(generator);
    }


    // Tests common settings

    private static Network threeGeneratorNetwork() {
        Network network = Mockito.mock(Network.class);
        addGenerator(network, "g1", 0, 10, 0);
        addGenerator(network, "g2", 0, 20, 0);
        addGenerator(network, "g3", 0, 100, 0);
        return network;
    }

    private static Map<String, Double> threeGeneratorWeights() {
        return Map.of("g1", 1.0, "g2", 1.0, "g3", 2.0);
    }

    private static CounterTradeGeneratorOrdering threeGeneratorOrdering(double maxUpwardMagnitude) {
        return GeneratorOrderingComputer.compute(threeGeneratorWeights(), threeGeneratorNetwork(), DOUBLE_TOLERANCE, -1000, maxUpwardMagnitude);
    }

    private static List<String> ids(List<GlskGeneratorAllocation> order) {
        return order.stream().map(GlskGeneratorAllocation::generatorId).toList();
    }


    // ----------  Layer and generator capacity tests ----------

    // The layers together must be able to dispatch exactly the total available capacity.
    private static void assertLayersCoverCapacity(List<GlskGeneratorAllocation> order) {
        double sumOfLayers = order.stream().mapToDouble(GlskGeneratorAllocation::layerCapacity).sum();
        double sumOfCapacities = order.stream().mapToDouble(GlskGeneratorAllocation::availableCapacity).sum();
        assertEquals(sumOfCapacities, sumOfLayers, EPS);
    }

    // No generator receives more than its capacity, and the total dispatched is min(magnitude, total capacity).
    private static void assertDispatchIsFeasible(List<GlskGeneratorAllocation> order, double magnitude) {
        Map<String, Double> dispatch = GeneratorOrderingComputer.computeDispatchQuantity(
            order, GeneratorOrderingComputer.layerAllocation(order, magnitude));
        double totalCapacity = 0;
        double totalDispatch = 0;
        for (GlskGeneratorAllocation allocation : order) {
            double quantity = dispatch.get(allocation.generatorId());
            assertTrue(quantity >= -EPS, allocation.generatorId() + " receives a negative quantity");
            assertTrue(quantity <= allocation.availableCapacity() + EPS, allocation.generatorId() + " is overloaded");
            totalCapacity += allocation.availableCapacity();
            totalDispatch += quantity;
        }
        assertEquals(Math.min(magnitude, totalCapacity), totalDispatch, EPS);
    }

    // ---------- compute: ordering and layer bounds

    @Test
    void upwardOrderIsSortedBySaturationRatio() {
        CounterTradeGeneratorOrdering ordering = threeGeneratorOrdering(1000);
        List<GlskGeneratorAllocation> up = ordering.upwardOrder();
        assertEquals(List.of("g1", "g2", "g3"), ids(up));
        assertEquals(List.of(40., 80., 200.), up.stream().map(GlskGeneratorAllocation::saturationRatio).toList());
    }

    // Check layers capacities
    @Test
    void layerCapacityGivesLayerBounds() {
        List<GlskGeneratorAllocation> up = threeGeneratorOrdering(1000).upwardOrder();
        assertEquals(40, up.get(0).layerCapacity(), EPS);
        assertEquals(30, up.get(1).layerCapacity(), EPS);
        assertEquals(60, up.get(2).layerCapacity(), EPS);
        assertLayersCoverCapacity(up);
    }

    @Test
    void downwardOrderHeadroom() {
        Network network = Mockito.mock(Network.class);
        addGenerator(network, "g1", 0, 100, 20);
        addGenerator(network, "g2", 10, 100, 70);
        CounterTradeGeneratorOrdering ordering = GeneratorOrderingComputer.compute(
            Map.of("g1", 1.0, "g2", 1.0), network, DOUBLE_TOLERANCE, -1000, 1000);

        // g2 has minP 10 and targetP 70, so its downward headroom is 60 and g1's is 20.
        List<GlskGeneratorAllocation> down = ordering.downwardOrder();
        assertEquals(List.of("g1", "g2"), ids(down));
        assertEquals(20, down.get(0).availableCapacity(), EPS);
        assertEquals(60, down.get(1).availableCapacity(), EPS);
    }

    @Test
    void downwardCapacityClampedByMaxMagnitude() {
        Network network = Mockito.mock(Network.class);
        addGenerator(network, "g1", 0, 100, 20);
        addGenerator(network, "g2", 10, 100, 70);
        CounterTradeGeneratorOrdering ordering = GeneratorOrderingComputer.compute(
            Map.of("g1", 1.0, "g2", 1.0), network, DOUBLE_TOLERANCE, -45, 1000);

        // g2's headroom of 60 is clamped to 45; layers are 40 / 25, so 45 MW reaches the second layer.
        List<GlskGeneratorAllocation> down = ordering.downwardOrder();
        assertEquals(List.of("g1", "g2"), ids(down));
        assertEquals(20, down.get(0).availableCapacity(), EPS);
        assertEquals(45, down.get(1).availableCapacity(), EPS);
        assertEquals(40, down.get(0).layerCapacity(), EPS);
        assertEquals(25, down.get(1).layerCapacity(), EPS);
        assertEquals(1, ordering.downwardLastStage());
    }

    @Test
    void capacityClampedByMaxMagnitude() {
        List<GlskGeneratorAllocation> up = threeGeneratorOrdering(15).upwardOrder();

        // Upward capacity is min(headroom, 15): g1 10, g2 15, g3 15. Ratio = capacity / weight,
        // so g3 (15 / 0.5 = 30) has the lowest ratio and comes first.
        assertEquals(List.of("g3", "g1", "g2"), ids(up));
        assertEquals(15, up.get(0).availableCapacity(), EPS);
    }

    @Test
    void equalRatiosOrderedById() {
        Network network = Mockito.mock(Network.class);
        addGenerator(network, "g2", 0, 50, 0);
        addGenerator(network, "g1", 0, 50, 0);
        addGenerator(network, "g3", 0, 50, 0);
        CounterTradeGeneratorOrdering ordering = GeneratorOrderingComputer.compute(
            Map.of("g3", 1.0, "g2", 1.0, "g1", 1.0), network, DOUBLE_TOLERANCE, -1000, 1000);

        assertEquals(List.of("g1", "g2", "g3"), ids(ordering.upwardOrder()));
    }

    @Test
    void emptyLayerGenerator() {
        Network network = Mockito.mock(Network.class);
        addGenerator(network, "full", 0, 50, 50);
        addGenerator(network, "free", 0, 50, 0);
        CounterTradeGeneratorOrdering ordering = GeneratorOrderingComputer.compute(
            Map.of("full", 1.0, "free", 1.0), network, DOUBLE_TOLERANCE, -1000, 1000);

        List<GlskGeneratorAllocation> up = ordering.upwardOrder();
        assertEquals(List.of("full", "free"), ids(up));
        assertEquals(0, up.get(0).layerCapacity(), EPS);
    }

    // ---------- compute: filtering ----------

    @Test
    void unusableGeneratorsFilter() {
        Network network = Mockito.mock(Network.class);
        addGenerator(network, "kept1", 0, 100, 0);
        addGenerator(network, "kept2", 0, 100, 0);
        addGenerator(network, "tinyWeight", 0, 100, 0);
        addGenerator(network, "zeroWeight", 0, 100, 0);
        addGenerator(network, "nanTargetP", 0, 100, Double.NaN);
        addGenerator(network, "aboveMaxP", 0, 100, 150);
        addGenerator(network, "belowMinP", 10, 100, 5);
        Map<String, Double> weights = Map.of("kept1", 0.2, "kept2", 0.6, "tinyWeight", DOUBLE_TOLERANCE / 2, "zeroWeight", 0.0,
            "nanTargetP", 0.1, "aboveMaxP", 0.1, "belowMinP", 0.1, "notInNetwork", 0.3);

        CounterTradeGeneratorOrdering ordering = GeneratorOrderingComputer.compute(weights, network, DOUBLE_TOLERANCE, -1000, 1000);

        assertEquals(Set.of("tinyWeight", "zeroWeight", "nanTargetP", "aboveMaxP", "belowMinP", "notInNetwork"),
            ordering.zeroWeightGeneratorIds());
        assertEquals(Set.of("kept1", "kept2"), ordering.initialTargetP().keySet());
        assertEquals(List.of("kept2", "kept1"), ids(ordering.upwardOrder()));
        assertEquals(0.75, ordering.upwardOrder().get(0).weight(), EPS);
        assertEquals(0.25, ordering.upwardOrder().get(1).weight(), EPS);
    }

    @Test
    void negativeToleranceThrows() {
        Map<String, Double> weights = threeGeneratorWeights();
        Network network = threeGeneratorNetwork();
        assertThrows(OpenRaoException.class,
            () -> GeneratorOrderingComputer.compute(weights, network, -1e-3, -1000, 1000));
    }

    @Test
    void emptyGlskGivesEmptyOrdering() {
        CounterTradeGeneratorOrdering ordering = GeneratorOrderingComputer.compute(
            Map.of(), Mockito.mock(Network.class), DOUBLE_TOLERANCE, -100, 200);

        assertTrue(ordering.upwardOrder().isEmpty());
        assertTrue(ordering.downwardOrder().isEmpty());
    }

    // ---------- compute: last stages ----------

    @Test
    void lastStageDependsOnMaxMagnitude() {
        assertEquals(0, threeGeneratorOrdering(30).upwardLastStage());
        assertEquals(0, threeGeneratorOrdering(40).upwardLastStage());
        assertEquals(1, threeGeneratorOrdering(41).upwardLastStage());
    }

    // ---------- layerAllocation ----------

    @Test
    void layerCapacityFillsLayersInOrder() {
        List<GlskGeneratorAllocation> up = threeGeneratorOrdering(1000).upwardOrder();

        Map<String, Double> x = GeneratorOrderingComputer.layerAllocation(up, 50);
        assertEquals(List.of("g1", "g2", "g3"), List.copyOf(x.keySet()));
        assertEquals(40, x.get("g1"), EPS);
        assertEquals(10, x.get("g2"), EPS);
        assertEquals(0, x.get("g3"), EPS);
    }

    @Test
    void layerCapacityEdgeMagnitudes() {
        List<GlskGeneratorAllocation> up = threeGeneratorOrdering(1000).upwardOrder();

        GeneratorOrderingComputer.layerAllocation(up, 0).values().forEach(v -> assertEquals(0, v, EPS));

        Map<String, Double> full = GeneratorOrderingComputer.layerAllocation(up, 130);
        assertEquals(40, full.get("g1"), EPS);
        assertEquals(30, full.get("g2"), EPS);
        assertEquals(60, full.get("g3"), EPS);

        // Above total capacity the excess is silently dropped.
        assertEquals(full, GeneratorOrderingComputer.layerAllocation(up, 500));
    }

    // ---------- computeDispatchQuantity ----------

    @Test
    void dispatchIsProportionalBeforeAnySaturation() {
        List<GlskGeneratorAllocation> up = threeGeneratorOrdering(1000).upwardOrder();
        Map<String, Double> dispatch = GeneratorOrderingComputer.computeDispatchQuantity(
            up, GeneratorOrderingComputer.layerAllocation(up, 20));

        assertEquals(5, dispatch.get("g1"), EPS);
        assertEquals(5, dispatch.get("g2"), EPS);
        assertEquals(10, dispatch.get("g3"), EPS);
    }

    @Test
    void dispatchRedistributesAfterSaturation() {
        List<GlskGeneratorAllocation> up = threeGeneratorOrdering(1000).upwardOrder();
        Map<String, Double> dispatch = GeneratorOrderingComputer.computeDispatchQuantity(
            up, GeneratorOrderingComputer.layerAllocation(up, 50));

        // g1 saturates at 10; the 10 MW beyond 40 go to g2 and g3 in a 1:2 ratio.
        assertEquals(10, dispatch.get("g1"), EPS);
        assertEquals(40.0 / 3, dispatch.get("g2"), EPS);
        assertEquals(80.0 / 3, dispatch.get("g3"), EPS);
    }

    @Test
    void dispatchAtFullCapacitySaturatesEveryGenerator() {
        List<GlskGeneratorAllocation> up = threeGeneratorOrdering(1000).upwardOrder();
        Map<String, Double> dispatch = GeneratorOrderingComputer.computeDispatchQuantity(
            up, GeneratorOrderingComputer.layerAllocation(up, 130));

        assertEquals(10, dispatch.get("g1"), EPS);
        assertEquals(20, dispatch.get("g2"), EPS);
        assertEquals(100, dispatch.get("g3"), EPS);
    }

    @Test
    void dispatchIsFeasibleForAnyMagnitude() {
        List<GlskGeneratorAllocation> up = threeGeneratorOrdering(1000).upwardOrder();
        for (double magnitude : new double[]{0, 1, 39.9, 40, 40.1, 70, 100, 130, 200}) {
            assertDispatchIsFeasible(up, magnitude);
        }
        List<GlskGeneratorAllocation> clamped = threeGeneratorOrdering(15).upwardOrder();
        for (double magnitude : new double[]{0, 10, 30, 35, 40, 60}) {
            assertDispatchIsFeasible(clamped, magnitude);
        }
    }

    // ---------- stageSensitivities ----------
    // With individual sensitivities 1 / 2 / 4, sigma[h] is the weight-averaged sensitivity of generators h..n-1.

    private static double sensitivity(String generatorId) {
        return switch (generatorId) {
            case "g1" -> 1.0;
            case "g2" -> 2.0;
            case "g3" -> 4.0;
            default -> throw new IllegalArgumentException(generatorId);
        };
    }

    @Test
    void stageSensitivitiesWithTailFromFirstStage() {
        List<GlskGeneratorAllocation> up = threeGeneratorOrdering(1000).upwardOrder();
        double tail = 0.25 * 1 + 0.25 * 2 + 0.5 * 4;

        double[] sigma = GeneratorOrderingComputer.stageSensitivities(up, 0, GeneratorOrderingComputerTest::sensitivity, tail);
        assertEquals(1, sigma.length);
        assertEquals(2.75, sigma[0], EPS);
    }

    @Test
    void stageSensitivitiesWithIntermediateStage() {
        List<GlskGeneratorAllocation> up = threeGeneratorOrdering(1000).upwardOrder();
        double tail = 0.25 * 2 + 0.5 * 4;

        double[] sigma = GeneratorOrderingComputer.stageSensitivities(up, 1, GeneratorOrderingComputerTest::sensitivity, tail);
        assertEquals(2, sigma.length);
        assertEquals(2.75, sigma[0], EPS);
        assertEquals(2.5 / 0.75, sigma[1], EPS);
    }

    @Test
    void stageSensitivitiesWithEmptyTail() {
        List<GlskGeneratorAllocation> up = threeGeneratorOrdering(1000).upwardOrder();

        double[] sigma = GeneratorOrderingComputer.stageSensitivities(up, 3, GeneratorOrderingComputerTest::sensitivity, 0);
        assertEquals(4, sigma.length);
        assertEquals(2.75, sigma[0], EPS);
        assertEquals(2.5 / 0.75, sigma[1], EPS);
        assertEquals(4, sigma[2], EPS);
        assertEquals(0, sigma[3], EPS);
    }

    @Test
    void lastReachableLayerEdgeCases() {
        List<GlskGeneratorAllocation> up = threeGeneratorOrdering(1000).upwardOrder();

        assertEquals(0, GeneratorOrderingComputer.lastReachableLayer(List.of(), 50));
        assertEquals(0, GeneratorOrderingComputer.lastReachableLayer(up, 0));
        assertEquals(0, GeneratorOrderingComputer.lastReachableLayer(up, 40));
        assertEquals(1, GeneratorOrderingComputer.lastReachableLayer(up, 50));
        assertEquals(2, GeneratorOrderingComputer.lastReachableLayer(up, 130));
        assertEquals(2, GeneratorOrderingComputer.lastReachableLayer(up, 1000));
    }
}
