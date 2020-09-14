/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_computation;

import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.DefaultComputationManagerConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
final class SystematicSensitivityService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystematicSensitivityService.class);

    private static SensitivityComputationFactory sensitivityComputationFactory;
    private static ComputationManager computationManager;

    static void init(SensitivityComputationFactory factory, ComputationManager computationManager) {
        sensitivityComputationFactory = factory;
        SystematicSensitivityService.computationManager = computationManager;
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                               String workingStateId,
                                                               SensitivityProvider sensitivityProvider,
                                                               SensitivityComputationParameters sensitivityComputationParameters,
                                                               ComputationManager computationManager) {
        if (!initialised()) {
            init(ComponentDefaultConfig.load().newFactoryImpl(SensitivityComputationFactory.class), computationManager);
        }
        SensitivityComputation computation = sensitivityComputationFactory.create(network, computationManager, 1);
        LOGGER.debug("Sensitivity computation [start]");
        CompletableFuture<SensitivityComputationResults> results = computation.run(sensitivityProvider, sensitivityProvider, workingStateId, sensitivityComputationParameters);
        try {
            SensitivityComputationResults joinedResults = results.join();
            LOGGER.debug("Sensitivity computation [end]");
            return new SystematicSensitivityResult(joinedResults);
        } catch (CompletionException e) {
            throw new SensitivityComputationException("Sensitivity computation failed");
        }
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                               String workingStateId,
                                                               SensitivityProvider sensitivityProvider,
                                                               SensitivityComputationParameters sensitivityComputationParameters) {
        return runSensitivity(network, workingStateId, sensitivityProvider, sensitivityComputationParameters, DefaultComputationManagerConfig.load().createLongTimeExecutionComputationManager());
    }

    private static boolean initialised() {
        return sensitivityComputationFactory != null && computationManager != null;
    }

    private SystematicSensitivityService() {
    }
}
