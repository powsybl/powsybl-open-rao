/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.PTDF_APPROXIMATION;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.PTDF_SUM_LOWER_BOUND;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.ST_RELATIVE_MARGINS_SECTION;

/**
 * Extension : relative margin parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class SearchTreeRaoRelativeMarginsParameters {

    static final double DEFAULT_PTDF_SUM_LOWER_BOUND = 0.01;
    static final PtdfApproximation DEFAULT_PTDF_APPROXIMATION = PtdfApproximation.FIXED_PTDF;
    // prevents relative margins from diverging to +infinity
    private double ptdfSumLowerBound = DEFAULT_PTDF_SUM_LOWER_BOUND;
    private PtdfApproximation ptdfApproximation = DEFAULT_PTDF_APPROXIMATION;

    public double getPtdfSumLowerBound() {
        return ptdfSumLowerBound;
    }

    public PtdfApproximation getPtdfApproximation() {
        return ptdfApproximation;
    }

    public void setPtdfApproximation(PtdfApproximation ptdfApproximation) {
        this.ptdfApproximation = ptdfApproximation;
    }

    public void setPtdfSumLowerBound(double ptdfSumLowerBound) {
        this.ptdfSumLowerBound = ptdfSumLowerBound;
    }

    public static Optional<SearchTreeRaoRelativeMarginsParameters> load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig(ST_RELATIVE_MARGINS_SECTION)
            .map(config -> {
                SearchTreeRaoRelativeMarginsParameters parameters = new SearchTreeRaoRelativeMarginsParameters();
                parameters.setPtdfApproximation(config.getEnumProperty(PTDF_APPROXIMATION, PtdfApproximation.class, SearchTreeRaoRelativeMarginsParameters.DEFAULT_PTDF_APPROXIMATION));
                parameters.setPtdfSumLowerBound(config.getDoubleProperty(PTDF_SUM_LOWER_BOUND, SearchTreeRaoRelativeMarginsParameters.DEFAULT_PTDF_SUM_LOWER_BOUND));
                return parameters;
            });
    }
}
