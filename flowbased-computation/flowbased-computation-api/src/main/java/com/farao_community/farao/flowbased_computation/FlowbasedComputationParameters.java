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
import com.powsybl.sensitivity.SensitivityAnalysisParameters;

import java.util.Objects;

/**
 * Parameters for flowbased computation.
 * Extensions may be added, for instance for implementation-specific parameters.
 *
 * @author Di Gallo Luc  {@literal <luc.di-gallo at rte-france.com>}
 */
public class FlowbasedComputationParameters extends AbstractExtendable<FlowbasedComputationParameters> {

    /**
     * A configuration loader interface for the FlowBasedComputationParameters extensions loaded from the platform configuration
     *
     * @param <E> The extension class
     */
    public interface ConfigLoader<E extends Extension<FlowbasedComputationParameters>> extends ExtensionConfigLoader<FlowbasedComputationParameters, E> {
    }

    public static final String VERSION = "1.0";

    private static final Supplier<ExtensionProviders<ConfigLoader>> SUPPLIER =
            Suppliers.memoize(() -> ExtensionProviders.createProvider(ConfigLoader.class, "fb-computation-parameters"));

    private LoadFlowParameters loadFlowParameters = new LoadFlowParameters();

    private SensitivityAnalysisParameters sensitivityAnalysisParameters = new SensitivityAnalysisParameters();

    public static FlowbasedComputationParameters load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static FlowbasedComputationParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);

        FlowbasedComputationParameters parameters = new FlowbasedComputationParameters();
        parameters.readExtensions(platformConfig);

        parameters.setLoadFlowParameters(LoadFlowParameters.load(platformConfig));
        parameters.setSensitivityAnalysisParameters(SensitivityAnalysisParameters.load(platformConfig));

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

    public FlowbasedComputationParameters setLoadFlowParameters(LoadFlowParameters loadFlowParameters) {
        this.loadFlowParameters = Objects.requireNonNull(loadFlowParameters);
        return this;
    }

    public SensitivityAnalysisParameters getSensitivityAnalysisParameters() {
        return sensitivityAnalysisParameters;
    }

    public FlowbasedComputationParameters setSensitivityAnalysisParameters(SensitivityAnalysisParameters sensitivityAnalysisParameters) {
        this.sensitivityAnalysisParameters = Objects.requireNonNull(sensitivityAnalysisParameters);
        return this;
    }
}
