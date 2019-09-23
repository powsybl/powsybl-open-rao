/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataDomain;
import com.farao_community.farao.data.flowbased_domain.DataMonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataPreContingency;
import com.farao_community.farao.data.flowbased_domain.DataPtdfPerCountry;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationProvider;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationResult;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationResultImpl;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.farao_community.farao.util.LoadFlowService;
import com.farao_community.farao.util.SensitivityComputationService;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFactorsProvider;
import com.powsybl.sensitivity.factors.BranchFlowPerLinearGlsk;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Flowbased computation implementation
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
@AutoService(FlowBasedComputationProvider.class)
public class FlowBasedComputationImpl implements FlowBasedComputationProvider {

    @Override
    public String getName() {
        return "SimpleIterativeFlowBased";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CompletableFuture<FlowBasedComputationResult> run(Network network, CracFile cracFile, GlskProvider glskProvider, ComputationManager computationManager, String workingVariantId, FlowBasedComputationParameters parameters) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(cracFile);
        Objects.requireNonNull(glskProvider);
        Objects.requireNonNull(computationManager);
        Objects.requireNonNull(workingVariantId);
        Objects.requireNonNull(parameters);

        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        network.getVariantManager().setWorkingVariant(workingVariantId);

        SensitivityFactorsProvider factorsProvider = net -> generateSensitivityFactorsProvider(net, cracFile, glskProvider);
        SensitivityComputationResults sensiResults = SensitivityComputationService.runSensitivity(network, network.getVariantManager().getWorkingVariantId(), factorsProvider);

        Map<String, Double> referenceFlows = computeReferenceFlows(network, cracFile);

        FlowBasedComputationResult flowBasedComputationResult = new FlowBasedComputationResultImpl(FlowBasedComputationResult.Status.SUCCESS, buildFlowbasedDomain(cracFile, sensiResults, referenceFlows));

        network.getVariantManager().setWorkingVariant(initialVariantId);
        return CompletableFuture.completedFuture(flowBasedComputationResult);
    }

    private List<SensitivityFactor> generateSensitivityFactorsProvider(Network network, CracFile cracFile, GlskProvider glskProvider) {
        List<SensitivityFactor> factors = new ArrayList<>();
        // TODO add curative monitored branches
        Map<String, LinearGlsk> mapCountryLinearGlsk = glskProvider.getAllGlsk(network);
        List<MonitoredBranch> monitoredBranchList = cracFile.getPreContingency().getMonitoredBranches();
        monitoredBranchList.forEach(branch -> mapCountryLinearGlsk.values().stream().map(linearGlsk -> new BranchFlowPerLinearGlsk(new BranchFlow(branch.getId(), branch.getName(), branch.getBranchId()), linearGlsk)).forEach(factors::add));
        return factors;
    }

    private Map<String, Double> computeReferenceFlows(Network network, CracFile cracFile) {
        Map<String, Double> referenceFlows = new HashMap<>();
        // TODO add curative monitored branches
        List<MonitoredBranch> monitoredBranchList = cracFile.getPreContingency().getMonitoredBranches();
        LoadFlowService.runLoadFlow(network, network.getVariantManager().getWorkingVariantId());
        monitoredBranchList.forEach(branch -> {
            double flow = network.getBranch(branch.getBranchId()).getTerminal1().getP(); referenceFlows.put(branch.getId(), Double.isNaN(flow) ? 0. : flow);
        });
        return referenceFlows;
    }

    private DataDomain buildFlowbasedDomain(CracFile cracFile, SensitivityComputationResults sensiResults, Map<String, Double> referenceFlows) {
        return DataDomain.builder()
                .id(UUID.randomUUID().toString())
                .name("FlowBased results")
                .description("")
                .sourceFormat("code")
                .dataPreContingency(buildDataPreContingency(cracFile, sensiResults, referenceFlows))
                .build();
    }

    private DataPreContingency buildDataPreContingency(CracFile cracFile, SensitivityComputationResults sensiResults, Map<String, Double> referenceFlows) {
        return DataPreContingency.builder()
                .dataMonitoredBranches(buildDataMonitoredBranches(cracFile, sensiResults, referenceFlows))
                .build();
    }

    private List<DataMonitoredBranch> buildDataMonitoredBranches(CracFile cracFile, SensitivityComputationResults sensiResults, Map<String, Double> referenceFlows) {
        List<MonitoredBranch> branches = cracFile.getPreContingency().getMonitoredBranches();
        List<DataMonitoredBranch> branchResultList = new ArrayList<>();
        for (MonitoredBranch branch : branches) {
            //get DataMonitoredBranch's ptdfPerCountryList from sensitivityComputationResults
            List<DataPtdfPerCountry> ptdfPerCountryList = new ArrayList<>();
            sensiResults.getSensitivityValues().forEach(
                sensitivityValue -> {
                    // find BranchFlow's ID = branch's ID
                    if (sensitivityValue.getFactor().getFunction().getId().equals(branch.getId())) {
                        double linearGlskSensitivity = sensitivityValue.getValue(); //sensi result
                        DataPtdfPerCountry ptdfPerCountry = new DataPtdfPerCountry(
                                sensitivityValue.getFactor().getVariable().getName(), // LinearGlsk country id
                                Double.isNaN(linearGlskSensitivity) ? 0. : linearGlskSensitivity
                        );
                        ptdfPerCountryList.add(ptdfPerCountry);
                    }
                });
            //fill in DataMonitoredBranch
            DataMonitoredBranch branchResult = new DataMonitoredBranch(
                    branch.getId(),
                    branch.getName(),
                    branch.getBranchId(),
                    branch.getFmax(),
                    referenceFlows.get(branch.getId()),
                    ptdfPerCountryList
            );
            branchResultList.add(branchResult);
        }
        return branchResultList;
    }
}
