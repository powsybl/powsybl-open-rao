/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.google.auto.service.AutoService;
import com.powsybl.commons.reporter.Reporter;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.sensitivity.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(SensitivityAnalysisProvider.class)
public final class MockSensiProvider implements SensitivityAnalysisProvider {

    @Override
    public CompletableFuture<SensitivityAnalysisResult> run(Network network, String s, SensitivityFactorsProvider sensitivityFactorsProvider, List<Contingency> contingencies, SensitivityAnalysisParameters sensitivityAnalysisParameters, ComputationManager computationManager) {

        TwoWindingsTransformer pst = network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1");

        if (pst == null || pst.getPhaseTapChanger().getTapPosition() == 0) {
            // used for most of the tests
            return getResultsIfPstIsAtNeutralTap(network, sensitivityFactorsProvider, contingencies);
        } else {
            // used for tests with already applied RangeActions in Curative states
            return getResultsIfPstIsNotAtNeutralTap(network, sensitivityFactorsProvider, contingencies);
        }
    }

    private CompletableFuture<SensitivityAnalysisResult> getResultsIfPstIsAtNeutralTap(Network network, SensitivityFactorsProvider sensitivityFactorsProvider, List<Contingency> contingencies) {
        List<SensitivityValue> nStateValues = sensitivityFactorsProvider.getAdditionalFactors(network).stream()
            .map(factor -> {
                if (factor.getFunction() instanceof BranchFlow && factor.getVariable() instanceof PhaseTapChangerAngle) {
                    return new SensitivityValue(factor, 0.5, 10, 0);
                } else if (factor.getFunction() instanceof BranchIntensity && factor.getVariable() instanceof PhaseTapChangerAngle) {
                    return new SensitivityValue(factor, 0.25, 25, 0);
                } else if (factor.getFunction() instanceof BranchFlow && factor.getVariable() instanceof LinearGlsk) {
                    return new SensitivityValue(factor, 0.140, 10, 0);
                } else {
                    throw new AssertionError();
                }
            })
            .collect(Collectors.toList());
        Map<String, List<SensitivityValue>> contingenciesValues = contingencies.stream()
            .collect(Collectors.toMap(
                Contingency::getId,
                contingency -> sensitivityFactorsProvider.getAdditionalFactors(network, contingency.getId()).stream()
                    .map(factor -> {
                        if (factor.getFunction() instanceof BranchFlow && factor.getVariable() instanceof PhaseTapChangerAngle) {
                            return new SensitivityValue(factor, -5, -20, 0);
                        } else if (factor.getFunction() instanceof BranchIntensity && factor.getVariable() instanceof PhaseTapChangerAngle) {
                            return new SensitivityValue(factor, 5, 200, 0);
                        } else if (factor.getFunction() instanceof BranchFlow && factor.getVariable() instanceof LinearGlsk) {
                            return new SensitivityValue(factor, 6, -20, 0);
                        } else {
                            throw new AssertionError();
                        }
                    })
                    .collect(Collectors.toList())
            ));
        return CompletableFuture.completedFuture(new SensitivityAnalysisResult(true, Collections.emptyMap(), "", nStateValues, contingenciesValues));
    }

    private CompletableFuture<SensitivityAnalysisResult> getResultsIfPstIsNotAtNeutralTap(Network network, SensitivityFactorsProvider sensitivityFactorsProvider, List<Contingency> contingencies) {
        List<SensitivityValue> nStateValues = sensitivityFactorsProvider.getAdditionalFactors(network).stream()
            .map(factor -> {
                if (factor.getFunction() instanceof BranchFlow && factor.getVariable() instanceof PhaseTapChangerAngle) {
                    return new SensitivityValue(factor, 1.5, 110, 0);
                } else if (factor.getFunction() instanceof BranchIntensity && factor.getVariable() instanceof PhaseTapChangerAngle) {
                    return new SensitivityValue(factor, 1.25, 1100, 0);
                } else if (factor.getFunction() instanceof BranchFlow && factor.getVariable() instanceof LinearGlsk) {
                    return new SensitivityValue(factor, 1.140, 110, 0);
                } else {
                    throw new AssertionError();
                }
            })
            .collect(Collectors.toList());
        Map<String, List<SensitivityValue>> contingenciesValues = contingencies.stream()
            .collect(Collectors.toMap(
                Contingency::getId,
                contingency -> sensitivityFactorsProvider.getAdditionalFactors(network, contingency.getId()).stream()
                    .map(factor -> {
                        if (factor.getFunction() instanceof BranchFlow && factor.getVariable() instanceof PhaseTapChangerAngle) {
                            return new SensitivityValue(factor, -2.5, -40, 0);
                        } else if (factor.getFunction() instanceof BranchIntensity && factor.getVariable() instanceof PhaseTapChangerAngle) {
                            return new SensitivityValue(factor, 4.5, 180, 0);
                        } else if (factor.getFunction() instanceof BranchFlow && factor.getVariable() instanceof LinearGlsk) {
                            return new SensitivityValue(factor, 6.6, -40, 0);
                        } else {
                            throw new AssertionError();
                        }
                    })
                    .collect(Collectors.toList())
            ));
        return CompletableFuture.completedFuture(new SensitivityAnalysisResult(true, Collections.emptyMap(), "", nStateValues, contingenciesValues));    }

    @Override
    public String getName() {
        return "MockSensi";
    }

    @Override
    public String getVersion() {
        return "0";
    }

    @Override
    public CompletableFuture<Void> run(Network network, String s, SensitivityFactorReader sensitivityFactorReader, SensitivityValueWriter sensitivityValueWriter, List<Contingency> list, List<SensitivityVariableSet> list1, SensitivityAnalysisParameters sensitivityAnalysisParameters, ComputationManager computationManager, Reporter reporter) {
        return null;
    }
}
