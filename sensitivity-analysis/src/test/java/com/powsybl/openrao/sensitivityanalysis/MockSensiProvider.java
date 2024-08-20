/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.sensitivityanalysis;

import com.google.auto.service.AutoService;
import com.powsybl.commons.report.ReportNode;
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
    public CompletableFuture<Void> run(Network network, String s, SensitivityFactorReader sensitivityFactorReader, SensitivityResultWriter sensitivityResultWriter, List<Contingency> contingencies, List<SensitivityVariableSet> glsks, SensitivityAnalysisParameters sensitivityAnalysisParameters, ComputationManager computationManager, ReportNode reportNode) {
        return CompletableFuture.runAsync(() -> {
            TwoWindingsTransformer pst = network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1");
            if (pst == null || pst.getPhaseTapChanger().getTapPosition() == 0) {
                // used for most of the tests
                writeResultsIfPstIsAtNeutralTap(sensitivityFactorReader, sensitivityResultWriter, contingencies, network);
            } else {
                // used for tests with already applied RangeActions in Curative states
                writeResultsIfPstIsNotAtNeutralTap(sensitivityFactorReader, sensitivityResultWriter, contingencies);
            }
        }, computationManager.getExecutor());
    }

    private void writeResultsIfPstIsAtNeutralTap(SensitivityFactorReader factorReader, SensitivityResultWriter sensitivityResultWriter, List<Contingency> contingencies, Network network) {
        AtomicReference<Integer> factorIndex = new AtomicReference<>(0);
        factorReader.read((functionType, functionId, variableType, variableId, variableSet, contingencyContext) -> {
            if (contingencyContext.getContextType() == ContingencyContextType.NONE || contingencyContext.getContextType() == ContingencyContextType.ALL) {
                if (variableType == SensitivityVariableType.TRANSFORMER_PHASE) {
                    switch (functionType) {
                        case BRANCH_ACTIVE_POWER_1:
                            sensitivityResultWriter.writeSensitivityValue(factorIndex.get(), -1, 0.5, 10);
                            break;
                        case BRANCH_ACTIVE_POWER_2:
                            sensitivityResultWriter.writeSensitivityValue(factorIndex.get(), -1, 0.55, 15);
                            break;
                        case BRANCH_CURRENT_1:
                            sensitivityResultWriter.writeSensitivityValue(factorIndex.get(), -1, 0.25, 25);
                            break;
                        case BRANCH_CURRENT_2:
                            sensitivityResultWriter.writeSensitivityValue(factorIndex.get(), -1, 0.30, 30);
                            break;
                        default:
                            throw new AssertionError();
                    }
                } else if (variableType == SensitivityVariableType.INJECTION_ACTIVE_POWER) {
                    switch (functionType) {
                        case BRANCH_ACTIVE_POWER_1:
                            sensitivityResultWriter.writeSensitivityValue(factorIndex.get(), -1, 0.14, 10);
                            break;
                        case BRANCH_ACTIVE_POWER_2:
                            sensitivityResultWriter.writeSensitivityValue(factorIndex.get(), -1, 0.19, 15);
                            break;
                        case BRANCH_CURRENT_1:
                            sensitivityResultWriter.writeSensitivityValue(factorIndex.get(), -1, 0.28, 20);
                            break;
                        case BRANCH_CURRENT_2:
                            sensitivityResultWriter.writeSensitivityValue(factorIndex.get(), -1, 0.33, 25);
                            break;
                        default:
                            throw new AssertionError();
                    }
                } else if (variableType == SensitivityVariableType.HVDC_LINE_ACTIVE_POWER) {
                    switch (functionType) {
                        case BRANCH_ACTIVE_POWER_1:
                            sensitivityResultWriter.writeSensitivityValue(factorIndex.get(), -1, 0.34, 30);
                            break;
                        case BRANCH_ACTIVE_POWER_2:
                            sensitivityResultWriter.writeSensitivityValue(factorIndex.get(), -1, 0.35, 35);
                            break;
                        case BRANCH_CURRENT_1:
                            sensitivityResultWriter.writeSensitivityValue(factorIndex.get(), -1, 0.44, 40);
                            break;
                        case BRANCH_CURRENT_2:
                            sensitivityResultWriter.writeSensitivityValue(factorIndex.get(), -1, 0.49, 45);
                            break;
                        default:
                            throw new AssertionError();
                    }
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
                    if (variableType == SensitivityVariableType.TRANSFORMER_PHASE) {
                        switch (functionType) {
                            case BRANCH_ACTIVE_POWER_1:
                                sensitivityResultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, -5, -20);
                                break;
                            case BRANCH_ACTIVE_POWER_2:
                                sensitivityResultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, -5.5, -25);
                                break;
                            case BRANCH_CURRENT_1:
                                sensitivityResultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, 5, 200);
                                break;
                            case BRANCH_CURRENT_2:
                                sensitivityResultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, 5.5, 205);
                                break;
                            default:
                                throw new AssertionError();
                        }
                    } else if (variableType == SensitivityVariableType.INJECTION_ACTIVE_POWER) {
                        switch (functionType) {
                            case BRANCH_ACTIVE_POWER_1:
                                sensitivityResultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, 6, -20);
                                break;
                            case BRANCH_ACTIVE_POWER_2:
                                sensitivityResultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, 6.5, -25);
                                break;
                            case BRANCH_CURRENT_1:
                                sensitivityResultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, 12, 40);
                                break;
                            case BRANCH_CURRENT_2:
                                sensitivityResultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, 17, 45);
                                break;
                            default:
                                throw new AssertionError();
                        }
                    } else if (variableType == SensitivityVariableType.HVDC_LINE_ACTIVE_POWER) {
                        switch (functionType) {
                            case BRANCH_ACTIVE_POWER_1:
                                sensitivityResultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, 7, -25);
                                break;
                            case BRANCH_ACTIVE_POWER_2:
                                sensitivityResultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, 7.5, -26);
                                break;
                            case BRANCH_CURRENT_1:
                                sensitivityResultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, 8, -30);
                                break;
                            case BRANCH_CURRENT_2:
                                sensitivityResultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, 8.5, -31);
                                break;
                            default:
                                throw new AssertionError();
                        }
                    } else {
                        throw new AssertionError();
                    }
                    if (contingencies.get(finalContingencyIndex).getElements().stream().anyMatch(e -> network.getIdentifiable(e.getId()) == null)) {
                        sensitivityResultWriter.writeContingencyStatus(finalContingencyIndex, SensitivityAnalysisResult.Status.FAILURE);
                    } else {
                        sensitivityResultWriter.writeContingencyStatus(finalContingencyIndex, SensitivityAnalysisResult.Status.SUCCESS);
                    }
                }
                factorIndexContingency.set(factorIndexContingency.get() + 1);
            });
        }
    }

    private void writeResultsIfPstIsNotAtNeutralTap(SensitivityFactorReader factorReader, SensitivityResultWriter resultWriter, List<Contingency> contingencies) {
        AtomicReference<Integer> factorIndex = new AtomicReference<>(0);
        factorReader.read((functionType, functionId, variableType, variableId, variableSet, contingencyContext) -> {
            if (contingencyContext.getContextType() == ContingencyContextType.NONE || contingencyContext.getContextType() == ContingencyContextType.ALL) {
                if (variableType == SensitivityVariableType.TRANSFORMER_PHASE) {
                    switch (functionType) {
                        case BRANCH_ACTIVE_POWER_1:
                            resultWriter.writeSensitivityValue(factorIndex.get(), -1, 1.5, 110);
                            break;
                        case BRANCH_ACTIVE_POWER_2:
                            resultWriter.writeSensitivityValue(factorIndex.get(), -1, 2.0, 115);
                            break;
                        case BRANCH_CURRENT_1:
                            resultWriter.writeSensitivityValue(factorIndex.get(), -1, 1.25, 1100);
                            break;
                        case BRANCH_CURRENT_2:
                            resultWriter.writeSensitivityValue(factorIndex.get(), -1, 1.30, 1105);
                            break;
                        default:
                            throw new AssertionError();
                    }
                } else if (variableType == SensitivityVariableType.INJECTION_ACTIVE_POWER) {
                    switch (functionType) {
                        case BRANCH_ACTIVE_POWER_1:
                            resultWriter.writeSensitivityValue(factorIndex.get(), -1, 1.14, 110);
                            break;
                        case BRANCH_ACTIVE_POWER_2:
                            resultWriter.writeSensitivityValue(factorIndex.get(), -1, 1.19, 115);
                            break;
                        case BRANCH_CURRENT_1:
                            resultWriter.writeSensitivityValue(factorIndex.get(), -1, 2.14, 210);
                            break;
                        case BRANCH_CURRENT_2:
                            resultWriter.writeSensitivityValue(factorIndex.get(), -1, 2.19, 215);
                            break;
                        default:
                            throw new AssertionError();
                    }
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
                    if (variableType == SensitivityVariableType.TRANSFORMER_PHASE) {
                        switch (functionType) {
                            case BRANCH_ACTIVE_POWER_1:
                                resultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, -2.5, -40);
                                break;
                            case BRANCH_ACTIVE_POWER_2:
                                resultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, -3.0, -45);
                                break;
                            case BRANCH_CURRENT_1:
                                resultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, 9, 90);
                                break;
                            case BRANCH_CURRENT_2:
                                resultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, 9.5, 95);
                                break;
                            default:
                                throw new AssertionError();
                        }
                    } else if (variableType == SensitivityVariableType.INJECTION_ACTIVE_POWER) {
                        switch (functionType) {
                            case BRANCH_ACTIVE_POWER_1:
                                resultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, 6.6, -40);
                                break;
                            case BRANCH_ACTIVE_POWER_2:
                                resultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, 7.1, -45);
                                break;
                            case BRANCH_CURRENT_1:
                                resultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, 12.6, -80);
                                break;
                            case BRANCH_CURRENT_2:
                                resultWriter.writeSensitivityValue(factorIndexContingency.get(), finalContingencyIndex, 13.1, -85);
                                break;
                            default:
                                throw new AssertionError();
                        }
                    }
                    resultWriter.writeContingencyStatus(finalContingencyIndex, SensitivityAnalysisResult.Status.SUCCESS);
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
        return "1.0";
    }
}
