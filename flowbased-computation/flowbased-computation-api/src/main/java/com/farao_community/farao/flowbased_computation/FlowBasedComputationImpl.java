package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataMonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataPtdfPerCountry;
import com.farao_community.farao.util.LoadFlowService;
import com.farao_community.farao.util.SensitivityComputationService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFactorsProvider;
import com.powsybl.sensitivity.factors.BranchFlowPerLinearGlsk;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class FlowBasedComputationImpl implements FlowBasedComputation {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowBasedComputationImpl.class);
    private static final double UNDEFINED_VALUE = Double.MIN_VALUE;

    private Network network;
    private CracFile cracFile;
    private Instant instant;
    private FlowBasedGlskValuesProvider flowBasedGlskValuesProvider;
    private ComputationManager computationManager;

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
        LoadFlowService.init(loadFlowFactory, computationManager);
    }

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

        //get SensitivityComputation status
        FlowBasedComputationResult flowBasedComputationResult;
        if (sensiResults.isOk()) {
            //Sensi computation success => fill FlowBased computation result
            flowBasedComputationResult = new FlowBasedComputationResult(FlowBasedComputationResult.Status.SUCCESS);
            fillFlowBasedComputationResult(network, cracFile, sensiResults, flowBasedComputationResult);
        } else {
            //Sensi computation fail
            flowBasedComputationResult = new FlowBasedComputationResult(FlowBasedComputationResult.Status.FAILURE);
        }

        return CompletableFuture.completedFuture(flowBasedComputationResult);
    }

    private void fillFlowBasedComputationResult(Network network,
                                               CracFile cracFile,
                                               SensitivityComputationResults sensitivityComputationResults,
                                               FlowBasedComputationResult flowBasedComputationResult) {
        // get list of Monitored Branch
        List<MonitoredBranch> branches = cracFile.getPreContingency().getMonitoredBranches();

        List<DataMonitoredBranch> branchResultList = new ArrayList<>();
        for (MonitoredBranch branch : branches) {

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

            DataMonitoredBranch branchResult = new DataMonitoredBranch(
                    branch.getId(),
                    branch.getName(),
                    branch.getBranchId(),
                    branch.getFmax(),
                    UNDEFINED_VALUE, // TODO : add reference flow result for monitored branches
                    ptdfPerCountryList
            );

            branchResultList.add(branchResult);
        }

        flowBasedComputationResult.getPtdflist().addAll(branchResultList);
    }
}
