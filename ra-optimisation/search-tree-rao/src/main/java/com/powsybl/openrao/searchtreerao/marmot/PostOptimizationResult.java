/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OneStateOnlyRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OptimizationResultImpl;

import java.util.Set;

import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getPreventivePerimeterCnecs;

/** This class concatenates all data around one individual timestamp from running Marmot:
 * - input data (before Marmot): RaoInput
 * - output data (after Marmot):
 *      -- RaoResult: output from initial Rao run, containing activated topological actions
 *      -- PreperimeterResult: output from initial sensitivity computation, after having applied topological actions but before inter-temporal MIP
 *      -- LinearOptimizationResult: output from inter-temporal MIP, containing activated range actions
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public record PostOptimizationResult(RaoInput raoInput, PrePerimeterResult initialResult, LinearOptimizationResult linearOptimizationResult, RaoResult topologicalOptimizationResult) {
    public RaoResult merge() {
        Crac crac = raoInput.getCrac();
        State preventiveState = crac.getPreventiveState();
        OptimizationResult mergedOptimizationResult = new OptimizationResultImpl(linearOptimizationResult, linearOptimizationResult, linearOptimizationResult, new NetworkActionsResultImpl(topologicalOptimizationResult.getActivatedNetworkActionsDuringState(preventiveState)), linearOptimizationResult, Set.of(preventiveState));
        return new OneStateOnlyRaoResultImpl(preventiveState, initialResult, mergedOptimizationResult, getPreventivePerimeterCnecs(crac));
    }
}
