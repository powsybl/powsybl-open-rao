/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.core;

import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;

import static java.lang.Math.max;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearProblemParameters extends AbstractExtension<RaoParameters> {

    public enum ObjectiveFunction {
        MAX_MIN_MARGIN_IN_MEGAWATT("MW"),
        MAX_MIN_MARGIN_IN_AMPERE("A");

        private String unit;

        ObjectiveFunction(String unit) {
            this.unit = unit;
        }

        public String getUnit() {
            return unit;
        }
    }

    public static final double DEFAULT_PST_PENALTY_COST = 0.01;
    public static final double DEFAULT_PST_SENSITIVITY_THRESHOLD = 0.0;
    public static final ObjectiveFunction DEFAULT_OBJECTIVE_FUNCTION = ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT;
    public static final double DEFAULT_LOOPFLOW_CONSTRAINT_ADJUSTMENT_COEFFICIENT = 0.0;

    private double pstPenaltyCost = DEFAULT_PST_PENALTY_COST;
    private double pstSensitivityThreshold = DEFAULT_PST_SENSITIVITY_THRESHOLD;
    private ObjectiveFunction objectiveFunction = DEFAULT_OBJECTIVE_FUNCTION;
    private double loopflowConstraintAdjustmentCoefficient = DEFAULT_LOOPFLOW_CONSTRAINT_ADJUSTMENT_COEFFICIENT;

    public LinearProblemParameters() {
        // Mandatory for deserialization
    }

    @Override
    public String getName() {
        return "LinearProblemParameters";
    }

    public double getPstPenaltyCost() {
        return pstPenaltyCost;
    }

    public LinearProblemParameters setPstPenaltyCost(double pstPenaltyCost) {
        this.pstPenaltyCost = max(0.0, pstPenaltyCost);
        return this;
    }

    public double getPstSensitivityThreshold() {
        return pstSensitivityThreshold;
    }

    public LinearProblemParameters setPstSensitivityThreshold(double pstSensitivityThreshold) {
        this.pstSensitivityThreshold = pstSensitivityThreshold;
        return this;
    }

    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    public LinearProblemParameters setObjectiveFunction(ObjectiveFunction objectiveFunction) {
        this.objectiveFunction = objectiveFunction;
        return this;
    }

    public double getLoopflowConstraintAdjustmentCoefficient() {
        return loopflowConstraintAdjustmentCoefficient;
    }

    public void setLoopflowConstraintAdjustmentCoefficient(double loopflowConstraintAdjustmentCoefficient) {
        this.loopflowConstraintAdjustmentCoefficient = loopflowConstraintAdjustmentCoefficient;
    }
}
