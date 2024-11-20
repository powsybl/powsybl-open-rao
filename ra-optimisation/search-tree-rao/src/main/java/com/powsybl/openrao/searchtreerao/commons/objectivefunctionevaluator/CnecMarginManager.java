/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.MarginEvaluator;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record CnecMarginManager(Set<FlowCnec> flowCnecs, MarginEvaluator marginEvaluator, Unit unit) {

    public List<FlowCnec> sortFlowCnecsByMargin(FlowResult flowResult, Set<String> contingenciesToExclude) {
        Map<FlowCnec, Double> margins = new HashMap<>();

        flowCnecs.stream()
            .filter(cnec -> cnec.getState().getContingency().isEmpty() || !contingenciesToExclude.contains(cnec.getState().getContingency().get().getId()))
            .filter(Cnec::isOptimized)
            .forEach(flowCnec -> margins.put(flowCnec, marginEvaluator.getMargin(flowResult, flowCnec, unit)));

        return margins.keySet().stream()
            .filter(Cnec::isOptimized)
            .sorted(Comparator.comparing(margins::get))
            .toList();
    }
}
