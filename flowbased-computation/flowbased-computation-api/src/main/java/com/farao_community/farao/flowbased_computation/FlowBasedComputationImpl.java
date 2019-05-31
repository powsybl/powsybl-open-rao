package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.util.LoadFlowService;
import com.farao_community.farao.util.SensitivityComputationService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
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

        // Change working variant
        network.getVariantManager().setWorkingVariant(workingVariantId);
        network.getVariantManager().allowVariantMultiThreadAccess(true);

        // Flowbased parameters extension if necessary. todo
        // ...

        //todo implementation

        FlowBasedComputationResult.Status status = FlowBasedComputationResult.Status.SUCCESS;

        FlowBasedComputationResult result = new FlowBasedComputationResult(status);
        network.getVariantManager().allowVariantMultiThreadAccess(false);
        return CompletableFuture.completedFuture(result);
    }
}
