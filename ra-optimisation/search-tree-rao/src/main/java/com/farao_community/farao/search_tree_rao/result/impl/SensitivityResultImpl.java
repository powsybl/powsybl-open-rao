/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.Set;

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
    public ComputationStatus getSensitivityStatus() {
        switch (systematicSensitivityResult.getStatus()) {
            case SUCCESS:
                return ComputationStatus.DEFAULT;
            case FALLBACK:
                return ComputationStatus.FALLBACK;
            default:
            case FAILURE:
                return ComputationStatus.FAILURE;
        }
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        switch (systematicSensitivityResult.getStatus(state)) {
            case SUCCESS:
                return ComputationStatus.DEFAULT;
            case FALLBACK:
                return ComputationStatus.FALLBACK;
            default:
            case FAILURE:
                return ComputationStatus.FAILURE;
        }
    }

    public Set<String> getContingencies() {
        return systematicSensitivityResult.getContingencies();
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, Side side, RangeAction<?> rangeAction, Unit unit) {
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getSensitivityOnFlow(rangeAction, flowCnec, side);
        } else {
            throw new FaraoException(format("Unhandled unit for sensitivity value on range action : %s.", unit));
        }
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, Side side, SensitivityVariableSet linearGlsk, Unit unit) {
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getSensitivityOnFlow(linearGlsk, flowCnec, side);
        } else {
            throw new FaraoException(format("Unknown unit for sensitivity value on linear GLSK : %s.", unit));
        }
    }
}
