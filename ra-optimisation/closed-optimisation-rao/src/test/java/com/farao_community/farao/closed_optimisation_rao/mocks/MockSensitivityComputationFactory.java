/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.mocks;

import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class MockSensitivityComputationFactory implements SensitivityComputationFactory {
    private static final long RANDOM_GENERATION_SEED = 42L;
    private static final Random RANDOM = new Random();

    {
        RANDOM.setSeed(RANDOM_GENERATION_SEED);
    }

    @Override
    public SensitivityComputation create(Network network, ComputationManager computationManager, int priority) {
        return new SensitivityComputation() {
            @Override
            public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider factorsProvider, ContingenciesProvider contingenciesProvider, String workingStateId, SensitivityComputationParameters sensiParameters) {
                List<SensitivityValue> sensitivityValuesN = factorsProvider.getFactors(network).stream().map(factor -> new SensitivityValue(factor, RANDOM.nextDouble(), RANDOM.nextDouble(), RANDOM.nextDouble())).collect(Collectors.toList());
                Map<String, List<SensitivityValue>> sensitivityValuesContingencies = contingenciesProvider.getContingencies(network).stream()
                        .collect(Collectors.toMap(
                            contingency -> contingency.getId(),
                            contingency -> factorsProvider.getFactors(network).stream().map(factor -> new SensitivityValue(factor, RANDOM.nextDouble(), RANDOM.nextDouble(), RANDOM.nextDouble())).collect(Collectors.toList())
                        ));
                SensitivityComputationResults results = new SensitivityComputationResults(true, Collections.emptyMap(), "", sensitivityValuesN, sensitivityValuesContingencies);
                return CompletableFuture.completedFuture(results);
            }

            @Override
            public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider factorsProvider, String workingStateId, SensitivityComputationParameters sensiParameters) {
                List<SensitivityValue> sensitivityValuesN = factorsProvider.getFactors(network).stream().map(factor -> new SensitivityValue(factor, RANDOM.nextDouble(), RANDOM.nextDouble(), RANDOM.nextDouble())).collect(Collectors.toList());
                SensitivityComputationResults results = new SensitivityComputationResults(true, Collections.emptyMap(), "", sensitivityValuesN, Collections.emptyMap());
                return CompletableFuture.completedFuture(results);
            }

            @Override
            public String getName() {
                return "Sensitivity computation mock";
            }

            @Override
            public String getVersion() {
                return null;
            }
        };
    }
}
