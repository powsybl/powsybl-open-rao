/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters.extensions;

import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;

import static com.farao_community.farao.rao_api.RaoParametersCommons.MNEC_PARAMETERS;
/**
 * Extension : MNEC parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class MnecParametersExtension extends AbstractExtension<RaoParameters> {
    static final double DEFAULT_ACCEPTABLE_MARGIN_DECREASE = 50.0;
    static final double DEFAULT_VIOLATION_COST = 10.0;
    static final double DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT = 0.0;
    private double acceptableMarginDecrease = DEFAULT_ACCEPTABLE_MARGIN_DECREASE;
    // "A equivalent cost per A violation" or "MW per MW", depending on the objective function
    private double violationCost = DEFAULT_VIOLATION_COST;
    private double constraintAdjustmentCoefficient = DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT;

    @Override
    public String getName() {
        return MNEC_PARAMETERS;
    }

    public double getAcceptableMarginDecrease() {
        return acceptableMarginDecrease;
    }

    public void setAcceptableMarginDecrease(double acceptableMarginDecrease) {
        this.acceptableMarginDecrease = acceptableMarginDecrease;
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
}
