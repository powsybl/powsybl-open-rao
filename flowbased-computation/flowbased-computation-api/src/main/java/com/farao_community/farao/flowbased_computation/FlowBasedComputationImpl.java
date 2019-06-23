/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataMonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataPtdfPerCountry;
import com.farao_community.farao.util.LoadFlowService;
import com.farao_community.farao.util.SensitivityComputationService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFactorsProvider;
import com.powsybl.sensitivity.factors.BranchFlowPerLinearGlsk;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Flowbased computation implementation
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public class FlowBasedComputationImpl implements FlowBasedComputation {

    /**
     * Network reference network
     */
    private Network network;
    /**
     * Crac file
     */
    private CracFile cracFile;
    /**
     * Instant of FlowBased domain to be calculated
     */
    private Instant instant;
    /**
     * Glsk file provider
     */
    private FlowBasedGlskValuesProvider flowBasedGlskValuesProvider;
    /**
     * For load flow computation manager
     */
    private ComputationManager computationManager;

    /**
     * Constructor
     * @param network reference network: we need a network to construct the linear glsk map from the glsk document
     * @param cracFile crac file
     * @param flowBasedGlskValuesProvider get linear glsk map from a glsk document
     * @param instant flow based domaine is time dependent
     * @param computationManager computation manager
     * @param loadFlowFactory load flow for reference flow calculation
     * @param sensitivityComputationFactory sensitivity calculation
     */
    public FlowBasedComputationImpl(Network network,
                                    CracFile cracFile,
                                    FlowBasedGlskValuesProvider flowBasedGlskValuesProvider,
                                    Instant instant,
                                    ComputationManager computationManager,
                                    LoadFlowFactory loadFlowFactory,
                                    SensitivityComputationFactory sensitivityComputationFactory) {
        this.network = network;
        this.cracFile = cracFile;
        this.instant = instant;
        this.flowBasedGlskValuesProvider = flowBasedGlskValuesProvider;
        this.computationManager = computationManager;

        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);
        LoadFlowService.init(loadFlowFactory, this.computationManager);
    }

    /**
     * Run Flowbased calculation
     * @param workingVariantId network working variant id
     * @param parameters flowbased computation parameters
     * @return
     */
    @Override
    public CompletableFuture<FlowBasedComputationResult> run(String workingVariantId,
                                                             FlowBasedComputationParameters parameters) {
        Objects.requireNonNull(workingVariantId);
        Objects.requireNonNull(parameters);

        //get list of Monitored branches from CRAC file
        List<MonitoredBranch> monitoredBranchList = cracFile.getPreContingency().getMonitoredBranches();

        //get Map<country, LinearGLSK> for Instant instant from FlowBasedGlskValuesProvider
        Map<String, LinearGlsk> mapCountryLinearGlsk = flowBasedGlskValuesProvider.getCountryLinearGlskMap(instant);

        // Fill SensitivityFactor List: BranchFlowPerLinearGlsk = SensitivityFactor<BranchFlow, LinearGlsk>
        SensitivityFactorsProvider factorsProvider = net -> {
            List<SensitivityFactor> factors = new ArrayList<>();
            monitoredBranchList.forEach(branch -> mapCountryLinearGlsk.values()
                    .stream()
                    .map(linearGlsk -> new BranchFlowPerLinearGlsk(
                            new BranchFlow(branch.getId(),
                                    branch.getName(),
                                    branch.getBranchId()),
                            linearGlsk))
                    .forEach(factors::add));
            return factors;
        };

        SensitivityComputationResults sensiResults = SensitivityComputationService.runSensitivity(network,
                network.getVariantManager().getWorkingVariantId(),
                factorsProvider);

        if (!sensiResults.isOk()) {
            throw new FaraoException("Failure in sensitivity computation during Flow based computation.");
        }

        FlowBasedComputationResult flowBasedComputationResult = new FlowBasedComputationResult(FlowBasedComputationResult.Status.SUCCESS);

        //calculate reference flow value by load flow => save in Map<String, Double> referenceFlows
        Map<String, Double> referenceFlows = new HashMap<>();
        LoadFlowResult loadFlowResult = LoadFlowService.runLoadFlow(network, network.getVariantManager().getWorkingVariantId());
        if (!loadFlowResult.isOk()) {
            throw new FaraoException("Divergence in loadflow computation during calculation of reference flow of Flow based computation.");
        }
        monitoredBranchList.forEach(branch -> {
            double flow = network.getBranch(branch.getBranchId()).getTerminal1().getP();
            referenceFlows.put(branch.getId(), Double.isNaN(flow) ? 0. : flow);
        });

        //fill in FlowBasedComputationResult
        fillFlowBasedComputationResult(cracFile, referenceFlows, sensiResults, flowBasedComputationResult);

        return CompletableFuture.completedFuture(flowBasedComputationResult);
    }

    /**
     * Internal post processing to fill in FlowBased Computation Result
     * @param cracFile crac file
     * @param referenceFlows reference flow for critical branches
     * @param sensitivityComputationResults sensitivity computation results
     * @param flowBasedComputationResult flow based computation result
     */
    private void fillFlowBasedComputationResult(CracFile cracFile,
                                                Map<String, Double> referenceFlows,
                                                SensitivityComputationResults sensitivityComputationResults,
                                                FlowBasedComputationResult flowBasedComputationResult) {
        // get list of Monitored Branch
        List<MonitoredBranch> branches = cracFile.getPreContingency().getMonitoredBranches();

        List<DataMonitoredBranch> branchResultList = new ArrayList<>();
        for (MonitoredBranch branch : branches) {
            //get DataMonitoredBranch's ptdfPerCountryList from sensitivityComputationResults
            List<DataPtdfPerCountry> ptdfPerCountryList = new ArrayList<>();
            sensitivityComputationResults.getSensitivityValues().forEach(
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

        flowBasedComputationResult.getPtdflist().addAll(branchResultList);
    }

}
