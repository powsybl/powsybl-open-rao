/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.parameters;

import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class UnoptimizedCnecParametersTest {

    @Test
    void buildWithoutOptimizingOperatorsNotSharingCras() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCurativeCnecsForTsosWithoutCras(true);

        UnoptimizedCnecParameters ocp = UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), Set.of("BE"));

        assertNotNull(ocp);
        assertEquals(Set.of("BE"), ocp.getOperatorsNotToOptimize());
    }

    @Test
    void buildWhileOptimizingOperatorsNotSharingCras() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCurativeCnecsForTsosWithoutCras(false);

        UnoptimizedCnecParameters ocp = UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), Set.of("BE"));
        assertNull(ocp);
    }

}
