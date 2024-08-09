/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SensitivityFailureOvercostEvaluator implements CostEvaluator {
    private final double sensitivityFailureOvercost;

    public SensitivityFailureOvercostEvaluator(double sensitivityFailureOvercost) {
        this.sensitivityFailureOvercost = sensitivityFailureOvercost;
    }

    @Override
    public String getName() {
        return "sensitivity-failure-cost";
    }

    @Override
    public Pair<Double, List<FlowCnec>> computeCostAndLimitingElements(FlowResult flowResult, ComputationStatus computationStatus, Set<String> contingenciesToExclude) {
        if (computationStatus == ComputationStatus.FAILURE || computationStatus == ComputationStatus.PARTIAL_FAILURE) {
            TECHNICAL_LOGS.info(String.format("Sensitivity failure : assigning virtual overcost of %s", sensitivityFailureOvercost));
            return Pair.of(sensitivityFailureOvercost, new ArrayList<>());
        }
        return Pair.of(0., new ArrayList<>());
    }

    @Override
    public Unit getUnit() {
        return Unit.MEGAWATT;
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return Collections.emptySet();
    }
}
