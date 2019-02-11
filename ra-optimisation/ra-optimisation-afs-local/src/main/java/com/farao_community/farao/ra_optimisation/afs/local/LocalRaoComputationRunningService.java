/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.afs.local;

import com.farao_community.farao.ra_optimisation.afs.RaoComputationRunner;
import com.farao_community.farao.ra_optimisation.afs.RaoComputationRunningService;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.afs.AppLogger;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.ra_optimisation.RaoComputation;
import com.farao_community.farao.data.crac_file.CracFileProvider;
import com.farao_community.farao.ra_optimisation.RaoComputationFactory;
import com.farao_community.farao.ra_optimisation.RaoComputationParameters;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
public class LocalRaoComputationRunningService implements RaoComputationRunningService {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.RaoComputationAfsLocale");

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LocalRaoComputationRunningService.class);

    private final Supplier<RaoComputationFactory> factorySupplier;

    public LocalRaoComputationRunningService(Supplier<RaoComputationFactory> factorySupplier) {
        this.factorySupplier = Suppliers.memoize(Objects.requireNonNull(factorySupplier));
    }

    public void run(RaoComputationRunner runner) {
        Objects.requireNonNull(runner);

        ProjectCase aCase = runner.getCase().orElseThrow(() -> new FaraoException("Invalide case"));
        CracFileProvider cracFileProvider = runner.getCracFileProvider().orElseThrow(() -> new FaraoException("Invalide case"));

        RaoComputationParameters parameters = runner.readParameters();
        ComputationManager computationManager = runner.getFileSystem().getData().getLongTimeExecutionComputationManager();

        UUID taskId = runner.startTask();

        try {
            AppLogger logger = runner.createLogger(taskId);

            logger.log(RESOURCE_BUNDLE.getString("LoadNetwork"));
            Network network = aCase.getNetwork();

            logger.log(RESOURCE_BUNDLE.getString("RunningRao"));
            RaoComputation raoComputation = factorySupplier.get().create(network, cracFileProvider.getCracFile(), computationManager, 0);
            raoComputation.run(network.getVariantManager().getWorkingVariantId(), parameters)
                    .handleAsync((result, throwable) -> {
                        if (throwable == null) {
                            logger.log(RESOURCE_BUNDLE.getString("RaoComplete"));
                            runner.writeResult(result);
                        } else {
                            logger.log(RESOURCE_BUNDLE.getString("FailedRaoComputation"));
                            LOGGER.error(throwable.toString(), throwable);
                        }
                        return null;
                    }).join();
        } finally {
            runner.stopTask(taskId);
        }
    }


}
