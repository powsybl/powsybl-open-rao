/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.results;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface BranchResult {

    double getFlow(BranchCnec branchCnec, Unit unit);

    default double getMargin(BranchCnec branchCnec, Unit unit) {
        return branchCnec.computeMargin(getFlow(branchCnec, unit), Side.LEFT, unit);
    }

    // A voir si on veut récupérer la marge relative quoi qu'il arrive ou la marge au sens de la fct obj donc relatif quand c'est positif et absolu quand c'est negatif
    default double getRelativeMargin(BranchCnec branchCnec, Unit unit) {
        if (getPtdfZonalSum(branchCnec) == 0) {
            throw new FaraoException("Relative margin cannot be computed because PTDF sum is null");
        }
        return branchCnec.computeMargin(getFlow(branchCnec, unit), Side.LEFT, unit) /
                getPtdfZonalSum(branchCnec);
    }

    double getLoopFlow(BranchCnec branchCnec, Unit unit);

    double getCommercialFlow(BranchCnec branchCnec, Unit unit);

    double getPtdfZonalSum(BranchCnec branchCnec);
}
