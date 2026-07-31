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

import static com.powsybl.openrao.raoapi.RaoParametersCommons.CONSTRAINT_ADJUSTMENT_COEFFICIENT;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.ST_MNEC_PARAMETERS_SECTION;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.VIOLATION_COST;

/**
 * Extension : MNEC parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class SearchTreeRaoMnecParameters {
    static final double DEFAULT_VIOLATION_COST = 10.0;
    static final double DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT = 0.0;
    // "A equivalent cost per A violation" or "MW per MW", depending on the objective function
    @JsonProperty(VIOLATION_COST)
    private double violationCost = DEFAULT_VIOLATION_COST;

    @JsonProperty(CONSTRAINT_ADJUSTMENT_COEFFICIENT)
    private double constraintAdjustmentCoefficient = DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT;

    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void handleUnknownProperty(String name, Object value) {
        throw new OpenRaoException(String.format("Cannot deserialize search tree MNEC parameters: unexpected field in search tree mnec-parameters (%s)", name));
    }

    public double getViolationCost() {
        return violationCost;
    }

    public void setViolationCost(double violationCost) {
        this.violationCost = violationCost;
    }

    public double getConstraintAdjustmentCoefficient() {
        return constraintAdjustmentCoefficient;
    }

    public void setConstraintAdjustmentCoefficient(double constraintAdjustmentCoefficient) {
        this.constraintAdjustmentCoefficient = constraintAdjustmentCoefficient;
    }

    public static Optional<SearchTreeRaoMnecParameters> load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig(ST_MNEC_PARAMETERS_SECTION)
            .map(config -> {
                SearchTreeRaoMnecParameters parameters = new SearchTreeRaoMnecParameters();
                parameters.setViolationCost(config.getDoubleProperty(VIOLATION_COST, SearchTreeRaoMnecParameters.DEFAULT_VIOLATION_COST));
                parameters.setConstraintAdjustmentCoefficient(config.getDoubleProperty(CONSTRAINT_ADJUSTMENT_COEFFICIENT, SearchTreeRaoMnecParameters.DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT));
                return parameters;
            });
    }
}
