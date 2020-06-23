/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
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
    public static final boolean DEFAULT_LOOP_FLOW_APPROXIMATION = true;
    public static final double DEFAULT_LOOP_FLOW_CONSTRAINT_ADJUSTMENT_COEFFICIENT = 0.0;
    public static final double DEFAULT_LOOP_FLOW_VIOLATION_COST = 1000000.0;

    private boolean isLoopFlowApproximation;
    private double loopFlowConstraintAdjustmentCoefficient;
    private double loopFlowViolationCost;

    public MaxLoopFlowFiller(boolean isLoopFlowApproximation, double loopFlowConstraintAdjustmentCoefficient, double loopFlowViolationCost) {
        this.isLoopFlowApproximation = isLoopFlowApproximation;
        this.loopFlowConstraintAdjustmentCoefficient = loopFlowConstraintAdjustmentCoefficient;
        this.loopFlowViolationCost = loopFlowViolationCost;
    }

    // Methods for tests
    public MaxLoopFlowFiller() {
        this(DEFAULT_LOOP_FLOW_APPROXIMATION, DEFAULT_LOOP_FLOW_CONSTRAINT_ADJUSTMENT_COEFFICIENT, DEFAULT_LOOP_FLOW_VIOLATION_COST);
    }

    void setLoopFlowApproximation(boolean loopFlowApproximation) {
        isLoopFlowApproximation = loopFlowApproximation;
    }

    void setLoopFlowConstraintAdjustmentCoefficient(double loopFlowConstraintAdjustmentCoefficient) {
        this.loopFlowConstraintAdjustmentCoefficient = loopFlowConstraintAdjustmentCoefficient;
    }

    void setLoopFlowViolationCost(double loopFlowViolationCost) {
        this.loopFlowViolationCost = loopFlowViolationCost;
    }
    // End of methods for tests

    @Override
    public void fill(RaoData raoData, LinearProblem linearProblem) {
        buildLoopFlowConstraintsAndUpdateObjectiveFunction(raoData, linearProblem);
    }

    @Override
    public void update(RaoData raoData, LinearProblem linearProblem) {
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
     * An additional MPVariable "loopflowViolationVariable" may be added when "Loopflow violation cost" is not zero:
     *     - (MaxLoopFlow + loopflowViolationVariable) <= currentLoopFlow <= MaxLoopFlow + loopflowViolationVariable
     * equivalent to 2 constraints:
     *     - MaxLoopFlow <= currentLoopFlow + loopflowViolationVariable
     *     currentLoopFlow - loopflowViolationVariable <= MaxLoopFlow
     * or:
     *     - MaxLoopFlow + LoopFlowShift <= flowVariable + loopflowViolationVariable <= POSITIVE_INF
     *     NEGATIVE_INF <= flowVariable - loopflowViolationVariable <= MaxLoopFlow + LoopFlowShift
     * and a "virtual cost" is added to objective function as "loopflowViolationVariable * Loopflow violation cost"
     */
    private void buildLoopFlowConstraintsAndUpdateObjectiveFunction(RaoData raoData, LinearProblem linearProblem) {
        Map<Cnec, Double> loopFlowShifts;
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(raoData.getCrac());
        if (isLoopFlowApproximation) {
            loopFlowShifts = loopFlowComputation.buildLoopflowShiftsApproximation(raoData.getCrac());
        } else {
            loopFlowShifts = loopFlowComputation.buildZeroBalanceFlowShift(raoData.getNetwork());
        }

        for (Cnec cnec : raoData.getCrac().getCnecs(raoData.getCrac().getPreventiveState())) {
            //security check
            if (Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class))) {
                continue;
            }

            //get and update MapLoopflowLimit with loopflowConstraintAdjustmentCoefficient
            double maxLoopFlowLimit = Math.abs(cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraint());
            maxLoopFlowLimit = Math.max(0.0, maxLoopFlowLimit - loopFlowConstraintAdjustmentCoefficient);

            //get loopflow shift
            double loopFlowShift = loopFlowShifts.getOrDefault(cnec, 0.0);

            //get MPVariable: flowVariable
            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);
            if (Objects.isNull(flowVariable)) {
                throw new FaraoException(String.format("Flow variable on %s has not been defined yet.", cnec.getId()));
            }

            if (loopFlowViolationCost == 0.0) {
                //no loopflow violation cost => 1 MP constraint
                //NOTE: The "zero-loopflowViolationCost" routine handles infeasible / non-optimal solver status in LinearRao.
                MPConstraint maxLoopflowConstraint = linearProblem.addMaxLoopFlowConstraint(
                        -maxLoopFlowLimit + loopFlowShift,
                        maxLoopFlowLimit + loopFlowShift,
                        cnec);
                maxLoopflowConstraint.setCoefficient(flowVariable, 1);
            } else {
                //loopflow violation cost is not zero => 2 MP constraints, additional MPVariable loopflowViolationVariable
                MPVariable loopflowViolationVariable = linearProblem.addLoopflowViolationVariable(
                        0, //virtual cost is strict positive, so lb = 0 for target variable solution at lb
                        linearProblem.infinity(),
                        cnec
                );

                // - MaxLoopFlow + LoopFlowShift <= flowVariable + loopflowViolationVariable <= POSITIVE_INF
                MPConstraint positiveLoopflowViolationConstraint = linearProblem.addPositiveLoopflowViolationConstraint(
                        -maxLoopFlowLimit + loopFlowShift,
                        linearProblem.infinity(),
                        cnec
                );
                positiveLoopflowViolationConstraint.setCoefficient(flowVariable, 1);
                positiveLoopflowViolationConstraint.setCoefficient(loopflowViolationVariable, 1);

                // NEGATIVE_INF <= flowVariable - loopflowViolationVariable <= MaxLoopFlow + LoopFlowShift
                MPConstraint negativeLoopflowViolationConstraint = linearProblem.addNegativeLoopflowViolationConstraint(
                        -linearProblem.infinity(),
                        maxLoopFlowLimit + loopFlowShift,
                        cnec
                );
                negativeLoopflowViolationConstraint.setCoefficient(flowVariable, 1);
                negativeLoopflowViolationConstraint.setCoefficient(loopflowViolationVariable, -1);

                //update objective function when non-zero-loopflowViolationCost
                linearProblem.getObjective().setCoefficient(loopflowViolationVariable, loopFlowViolationCost);
            }
        } // end cnec loop
    }
}
