/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.afs.local;

import com.farao_community.farao.flowbased_computation.FlowBasedGlskValuesProvider;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.afs.AppLogger;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.CracFileProvider;
import com.farao_community.farao.flowbased_computation.FlowBasedComputation;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationFactory;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;
import com.farao_community.farao.flowbased_computation.afs.FlowBasedComputationRunner;
import com.farao_community.farao.flowbased_computation.afs.FlowBasedComputationRunningService;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
public class LocalFlowBasedComputationRunningService implements FlowBasedComputationRunningService {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.FlowBasedComputationAfsLocale");

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LocalFlowBasedComputationRunningService.class);

    private final Supplier<FlowBasedComputationFactory> factorySupplier;

    public LocalFlowBasedComputationRunningService(Supplier<FlowBasedComputationFactory> factorySupplier) {
        this.factorySupplier = Suppliers.memoize(Objects.requireNonNull(factorySupplier));
    }

    public void run(FlowBasedComputationRunner runner) {
        Objects.requireNonNull(runner);

        ProjectCase aCase = runner.getCase().orElseThrow(() -> new FaraoException("Invalide case"));
        CracFileProvider cracFileProvider = runner.getCracFileProvider().orElseThrow(() -> new FaraoException("Invalide case"));
        FlowBasedGlskValuesProvider flowBasedGlskValuesProvider = runner.getGlskProvider().orElseThrow(() -> new FaraoException("Invalide case"));

        FlowBasedComputationParameters parameters = runner.readParameters();
        ComputationManager computationManager = runner.getFileSystem().getData().getLongTimeExecutionComputationManager();

        UUID taskId = runner.startTask();

        try {
            AppLogger logger = runner.createLogger(taskId);

            logger.log(RESOURCE_BUNDLE.getString("LoadNetwork"));
            Network network = aCase.getNetwork();

            logger.log(RESOURCE_BUNDLE.getString("RunningFlowBased"));
            FlowBasedComputation raoComputation = factorySupplier.get().create(network,
                    cracFileProvider.getCracFile(),
                    flowBasedGlskValuesProvider,
                    computationManager,
                    0);
            raoComputation.run(network.getVariantManager().getWorkingVariantId(), parameters)
                    .handleAsync((result, throwable) -> {
                        if (throwable == null) {
                            logger.log(RESOURCE_BUNDLE.getString("FlowBasedComplete"));
                            runner.writeResult(result);
                        } else {
                            logger.log(RESOURCE_BUNDLE.getString("FailedFlowBasedComputation"));
                            LOGGER.error(throwable.toString(), throwable);
                        }
                        return null;
                    }).join();
        } finally {
            runner.stopTask(taskId);
        }
    }
}
