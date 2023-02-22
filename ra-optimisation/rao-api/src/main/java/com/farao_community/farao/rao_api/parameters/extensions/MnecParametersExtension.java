/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters.extensions;

import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtension;

import java.util.Objects;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;
/**
 * Extension : MNEC parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class MnecParametersExtension extends AbstractExtension<RaoParameters> {
    private double acceptableMarginDecrease;
    // "A equivalent cost per A violation" or "MW per MW", depending on the objective function
    private double violationCost;
    private double constraintAdjustmentCoefficient;

    private static final double DEFAULT_ACCEPTABLE_MARGIN_DIMINUTION = 50.0;
    private static final double DEFAULT_VIOLATION_COST = 10.0;
    private static final double DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT = 0.0;

    public MnecParametersExtension(double acceptableMarginDecrease, double violationCost, double constraintAdjustmentCoefficient) {
        this.acceptableMarginDecrease = acceptableMarginDecrease;
        this.violationCost = violationCost;
        this.constraintAdjustmentCoefficient = constraintAdjustmentCoefficient;
    }

    public static MnecParametersExtension loadDefault() {
        return new MnecParametersExtension(DEFAULT_ACCEPTABLE_MARGIN_DIMINUTION, DEFAULT_VIOLATION_COST, DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT);
    }

    @Override
    public String getName() {
        return MNEC_PARAMETERS_EXTENSION_NAME;
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

    @AutoService(RaoParameters.ConfigLoader.class)
    public class MnecParametersConfigLoader implements RaoParameters.ConfigLoader<MnecParametersExtension> {

        @Override
        public MnecParametersExtension load(PlatformConfig platformConfig) {
            Objects.requireNonNull(platformConfig);
            MnecParametersExtension parameters = loadDefault();
            platformConfig.getOptionalModuleConfig(MNEC_PARAMETERS)
                    .ifPresent(config -> {
                        parameters.setAcceptableMarginDecrease(config.getDoubleProperty(ACCEPTABLE_MARGIN_DECREASE, DEFAULT_ACCEPTABLE_MARGIN_DIMINUTION));
                        parameters.setViolationCost(config.getDoubleProperty(VIOLATION_COST, DEFAULT_VIOLATION_COST));
                        parameters.setConstraintAdjustmentCoefficient(config.getDoubleProperty(CONSTRAINT_ADJUSTMENT_COEFFICIENT, DEFAULT_CONSTRAINT_ADJUSTMENT_COEFFICIENT));
                    });
            return parameters;
        }

        @Override
        public String getExtensionName() {
            return MNEC_PARAMETERS_EXTENSION_NAME;
        }

        @Override
        public String getCategoryName() {
            return "rao-parameters";
        }

        @Override
        public Class<? super MnecParametersExtension> getExtensionClass() {
            return MnecParametersExtension.class;
        }
    }
}
