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
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class LoadFlowService {
    private static LoadFlowFactory loadFlowFactory;
    private static ComputationManager computationManager;

    private LoadFlowService() {
        throw new AssertionError("Utility class should not be instanciated");
    }

    public static void init(LoadFlowFactory factory, ComputationManager computationManager) {
        LoadFlowService.loadFlowFactory = factory;
        LoadFlowService.computationManager = computationManager;
    }

    public static LoadFlowResult runLoadFlow(Network network,
                                                String workingStateId) {
        if (!initialised()) {
            init(ComponentDefaultConfig.load().newFactoryImpl(LoadFlowFactory.class), DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager());
        }
        LoadFlow computation = loadFlowFactory.create(network, computationManager, 1);
        return computation.run(workingStateId, LoadFlowParameters.load()).join();
    }

    private static boolean initialised() {
        return loadFlowFactory != null && computationManager != null;
    }
}
