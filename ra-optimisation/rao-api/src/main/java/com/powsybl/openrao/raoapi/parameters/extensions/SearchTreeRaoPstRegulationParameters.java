/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.PSTS_TO_REGULATE;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.ST_PST_REGULATION_SECTION;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class SearchTreeRaoPstRegulationParameters {
    private static final List<String> DEFAULT_PSTS_TO_REGULATE = new ArrayList<>();
    private List<String> pstsToRegulate = DEFAULT_PSTS_TO_REGULATE;

    public List<String> getPstsToRegulate() {
        return pstsToRegulate;
    }

    public void setPstsToRegulate(List<String> pstsToRegulate) {
        this.pstsToRegulate = pstsToRegulate;
    }

    public List<String> getPstsToRegulate(RaoParameters raoParameters) {
        if (raoParameters.hasExtension(SearchTreeRaoPstRegulationParameters.class)) {
            return raoParameters.getExtension(SearchTreeRaoPstRegulationParameters.class).getPstsToRegulate();
        }
        return DEFAULT_PSTS_TO_REGULATE;
    }

    public static Optional<SearchTreeRaoPstRegulationParameters> load(PlatformConfig platformConfig) {
        return platformConfig.getOptionalModuleConfig(ST_PST_REGULATION_SECTION).map(
            config -> {
                SearchTreeRaoPstRegulationParameters pstRegulationParameters = new SearchTreeRaoPstRegulationParameters();
                pstRegulationParameters.setPstsToRegulate(config.getStringListProperty(PSTS_TO_REGULATE, DEFAULT_PSTS_TO_REGULATE));
                return pstRegulationParameters;
            });
    }
}
