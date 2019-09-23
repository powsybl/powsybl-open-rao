/*
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
import com.powsybl.sensitivity.SensitivityComputationParameters;

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

    public static final String VERSION = "1.0";

    private static final Supplier<ExtensionProviders<ConfigLoader>> SUPPLIER =
            Suppliers.memoize(() -> ExtensionProviders.createProvider(ConfigLoader.class, "fb-computation-parameters"));

    private LoadFlowParameters loadFlowParameters = new LoadFlowParameters();

    private SensitivityComputationParameters sensitivityComputationParameters = new SensitivityComputationParameters();

    public static FlowBasedComputationParameters load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static FlowBasedComputationParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);

        FlowBasedComputationParameters parameters = new FlowBasedComputationParameters();
        parameters.readExtensions(platformConfig);

        parameters.setLoadFlowParameters(LoadFlowParameters.load(platformConfig));
        parameters.setSensitivityComputationParameters(SensitivityComputationParameters.load(platformConfig));

        return parameters;
    }

    private void readExtensions(PlatformConfig platformConfig) {
        for (ExtensionConfigLoader provider : SUPPLIER.get().getProviders()) {
            addExtension(provider.getExtensionClass(), provider.load(platformConfig));
        }
    }

    public LoadFlowParameters getLoadFlowParameters() {
        return loadFlowParameters;
    }

    public FlowBasedComputationParameters setLoadFlowParameters(LoadFlowParameters loadFlowParameters) {
        this.loadFlowParameters = Objects.requireNonNull(loadFlowParameters);
        return this;
    }

    public SensitivityComputationParameters getSensitivityComputationParameters() {
        return sensitivityComputationParameters;
    }

    public FlowBasedComputationParameters setSensitivityComputationParameters(SensitivityComputationParameters sensitivityComputationParameters) {
        this.sensitivityComputationParameters = Objects.requireNonNull(sensitivityComputationParameters);
        return this;
    }
}
