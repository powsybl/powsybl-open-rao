/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.MarginEvaluator;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class FlowCnecSorting {

    private FlowCnecSorting() {
    }

    public static List<FlowCnec> sortByMargin(Set<FlowCnec> flowCnecs, Unit unit, MarginEvaluator marginEvaluator, FlowResult flowResult) {
        return flowCnecs.stream()
            .filter(Cnec::isOptimized)
            .sorted(Comparator.comparingDouble(flowCnec -> marginEvaluator.getMargin(flowResult, flowCnec, unit)))
            .toList();
    }

    public static List<FlowCnec> sortByNegativeMargin(Set<FlowCnec> flowCnecs, Unit unit, MarginEvaluator marginEvaluator, FlowResult flowResult) {
        return flowCnecs.stream()
            .filter(Cnec::isOptimized)
            .filter(flowCnec -> marginEvaluator.getMargin(flowResult, flowCnec, unit) < 0)
            .sorted(Comparator.comparingDouble(flowCnec -> marginEvaluator.getMargin(flowResult, flowCnec, unit)))
            .toList();
    }
}
