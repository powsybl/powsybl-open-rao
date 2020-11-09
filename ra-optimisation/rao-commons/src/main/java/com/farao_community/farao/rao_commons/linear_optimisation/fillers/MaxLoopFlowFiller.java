/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;

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

    private boolean isLoopFlowApproximation;
    private double loopFlowAcceptableAugmentation;
    private double loopFlowConstraintAdjustmentCoefficient;
    private double loopFlowViolationCost;
    private SensitivityAnalysisParameters sensitivityAnalysisParameters;

    public MaxLoopFlowFiller(boolean isLoopFlowApproximation, double loopFlowAcceptableAugmentation, double loopFlowConstraintAdjustmentCoefficient, double loopFlowViolationCost, SensitivityAnalysisParameters sensitivityAnalysisParameters) {
        this.isLoopFlowApproximation = isLoopFlowApproximation;
        this.loopFlowAcceptableAugmentation = loopFlowAcceptableAugmentation;
        this.loopFlowConstraintAdjustmentCoefficient = loopFlowConstraintAdjustmentCoefficient;
        this.loopFlowViolationCost = loopFlowViolationCost;
        this.sensitivityAnalysisParameters = sensitivityAnalysisParameters;
    }

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

        LoopFlowResult loopFlowResult = null;
        //todo : do not compute loopFlow from scratch here : not necessary
        if (!isLoopFlowApproximation) {
            loopFlowResult = new LoopFlowComputation(raoData.getGlskProvider(), raoData.getReferenceProgram())
                .calculateLoopFlows(raoData.getNetwork(), sensitivityAnalysisParameters, raoData.getLoopflowCnecs());
        }
        String initialVariantId =  raoData.getCrac().getExtension(ResultVariantManager.class).getInitialVariantId();
        for (Cnec cnec : raoData.getLoopflowCnecs()) {

            //get and update MapLoopFlowLimit with loopflowConstraintAdjustmentCoefficient
            double maxLoopFlowLimit = cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraintInMW();
            Double initialLoopFlow = cnec.getExtension(CnecResultExtension.class).getVariant(initialVariantId).getFlowInMW();
            double inputThreshold = cnec.getExtension(CnecLoopFlowExtension.class).getInputThreshold(Unit.MEGAWATT, raoData.getNetwork());
            if (maxLoopFlowLimit == Double.POSITIVE_INFINITY) {
                continue;
            }

            maxLoopFlowLimit = Math.max(inputThreshold, initialLoopFlow + loopFlowAcceptableAugmentation) - loopFlowConstraintAdjustmentCoefficient;

            double commercialFlow;
            //get commercial flow
            if (!isLoopFlowApproximation) {
                commercialFlow = loopFlowResult.getCommercialFlow(cnec);
            } else {
                commercialFlow = cnec.getExtension(CnecLoopFlowExtension.class).getLoopflowShift();
            }

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

            // - MaxLoopFlow + LoopFlowShift <= flowVariable + loopflowViolationVariable <= POSITIVE_INF
            MPConstraint positiveLoopflowViolationConstraint = linearProblem.addMaxLoopFlowConstraint(
                -maxLoopFlowLimit + commercialFlow,
                linearProblem.infinity(),
                cnec,
                LinearProblem.BoundExtension.LOWER_BOUND
            );
            positiveLoopflowViolationConstraint.setCoefficient(flowVariable, 1);
            positiveLoopflowViolationConstraint.setCoefficient(loopflowViolationVariable, 1);

            // NEGATIVE_INF <= flowVariable - loopflowViolationVariable <= MaxLoopFlow + LoopFlowShift
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
}
