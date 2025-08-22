/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;

import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface NetworkActionCombinationFilter {

    /**
     * Remove network action combinations that cannot be applied for a given reason so the search tree bloomer does not
     * create pointless leaves.
     *
     * @param naCombinations set of potentially applicable network action combinations
     * @param optimizationResult optimization result from the previous leaf
     * @return filtered set of applicable network action combinations from which the non-applicable ones where removed
     */
    Set<NetworkActionCombination> filter(Set<NetworkActionCombination> naCombinations, OptimizationResult optimizationResult);
}
