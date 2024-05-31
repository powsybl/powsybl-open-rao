/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;

import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowResultImpl implements FlowResult {
    protected final SystematicSensitivityResult systematicSensitivityResult;
    private final FlowResult fixedCommercialFlows;
    private final FlowResult fixedPtdfs;

    public FlowResultImpl(SystematicSensitivityResult systematicSensitivityResult,
                          FlowResult fixedCommercialFlows,
                          FlowResult fixedPtdfs) {
        this.systematicSensitivityResult = systematicSensitivityResult;
        this.fixedCommercialFlows = fixedCommercialFlows;
        this.fixedPtdfs = fixedPtdfs;
    }

    public double getFlow(FlowCnec flowCnec, Side side, Unit unit, Instant instant) {
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getReferenceFlow(flowCnec, side, instant);
        } else if (unit == Unit.AMPERE) {
            double intensity = systematicSensitivityResult.getReferenceIntensity(flowCnec, side, instant);
            if (Double.isNaN(intensity) || Math.abs(intensity) <= 1e-6) {
                return systematicSensitivityResult.getReferenceFlow(flowCnec, side, instant) * RaoUtil.getFlowUnitMultiplier(flowCnec, side, Unit.MEGAWATT, Unit.AMPERE);
            } else {
                return intensity;
            }
        } else {
            throw new OpenRaoException("Unknown unit for flow.");
        }
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, Side side, Unit unit) {
        if (unit == Unit.MEGAWATT) {
            return fixedCommercialFlows.getCommercialFlow(flowCnec, side, unit);
        } else {
            throw new OpenRaoException("Commercial flows only in MW.");
        }
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, Side side) {
        return fixedPtdfs.getPtdfZonalSum(flowCnec, side);
    }

    @Override
    public Map<FlowCnec, Map<Side, Double>> getPtdfZonalSums() {
        return fixedPtdfs.getPtdfZonalSums();
    }

}
