/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.INTER_TEMPORAL_PARAMETERS;

/**
 * Extension: parameters for inter-temporal RAO computations
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class InterTemporalParametersExtension extends AbstractExtension<RaoParameters> {
    static final int DEFAULT_SENSITIVITY_COMPUTATIONS_IN_PARALLEL = 1;
    private int sensitivityComputationsInParallel = DEFAULT_SENSITIVITY_COMPUTATIONS_IN_PARALLEL;

    @Override
    public String getName() {
        return INTER_TEMPORAL_PARAMETERS;
    }

    public int getSensitivityComputationsInParallel() {
        return sensitivityComputationsInParallel;
    }

    public void setSensitivityComputationsInParallel(int sensitivityComputationsInParallel) {
        this.sensitivityComputationsInParallel = sensitivityComputationsInParallel;
    }
}
