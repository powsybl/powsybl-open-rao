/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.commons.Versionable;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * FlowBased main API. It is a utility class (so with only static methods) used as an entry point for running
 * a flowbased computation allowing to choose either a specific find implementation or just to rely on default one.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class FlowBasedComputation {

    private FlowBasedComputation() {
        throw new AssertionError("Utility class should not been instantiated");
    }

    private static final Supplier<List<FlowBasedComputationProvider>> PROVIDERS_SUPPLIERS
            = Suppliers.memoize(() -> new ServiceLoaderCache<>(FlowBasedComputationProvider.class).getServices());

    /**
     * A FlowBased  computation runner is responsible for providing convenient methods on top of {@link FlowBasedComputationProvider}:
     * several variants of synchronous and asynchronous run with default parameters.
     */
    public static class Runner implements Versionable {

        private final FlowBasedComputationProvider provider;

        public Runner(FlowBasedComputationProvider provider) {
            this.provider = Objects.requireNonNull(provider);
        }

        public CompletableFuture<FlowBasedComputationResult> runAsync(Network network, CracFile cracFile, GlskProvider glskProvider, String workingStateId, ComputationManager computationManager, FlowBasedComputationParameters parameters) {
            Objects.requireNonNull(workingStateId);
            Objects.requireNonNull(parameters);
            return provider.run(network, cracFile, glskProvider, computationManager, workingStateId, parameters);
        }

        public CompletableFuture<FlowBasedComputationResult> runAsync(Network network, CracFile cracFile, GlskProvider glskProvider, ComputationManager computationManager, FlowBasedComputationParameters parameters) {
            return runAsync(network, cracFile, glskProvider, network.getVariantManager().getWorkingVariantId(), computationManager, parameters);
        }

        public CompletableFuture<FlowBasedComputationResult> runAsync(Network network, CracFile cracFile, GlskProvider glskProvider, FlowBasedComputationParameters parameters) {
            return runAsync(network, cracFile, glskProvider, LocalComputationManager.getDefault(), parameters);
        }

        public CompletableFuture<FlowBasedComputationResult> runAsync(Network network, CracFile cracFile, GlskProvider glskProvider) {
            return runAsync(network, cracFile, glskProvider, FlowBasedComputationParameters.load());
        }

        public FlowBasedComputationResult run(Network network, CracFile cracFile, GlskProvider glskProvider, String workingStateId, ComputationManager computationManager, FlowBasedComputationParameters parameters) {
            Objects.requireNonNull(workingStateId);
            Objects.requireNonNull(parameters);
            return provider.run(network, cracFile, glskProvider, computationManager, workingStateId, parameters).join();
        }

        public FlowBasedComputationResult run(Network network, CracFile cracFile, GlskProvider glskProvider, ComputationManager computationManager, FlowBasedComputationParameters parameters) {
            return run(network, cracFile, glskProvider, network.getVariantManager().getWorkingVariantId(), computationManager, parameters);
        }

        public FlowBasedComputationResult run(Network network, CracFile cracFile, GlskProvider glskProvider, FlowBasedComputationParameters parameters) {
            return run(network, cracFile, glskProvider, LocalComputationManager.getDefault(), parameters);
        }

        public FlowBasedComputationResult run(Network network, CracFile cracFile, GlskProvider glskProvider) {
            return run(network, cracFile, glskProvider, FlowBasedComputationParameters.load());
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

    /**
     * Get a runner for flowbased implementation named {@code name}. In the case of a null {@code name}, default
     * implementation is used.
     *
     * @param name name of the flowbased implementation, null if we want to use default one
     * @return a runner for flowbased implementation named {@code name}
     */
    public static Runner find(String name) {
        return find(name, PROVIDERS_SUPPLIERS.get(), PlatformConfig.defaultConfig());
    }

    /**
     * Get a runner for default flowbased implementation.
     *
     * @throws FaraoException in case we cannot find a default implementation
     * @return a runner for default flowbased implementation
     */
    public static Runner find() {
        return find(null);
    }

    /**
     * A variant of {@link FlowBasedComputation#find(String)} intended to be used for unit testing that allow passing
     * an explicit provider list instead of relying on service loader and an explicit {@link PlatformConfig}
     * instead of global one.
     *
     * @param name name of the flowbased implementation, null if we want to use default one
     * @param providers flowbased provider list
     * @param platformConfig platform config to look for default flowbased implementation name
     * @return a runner for flowbased implementation named {@code name}
     */
    public static Runner find(String name, List<FlowBasedComputationProvider> providers, PlatformConfig platformConfig) {
        Objects.requireNonNull(providers);
        Objects.requireNonNull(platformConfig);

        if (providers.isEmpty()) {
            throw new FaraoException("No flowbased providers found");
        }

        // if no flowbased implementation name is provided through the API we look for information
        // in platform configuration
        String flowbasedName = name != null ? name : platformConfig.getOptionalModuleConfig("flowbased-computation")
                .flatMap(mc -> mc.getOptionalStringProperty("default"))
                .orElse(null);
        FlowBasedComputationProvider provider;
        if (providers.size() == 1 && flowbasedName == null) {
            // no information to select the implementation but only one provider, so we can use it by default
            // (that is be the most common use case)
            provider = providers.get(0);
        } else {
            if (providers.size() > 1 && flowbasedName == null) {
                // several providers and no information to select which one to choose, we can only throw
                // an exception
                List<String> flowbasedNames = providers.stream().map(FlowBasedComputationProvider::getName).collect(Collectors.toList());
                throw new FaraoException("Several flowbased implementations found (" + flowbasedNames
                        + "), you must add configuration to select the implementation");
            }
            provider = providers.stream()
                    .filter(p -> p.getName().equals(flowbasedName))
                    .findFirst()
                    .orElseThrow(() -> new FaraoException("FlowBased computation provider '" + flowbasedName + "' not found"));
        }

        return new Runner(provider);
    }

    public static CompletableFuture<FlowBasedComputationResult> runAsync(Network network, CracFile cracFile, GlskProvider glskProvider, String workingStateId, ComputationManager computationManager, FlowBasedComputationParameters parameters) {
        return find().runAsync(network, cracFile, glskProvider, workingStateId, computationManager, parameters);
    }

    public static CompletableFuture<FlowBasedComputationResult> runAsync(Network network, CracFile cracFile, GlskProvider glskProvider, ComputationManager computationManager, FlowBasedComputationParameters parameters) {
        return find().runAsync(network, cracFile, glskProvider, computationManager, parameters);
    }

    public static CompletableFuture<FlowBasedComputationResult> runAsync(Network network, CracFile cracFile, GlskProvider glskProvider, FlowBasedComputationParameters parameters) {
        return find().runAsync(network, cracFile, glskProvider, parameters);
    }

    public static CompletableFuture<FlowBasedComputationResult> runAsync(Network network, CracFile cracFile, GlskProvider glskProvider) {
        return find().runAsync(network, cracFile, glskProvider);
    }

    public static FlowBasedComputationResult run(Network network, CracFile cracFile, GlskProvider glskProvider, String workingStateId, ComputationManager computationManager, FlowBasedComputationParameters parameters) {
        return find().run(network, cracFile, glskProvider, workingStateId, computationManager, parameters);
    }

    public static FlowBasedComputationResult run(Network network, CracFile cracFile, GlskProvider glskProvider, ComputationManager computationManager, FlowBasedComputationParameters parameters) {
        return find().run(network, cracFile, glskProvider, computationManager, parameters);
    }

    public static FlowBasedComputationResult run(Network network, CracFile cracFile, GlskProvider glskProvider, FlowBasedComputationParameters parameters) {
        return find().run(network, cracFile, glskProvider, parameters);
    }

    public static FlowBasedComputationResult run(Network network, CracFile cracFile, GlskProvider glskProvider) {
        return find().run(network, cracFile, glskProvider);
    }
}
