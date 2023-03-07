/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.parameters.extensions.MnecParametersExtension;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
// TODO : Replace with MnecParametersExtension
public class MnecParameters {
    private final double mnecAcceptableMarginDiminution;
    private final double mnecViolationCost;
    private final double mnecConstraintAdjustmentCoefficient;

    public MnecParameters(double mnecAcceptableMarginDiminution, double mnecViolationCost, double mnecConstraintAdjustmentCoefficient) {
        this.mnecAcceptableMarginDiminution = mnecAcceptableMarginDiminution;
        this.mnecViolationCost = mnecViolationCost;
        this.mnecConstraintAdjustmentCoefficient = mnecConstraintAdjustmentCoefficient;
    }

    public double getMnecAcceptableMarginDiminution() {
        return mnecAcceptableMarginDiminution;
    }

    public double getMnecViolationCost() {
        return mnecViolationCost;
    }

    public double getMnecConstraintAdjustmentCoefficient() {
        return mnecConstraintAdjustmentCoefficient;
    }

    public static MnecParameters buildFromRaoParameters(RaoParameters raoParameters) {
        MnecParametersExtension mnecParameters = raoParameters.getExtension(MnecParametersExtension.class);
        if (raoParameters.hasExtension(raoParameters, MnecParametersExtension.class)) {
            return new MnecParameters(mnecParameters.getAcceptableMarginDecrease(),
                mnecParameters.getViolationCost(),
                mnecParameters.getConstraintAdjustmentCoefficient());
        } else {
            return null;
        }
    }
}
