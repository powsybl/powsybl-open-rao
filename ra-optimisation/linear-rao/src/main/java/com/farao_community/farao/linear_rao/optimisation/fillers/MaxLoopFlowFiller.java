/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.optimisation.LinearRaoProblem;
import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.*;

/**
 * Filler of loopflow constraint in linear rao problem.
 * - Current loopflow will only be checked for preventive state cnec.
 * - This constraint is set at the beginning of the linear rao. It is not updated during optimization. It could be updated
 * by re-computing loopflow's constraint bound following each network's update.
 * - NOTE: It should note that the pst tap changer positions are considered as continuous variables by the solver
 * so that the loopflow constraint used during optimization is satisfied for a network situation where pst tap changers
 * are not all integers. We do not currently re-check the loopflow constraint on integer pst tap changer network. This
 * is a (hopefully-) reasonable approximation (considering input data quality, and measuring errors etc.).
 *
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class MaxLoopFlowFiller implements ProblemFiller {

    @Override
    public void fill(LinearRaoData linearRaoData, LinearRaoProblem linearRaoProblem, LinearRaoParameters linearRaoParameters) {
        buildLoopFlowConstraintsAndUpdateObjectiveFunction(linearRaoData, linearRaoProblem, linearRaoParameters);
    }

    @Override
    public void update(LinearRaoData linearRaoData, LinearRaoProblem linearRaoProblem, LinearRaoParameters linearRaoParameters) {
        // do nothing
    }

    /**
     * currentLoopflow = flowVariable - PTDF * NetPosition, where flowVariable a MPVariable
     * Constraint for loopflow in optimization is: - MaxLoopFlow <= currentLoopFlow <= MaxLoopFlow,
     * where MaxLoopFlow is calculated as
     *     max(Input TSO loopflow limit, Loopflow value computed from initial network)
     *
     * let LoopFlowShift = PTDF * NetPosition, then
     *     - MaxLoopFlow + LoopFlowShift <= flowVariable <= MaxLoopFlow + LoopFlowShift
     *
     * Loopflow limit may be tuned by a "Loopflow adjustment coefficient":
     *     MaxLoopFlow = Loopflow constraint - Loopflow adjustment coefficient
     *
     * An additional MPVariable "loopflowBreachVariable" may be added when "Loopflow violation cost" is not zero:
     *     - (MaxLoopFlow + loopflowBreachVariable) <= currentLoopFlow <= MaxLoopFlow + loopflowBreachVariable
     * equivalent to 2 constraints:
     *     - MaxLoopFlow <= currentLoopFlow + loopflowBreachVariable
     *     currentLoopFlow - loopflowBreachVariable <= MaxLoopFlow
     * or:
     *     - MaxLoopFlow + LoopFlowShift <= flowVariable + loopflowBreachVariable <= POSITIVE_INF
     *     NEGATIVE_INF <= flowVariable - loopflowBreachVariable <= MaxLoopFlow + LoopFlowShift
     * and a "virtual cost" is added to objective function as "loopflowBreachVariable * Loopflow violation cost"
     */
    private void buildLoopFlowConstraintsAndUpdateObjectiveFunction(LinearRaoData linearRaoData, LinearRaoProblem linearRaoProblem, LinearRaoParameters linearRaoParameters) {
        Map<Cnec, Double> loopFlowShifts;
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(linearRaoData.getCrac());
        if (!Objects.isNull(linearRaoParameters.getExtendable()) && linearRaoParameters.getExtendable().isLoopflowApproximation()) {
            loopFlowShifts = loopFlowComputation.buildLoopflowShiftsApproximation(linearRaoData.getCrac());
        } else {
            loopFlowShifts = loopFlowComputation.buildZeroBalanceFlowShift(linearRaoData.getNetwork());
        }

        //get additional config parameters
        double loopflowConstraintAdjustmentCoefficient = 0.0;
        double loopflowViolationCost = 0.0;
        if (!Objects.isNull(linearRaoParameters.getExtendable())) {
            loopflowConstraintAdjustmentCoefficient = linearRaoParameters.getExtendable().getLoopflowConstraintAdjustmentCoefficient();
            loopflowViolationCost = linearRaoParameters.getExtendable().getLoopflowViolationCost();
        }

        for (Cnec cnec : linearRaoData.getCrac().getCnecs(linearRaoData.getCrac().getPreventiveState())) {
            //security check
            if (Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class))) {
                continue;
            }

            //get and update MapLoopflowLimit with loopflowConstraintAdjustmentCoefficient
            double maxLoopFlowLimit = Math.abs(cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraint());
            maxLoopFlowLimit = Math.max(0.0, maxLoopFlowLimit - loopflowConstraintAdjustmentCoefficient);

            //get loopflow shift
            double loopFlowShift = loopFlowShifts.getOrDefault(cnec, 0.0);

            //get MPVariable: flowVariable
            MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec);
            if (Objects.isNull(flowVariable)) {
                throw new FaraoException(String.format("Flow variable on %s has not been defined yet.", cnec.getId()));
            }

            if (loopflowViolationCost == 0.0) {
                //no loopflow violation cost => 1 MP constraint
                //NOTE: The "zero-loopflowViolationCost" routine handles infeasible / non-optimal solver status in LinearRao.
                MPConstraint maxLoopflowConstraint = linearRaoProblem.addMaxLoopFlowConstraint(
                        -maxLoopFlowLimit + loopFlowShift,
                        maxLoopFlowLimit + loopFlowShift,
                        cnec);
                maxLoopflowConstraint.setCoefficient(flowVariable, 1);
            } else {
                //loopflow violation cost is not zero => 2 MP constraints, additional MPVariable loopflowBreachVariable
                MPVariable loopflowBreachVariable = linearRaoProblem.addLoopflowBreachVariable(
                        0, //virtual cost is strict positive, so lb = 0 for target variable solution at lb
                        linearRaoProblem.infinity(),
                        cnec
                );

                // - MaxLoopFlow + LoopFlowShift <= flowVariable + loopflowBreachVariable <= POSITIVE_INF
                MPConstraint positiveLoopflowBreachConstraint = linearRaoProblem.addPositiveLoopflowBreachConstraint(
                        -maxLoopFlowLimit + loopFlowShift,
                        linearRaoProblem.infinity(),
                        cnec
                );
                positiveLoopflowBreachConstraint.setCoefficient(flowVariable, 1);
                positiveLoopflowBreachConstraint.setCoefficient(loopflowBreachVariable, 1);

                // NEGATIVE_INF <= flowVariable - loopflowBreachVariable <= MaxLoopFlow + LoopFlowShift
                MPConstraint negativeLoopflowBreachConstraint = linearRaoProblem.addNegativeLoopflowBreachConstraint(
                        -linearRaoProblem.infinity(),
                        maxLoopFlowLimit + loopFlowShift,
                        cnec
                );
                negativeLoopflowBreachConstraint.setCoefficient(flowVariable, 1);
                negativeLoopflowBreachConstraint.setCoefficient(loopflowBreachVariable, -1);

                //update objective function when non-zero-loopflowViolationCost
                linearRaoProblem.getObjective().setCoefficient(loopflowBreachVariable, loopflowViolationCost);
            }
        } // end cnec loop
    }
}
