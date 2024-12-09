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

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.CONSTRAINT_ADJUSTMENT_COEFFICIENT;

/**
 * Extension : MNEC parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class MnecParameters {
    static final double DEFAULT_VIOLATION_COST = 10.0;
    static final double DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT = 0.0;
    // "A equivalent cost per A violation" or "MW per MW", depending on the objective function
    private double violationCost = DEFAULT_VIOLATION_COST;
    private double constraintAdjustmentCoefficient = DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT;

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

    public static Optional<MnecParameters> load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig(ST_MNEC_PARAMETERS_SECTION)
            .map(config -> {
                MnecParameters parameters = new MnecParameters();
                parameters.setViolationCost(config.getDoubleProperty(VIOLATION_COST, MnecParameters.DEFAULT_VIOLATION_COST));
                parameters.setConstraintAdjustmentCoefficient(config.getDoubleProperty(CONSTRAINT_ADJUSTMENT_COEFFICIENT, MnecParameters.DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT));
                return parameters;
            });
    }
}
