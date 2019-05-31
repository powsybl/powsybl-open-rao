package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
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
    public CompletableFuture<FlowBasedComputationResult> run(String workingVariantId, FlowBasedComputationParameters parameters) {
        Objects.requireNonNull(workingVariantId);
        Objects.requireNonNull(parameters);

        //get list of Monitored branches from CRAC file
        List<MonitoredBranch> monitoredBranchList = cracFile.getPreContingency().getMonitoredBranches();

        //get Map<country, LinearGLSK> for Instant instant from FlowBasedGlskValuesProvider
        Map<String, LinearGlsk> mapCountryLinearGlsk = flowBasedGlskValuesProvider.getCountryLinearGlskMap(instant);

        //for each Monitored branches in monitoredBranchList, calculate Sensi : BranchFlowPerLinearGlsk
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

        SensitivityComputationResults sensitivityComputationResults = SensitivityComputationService.runSensitivity(network, network.getVariantManager().getWorkingVariantId(), factorsProvider);

        //fill FlowBasedComputationResult
        //todo : case fail
        FlowBasedComputationResult.Status status = FlowBasedComputationResult.Status.SUCCESS;
        FlowBasedComputationResult flowBasedComputationResult = new FlowBasedComputationResult(status);
        fillFlowBasedComputationResult(network, cracFile, sensitivityComputationResults, flowBasedComputationResult);

        return CompletableFuture.completedFuture(flowBasedComputationResult);
    }

    private void fillFlowBasedComputationResult(Network network,
                                               CracFile cracFile,
                                               SensitivityComputationResults sensitivityComputationResults,
                                               FlowBasedComputationResult flowBasedComputationResult) {

        List<FlowBasedMonitoredBranchResult> flowBasedMonitoredBranchResultList = new ArrayList<>();
        List<MonitoredBranch> monitoredBranchList = cracFile.getPreContingency().getMonitoredBranches();
        for (MonitoredBranch monitoredBranch : monitoredBranchList) {
            FlowBasedMonitoredBranchResult flowBasedMonitoredBranchResult = new FlowBasedMonitoredBranchResult(
                    monitoredBranch.getId(),
                    monitoredBranch.getName(),
                    monitoredBranch.getBranchId(),
                    monitoredBranch.getFmax()
            );
            List<FlowBasedBranchPtdfPerCountry> ptdfPerCountryList = new ArrayList<>();

            sensitivityComputationResults.getSensitivityValues().forEach(
                sensitivityValue -> {
                    if (sensitivityValue.getFactor().getFunction().getId().equals(monitoredBranch.getId())) {
                        double linearGlskSensitivity = sensitivityValue.getValue(); //sensi result
                        FlowBasedBranchPtdfPerCountry flowBasedBranchPtdfPerCountry = new FlowBasedBranchPtdfPerCountry(
                                sensitivityValue.getFactor().getVariable().getName(), // LinearGlsk country id
                                Double.isNaN(linearGlskSensitivity) ? 0. : linearGlskSensitivity
                        );
                        ptdfPerCountryList.add(flowBasedBranchPtdfPerCountry);
                    }
                });

            flowBasedMonitoredBranchResult.getPtdfList().addAll(ptdfPerCountryList);
            flowBasedMonitoredBranchResultList.add(flowBasedMonitoredBranchResult);
        }

        flowBasedComputationResult.getFlowBasedMonitoredBranchResultList().addAll(flowBasedMonitoredBranchResultList);
    }
}
