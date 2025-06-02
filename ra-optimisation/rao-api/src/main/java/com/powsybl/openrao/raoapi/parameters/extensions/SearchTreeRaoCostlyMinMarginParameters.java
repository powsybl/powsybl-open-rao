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
public class SearchTreeRaoCostlyMinMarginParameters {
    static final double DEFAULT_SHIFTED_VIOLATION_PENALTY = 1000.0;
    static final double DEFAULT_SHIFTED_VIOLATION_THRESHOLD = 0.0;
    private double shiftedViolationPenalty = DEFAULT_SHIFTED_VIOLATION_PENALTY;
    private double shiftedViolationThreshold = DEFAULT_SHIFTED_VIOLATION_THRESHOLD;

    public double getShiftedViolationPenalty() {
        return shiftedViolationPenalty;
    }

    public double getShiftedViolationThreshold() {
        return shiftedViolationThreshold;
    }

    public void setShiftedViolationPenalty(double shiftedViolationPenalty) {
        this.shiftedViolationPenalty = shiftedViolationPenalty;
    }

    public void setShiftedViolationThreshold(double shiftedViolationThreshold) {
        this.shiftedViolationThreshold = shiftedViolationThreshold;
    }

    public static Optional<SearchTreeRaoCostlyMinMarginParameters> load(PlatformConfig platformConfig) {

        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig(ST_COSTLY_MIN_MARGIN_SECTION)
            .map(config -> {
                SearchTreeRaoCostlyMinMarginParameters parameters = new SearchTreeRaoCostlyMinMarginParameters();
                parameters.setShiftedViolationPenalty(config.getDoubleProperty(SHIFTED_VIOLATION_PENALTY, SearchTreeRaoCostlyMinMarginParameters.DEFAULT_SHIFTED_VIOLATION_PENALTY));
                parameters.setShiftedViolationThreshold(config.getDoubleProperty(SHIFTED_VIOLATION_THRESHOLD, SearchTreeRaoCostlyMinMarginParameters.DEFAULT_SHIFTED_VIOLATION_THRESHOLD));
                return parameters;
            });

    }
}
