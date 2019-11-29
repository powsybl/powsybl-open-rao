/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResult;
import com.farao_community.farao.commons.FaraoException;

import java.util.Objects;

/**
 * Loadflow computation wrapper object
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class LoadFlowService {
    private LoadFlow.Runner loadFlowRunner;
    private ComputationManager computationManager;

    public LoadFlowResult compute(Network network, String workingStateId, FullLineDecompositionParameters parameters) {
        try {
            return loadFlowRunner.run(network, workingStateId, computationManager, parameters.getExtendable().getLoadFlowParameters());
        } catch (Exception e) {
            throw new FaraoException(e);
        }
    }

    public LoadFlowService(LoadFlow.Runner loadFlowRunner, ComputationManager computationManager) {
        this.loadFlowRunner = Objects.requireNonNull(loadFlowRunner);
        this.computationManager = Objects.requireNonNull(computationManager);

    }
}
