/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition;

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
 * Parameters for flow decomposition.
 * Extensions may be added, for instance for implementation-specific parameters.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowDecompositionParameters extends AbstractExtendable<FlowDecompositionParameters> {
    /**
     * A configuration loader interface for the FlowDecompositionParameters extensions loaded from the platform configuration
     * @param <E> The extension class
     */
    public interface ConfigLoader<E extends Extension<FlowDecompositionParameters>> extends ExtensionConfigLoader<FlowDecompositionParameters, E> {
    }

    public static final String VERSION = "1.0";

    private static final Supplier<ExtensionProviders<ConfigLoader>> SUPPLIER =
            Suppliers.memoize(() -> ExtensionProviders.createProvider(ConfigLoader.class, "flow-decomposition-parameters"));

    private LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
    private SensitivityComputationParameters sensitivityComputationParameters = new SensitivityComputationParameters();

    /**
     * Load parameters from platform default config.
     */
    public static FlowDecompositionParameters load() {
        return load(PlatformConfig.defaultConfig());
    }

    /**
     * Load parameters from a provided platform config.
     */
    public static FlowDecompositionParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);

        FlowDecompositionParameters parameters = new FlowDecompositionParameters();
        parameters.setLoadFlowParameters(LoadFlowParameters.load(platformConfig));
        parameters.setSensitivityComputationParameters(SensitivityComputationParameters.load(platformConfig));

        parameters.readExtensions(platformConfig);

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

    public FlowDecompositionParameters setLoadFlowParameters(LoadFlowParameters loadFlowParameters) {
        this.loadFlowParameters = Objects.requireNonNull(loadFlowParameters);
        return this;
    }

    public SensitivityComputationParameters getSensitivityComputationParameters() {
        return sensitivityComputationParameters;
    }

    public FlowDecompositionParameters setSensitivityComputationParameters(SensitivityComputationParameters sensitivityComputationParameters) {
        this.sensitivityComputationParameters = Objects.requireNonNull(sensitivityComputationParameters);
        return this;
    }
}
