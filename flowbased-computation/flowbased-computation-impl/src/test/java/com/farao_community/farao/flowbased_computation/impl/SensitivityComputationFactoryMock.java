/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(SensitivityAnalysisProvider.class)
public class SensitivityComputationFactoryMock implements SensitivityAnalysisProvider {
    private final Map<String, Double> preContingencyFref;
    private final Map<String, Double> postContingencyFref;
    private final Map<String, Map<String, Double>> preContingencyPtdf;
    private final Map<String, Map<String, Double>> postContingencyPtdf;

    public SensitivityComputationFactoryMock() {
        preContingencyFref = getPreContingencyFref();
        postContingencyFref = getPostContingencyFref();
        preContingencyPtdf = getPreContingencyPtdf();
        postContingencyPtdf = getPostContingencyPtdf();
    }

    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    public static <K, U> Collector<Map.Entry<K, U>, ?, Map<K, U>> entriesToMap() {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    private Map<String, Double> getPreContingencyFref() {
        Map<String, Double> expectedFrefByBranch = new HashMap<>();
        expectedFrefByBranch.put("FR-BE", 50.);
        expectedFrefByBranch.put("FR-DE", 50.);
        expectedFrefByBranch.put("BE-NL", 50.);
        expectedFrefByBranch.put("DE-NL", 50.);
        return expectedFrefByBranch;
    }

    private Map<String, Double> getPostContingencyFref() {
        Map<String, Double> expectedFrefByBranch = new HashMap<>();
        expectedFrefByBranch.put("FR-BE", 0.);
        expectedFrefByBranch.put("FR-DE", 100.);
        expectedFrefByBranch.put("BE-NL", 0.);
        expectedFrefByBranch.put("DE-NL", 100.);
        return expectedFrefByBranch;
    }

    private Map<String, Map<String, Double>> getPreContingencyPtdf() {
        Map<String, Map<String, Double>> expectedPtdfByBranch = new HashMap<>();
        expectedPtdfByBranch.put("FR-BE", Collections.unmodifiableMap(
                Stream.of(
                        entry("10YFR-RTE------C", 0.375),
                        entry("10YBE----------2", -0.375),
                        entry("10YCB-GERMANY--8", 0.125),
                        entry("10YNL----------L", -0.125)
                )
                        .collect(entriesToMap())
        ));
        expectedPtdfByBranch.put("FR-DE", Collections.unmodifiableMap(
                Stream.of(
                        entry("10YFR-RTE------C", 0.375),
                        entry("10YBE----------2", 0.125),
                        entry("10YCB-GERMANY--8", -0.375),
                        entry("10YNL----------L", -0.125)
                )
                        .collect(entriesToMap())
        ));
        expectedPtdfByBranch.put("BE-NL", Collections.unmodifiableMap(
                Stream.of(
                        entry("10YFR-RTE------C", 0.125),
                        entry("10YBE----------2", 0.375),
                        entry("10YCB-GERMANY--8", -0.125),
                        entry("10YNL----------L", -0.375)
                )
                        .collect(entriesToMap())
        ));
        expectedPtdfByBranch.put("DE-NL", Collections.unmodifiableMap(
                Stream.of(
                        entry("10YFR-RTE------C", 0.125),
                        entry("10YBE----------2", -0.125),
                        entry("10YCB-GERMANY--8", 0.375),
                        entry("10YNL----------L", -0.375)
                )
                        .collect(entriesToMap())
        ));
        return expectedPtdfByBranch;
    }

    private Map<String, Map<String, Double>> getPostContingencyPtdf() {
        Map<String, Map<String, Double>> expectedPtdfByBranch = new HashMap<>();
        expectedPtdfByBranch.put("FR-BE", Collections.unmodifiableMap(
                Stream.of(
                        entry("10YFR-RTE------C", Double.NaN),
                        entry("10YBE----------2", Double.NaN),
                        entry("10YCB-GERMANY--8", Double.NaN),
                        entry("10YNL----------L", Double.NaN)
                )
                        .collect(entriesToMap())
        ));
        expectedPtdfByBranch.put("FR-DE", Collections.unmodifiableMap(
                Stream.of(
                        entry("10YFR-RTE------C", 0.75),
                        entry("10YBE----------2", -0.25),
                        entry("10YCB-GERMANY--8", -0.25),
                        entry("10YNL----------L", -0.25)
                )
                        .collect(entriesToMap())
        ));
        expectedPtdfByBranch.put("BE-NL", Collections.unmodifiableMap(
                Stream.of(
                        entry("10YFR-RTE------C", -0.25),
                        entry("10YBE----------2", 0.75),
                        entry("10YCB-GERMANY--8", -0.25),
                        entry("10YNL----------L", -0.25)
                )
                        .collect(entriesToMap())
        ));
        expectedPtdfByBranch.put("DE-NL", Collections.unmodifiableMap(
                Stream.of(
                        entry("10YFR-RTE------C", 0.5),
                        entry("10YBE----------2", -0.5),
                        entry("10YCB-GERMANY--8", 0.5),
                        entry("10YNL----------L", -0.5)
                )
                        .collect(entriesToMap())
        ));
        return expectedPtdfByBranch;
    }

    private List<SensitivityValue> getPreContingencySensitivityValues(SensitivityFactorsProvider sensitivityFactorsProvider, Network network) {
        return sensitivityFactorsProvider.getFactors(network).stream()
                .map(factor -> new SensitivityValue(factor, preContingencyPtdf.get(factor.getFunction().getId()).get(factor.getVariable().getId()), preContingencyFref.get(factor.getFunction().getId()), Double.NaN))
                .collect(Collectors.toList());
    }

    private List<SensitivityValue> getPostContingencySensitivityValues(SensitivityFactorsProvider sensitivityFactorsProvider, Network network) {
        return sensitivityFactorsProvider.getFactors(network).stream()
                .map(factor -> new SensitivityValue(factor, postContingencyPtdf.get(factor.getFunction().getId()).get(factor.getVariable().getId()), postContingencyFref.get(factor.getFunction().getId()), Double.NaN))
                .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<SensitivityAnalysisResult> run(Network network, String s, SensitivityFactorsProvider sensitivityFactorsProvider, ContingenciesProvider contingenciesProvider, SensitivityAnalysisParameters sensitivityAnalysisParameters, ComputationManager computationManager) {
        List<SensitivityValue> preContingencySensitivityValues = getPreContingencySensitivityValues(sensitivityFactorsProvider, network);
        Map<String, List<SensitivityValue>> postContingencySensitivityValues = contingenciesProvider.getContingencies(network).stream()
                .collect(Collectors.toMap(Contingency::getId, co -> getPostContingencySensitivityValues(sensitivityFactorsProvider, network)));
        return CompletableFuture.completedFuture(new SensitivityAnalysisResult(true, Collections.emptyMap(), "", preContingencySensitivityValues, postContingencySensitivityValues));
    }

    @Override
    public String getName() {
        return "MockSensitivity";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
