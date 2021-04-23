/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.results;

import com.farao_community.farao.data.crac_api.cnec.BranchCnec;

import java.util.List;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface ObjectiveFunctionResult {

    default double getCost() {
        return getFunctionalCost() + getVirtualCost();
    }

    double getFunctionalCost();

    // A voir dans l'implem si on stocke ou non
    List<BranchCnec> getMostLimitingElements(int number);

    double getVirtualCost();

    Set<String> getVirtualCostNames();

    double getVirtualCost(String virtualCostName);

    // Ça marche pas pour le sensitivity fallback
    List<BranchCnec> getCostlyElements(String virtualCostName);
}
