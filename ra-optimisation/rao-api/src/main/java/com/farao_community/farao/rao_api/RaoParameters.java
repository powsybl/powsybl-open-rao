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
        parameters.readExtensions(platformConfig);

        return parameters;
    }

    private void readExtensions(PlatformConfig platformConfig) {
        for (ExtensionConfigLoader provider : PARAMETERS_EXTENSIONS_SUPPLIER.get().getProviders()) {
            addExtension(provider.getExtensionClass(), provider.load(platformConfig));
        }
    }

}
