/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.commons.Versionable;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.util.ServiceLoaderCache;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * RA optimisation main API. It is a utility class (so with only static methods) used as an entry point for running
 * a RA optimisation allowing to choose either a specific find implementation or just to rely on default one.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class Rao {

    private Rao() {
        throw new AssertionError("Utility class should not been instantiated");
    }

    private static final Supplier<List<RaoProvider>> RAO_PROVIDERS
            = Suppliers.memoize(() -> new ServiceLoaderCache<>(RaoProvider.class).getServices());

    /**
     * A RA optimisation runner is responsible for providing convenient methods on top of {@link RaoProvider}:
     * several variants of synchronous and asynchronous run with default parameters.
     */
    public static class Runner implements Versionable {

        private final RaoProvider provider;

        public Runner(RaoProvider provider) {
            this.provider = Objects.requireNonNull(provider);
        }

        public CompletableFuture<RaoResult> runAsync(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant) {
            Objects.requireNonNull(raoInput, "RAO input should not be null");
            Objects.requireNonNull(parameters, "parameters should not be null");

            return provider.run(raoInput, parameters, targetEndInstant);
        }

        public CompletableFuture<RaoResult> runAsync(RaoInput raoInput, RaoParameters parameters) {
            return runAsync(raoInput, parameters, null);
        }

        public CompletableFuture<RaoResult> runAsync(RaoInput raoInput) {
            return runAsync(raoInput, (Instant) null);
        }

        public CompletableFuture<RaoResult> runAsync(RaoInput raoInput, Instant targetEndInstant) {
            return runAsync(raoInput, RaoParameters.load(), targetEndInstant);
        }

        public RaoResult run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant) {
            Objects.requireNonNull(raoInput, "RAO input should not be null");
            Objects.requireNonNull(parameters, "parameters should not be null");

            return provider.run(raoInput, parameters, targetEndInstant).join();
        }

        public RaoResult run(RaoInput raoInput, RaoParameters parameters) {
            return run(raoInput, parameters, null);
        }

        public RaoResult run(RaoInput raoInput) {
            return run(raoInput, RaoParameters.load(), null);
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
     * Get a runner for a RAO named {@code name}. In the case of a null {@code name}, default
     * implementation is used.
     *
     * @param name name of the RAO implementation, null if we want to use default one
     * @return a runner for RAO implementation named {@code name}
     */
    public static Runner find(String name) {
        return find(name, RAO_PROVIDERS.get(), PlatformConfig.defaultConfig());
    }

    /**
     * Get a runner for default RAO implementation.
     *
     * @throws FaraoException in case we cannot find a default implementation
     * @return a runner for default RAO implementation
     */
    public static Runner find() {
        return find(null);
    }

    /**
     * A variant of {@link Rao#find(String)} intended to be used for unit testing that allow passing
     * an explicit provider list instead of relying on service loader and an explicit {@link PlatformConfig}
     * instead of global one.
     *
     * @param name name of the RAO implementation, null if we want to use default one
     * @param providers flowbased provider list
     * @param platformConfig platform config to look for default flowbased implementation name
     * @return a runner for flowbased implementation named {@code name}
     */
    public static Runner find(String name, List<RaoProvider> providers, PlatformConfig platformConfig) {
        Objects.requireNonNull(providers);
        Objects.requireNonNull(platformConfig);

        if (providers.isEmpty()) {
            throw new FaraoException("No RAO providers found");
        }

        // if no RAO implementation name is provided through the API we look for information
        // in platform configuration
        String raOptimizerName = name != null ? name : platformConfig.getOptionalModuleConfig("rao")
                .flatMap(mc -> mc.getOptionalStringProperty("default"))
                .orElse(null);
        RaoProvider provider;
        if (providers.size() == 1 && raOptimizerName == null) {
            // no information to select the implementation but only one provider, so we can use it by default
            // (that is be the most common use case)
            provider = providers.get(0);
        } else {
            if (providers.size() > 1 && raOptimizerName == null) {
                // several providers and no information to select which one to choose, we can only throw
                // an exception
                List<String> raOptimizerNames = providers.stream().map(RaoProvider::getName).collect(Collectors.toList());
                throw new FaraoException("Several RAO implementations found (" + raOptimizerNames
                        + "), you must add configuration to select the implementation");
            }
            provider = providers.stream()
                    .filter(p -> p.getName().equals(raOptimizerName))
                    .findFirst()
                    .orElseThrow(() -> new FaraoException("RA optimizer provider '" + raOptimizerName + "' not found"));
        }

        return new Runner(provider);
    }

    public static CompletableFuture<RaoResult> runAsync(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant) {
        return find().runAsync(raoInput, parameters, targetEndInstant);
    }

    public static CompletableFuture<RaoResult> runAsync(RaoInput raoInput, Instant targetEndInstant) {
        return find().runAsync(raoInput, targetEndInstant);
    }

    public static RaoResult run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant) {
        return find().run(raoInput, parameters, targetEndInstant);
    }

    public static RaoResult run(RaoInput raoInput, RaoParameters parameters) {
        return find().run(raoInput, parameters);
    }

    public static RaoResult run(RaoInput raoInput) {
        return find().run(raoInput);
    }
}
