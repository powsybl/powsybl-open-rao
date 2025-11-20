/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.commons.FlowCnecSorting;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.CostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.SumMaxPerTimestampCostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.MarginEvaluator;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MinMarginEvaluator implements CostEvaluator {
    protected final Set<FlowCnec> flowCnecs;
    protected final Unit unit;
    protected final MarginEvaluator marginEvaluator;

    public MinMarginEvaluator(Set<FlowCnec> flowCnecs, Unit unit, MarginEvaluator marginEvaluator) {
        this.flowCnecs = flowCnecs;
        this.unit = unit;
        this.marginEvaluator = marginEvaluator;
    }

    @Override
    public String getName() {
        return "min-margin-evaluator";
    }

    @Override
    public CostEvaluatorResult evaluate(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult) {
        Map<FlowCnec, Double> marginPerCnec = getMarginPerCnec(flowCnecs, flowResult, unit);
        return new SumMaxPerTimestampCostEvaluatorResult(marginPerCnec, FlowCnecSorting.sortByMargin(flowCnecs, unit, marginEvaluator, flowResult), unit);
    }

    protected Map<FlowCnec, Double> getMarginPerCnec(Set<FlowCnec> flowCnecs, FlowResult flowResult, Unit unit) {
        Map<FlowCnec, Double> marginPerCnec = new HashMap<>();
        flowCnecs.forEach(cnec -> marginPerCnec.put(cnec, marginEvaluator.getMargin(flowResult, cnec, unit)));
        return marginPerCnec;
    }
}
