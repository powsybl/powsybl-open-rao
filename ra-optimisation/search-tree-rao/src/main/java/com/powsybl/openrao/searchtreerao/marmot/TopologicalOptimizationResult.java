/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record TopologicalOptimizationResult(RaoInput raoInput, RaoResult topologicalOptimizationResult) {
    private static final String VARIANT_NAME_SUFFIX = "_with_topological_actions";

    public void applyTopologicalActions() {
        String currentNetworkVariantId = raoInput.getNetwork().getVariantManager().getWorkingVariantId();
        String newNetworkVariantId = currentNetworkVariantId + VARIANT_NAME_SUFFIX;
        raoInput.getNetwork().getVariantManager().cloneVariant(currentNetworkVariantId, newNetworkVariantId);
        raoInput.getNetwork().getVariantManager().setWorkingVariant(newNetworkVariantId);
        topologicalOptimizationResult.getActivatedNetworkActionsDuringState(raoInput.getCrac().getPreventiveState())
            .forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));
    }
}
