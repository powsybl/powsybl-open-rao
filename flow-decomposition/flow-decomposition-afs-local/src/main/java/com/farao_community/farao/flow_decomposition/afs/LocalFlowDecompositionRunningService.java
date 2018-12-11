/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.afs;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.afs.AppLogger;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.CracFileProvider;
import com.farao_community.farao.flow_decomposition.FlowDecomposition;
import com.farao_community.farao.flow_decomposition.FlowDecompositionFactory;
import com.farao_community.farao.flow_decomposition.FlowDecompositionParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Local computation of flow decomposition based on default platform config
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class LocalFlowDecompositionRunningService implements FlowDecompositionRunningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFlowDecompositionRunningService.class);

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.LocalFlowDecompositionRunningService");

    private final Supplier<FlowDecompositionFactory> factorySupplier;

    public LocalFlowDecompositionRunningService(Supplier<FlowDecompositionFactory> factorySupplier) {
        this.factorySupplier = Suppliers.memoize(Objects.requireNonNull(factorySupplier));
    }

    @Override
    public void run(FlowDecompositionRunner runner) {
        Objects.requireNonNull(runner);

        ProjectCase aCase = runner.getCase().orElseThrow(() -> new FaraoException("Invalid case link"));
        CracFileProvider cracFileProvider = runner.getCracFileProvider().orElseThrow(() -> new FaraoException("Invalid CRAC file link"));
        FlowDecompositionParameters parameters = runner.readParameters();
        ComputationManager computationManager = runner.getFileSystem().getData().getLongTimeExecutionComputationManager();

        UUID taskId = runner.startTask();
        try {
            AppLogger logger = runner.createLogger(taskId);

            logger.log(RESOURCE_BUNDLE.getString("LoadingNetwork"));
            Network network = aCase.getNetwork();

            logger.log(RESOURCE_BUNDLE.getString("LoadingCrac"));
            CracFile cracFile = cracFileProvider.getCracFile();

            logger.log(RESOURCE_BUNDLE.getString("RunningFlowDecomposition"));
            FlowDecomposition flowDecomposition = factorySupplier.get().create(network, computationManager, 0);
            flowDecomposition.run(network.getStateManager().getWorkingStateId(), parameters, cracFile)
                    .handleAsync((result, throwable) -> {
                        if (throwable == null) {
                            logger.log("Flow decomposition complete, storing results...");
                            runner.writeResult(result);
                        } else {
                            logger.log("Flow decomposition failed");
                            LOGGER.error(throwable.toString(), throwable);
                        }
                        return null;
                    }).join();
        } finally {
            runner.stopTask(taskId);
        }
    }
}
