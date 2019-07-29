/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionConfigLoader;
import com.powsybl.commons.extensions.ExtensionProviders;
import com.powsybl.loadflow.LoadFlowParameters;

import java.util.Objects;

/**
 * Parameters for flowbased computation.
 * Extensions may be added, for instance for implementation-specific parameters.
 *
 * @author Di Gallo Luc  {@literal <luc.di-gallo at rte-france.com>}
 */
public class FlowBasedComputationParameters extends AbstractExtendable<FlowBasedComputationParameters> {

    /**
     * A configuration loader interface for the FlowBasedComputationParameters extensions loaded from the platform configuration
     *
     * @param <E> The extension class
     */
    public interface ConfigLoader<E extends Extension<FlowBasedComputationParameters>> extends ExtensionConfigLoader<FlowBasedComputationParameters, E> {
    }

    /**
     * parameter version
     */
    public static final String VERSION = "1.0";

    /**
     * Supplier
     */
    private static final Supplier<ExtensionProviders<ConfigLoader>> SUPPLIER =
            Suppliers.memoize(() -> ExtensionProviders.createProvider(ConfigLoader.class, "fb-computation-parameters"));

    /**
     * Load Flow parameters
     */
    private LoadFlowParameters loadFlowParameters = new LoadFlowParameters();

    /**
     * Load parameters from platform default config.
     */
    public static FlowBasedComputationParameters load() {
        return load(PlatformConfig.defaultConfig());
    }

    /**
     * Load parameters from a provided platform config.
     */
    public static FlowBasedComputationParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);

        FlowBasedComputationParameters parameters = new FlowBasedComputationParameters();
        parameters.readExtensions(platformConfig);

        parameters.setLoadFlowParameters(LoadFlowParameters.load(platformConfig));

        return parameters;
    }

    /**
     * read extension of parmameter file
     * @param platformConfig
     */
    private void readExtensions(PlatformConfig platformConfig) {
        for (ExtensionConfigLoader provider : SUPPLIER.get().getProviders()) {
            addExtension(provider.getExtensionClass(), provider.load(platformConfig));
        }
    }

    /**
     * @return load flow parameters getter
     */
    public LoadFlowParameters getLoadFlowParameters() {
        return loadFlowParameters;
    }

    /**
     * @param loadFlowParameters setter LoadFlowParameters for flow based computation
     * @return FlowBasedComputationParameters
     */
    public FlowBasedComputationParameters setLoadFlowParameters(LoadFlowParameters loadFlowParameters) {
        this.loadFlowParameters = Objects.requireNonNull(loadFlowParameters);
        return this;
    }
}
