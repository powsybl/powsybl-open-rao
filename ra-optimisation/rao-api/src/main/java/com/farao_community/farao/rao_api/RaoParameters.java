/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api;

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
 * Parameters for rao
 * Extensions may be added, for instance for implementation-specific parameters.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoParameters extends AbstractExtendable<RaoParameters> {

    /**
     * A configuration loader interface for the RaoParameters extensions loaded from the platform configuration
     * @param <E> The extension class
     */
    public static interface ConfigLoader<E extends Extension<RaoParameters>> extends ExtensionConfigLoader<RaoParameters, E> {
    }

    public static final String VERSION = "1.0";
    static final boolean DEFAULT_DC_MODE = false;
    static final boolean DEFAULT_AC_TO_DC_FALLBACK = false;

    private boolean dcMode = DEFAULT_DC_MODE;
    private boolean acToDcFallback = DEFAULT_AC_TO_DC_FALLBACK;

    private LoadFlowParameters loadFlowParameters = new LoadFlowParameters();

    private static final Supplier<ExtensionProviders<ConfigLoader>> PARAMETERS_EXTENSIONS_SUPPLIER =
        Suppliers.memoize(() -> ExtensionProviders.createProvider(ConfigLoader.class, "rao-parameters"));

    /**
     * Load parameters from platform default config.
     */
    public static RaoParameters load() {
        return load(PlatformConfig.defaultConfig());
    }

    /**
     * Load parameters from a provided platform config.
     */
    public static RaoParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);

        RaoParameters parameters = new RaoParameters();
        load(parameters, platformConfig);
        parameters.readExtensions(platformConfig);

        parameters.setLoadFlowParameters(LoadFlowParameters.load(platformConfig));

        return parameters;
    }

    protected static void load(RaoParameters parameters, PlatformConfig platformConfig) {
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(platformConfig);

        platformConfig.getOptionalModuleConfig("rao-parameters")
            .ifPresent(config -> {
                parameters.setDcMode(config.getBooleanProperty("dc-mode", DEFAULT_DC_MODE));
                parameters.setAcToDcFallback(config.getBooleanProperty("ac-to-dc-fallback", DEFAULT_AC_TO_DC_FALLBACK));
            });
    }

    private void readExtensions(PlatformConfig platformConfig) {
        for (ExtensionConfigLoader provider : PARAMETERS_EXTENSIONS_SUPPLIER.get().getProviders()) {
            addExtension(provider.getExtensionClass(), provider.load(platformConfig));
        }
    }

    public boolean isDcMode() {
        return dcMode;
    }

    public void setDcMode(boolean dcMode) {
        this.dcMode = dcMode;
    }

    public boolean isAcToDcFallback() {
        return acToDcFallback;
    }

    public void setAcToDcFallback(boolean acToDcFallback) {
        this.acToDcFallback = acToDcFallback;
    }

    public LoadFlowParameters getLoadFlowParameters() {
        return loadFlowParameters;
    }

    public RaoParameters setLoadFlowParameters(LoadFlowParameters loadFlowParameters) {
        this.loadFlowParameters = Objects.requireNonNull(loadFlowParameters);
        return this;
    }

}
