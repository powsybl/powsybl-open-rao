/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.result;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.SensitivityResult;
import com.farao_community.farao.rao_api.results.SensitivityStatus;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SensitivityResultImpl implements SensitivityResult {
    private final SystematicSensitivityResult systematicSensitivityResult;

    public SensitivityResultImpl(SystematicSensitivityResult systematicSensitivityResult) {
        this.systematicSensitivityResult = systematicSensitivityResult;
    }

    @Override
    public SensitivityStatus getSensitivityStatus() {
        switch (systematicSensitivityResult.getStatus()) {
            case SUCCESS:
                return SensitivityStatus.DEFAULT;
            case FALLBACK:
                return SensitivityStatus.FALLBACK;
            default:
            case FAILURE:
                return SensitivityStatus.FAILURE;
        }
    }

    @Override
    public double getSensitivityValue(BranchCnec branchCnec, RangeAction rangeAction, Unit unit) {
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getSensitivityOnFlow(rangeAction, branchCnec);
        } else if (unit == Unit.AMPERE) {
            return systematicSensitivityResult.getSensitivityOnIntensity(rangeAction, branchCnec);
        } else {
            throw new FaraoException(format("Unknown unit for sensitivity value on range action : %s.", unit));
        }
    }

    @Override
    public double getSensitivityValue(BranchCnec branchCnec, LinearGlsk linearGlsk, Unit unit) {
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getSensitivityOnFlow(linearGlsk, branchCnec);
        } else {
            throw new FaraoException(format("Unknown unit for sensitivity value on linear GLSK : %s.", unit));
        }
    }
}
