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
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.sensitivity.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(SensitivityAnalysisProvider.class)
public final class MockSensiProvider implements SensitivityAnalysisProvider {

    @Override
    public CompletableFuture<Void> run(Network network, String s, SensitivityFactorReader sensitivityFactorReader, SensitivityValueWriter sensitivityValueWriter, List<Contingency> contingencies, List<SensitivityVariableSet> glsks, SensitivityAnalysisParameters sensitivityAnalysisParameters, ComputationManager computationManager, Reporter reporter) {
        return CompletableFuture.runAsync(() -> {
            TwoWindingsTransformer pst = network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1");
            if (pst == null || pst.getPhaseTapChanger().getTapPosition() == 0) {
                // used for most of the tests
                writeResultsIfPstIsAtNeutralTap(sensitivityFactorReader, sensitivityValueWriter, contingencies);
            } else {
                // used for tests with already applied RangeActions in Curative states
                writeResultsIfPstIsNotAtNeutralTap(sensitivityFactorReader, sensitivityValueWriter, contingencies);
            }
        }, computationManager.getExecutor());
    }

    private void writeResultsIfPstIsAtNeutralTap(SensitivityFactorReader factorReader, SensitivityValueWriter valueWriter, List<Contingency> contingencies) {
        AtomicReference<Integer> factorIndex = new AtomicReference<>(0);
        factorReader.read((functionType, functionId, variableType, variableId, variableSet, contingencyContext) -> {
            if (contingencyContext.getContextType() == ContingencyContextType.NONE || contingencyContext.getContextType() == ContingencyContextType.ALL) {
                if (functionType == SensitivityFunctionType.BRANCH_ACTIVE_POWER && variableType == SensitivityVariableType.TRANSFORMER_PHASE) {
                    valueWriter.write(factorIndex.get(), -1, 0.5, 10);
                } else if (functionType == SensitivityFunctionType.BRANCH_CURRENT && variableType == SensitivityVariableType.TRANSFORMER_PHASE) {
                    valueWriter.write(factorIndex.get(), -1, 0.25, 25);
                } else if (functionType == SensitivityFunctionType.BRANCH_ACTIVE_POWER && variableType == SensitivityVariableType.INJECTION_ACTIVE_POWER) {
                    valueWriter.write(factorIndex.get(), -1, 0.14, 10);
                } else {
                    throw new AssertionError();
                }
            }
            factorIndex.set(factorIndex.get() + 1);
        });

        for (int contingencyIndex = 0; contingencyIndex < contingencies.size(); contingencyIndex++) {
            int finalContingencyIndex = contingencyIndex;
            AtomicReference<Integer> factorIndexContingency = new AtomicReference<>(0);
            factorReader.read((functionType, functionId, variableType, variableId, variableSet, contingencyContext) -> {
                if (contingencyContext.getContextType() == ContingencyContextType.SPECIFIC && contingencyContext.getContingencyId().equals(contingencies.get(finalContingencyIndex).getId())) {
                    if (functionType == SensitivityFunctionType.BRANCH_ACTIVE_POWER && variableType == SensitivityVariableType.TRANSFORMER_PHASE) {
                        valueWriter.write(factorIndexContingency.get(), finalContingencyIndex, -5, -20);
                    } else if (functionType == SensitivityFunctionType.BRANCH_CURRENT && variableType == SensitivityVariableType.TRANSFORMER_PHASE) {
                        valueWriter.write(factorIndexContingency.get(), finalContingencyIndex, 5, 200);
                    } else if (functionType == SensitivityFunctionType.BRANCH_ACTIVE_POWER && variableType == SensitivityVariableType.INJECTION_ACTIVE_POWER) {
                        valueWriter.write(factorIndexContingency.get(), finalContingencyIndex, 6, -20);
                    } else {
                        throw new AssertionError();
                    }
                }
                factorIndexContingency.set(factorIndexContingency.get() + 1);
            });
        }
    }

    private void writeResultsIfPstIsNotAtNeutralTap(SensitivityFactorReader factorReader, SensitivityValueWriter valueWriter, List<Contingency> contingencies) {
        AtomicReference<Integer> factorIndex = new AtomicReference<>(0);
        factorReader.read((functionType, functionId, variableType, variableId, variableSet, contingencyContext) -> {
            if (contingencyContext.getContextType() == ContingencyContextType.NONE || contingencyContext.getContextType() == ContingencyContextType.ALL) {
                if (functionType == SensitivityFunctionType.BRANCH_ACTIVE_POWER && variableType == SensitivityVariableType.TRANSFORMER_PHASE) {
                    valueWriter.write(factorIndex.get(), -1, 1.5, 110);
                } else if (functionType == SensitivityFunctionType.BRANCH_CURRENT && variableType == SensitivityVariableType.TRANSFORMER_PHASE) {
                    valueWriter.write(factorIndex.get(), -1, 1.25, 1100);
                } else if (functionType == SensitivityFunctionType.BRANCH_ACTIVE_POWER && variableType == SensitivityVariableType.INJECTION_ACTIVE_POWER) {
                    valueWriter.write(factorIndex.get(), -1, 1.14, 110);
                } else {
                    throw new AssertionError();
                }
            }
            factorIndex.set(factorIndex.get() + 1);
        });

        for (int contingencyIndex = 0; contingencyIndex < contingencies.size(); contingencyIndex++) {
            int finalContingencyIndex = contingencyIndex;
            AtomicReference<Integer> factorIndexContingency = new AtomicReference<>(0);
            factorReader.read((functionType, functionId, variableType, variableId, variableSet, contingencyContext) -> {
                if (contingencyContext.getContextType() == ContingencyContextType.SPECIFIC && contingencyContext.getContingencyId().equals(contingencies.get(finalContingencyIndex).getId())) {
                    if (functionType == SensitivityFunctionType.BRANCH_ACTIVE_POWER && variableType == SensitivityVariableType.TRANSFORMER_PHASE) {
                        valueWriter.write(factorIndexContingency.get(), finalContingencyIndex, -2.5, -40);
                    } else if (functionType == SensitivityFunctionType.BRANCH_CURRENT && variableType == SensitivityVariableType.TRANSFORMER_PHASE) {
                        valueWriter.write(factorIndexContingency.get(), finalContingencyIndex, 4.5, 180);
                    } else if (functionType == SensitivityFunctionType.BRANCH_ACTIVE_POWER && variableType == SensitivityVariableType.INJECTION_ACTIVE_POWER) {
                        valueWriter.write(factorIndexContingency.get(), finalContingencyIndex, 6.6, -40);
                    } else {
                        throw new AssertionError();
                    }
                }
                factorIndexContingency.set(factorIndexContingency.get() + 1);
            });
        }
    }

    @Override
    public String getName() {
        return "MockSensi";
    }

    @Override
    public String getVersion() {
        return "0";
    }

}
