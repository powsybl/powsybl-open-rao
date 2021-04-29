/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.adapter;

import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_commons.result.BranchResultImpl;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;

import java.util.Collections;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class BranchResultAdapterWithFixedPtdfs implements BranchResultAdapter {
    protected final Map<BranchCnec, Double> ptdfZonalSums;

    public BranchResultAdapterWithFixedPtdfs(Map<BranchCnec, Double> ptdfZonalSums) {
        this.ptdfZonalSums = ptdfZonalSums;
    }

    @Override
    public BranchResult getResult(SystematicSensitivityResult systematicSensitivityResult) {
        return new BranchResultImpl(systematicSensitivityResult, Collections.emptyMap(), ptdfZonalSums);
    }
}
