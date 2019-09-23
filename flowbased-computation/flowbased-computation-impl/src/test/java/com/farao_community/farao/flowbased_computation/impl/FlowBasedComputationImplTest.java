package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationProvider;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationResult;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Test case is a 4 nodes network, with 4 countries.
 *
 *       FR   (+100 MW)       BE  (0 MW)
 *          + ------------ +
 *          |              |
 *          |              |
 *          |              |
 *          + ------------ +
 *       DE   (0 MW)          NL  (-100 MW)
 *
 * All lines have same impedance and are monitored.
 * One contingency is simulated, the loss of FR-BE interconnection line.
 * Each Country GLSK is a simple one node GLSK.
 * Compensation is considered as equally shared on each country, and there are no losses.
 */
public class FlowBasedComputationImplTest {
    private FlowBasedComputationProvider flowBasedComputationProvider;
    private Network network;
    private CracFile cracFile;
    private GlskProvider glskProvider;
    private ComputationManager computationManager;
    private FlowBasedComputationParameters parameters;

    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    public static <K, U> Collector<Map.Entry<K, U>, ?, Map<K, U>> entriesToMap() {
        return Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue());
    }

    @Before
    public void setUp() {
        flowBasedComputationProvider = new FlowBasedComputationImpl();

        Map<String, Double> expectedFrefByBranch = new HashMap<>();
        expectedFrefByBranch.put("FR-BE", 50.);
        expectedFrefByBranch.put("FR-DE", 50.);
        expectedFrefByBranch.put("BE-NL", 50.);
        expectedFrefByBranch.put("DE-NL", 50.);
        expectedFrefByBranch.put("N-1 FR-BE / FR-BE", 0.);
        expectedFrefByBranch.put("N-1 FR-BE / FR-DE", 100.);
        expectedFrefByBranch.put("N-1 FR-BE / BE-NL", 0.);
        expectedFrefByBranch.put("N-1 FR-BE / DE-NL", 100.);
        Map<String, Map<String, Double>> expectedPtdfByBranch = new HashMap<>();
        expectedPtdfByBranch.put("FR-BE", Collections.unmodifiableMap(
            Stream.of(
                entry("FR GLSK", 0.375),
                entry("BE GLSK", -0.375),
                entry("DE GLSK", 0.125),
                entry("NL GLSK", -0.125)
            )
            .collect(entriesToMap())
        ));
        expectedPtdfByBranch.put("FR-DE", Collections.unmodifiableMap(
            Stream.of(
                entry("FR GLSK", 0.375),
                entry("BE GLSK", 0.125),
                entry("DE GLSK", -0.375),
                entry("NL GLSK", -0.125)
            )
            .collect(entriesToMap())
        ));
        expectedPtdfByBranch.put("BE-NL", Collections.unmodifiableMap(
            Stream.of(
                entry("FR GLSK", 0.125),
                entry("BE GLSK", 0.375),
                entry("DE GLSK", -0.125),
                entry("NL GLSK", -0.375)
            )
            .collect(entriesToMap())
        ));
        expectedPtdfByBranch.put("DE-NL", Collections.unmodifiableMap(
            Stream.of(
                entry("FR GLSK", 0.125),
                entry("BE GLSK", -0.125),
                entry("DE GLSK", 0.375),
                entry("NL GLSK", -0.375)
            )
            .collect(entriesToMap())
        ));
        expectedPtdfByBranch.put("N-1 FR-BE / FR-BE", Collections.unmodifiableMap(
            Stream.of(
                entry("FR GLSK", 0.),
                entry("BE GLSK", 0.),
                entry("DE GLSK", 0.),
                entry("NL GLSK", 0.)
            )
            .collect(entriesToMap())
        ));
        expectedPtdfByBranch.put("N-1 FR-BE / FR-DE", Collections.unmodifiableMap(
            Stream.of(
                entry("FR GLSK", 0.75),
                entry("BE GLSK", -0.25),
                entry("DE GLSK", -0.25),
                entry("NL GLSK", -0.25)
            )
            .collect(entriesToMap())
        ));
        expectedPtdfByBranch.put("N-1 FR-BE / BE-NL", Collections.unmodifiableMap(
            Stream.of(
                entry("FR GLSK", -0.25),
                entry("BE GLSK", 0.75),
                entry("DE GLSK", -0.25),
                entry("NL GLSK", -0.25)
            )
            .collect(entriesToMap())
        ));
        expectedPtdfByBranch.put("N-1 FR-BE / DE-NL", Collections.unmodifiableMap(
            Stream.of(
                entry("FR GLSK", 0.5),
                entry("BE GLSK", -0.5),
                entry("DE GLSK", 0.5),
                entry("NL GLSK", -0.5)
            )
            .collect(entriesToMap())
        ));
    }

    @Test
    public void testProviderName() {
        assertEquals("SimpleIterativeFlowBased", flowBasedComputationProvider.getName());
    }

    @Test
    public void testProviderVersion() {
        assertEquals("1.0.0", flowBasedComputationProvider.getVersion());
    }

    @Test
    @Ignore("To be completed")
    public void testRun() {
        FlowBasedComputationResult result = flowBasedComputationProvider.run(network, cracFile, glskProvider, computationManager, network.getVariantManager().getWorkingVariantId(), parameters).join();
        assertEquals(FlowBasedComputationResult.Status.SUCCESS, result.getStatus());
    }
}
