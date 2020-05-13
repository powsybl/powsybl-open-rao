/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.DefaultComputationManagerConfig;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.EmptyContingencyListProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class SensitivityComputationService {
    private static SensitivityComputationFactory sensitivityComputationFactory;
    private static ComputationManager computationManager;

    private SensitivityComputationService() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static void init(SensitivityComputationFactory factory, ComputationManager computationManager) {
        SensitivityComputationService.sensitivityComputationFactory = factory;
        SensitivityComputationService.computationManager = computationManager;
    }

    public static SensitivityComputationResults runSensitivity(Network network,
                                                               String workingStateId,
                                                               SensitivityFactorsProvider factorsProvider,
                                                               ContingenciesProvider contingenciesProvider,
                                                               SensitivityComputationParameters sensitivityComputationParameters,
                                                               ComputationManager computationManager) {
        if (!initialised()) {
            init(ComponentDefaultConfig.load().newFactoryImpl(SensitivityComputationFactory.class), computationManager);
        }
        SensitivityComputation computation = sensitivityComputationFactory.create(network, computationManager, 1);
        CompletableFuture<SensitivityComputationResults> results = computation.run(factorsProvider, contingenciesProvider, workingStateId, sensitivityComputationParameters);
        try {
            return results.join();
        } catch (CompletionException e) {
            throw new FaraoException("Sensitivity computation failed");
        }
    }

    public static SensitivityComputationResults runSensitivity(Network network,
                                                               String workingStateId,
                                                               SensitivityFactorsProvider factorsProvider,
                                                               ContingenciesProvider contingenciesProvider,
                                                               SensitivityComputationParameters sensitivityComputationParameters) {
        return runSensitivity(network, workingStateId, factorsProvider, contingenciesProvider, sensitivityComputationParameters, DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager());
    }

    public static SensitivityComputationResults runSensitivity(Network network,
                                                               String workingStateId,
                                                               SensitivityFactorsProvider factorsProvider,
                                                               SensitivityComputationParameters sensitivityComputationParameters) {
        return runSensitivity(network, workingStateId, factorsProvider, new EmptyContingencyListProvider(), sensitivityComputationParameters);
    }

    public static SensitivityComputationResults runSensitivity(Network network,
                                                               String workingStateId,
                                                               SensitivityFactorsProvider factorsProvider) {
        return runSensitivity(network, workingStateId, factorsProvider, SensitivityComputationParameters.load());
    }

    private static boolean initialised() {
        return sensitivityComputationFactory != null && computationManager != null;
    }
}
