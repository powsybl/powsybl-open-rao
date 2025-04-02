/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunction;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.MarginEvaluator;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.CostEvaluator;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.MinMarginViolationEvaluator;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.SensitivityFailureOvercostEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class InitialSensitivityAnalysisObjectiveFunctionCreator extends AbstractObjectiveFunctionCreator {

    protected InitialSensitivityAnalysisObjectiveFunctionCreator(Set<FlowCnec> flowCnecs, Set<State> optimizedStates, RaoParameters raoParameters) {
        super(flowCnecs, optimizedStates, raoParameters);
    }

    @Override
    protected List<CostEvaluator> getVirtualCostEvaluators(MarginEvaluator marginEvaluator) {
        List<CostEvaluator> virtualCostEvaluators = new ArrayList<>();

        if (raoParameters.getObjectiveFunctionParameters().getType().costOptimization()) {
            virtualCostEvaluators.add(new MinMarginViolationEvaluator(flowCnecs, unit, marginEvaluator, raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getMinMarginsParameters().orElseThrow().getOverloadPenalty()));
        }

        // sensitivity failure over-cost should be computed on initial sensitivity result too
        // (this allows the RAO to prefer RAs that can remove sensitivity failures)
        if (LoadFlowAndSensitivityParameters.getSensitivityFailureOvercost(raoParameters) > 0) {
            virtualCostEvaluators.add(new SensitivityFailureOvercostEvaluator(flowCnecs, LoadFlowAndSensitivityParameters.getSensitivityFailureOvercost(raoParameters)));
        }

        return virtualCostEvaluators;
    }
}
