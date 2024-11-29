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

/**
 * Extension : loopFlow parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class LoopFlowParameters {
    static final PtdfApproximation DEFAULT_PTDF_APPROXIMATION = PtdfApproximation.FIXED_PTDF;
    static final double DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT = 0.0;
    static final double DEFAULT_VIOLATION_COST = 0.0;
    private PtdfApproximation ptdfApproximation = DEFAULT_PTDF_APPROXIMATION;

    private double constraintAdjustmentCoefficient = DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT;
    private double violationCost = DEFAULT_VIOLATION_COST;

    // Getters and setters

    public PtdfApproximation getPtdfApproximation() {
        return ptdfApproximation;
    }

    public void setPtdfApproximation(PtdfApproximation ptdfApproximation) {
        this.ptdfApproximation = ptdfApproximation;
    }

    public double getConstraintAdjustmentCoefficient() {
        return constraintAdjustmentCoefficient;
    }

    public void setConstraintAdjustmentCoefficient(double constraintAdjustmentCoefficient) {
        this.constraintAdjustmentCoefficient = constraintAdjustmentCoefficient;
    }

    public double getViolationCost() {
        return violationCost;
    }

    public void setViolationCost(double violationCost) {
        this.violationCost = violationCost;
    }

    public static Optional<LoopFlowParameters> load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        return platformConfig.getOptionalModuleConfig(ST_LOOP_FLOW_PARAMETERS_SECTION)
            .map(config -> {
                LoopFlowParameters parameters = new LoopFlowParameters();
                parameters.setPtdfApproximation(config.getEnumProperty(PTDF_APPROXIMATION, PtdfApproximation.class, LoopFlowParameters.DEFAULT_PTDF_APPROXIMATION));
                parameters.setConstraintAdjustmentCoefficient(config.getDoubleProperty(CONSTRAINT_ADJUSTMENT_COEFFICIENT, LoopFlowParameters.DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT));
                parameters.setViolationCost(config.getDoubleProperty(VIOLATION_COST, LoopFlowParameters.DEFAULT_VIOLATION_COST));
                return parameters;
            });
    }
}

