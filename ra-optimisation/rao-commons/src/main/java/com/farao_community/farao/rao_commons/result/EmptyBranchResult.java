/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.result;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.BranchResult;

import java.util.Collections;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class EmptyBranchResult implements BranchResult {
    @Override
    public double getFlow(BranchCnec branchCnec, Unit unit) {
        return Double.NaN;
    }

    @Override
    public double getCommercialFlow(BranchCnec branchCnec, Unit unit) {
        return Double.NaN;
    }

    @Override
    public double getPtdfZonalSum(BranchCnec branchCnec) {
        return Double.NaN;
    }

    @Override
    public Map<BranchCnec, Double> getPtdfZonalSums() {
        return Collections.emptyMap();
    }
}
