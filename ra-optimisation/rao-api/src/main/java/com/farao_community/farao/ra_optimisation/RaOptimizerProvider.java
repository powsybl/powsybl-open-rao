package com.farao_community.farao.ra_optimisation;

import com.farao_community.farao.data.crac_file.CracFile;
import com.powsybl.commons.Versionable;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;

import java.util.concurrent.CompletableFuture;

public interface RaOptimizerProvider extends Versionable {
    CompletableFuture<RaoComputationResult> run(Network network, CracFile cracFile, ComputationManager computationManager, String workingVariantId, RaoComputationParameters parameters);
}
