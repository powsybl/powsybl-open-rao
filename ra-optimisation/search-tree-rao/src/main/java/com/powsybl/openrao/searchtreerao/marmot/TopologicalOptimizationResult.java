/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;

import java.util.Set;

/**
 * This class concatenates all data from running a Rao:
 * - input data (before Rao): RaoInput
 * - output data (after Rao): RaoResult
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public record TopologicalOptimizationResult(RaoInput raoInput, RaoResult topologicalOptimizationResult) {

    public void applyTopologicalActions() {
        Set<NetworkAction> networkActionsToBeApplied = topologicalOptimizationResult.getActivatedNetworkActionsDuringState(raoInput.getCrac().getPreventiveState());
        if (networkActionsToBeApplied.isEmpty()) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("[MARMOT] No topological actions applied for timestamp {}", raoInput.getCrac().getTimestamp().orElseThrow());
            return;
        }
        topologicalOptimizationResult.getActivatedNetworkActionsDuringState(raoInput.getCrac().getPreventiveState())
            .forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));
    }
}
