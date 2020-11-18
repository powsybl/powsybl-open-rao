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
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Objects;

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

    private double loopFlowAcceptableAugmentation;
    private double loopFlowConstraintAdjustmentCoefficient;
    private double loopFlowViolationCost;
    private RaoParameters.LoopFlowApproximationLevel loopFlowApproximationLevel;

    public MaxLoopFlowFiller(double loopFlowConstraintAdjustmentCoefficient, double loopFlowViolationCost, RaoParameters.LoopFlowApproximationLevel loopFlowApproximationLevel) {
        this.loopFlowConstraintAdjustmentCoefficient = loopFlowConstraintAdjustmentCoefficient;
        this.loopFlowViolationCost = loopFlowViolationCost;
        this.loopFlowApproximationLevel = loopFlowApproximationLevel;
    }

    @Override
    public void fill(RaoData raoData, LinearProblem linearProblem) {
        buildLoopFlowConstraintsAndUpdateObjectiveFunction(raoData, linearProblem);
    }

    @Override
    public void update(RaoData raoData, LinearProblem linearProblem) {
        if (loopFlowApproximationLevel.shouldUpdatePtdfWithPstChange()) {
            updateLoopFlowConstraints(raoData, linearProblem);
        }
    }

    /**
     * currentLoopflow = flowVariable - PTDF * NetPosition, where flowVariable a MPVariable
     * Constraint for loopflow in optimization is: - MaxLoopFlow <= currentLoopFlow <= MaxLoopFlow,
     * where MaxLoopFlow is calculated as
     * max(Input TSO loopflow limit, Loopflow value computed from initial network)
     * <p>
     * let CommercialFlow = PTDF * NetPosition, then
     * - MaxLoopFlow + CommercialFlow <= flowVariable <= MaxLoopFlow + CommercialFlow
     * <p>
     * Loopflow limit may be tuned by a "Loopflow adjustment coefficient":
     * MaxLoopFlow = Loopflow constraint - Loopflow adjustment coefficient
     * <p>
     * An additional MPVariable "loopflowViolationVariable" may be added when "Loopflow violation cost" is not zero:
     * - (MaxLoopFlow + loopflowViolationVariable) <= currentLoopFlow <= MaxLoopFlow + loopflowViolationVariable
     * equivalent to 2 constraints:
     * - MaxLoopFlow <= currentLoopFlow + loopflowViolationVariable
     * currentLoopFlow - loopflowViolationVariable <= MaxLoopFlow
     * or:
     * - MaxLoopFlow + CommercialFlow <= flowVariable + loopflowViolationVariable <= POSITIVE_INF
     * NEGATIVE_INF <= flowVariable - loopflowViolationVariable <= MaxLoopFlow + CommercialFlow
     * and a "virtual cost" is added to objective function as "loopflowViolationVariable * Loopflow violation cost"
     */
    private void buildLoopFlowConstraintsAndUpdateObjectiveFunction(RaoData raoData, LinearProblem linearProblem) {
        for (Cnec cnec : raoData.getLoopflowCnecs()) {
            //get and update MapLoopFlowLimit with loopflowConstraintAdjustmentCoefficient
            double maxLoopFlowLimit = cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraintInMW();

            if (maxLoopFlowLimit == Double.POSITIVE_INFINITY) {
                continue;
            }

            maxLoopFlowLimit = Math.max(0.0, maxLoopFlowLimit - loopFlowConstraintAdjustmentCoefficient);

            //get MPVariable: flowVariable
            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);
            if (Objects.isNull(flowVariable)) {
                throw new FaraoException(String.format("Flow variable on %s has not been defined yet.", cnec.getId()));
            }

            MPVariable loopflowViolationVariable = linearProblem.addLoopflowViolationVariable(
                    0,
                    linearProblem.infinity(),
                    cnec
            );

            double commercialFlow = cnec.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId()).getCommercialFlowInMW();
            // - MaxLoopFlow + commercialFlow <= flowVariable + loopflowViolationVariable <= POSITIVE_INF
            MPConstraint positiveLoopflowViolationConstraint = linearProblem.addMaxLoopFlowConstraint(
                    -maxLoopFlowLimit + commercialFlow,
                    linearProblem.infinity(),
                    cnec,
                    LinearProblem.BoundExtension.LOWER_BOUND
            );
            positiveLoopflowViolationConstraint.setCoefficient(flowVariable, 1);
            positiveLoopflowViolationConstraint.setCoefficient(loopflowViolationVariable, 1);

            // NEGATIVE_INF <= flowVariable - loopflowViolationVariable <= MaxLoopFlow + commercialFlow
            MPConstraint negativeLoopflowViolationConstraint = linearProblem.addMaxLoopFlowConstraint(
                    -linearProblem.infinity(),
                    maxLoopFlowLimit + commercialFlow,
                    cnec,
                    LinearProblem.BoundExtension.UPPER_BOUND
            );
            negativeLoopflowViolationConstraint.setCoefficient(flowVariable, 1);
            negativeLoopflowViolationConstraint.setCoefficient(loopflowViolationVariable, -1);

            //update objective function with loopflowViolationCost
            linearProblem.getObjective().setCoefficient(loopflowViolationVariable, loopFlowViolationCost);
        }
    } // end cnec loop

    /**
     * Update LoopFlow constraints' bounds when commercial flows have changed
     */
    private void updateLoopFlowConstraints(RaoData raoData, LinearProblem linearProblem) {
        for (Cnec cnec : raoData.getLoopflowCnecs()) {

            double maxLoopFlowLimit = cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraintInMW();
            if (maxLoopFlowLimit == Double.POSITIVE_INFINITY) {
                continue;
            }
            maxLoopFlowLimit = Math.max(0.0, maxLoopFlowLimit - loopFlowConstraintAdjustmentCoefficient);

            double commercialFlow = cnec.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId()).getCommercialFlowInMW();

            MPConstraint positiveLoopflowViolationConstraint = linearProblem.getMaxLoopFlowConstraint(cnec, LinearProblem.BoundExtension.LOWER_BOUND);
            if (positiveLoopflowViolationConstraint == null) {
                throw new FaraoException(String.format("Positive LoopFlow violation constraint on %s has not been defined yet.", cnec.getId()));
            }
            positiveLoopflowViolationConstraint.setLb(-maxLoopFlowLimit + commercialFlow);

            MPConstraint negativeLoopflowViolationConstraint = linearProblem.getMaxLoopFlowConstraint(cnec, LinearProblem.BoundExtension.UPPER_BOUND);
            if (negativeLoopflowViolationConstraint == null) {
                throw new FaraoException(String.format("Negative LoopFlow violation constraint on %s has not been defined yet.", cnec.getId()));
            }
            negativeLoopflowViolationConstraint.setUb(maxLoopFlowLimit + commercialFlow);
        }
    }
}
