/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.mocks;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.*;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class MockLoadFlowFactory implements LoadFlowFactory {
    @Override
    public LoadFlow create(Network network, ComputationManager computationManager, int i) {
        return new LoadFlow() {
            @Override
            public CompletableFuture<LoadFlowResult> run(String s, LoadFlowParameters loadFlowParameters) {
                return CompletableFuture.completedFuture(generateResults());
            }

            private LoadFlowResult generateResults() {
                return new LoadFlowResultImpl(true, Collections.emptyMap(), "");
            }

            @Override
            public String getName() {
                return "Mock";
            }

            @Override
            public String getVersion() {
                return "Mock";
            }
        };
    }
}
