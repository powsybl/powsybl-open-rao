/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.parameters;

import com.powsybl.openrao.raoapi.parameters.NotOptimizedCnecsParameters;

import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class UnoptimizedCnecParameters {
    private final Set<String> operatorNotToOptimize;

    public UnoptimizedCnecParameters(Set<String> operatorNotToOptimize) {
        this.operatorNotToOptimize = operatorNotToOptimize;
    }

    public Set<String> getOperatorsNotToOptimize() {
        return operatorNotToOptimize;
    }

    public static UnoptimizedCnecParameters build(NotOptimizedCnecsParameters parameters, Set<String> operatorsNotSharingCras) {
        return parameters.getDoNotOptimizeCurativeCnecsForTsosWithoutCras() ? new UnoptimizedCnecParameters(operatorsNotSharingCras) : null;
    }
}
