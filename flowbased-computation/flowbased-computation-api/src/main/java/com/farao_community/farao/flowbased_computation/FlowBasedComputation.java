/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.util.LoadFlowService;
import com.farao_community.farao.util.SensitivityComputationService;
import com.powsybl.commons.Versionable;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Flowbased computation
 *
 */
public class FlowBasedComputation {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowBasedComputation.class);

    private Network network;
    private CracFile cracFile;
    private ComputationManager computationManager;

    public FlowBasedComputation(Network network,
                                CracFile cracFile,
                                ComputationManager computationManager,
                                LoadFlowFactory loadFlowFactory,
                                SensitivityComputationFactory sensitivityComputationFactory) {
        this.network = network;
        this.cracFile = cracFile;
        this.computationManager = computationManager;

        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);
        LoadFlowService.init(loadFlowFactory, computationManager);
    }

    public CompletableFuture<FlowBasedComputationResult> run(String workingVariantId, FlowBasedComputationParameters parameters) {
        Objects.requireNonNull(workingVariantId);
        Objects.requireNonNull(parameters);

        // Change working variant
        network.getVariantManager().setWorkingVariant(workingVariantId);
        network.getVariantManager().allowVariantMultiThreadAccess(true);

        FlowBasedComputationParameters parametersExtension = Objects.requireNonNull(parameters.getExtension(FlowBasedComputationParameters.class)); // Should not be null, checked previously

        //todo implementation computation details
        FlowBasedComputationResult result = new FlowBasedComputationResult(FlowBasedComputationResult.Status.SUCCESS);

        network.getVariantManager().allowVariantMultiThreadAccess(false);
        return CompletableFuture.completedFuture(result);
    }

}
