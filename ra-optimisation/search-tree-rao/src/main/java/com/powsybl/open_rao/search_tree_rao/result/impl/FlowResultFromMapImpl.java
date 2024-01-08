/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.search_tree_rao.result.impl;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.search_tree_rao.commons.RaoUtil;
import com.powsybl.open_rao.search_tree_rao.result.api.FlowResult;
import com.powsybl.open_rao.sensitivity_analysis.SystematicSensitivityResult;

import java.util.Map;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowResultFromMapImpl implements FlowResult {
    protected final SystematicSensitivityResult systematicSensitivityResult;
    private final Map<FlowCnec, Map<Side, Double>> commercialFlows;
    private final Map<FlowCnec, Map<Side, Double>> ptdfZonalSums;

    public FlowResultFromMapImpl(SystematicSensitivityResult systematicSensitivityResult,
                                 Map<FlowCnec, Map<Side, Double>> commercialFlows,
                                 Map<FlowCnec, Map<Side, Double>> ptdfZonalSums) {
        this.systematicSensitivityResult = systematicSensitivityResult;
        this.commercialFlows = commercialFlows;
        this.ptdfZonalSums = ptdfZonalSums;
    }

    @Override
    public double getFlow(FlowCnec flowCnec, Side side, Unit unit) {
        if (unit == Unit.MEGAWATT) {
            return systematicSensitivityResult.getReferenceFlow(flowCnec, side);
        } else if (unit == Unit.AMPERE) {
            double intensity = systematicSensitivityResult.getReferenceIntensity(flowCnec, side);
            if (Double.isNaN(intensity) || Math.abs(intensity) <= 1e-6) {
                return systematicSensitivityResult.getReferenceFlow(flowCnec, side) * RaoUtil.getFlowUnitMultiplier(flowCnec, side, Unit.MEGAWATT, Unit.AMPERE);
            } else {
                return intensity;
            }
        } else {
            throw new OpenRaoException("Unknown unit for flow.");
        }
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, Side side, Unit unit) {
        if (unit != Unit.MEGAWATT) {
            throw new OpenRaoException("Commercial flows only in MW.");
        }
        if (!commercialFlows.containsKey(flowCnec) || !commercialFlows.get(flowCnec).containsKey(side)) {
            throw new OpenRaoException(format("No commercial flow on the CNEC %s on side %s", flowCnec.getName(), side));
        }
        return commercialFlows.get(flowCnec).get(side);
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, Side side) {
        if (!ptdfZonalSums.containsKey(flowCnec) || !ptdfZonalSums.get(flowCnec).containsKey(side)) {
            throw new OpenRaoException(format("No PTDF computed on the CNEC %s on side %s", flowCnec.getName(), side));
        }
        return ptdfZonalSums.get(flowCnec).get(side);
    }

    @Override
    public Map<FlowCnec, Map<Side, Double>> getPtdfZonalSums() {
        return ptdfZonalSums;
    }
}
