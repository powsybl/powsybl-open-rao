/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.POST_PROCESSING_SECTION;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.REMOVE_ADDED_VARIANTS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class PostProcessingParameters {
    private static final boolean DEFAULT_REMOVE_ADDED_VARIANTS = true;
    private boolean removeAddedVariants = DEFAULT_REMOVE_ADDED_VARIANTS;

    public boolean mustRemoveAddedVariants() {
        return removeAddedVariants;
    }

    public void setRemoveAddedVariants(boolean removeAddedVariants) {
        this.removeAddedVariants = removeAddedVariants;
    }

    public static PostProcessingParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        PostProcessingParameters parameters = new PostProcessingParameters();
        platformConfig.getOptionalModuleConfig(POST_PROCESSING_SECTION)
            .ifPresent(config -> {
                parameters.setRemoveAddedVariants(config.getBooleanProperty(REMOVE_ADDED_VARIANTS, DEFAULT_REMOVE_ADDED_VARIANTS));
            });
        return parameters;
    }
}
