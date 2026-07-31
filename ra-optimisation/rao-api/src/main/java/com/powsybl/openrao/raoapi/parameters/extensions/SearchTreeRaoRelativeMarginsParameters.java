/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.openrao.commons.OpenRaoException;

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
    @JsonProperty(PTDF_SUM_LOWER_BOUND)
    private double ptdfSumLowerBound = DEFAULT_PTDF_SUM_LOWER_BOUND;

    @JsonProperty(PTDF_APPROXIMATION)
    private PtdfApproximation ptdfApproximation = DEFAULT_PTDF_APPROXIMATION;

    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void handleUnknownProperty(String name, Object value) {
        throw new OpenRaoException(String.format("Cannot deserialize search tree relative margin parameters: unexpected field in search tree relative-margins-parameters (%s)", name));
    }

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
