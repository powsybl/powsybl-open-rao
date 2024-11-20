/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MinMarginEvaluator implements CostEvaluator {
    private final CnecMarginManager cnecMarginManager;

    public MinMarginEvaluator(CnecMarginManager cnecMarginManager) {
        this.cnecMarginManager = cnecMarginManager;
    }

    @Override
    public String getName() {
        return "min-margin-evaluator";
    }

    @Override
    public double evaluate(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult, Set<String> contingenciesToExclude) {
        List<FlowCnec> costlyElements = cnecMarginManager.sortFlowCnecsByMargin(flowResult, contingenciesToExclude);
        FlowCnec limitingElement;
        if (costlyElements.isEmpty()) {
            limitingElement = null;
        } else {
            limitingElement = costlyElements.get(0);
        }
        if (limitingElement == null) {
            // In case there is no limiting element (may happen in perimeters where only MNECs exist),
            // return a finite value, so that the virtual cost is not hidden by the functional cost
            // This finite value should only be equal to the highest possible margin, i.e. the highest cnec threshold
            return -getHighestThresholdAmongFlowCnecs();
        }
        double margin = cnecMarginManager.marginEvaluator().getMargin(flowResult, limitingElement, cnecMarginManager.unit());
        if (margin >= Double.MAX_VALUE / 2) {
            // In case margin is infinite (may happen in perimeters where only unoptimized CNECs exist, none of which has seen its margin degraded),
            // return a finite value, like MNEC case above
            return -getHighestThresholdAmongFlowCnecs();
        }
        return -margin;
    }

    private double getHighestThresholdAmongFlowCnecs() {
        return cnecMarginManager.flowCnecs().stream().map(this::getHighestThreshold).max(Double::compareTo).orElse(0.0);
    }

    private double getHighestThreshold(FlowCnec flowCnec) {
        return Math.max(
            Math.max(
                flowCnec.getUpperBound(TwoSides.ONE, cnecMarginManager.unit()).orElse(0.0),
                flowCnec.getUpperBound(TwoSides.TWO, cnecMarginManager.unit()).orElse(0.0)),
            Math.max(
                -flowCnec.getLowerBound(TwoSides.ONE, cnecMarginManager.unit()).orElse(0.0),
                -flowCnec.getLowerBound(TwoSides.TWO, cnecMarginManager.unit()).orElse(0.0)));
    }
}
