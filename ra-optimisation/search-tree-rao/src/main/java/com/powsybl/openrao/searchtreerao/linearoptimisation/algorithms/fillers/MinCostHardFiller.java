/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.util.*;

import static com.powsybl.openrao.commons.Unit.MEGAWATT;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class MinCostHardFiller implements ProblemFiller {
    protected final Set<FlowCnec> optimizedCnecs;
    private final Unit unit;
    private final Map<State, Set<PstRangeAction>> rangeActions;

    public MinCostHardFiller(Set<FlowCnec> optimizedCnecs,
                             Unit unit, Map<State, Set<PstRangeAction>> rangeActions) {
        this.rangeActions = rangeActions;
        this.optimizedCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.optimizedCnecs.addAll(optimizedCnecs);
        this.unit = unit;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        Set<FlowCnec> validFlowCnecs = FillersUtil.getFlowCnecsComputationStatusOk(optimizedCnecs, sensitivityResult);

        // build variables
        buildTotalCostVariable(linearProblem, validFlowCnecs);
        buildRangeActionCostVariable(linearProblem);

        // build constraints
        buildSecureCnecsHardConstraints(linearProblem, validFlowCnecs);
        buildRangeActionCostConstraints(linearProblem);
        buildTotalCostConstraints(linearProblem);

        // complete objective
        fillObjectiveWithActivationCost(linearProblem);
    }

    @Override
    public void updateBetweenSensiIteration(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        // Objective does not change, nothing to do
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // Objective does not change, nothing to do
    }

    /**
     * Build the total cost variable TC.
     * TC represents the activation cost of all range actions.
     */
    private void buildTotalCostVariable(LinearProblem linearProblem, Set<FlowCnec> validFlowCnecs) {
//        if (!rangeActions.isEmpty()) {
        linearProblem.addTotalCostVariable(0, LinearProblem.infinity());
//        } else {
        // if there is no RangeActions, the cost variable is forced to zero.
        // otherwise it would be unbounded in the LP
//            linearProblem.addActivationCostVariable(0.0, 0.0);
//        }
    }

    /**
     * Build one varible cost C[r] for each RangeAction r.
     * This variable describes the cost of applying a RangeAction.
     */
    private void buildRangeActionCostVariable(LinearProblem linearProblem) {
        rangeActions.forEach((state, rangeActionSet) ->
            rangeActionSet.forEach(rangeAction -> {
                linearProblem.addRangeActionCostVariable(0, LinearProblem.infinity(), rangeAction, state);
            }));
    }

    /**
     * Build two min/max constraints for each Cnec c.
     * <p>
     * For each Cnec c, the constraints are:
     * <p>
     * F[c] <= fmax[c]   (ABOVE_THRESHOLD)
     * fmin[c] <= F[c]   (BELOW_THRESHOLD)
     */
    private void buildSecureCnecsHardConstraints(LinearProblem linearProblem, Set<FlowCnec> validFlowCnecs) {
        validFlowCnecs.forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec, side);

            Optional<Double> minFlow;
            Optional<Double> maxFlow;
            minFlow = cnec.getLowerBound(side, MEGAWATT);
            maxFlow = cnec.getUpperBound(side, MEGAWATT);

            if (minFlow.isPresent()) {
                OpenRaoMPConstraint minimumMarginNegative = linearProblem.addMinimumMarginConstraint(-LinearProblem.infinity(), -minFlow.get(), cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD);
                minimumMarginNegative.setCoefficient(flowVariable, -1);
            }

            if (maxFlow.isPresent()) {
                OpenRaoMPConstraint minimumMarginPositive = linearProblem.addMinimumMarginConstraint(-LinearProblem.infinity(), maxFlow.get(), cnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                minimumMarginPositive.setCoefficient(flowVariable, 1);
            }
        }));
    }

    /**
     * Build constraint for total cost
     * total cost is the sum of all costs for all RangeActions
     * TC = sum(C[r])
     */
    private void buildTotalCostConstraints(LinearProblem linearProblem) {
        // create constraint & add variable cost (objective function)
        OpenRaoMPVariable totalCostVariable = linearProblem.getTotalCostVariable();
        OpenRaoMPConstraint totalCostConstraint = linearProblem.addActivationCostConstraint(0, 0);
        totalCostConstraint.setCoefficient(totalCostVariable, 1);
        rangeActions.forEach((state, rangeActionSet) ->
            rangeActionSet.forEach(rangeAction -> {
                OpenRaoMPVariable rangeActionCostVariable = linearProblem.getRangeActionCostVariable(rangeAction, state);
                totalCostConstraint.setCoefficient(rangeActionCostVariable, -1);
            }));
    }

    /**
     * Build constraints for each RangeAction cost C[r].
     * The cost is
     * C[r] = activationCost * AV[r]
     * where AV[r] is the absolute variation variable
     */
    private void buildRangeActionCostConstraints(LinearProblem linearProblem) {
        rangeActions.forEach((state, rangeActionSet) ->
            rangeActionSet.forEach(rangeAction -> {
                OpenRaoMPConstraint rangeActionCostConstraint = linearProblem.addRangeActionCostConstraint(0, 0, rangeAction, state);
                OpenRaoMPVariable rangeActionCostVariable = linearProblem.getRangeActionCostVariable(rangeAction, state);
                OpenRaoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, state);
                rangeActionCostConstraint.setCoefficient(rangeActionCostVariable, 1);
                rangeActionCostConstraint.setCoefficient(absoluteVariationVariable, -rangeAction.getActivationCost());
            }));
    }

    /**
     * Add in the objective function of the linear problem the total cost TC
     */
    private void fillObjectiveWithActivationCost(LinearProblem linearProblem) {
        OpenRaoMPVariable totalCostVariable = linearProblem.getTotalCostVariable();
        linearProblem.getObjective().setCoefficient(totalCostVariable, 1);
    }

}

