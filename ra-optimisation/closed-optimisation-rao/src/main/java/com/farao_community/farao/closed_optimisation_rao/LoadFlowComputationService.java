/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.sensitivity.*;

/**
 * @author Luc DI GALLO {@literal <luc.di-gallo at rte-france.com>}
 */
public final class LoadFlowComputationService {
    private static LoadFlowFactory loadFlowFactory;
    private static ComputationManager computationManager;

    private LoadFlowComputationService() {
        throw new AssertionError("Utility class should not be instanciated");
    }

    public static void init(LoadFlowFactory factory, ComputationManager computationManager) {
        LoadFlowComputationService.loadFlowFactory = factory;
        LoadFlowComputationService.computationManager = computationManager;
    }

    public static LoadFlowResult runLoadFlow(Network network) {

        LoadFlow loadFlow = loadFlowFactory.create(network, computationManager, 1);
        return loadFlow.run(network.getVariantManager().getWorkingVariantId(), LoadFlowParameters.load()).join();
    }
}
