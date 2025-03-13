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
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OneStateOnlyRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RemedialActionActivationResultImpl;

import java.util.Set;

import static com.powsybl.openrao.searchtreerao.marmot.MarmotUtils.getPreventivePerimeterCnecs;

/** This class concatenates all data around one individual timestamp from running Marmot:
 * - input data (before Marmot): RaoInput
 * - output data (after Marmot):
 *      -- RaoResult: output from initial Rao run, containing activated topological actions
 *      -- PrePerimeterResult: output from initial sensitivity computation, after having applied topological actions but before inter-temporal MIP
 *      -- LinearOptimizationResult: output from inter-temporal MIP, containing activated range actions
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public record PostOptimizationResult(RaoInput raoInput, PrePerimeterResult initialResult, FlowResult finalFlowResult, SensitivityResult finalSensitivityResult, RangeActionActivationResult finalRangeActionActivationResult, RaoResult topologicalOptimizationResult, RaoParameters raoParameters) {
    public RaoResult merge() {
        Crac crac = raoInput.getCrac();
        State preventiveState = crac.getPreventiveState();
        // TODO: should it be flow before and after topos?
        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(MarmotUtils.getPreventivePerimeterCnecs(crac), Set.of(), initialResult, initialResult, Set.of(), raoParameters, Set.of(preventiveState));
        NetworkActionsResult networkActionsResult = new NetworkActionsResultImpl(topologicalOptimizationResult.getActivatedNetworkActionsDuringState(preventiveState));
        RemedialActionActivationResult remedialActionActivationResult = new RemedialActionActivationResultImpl(finalRangeActionActivationResult, networkActionsResult);
        ObjectiveFunctionResult objectiveFunctionResult = objectiveFunction.evaluate(finalFlowResult, remedialActionActivationResult);
        OptimizationResult mergedOptimizationResult = new OptimizationResultImpl(objectiveFunctionResult, finalFlowResult, finalSensitivityResult, networkActionsResult, finalRangeActionActivationResult);
        return new OneStateOnlyRaoResultImpl(preventiveState, initialResult, mergedOptimizationResult, getPreventivePerimeterCnecs(crac));
    }
}
