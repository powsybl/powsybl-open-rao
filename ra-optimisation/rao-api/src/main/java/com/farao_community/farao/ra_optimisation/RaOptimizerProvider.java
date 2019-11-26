package com.farao_community.farao.ra_optimisation;

import com.powsybl.commons.Versionable;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;

import java.util.concurrent.CompletableFuture;

public interface RaOptimizerProvider extends Versionable {
    CompletableFuture<RaoComputationResult> run(Network network, Crac crac, ComputationManager computationManager, String workingVariantId, RaoComputationParameters parameters);
}
