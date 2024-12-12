/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunction;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.BasicMarginEvaluator;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.BasicRelativeMarginEvaluator;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.MarginEvaluator;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.CostEvaluator;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.MinMarginEvaluator;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.RemedialActionCostEvaluator;

import java.util.List;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public abstract class AbstractObjectiveFunctionCreator {
    protected final Set<FlowCnec> flowCnecs;
    protected final Set<State> optimizedStates;
    protected final RaoParameters raoParameters;
    protected final Unit unit;

    protected AbstractObjectiveFunctionCreator(Set<FlowCnec> flowCnecs, Set<State> optimizedStates, RaoParameters raoParameters) {
        this.flowCnecs = flowCnecs;
        this.optimizedStates = optimizedStates;
        this.raoParameters = raoParameters;
        this.unit = raoParameters.getObjectiveFunctionParameters().getType().getUnit();
    }

    protected MarginEvaluator getMarginEvaluator() {
        return raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins() ? new BasicRelativeMarginEvaluator() : new BasicMarginEvaluator();
    }

    protected CostEvaluator getFunctionalCostEvaluator(MarginEvaluator marginEvaluator) {
        return raoParameters.getObjectiveFunctionParameters().getType().costOptimization() ? new RemedialActionCostEvaluator(optimizedStates) : new MinMarginEvaluator(flowCnecs, unit, marginEvaluator);
    }

    protected abstract List<CostEvaluator> getVirtualCostEvaluators(MarginEvaluator marginEvaluator);

    public ObjectiveFunction create() {
        MarginEvaluator marginEvaluator = getMarginEvaluator();
        CostEvaluator functionalCostEvaluator = getFunctionalCostEvaluator(marginEvaluator);
        List<CostEvaluator> virtualCostEvaluators = getVirtualCostEvaluators(marginEvaluator);
        return new ObjectiveFunction(functionalCostEvaluator, virtualCostEvaluators, flowCnecs, unit, marginEvaluator);
    }
}
