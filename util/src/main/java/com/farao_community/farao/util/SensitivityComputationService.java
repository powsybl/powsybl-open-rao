/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.DefaultComputationManagerConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class SensitivityComputationService {
    private static SensitivityComputationFactory sensitivityComputationFactory;
    private static ComputationManager computationManager;

    private SensitivityComputationService() {
        throw new AssertionError("Utility class should not be instanciated");
    }

    public static void init(SensitivityComputationFactory factory, ComputationManager computationManager) {
        SensitivityComputationService.sensitivityComputationFactory = factory;
        SensitivityComputationService.computationManager = computationManager;
    }

    public static SensitivityComputationResults runSensitivity(Network network,
                                                        String workingStateId,
                                                        SensitivityFactorsProvider factorsProvider) {
        if (!initialised()) {
            init(ComponentDefaultConfig.load().newFactoryImpl(SensitivityComputationFactory.class), DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager());
        }
        SensitivityComputation computation = sensitivityComputationFactory.create(network, computationManager, 1);
        return computation.run(factorsProvider, workingStateId, SensitivityComputationParameters.load()).join();
    }

    private static boolean initialised() {
        return sensitivityComputationFactory != null && computationManager != null;
    }
}
