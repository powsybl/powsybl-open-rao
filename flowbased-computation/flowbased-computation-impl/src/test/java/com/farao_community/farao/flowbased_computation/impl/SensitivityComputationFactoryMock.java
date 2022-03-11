/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.google.auto.service.AutoService;
import com.powsybl.commons.reporter.Reporter;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
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

    private void writePreContingencySensitivityValues(SensitivityFactorReader factorReader, SensitivityValueWriter valueWriter) {
        AtomicReference<Integer> factorIndex = new AtomicReference<>(0);
        factorReader.read((functionType, functionId, variableType, variableId, variableSet, contingencyContext) -> {
            if (contingencyContext.getContextType() == ContingencyContextType.NONE || contingencyContext.getContextType() == ContingencyContextType.ALL) {
                valueWriter.write(factorIndex.get(), -1, preContingencyPtdf.get(functionId).get(variableId), preContingencyFref.get(functionId));
            }
            factorIndex.set(factorIndex.get() + 1);
        });
    }

    private void writePostContingencySensitivityValues(Contingency contingency, int contingencyIndex, SensitivityFactorReader factorReader, SensitivityValueWriter valueWriter) {
        AtomicReference<Integer> factorIndex = new AtomicReference<>(0);
        factorReader.read((functionType, functionId, variableType, variableId, variableSet, contingencyContext) -> {
            if (contingencyContext.getContextType() == ContingencyContextType.SPECIFIC && contingencyContext.getContingencyId().equals(contingency.getId())) {
                valueWriter.write(factorIndex.get(), contingencyIndex, postContingencyPtdf.get(functionId).get(variableId), postContingencyFref.get(functionId));
            }
            factorIndex.set(factorIndex.get() + 1);
        });
    }

    @Override
    public String getName() {
        return "MockSensitivity";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CompletableFuture<Void> run(Network network, String s, SensitivityFactorReader sensitivityFactorReader, SensitivityValueWriter sensitivityValueWriter, List<Contingency> contingencies, List<SensitivityVariableSet> glsks, SensitivityAnalysisParameters sensitivityAnalysisParameters, ComputationManager computationManager, Reporter reporter) {
        return CompletableFuture.runAsync(() -> {
            writePreContingencySensitivityValues(sensitivityFactorReader, sensitivityValueWriter);
            for (int contingencyIndex = 0; contingencyIndex < contingencies.size(); contingencyIndex++) {
                writePostContingencySensitivityValues(contingencies.get(contingencyIndex), contingencyIndex, sensitivityFactorReader, sensitivityValueWriter);
            }
        }, computationManager.getExecutor());
    }
}
