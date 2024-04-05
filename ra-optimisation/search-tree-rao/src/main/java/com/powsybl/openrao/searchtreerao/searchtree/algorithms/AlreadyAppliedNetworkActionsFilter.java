/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AlreadyAppliedNetworkActionsFilter implements NetworkActionCombinationFilter{
    public Map<NetworkActionCombination, Boolean> filter(Map<NetworkActionCombination, Boolean> naCombinations, Leaf fromLeaf) {
        return naCombinations.keySet().stream()
            .filter(naCombination -> naCombination.getNetworkActionSet().stream().noneMatch(na -> fromLeaf.getActivatedNetworkActions().contains(na)))
            .collect(Collectors.toMap(naCombination -> naCombination, naCombinations::get));
    }
}
