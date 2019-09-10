/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.mocks;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class MockSensitivityComputationFactory implements SensitivityComputationFactory {
    class MockSensitivityComputation implements SensitivityComputation {
        private final Network network;

        MockSensitivityComputation(Network network) {
            this.network = network;
        }

        @Override
        public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider sensitivityFactorsProvider, String s, SensitivityComputationParameters sensitivityComputationParameters) {
            return CompletableFuture.completedFuture(randomResults(network, sensitivityFactorsProvider));
        }

        private SensitivityComputationResults randomResults(Network network, SensitivityFactorsProvider sensitivityFactorsProvider) {
            List<SensitivityValue> randomSensitivities = sensitivityFactorsProvider.getFactors(network).stream().map(factor -> new SensitivityValue(factor, Math.random(), Math.random(), Math.random())).collect(Collectors.toList());
            return new SensitivityComputationResults(true, Collections.emptyMap(), "", randomSensitivities);
        }

        @Override
        public String getName() {
            return "Mock";
        }

        @Override
        public String getVersion() {
            return "Mock";
        }
    }

    @Override
    public SensitivityComputation create(Network network, ComputationManager computationManager, int i) {
        return new MockSensitivityComputation(network);
    }
}
