/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.util.Map;

/**
 * It enables to evaluate the absolute margin of a FlowCnec
 * For cnecs parameterized as in series with a pst, margin is considered infinite
 * if the PST has enough setpoints left to absorb the cnec's margin deficit
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class MarginEvaluatorWithPstLimitationUnoptimizedCnecs implements MarginEvaluator {
    private final MarginEvaluator marginEvaluator;
    private final Map<FlowCnec, RangeAction<?>> flowCnecRangeActionMap;
    private final RangeActionSetpointResult prePerimeterRangeActionSetpointResult;

    public MarginEvaluatorWithPstLimitationUnoptimizedCnecs(MarginEvaluator marginEvaluator,
                                                            Map<FlowCnec, RangeAction<?>> flowCnecRangeActionMap,
                                                            RangeActionSetpointResult rangeActionActivationResult) {
        this.marginEvaluator = marginEvaluator;
        this.flowCnecRangeActionMap = flowCnecRangeActionMap;
        this.prePerimeterRangeActionSetpointResult = rangeActionActivationResult;
    }

    @Override
    public double getMargin(FlowResult flowResult, FlowCnec flowCnec, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, Unit unit) {
        return flowCnec.getMonitoredSides().stream()
                .map(side -> getMargin(flowResult, flowCnec, side, rangeActionActivationResult, sensitivityResult, unit))
                .min(Double::compareTo).orElseThrow();
    }

    @Override
    public double getMargin(FlowResult flowResult, FlowCnec flowCnec, Side side, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, Unit unit) {
        if (RaoUtil.cnecShouldBeOptimized(flowCnecRangeActionMap, flowResult, flowCnec, side, rangeActionActivationResult, prePerimeterRangeActionSetpointResult, sensitivityResult, unit)) {
            return marginEvaluator.getMargin(flowResult, flowCnec, side, rangeActionActivationResult, sensitivityResult, unit);
        } else {
            return Double.MAX_VALUE;
        }
    }
}
