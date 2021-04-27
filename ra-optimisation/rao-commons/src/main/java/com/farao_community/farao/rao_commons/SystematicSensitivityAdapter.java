/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.SensitivityResult;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SystematicSensitivityAdapter implements BranchResult, SensitivityResult {
    private final SystematicSensitivityResult systematicSensitivityResult;
    private SystematicSensitivityStatus status;

    public SystematicSensitivityAdapter(SystematicSensitivityResult systematicSensitivityResult) {
        this.systematicSensitivityResult = systematicSensitivityResult;
    }

    public SystematicSensitivityStatus getStatus() {
        return status;
    }

    @Override
    public double getFlow(BranchCnec branchCnec, Unit unit) {
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getReferenceFlow(branchCnec);
        } else if (unit == Unit.AMPERE) {
            return systematicSensitivityResult.getReferenceIntensity(branchCnec);
        } else {
            throw new FaraoException("Unknown unit for flow.");
        }
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
    public double getSensitivityValue(BranchCnec branchCnec, RangeAction rangeAction, Unit unit) {
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getSensitivityOnFlow(rangeAction, branchCnec);
        } else if (unit == Unit.AMPERE) {
            return systematicSensitivityResult.getSensitivityOnIntensity(rangeAction, branchCnec);
        } else {
            throw new FaraoException("Unknown unit for flow.");
        }
    }
}
