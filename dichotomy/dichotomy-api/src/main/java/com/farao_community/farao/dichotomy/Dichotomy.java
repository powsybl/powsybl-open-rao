/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.dichotomy;

import com.farao_community.farao.data.crac_api.Crac;
import com.powsybl.commons.Versionable;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.config.PlatformConfigNamedProvider;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class Dichotomy {

    private Dichotomy() {
        throw new AssertionError("Utility class should not been instantiated");
    }

    public static class Runner implements Versionable {

        private final DichotomyProvider provider;

        public Runner(DichotomyProvider provider) {
            this.provider = Objects.requireNonNull(provider);
        }

        public CompletableFuture<DichotomyResult> runAsync(Network network, Crac crac, Set<Country> region, DichotomyParameters parameters, ComputationManager computationManager) {
            Objects.requireNonNull(network, "Network should not be null");
            Objects.requireNonNull(crac, "CRAC should not be null");
            Objects.requireNonNull(region, "Region should not be null");
            Objects.requireNonNull(parameters, "Parameters should not be null");
            Objects.requireNonNull(computationManager, "Computation manager should not be null");

            return provider.run(network, crac, region, parameters, computationManager);
        }

        public DichotomyResult run(Network network, Crac crac, Set<Country> region, DichotomyParameters parameters, ComputationManager computationManager) {
            return runAsync(network, crac, region, parameters, computationManager).join();
        }

        @Override
        public String getName() {
            return provider.getName();
        }

        @Override
        public String getVersion() {
            return provider.getVersion();
        }
    }

    public static Runner find(String name) {
        return new Runner(PlatformConfigNamedProvider.Finder
            .find(name, "dichotomy", DichotomyProvider.class, PlatformConfig.defaultConfig()));
    }

    public static Runner find() {
        return find(null);
    }

    public CompletableFuture<DichotomyResult> runAsync(Network network, Crac crac, Set<Country> region, DichotomyParameters parameters, ComputationManager computationManager) {
        return find().runAsync(network, crac, region, parameters, computationManager);
    }

    public DichotomyResult run(Network network, Crac crac, Set<Country> region, DichotomyParameters parameters, ComputationManager computationManager) {
        return find().run(network, crac, region, parameters, computationManager);
    }
}
