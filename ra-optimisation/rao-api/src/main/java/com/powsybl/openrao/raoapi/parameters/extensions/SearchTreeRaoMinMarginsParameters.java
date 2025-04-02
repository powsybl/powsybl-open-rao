/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class SearchTreeRaoMinMarginsParameters {
    static final double DEFAULT_OVERLOAD_PENALTY = 1000.0;
    private double overloadPenalty = DEFAULT_OVERLOAD_PENALTY;

    public double getOverloadPenalty() {
        return overloadPenalty;
    }

    public void setOverloadPenalty(double overloadPenalty) {
        this.overloadPenalty = overloadPenalty;
    }

    public static Optional<SearchTreeRaoMinMarginsParameters> load(PlatformConfig platformConfig) {

        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig(ST_MIN_MARGINS_SECTION)
            .map(config -> {
                SearchTreeRaoMinMarginsParameters parameters = new SearchTreeRaoMinMarginsParameters();
                parameters.setOverloadPenalty(config.getDoubleProperty(OVERLOAD_PENALTY, SearchTreeRaoMinMarginsParameters.DEFAULT_OVERLOAD_PENALTY));
                return parameters;
            });

    }
}
