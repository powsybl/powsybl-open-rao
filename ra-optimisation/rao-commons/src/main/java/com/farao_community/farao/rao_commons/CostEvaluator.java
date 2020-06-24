/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface CostEvaluator {

    /**
     * It evaluates the cost of RaoData containing a Network, a Crac and a SystematicSensitivityAnalysisResult on
     * the current RaoData variant.
     *
     * @param raoData: RaoData object to evaluate the cost on.
     * @return Double value of the RaoData cost.
     */
    double getCost(RaoData raoData);

    Unit getUnit();
}
