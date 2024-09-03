/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
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
        return switch (systematicSensitivityResult.getStatus()) {
            case SUCCESS -> ComputationStatus.DEFAULT;
            case PARTIAL_FAILURE -> ComputationStatus.PARTIAL_FAILURE;
            default -> ComputationStatus.FAILURE;
        };
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        return switch (systematicSensitivityResult.getStatus(state)) {
            case SUCCESS -> ComputationStatus.DEFAULT;
            default -> ComputationStatus.FAILURE;
        };
    }

    public Set<String> getContingencies() {
        return systematicSensitivityResult.getContingencies();
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, RangeAction<?> rangeAction, Unit unit) {
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getSensitivityOnFlow(rangeAction, flowCnec, side);
        } else {
            throw new OpenRaoException(format("Unhandled unit for sensitivity value on range action : %s.", unit));
        }
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, SensitivityVariableSet linearGlsk, Unit unit) {
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getSensitivityOnFlow(linearGlsk, flowCnec, side);
        } else {
            throw new OpenRaoException(format("Unknown unit for sensitivity value on linear GLSK : %s.", unit));
        }
    }
}
