/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.tools.Version;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class TimeCoupledRao {
    private TimeCoupledRao() {
        throw new AssertionError("Utility class should not been instantiated");
    }

    private static final Supplier<List<TimeCoupledRaoProvider>> RAO_PROVIDERS
        = Suppliers.memoize(() -> new ServiceLoaderCache<>(TimeCoupledRaoProvider.class).getServices());

    /**
     * A time-coupled RA optimization runner is responsible for providing convenient methods on top of
     * {@link TimeCoupledRaoProvider}: several variants of synchronous and asynchronous run with default parameters.
     */
    public static class Runner {

        private final TimeCoupledRaoProvider provider;

        public Runner(TimeCoupledRaoProvider provider) {
            this.provider = Objects.requireNonNull(provider);
        }

        public TimeCoupledRaoResult run(TimeCoupledRaoInputWithNetworkPaths raoInput, RaoParameters parameters) {
            Objects.requireNonNull(raoInput, "RAO input should not be null");
            Objects.requireNonNull(parameters, "parameters should not be null");

            Version openRaoVersion = ServiceLoader.load(Version.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(version -> version.getRepositoryName().equals("open-rao"))
                .findFirst().orElseThrow();
            BUSINESS_WARNS.warn("Running RAO using Open RAO version {} from git commit {}.", openRaoVersion.getMavenProjectVersion(), openRaoVersion.getGitVersion());

            return provider.run(raoInput, parameters).join();
        }

        public TimeCoupledRaoResult run(TimeCoupledRaoInputWithNetworkPaths raoInput) {
            return run(raoInput, RaoParameters.load());
        }

        public String getName() {
            return provider.getName();
        }
    }

    /**
     * Get a runner for a time-coupled RAO named {@code name}. In the case of a null {@code name}, default
     * implementation is used.
     *
     * @param name name of the RAO implementation, null if we want to use default one
     * @return a runner for RAO implementation named {@code name}
     */
    public static TimeCoupledRao.Runner find(String name) {
        return find(name, RAO_PROVIDERS.get(), PlatformConfig.defaultConfig());
    }

    /**
     * Get a runner for default time-coupled RAO implementation.
     *
     * @throws OpenRaoException in case we cannot find a default implementation
     * @return a runner for default RAO implementation
     */
    public static TimeCoupledRao.Runner find() {
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
    public static TimeCoupledRao.Runner find(String name, List<TimeCoupledRaoProvider> providers, PlatformConfig platformConfig) {
        Objects.requireNonNull(providers);
        Objects.requireNonNull(platformConfig);

        if (providers.isEmpty()) {
            throw new OpenRaoException("No time-coupled RAO providers found");
        }

        // if no RAO implementation name is provided through the API we look for information
        // in platform configuration
        String raOptimizerName = name != null ? name : platformConfig.getOptionalModuleConfig("rao")
            .flatMap(mc -> mc.getOptionalStringProperty("default"))
            .orElse(null);
        TimeCoupledRaoProvider provider;
        if (providers.size() == 1 && raOptimizerName == null) {
            // no information to select the implementation but only one provider, so we can use it by default
            // (that is the most common use case)
            provider = providers.getFirst();
        } else {
            if (providers.size() > 1 && raOptimizerName == null) {
                // several providers and no information to select which one to choose, we can only throw
                // an exception
                List<String> raOptimizerNames = providers.stream().map(TimeCoupledRaoProvider::getName).toList();
                throw new OpenRaoException("Several RAO implementations found (" + raOptimizerNames
                    + "), you must add configuration to select the implementation");
            }
            provider = providers.stream()
                .filter(p -> p.getName().equals(raOptimizerName))
                .findFirst()
                .orElseThrow(() -> new OpenRaoException("RA optimizer provider '" + raOptimizerName + "' not found"));
        }

        return new TimeCoupledRao.Runner(provider);
    }

    public static TimeCoupledRaoResult run(TimeCoupledRaoInputWithNetworkPaths raoInput, RaoParameters parameters) {
        return find().run(raoInput, parameters);
    }

    public static TimeCoupledRaoResult run(TimeCoupledRaoInputWithNetworkPaths raoInput) {
        return find().run(raoInput);
    }
}
