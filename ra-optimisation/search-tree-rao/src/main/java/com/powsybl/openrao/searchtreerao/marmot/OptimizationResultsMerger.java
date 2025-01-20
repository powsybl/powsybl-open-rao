/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OneStateOnlyRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OptimizationResultImpl;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getPreventivePerimeterCnecs;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class OptimizationResultsMerger {
    private OptimizationResultsMerger() {
    }

    public static TemporalData<RaoResult> mergeResults(TemporalData<RaoResult> results, TemporalData<LinearOptimizationResult> linearOptimizationResults, TemporalData<RaoInput> raoInput, TemporalData<PrePerimeterResult> prePerimeterResults) {

        Map<OffsetDateTime, RaoResult> mergedResults = new HashMap<>();
        linearOptimizationResults.getDataPerTimestamp().forEach((timestamp, linearOptimizationResult) -> {
            Crac crac = raoInput.getData(timestamp).get().getCrac();
            State preventiveState = crac.getPreventiveState();
            OptimizationResult postOptimizationResult = new OptimizationResultImpl(linearOptimizationResult, linearOptimizationResult, linearOptimizationResult, new NetworkActionsResultImpl(results.getData(timestamp).get().getActivatedNetworkActionsDuringState(preventiveState)), linearOptimizationResult);
            mergedResults.put(timestamp, new OneStateOnlyRaoResultImpl(preventiveState, prePerimeterResults.getData(timestamp).get(), postOptimizationResult, getPreventivePerimeterCnecs(crac)));
        });
        return new TemporalDataImpl<>(mergedResults);

    }
}
