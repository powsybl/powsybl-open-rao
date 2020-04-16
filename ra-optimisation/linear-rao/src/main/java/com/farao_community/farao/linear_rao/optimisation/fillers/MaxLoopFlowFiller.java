/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.flowbased_computation.impl.LoopFlowComputation;
import com.farao_community.farao.linear_rao.optimisation.AbstractProblemFiller;
import com.farao_community.farao.linear_rao.optimisation.LinearRaoData;
import com.farao_community.farao.linear_rao.optimisation.LinearRaoProblem;
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
public class MaxLoopFlowFiller extends AbstractProblemFiller {

    private Set<Cnec> preventiveCnecs; //currently we only forcus on preventive state cnec
    private CracLoopFlowExtension cracLoopFlowExtension;
    private double loopflowConstraintAdjustmentCoefficient;
    private double loopflowViolationCost;

    public MaxLoopFlowFiller(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData,
                             double loopflowConstraintAdjustmentCoefficient,
                             double loopflowViolationCost) {
        super(linearRaoProblem, linearRaoData);
        Crac crac = linearRaoData.getCrac();
        this.preventiveCnecs = crac.getCnecs(crac.getPreventiveState());
        this.cracLoopFlowExtension = crac.getExtension(CracLoopFlowExtension.class);
        this.loopflowConstraintAdjustmentCoefficient = loopflowConstraintAdjustmentCoefficient;
        this.loopflowViolationCost = loopflowViolationCost;
    }

    @Override
    public void fill() {
        buildMaxLoopFlowConstraint();
        buildObjectiveFunctionWithLoopFlowViolation();
    }

    /**
     * Loopflow F_(0,all)_current = flowVariable - PTDF * NetPosition, where flowVariable is a MPVariable in linearRao
     * then max loopflow MPConstraint is: - maxLoopFlow <= currentLoopFlow <= maxLoopFlow
     * we define loopFlowShift = PTDF * NetPosition, then
     * -maxLoopFlow + loopFlowShift <= flowVariable <= maxLoopFlow + loopFlowShift,
     * the "maxLoopFlow" contains a adjustment coefficient read from config file
     *
     * In addition we introduce a "loopflow violation variable" into the above function:
     * -maxLoopFlow + loopFlowShift <= flowVariable +/- "loopflow violation variable" <= maxLoopFlow + loopFlowShift,
     */
    private void buildMaxLoopFlowConstraint() {
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(linearRaoData.getCrac(), cracLoopFlowExtension);
        Map<Cnec, Double> loopFlowShifts = loopFlowComputation.buildZeroBalanceFlowShift(linearRaoData.getNetwork());

        for (Cnec cnec : preventiveCnecs) {
            double loopFlowShift = 0.0;
            CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(linearRaoData.getResultVariantId());
            double maxLoopFlowLimit = Math.abs(cnecResult.getLoopflowconstraint());
            maxLoopFlowLimit = Math.max(0, maxLoopFlowLimit - this.loopflowConstraintAdjustmentCoefficient);
            if (loopFlowShifts.containsKey(cnec)) {
                loopFlowShift = loopFlowShifts.get(cnec);
            }
            MPConstraint maxLoopflowConstraintPositiveViolation = linearRaoProblem.addMaxLoopFlowConstraintPositiveViolation(
                    -maxLoopFlowLimit + loopFlowShift,
                    maxLoopFlowLimit + loopFlowShift,
                    cnec);
            MPConstraint maxLoopflowConstraintNegativeViolation = linearRaoProblem.addMaxLoopFlowConstraintNegativeViolation(
                    -maxLoopFlowLimit + loopFlowShift,
                    maxLoopFlowLimit + loopFlowShift,
                    cnec);
            MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec);
            if (Objects.isNull(flowVariable)) {
                throw new FaraoException(String.format("Flow variable on %s has not been defined yet.", cnec.getId()));
            }
            maxLoopflowConstraintPositiveViolation.setCoefficient(flowVariable, 1);
            maxLoopflowConstraintNegativeViolation.setCoefficient(flowVariable, 1);

            MPVariable cnecLoopflowViolationVariable = linearRaoProblem.addLoopflowViolationVariable(0, linearRaoProblem.infinity(), cnec);
            if (Objects.isNull(cnecLoopflowViolationVariable)) {
                throw new FaraoException(String.format("LoopflowViolationVariable on %s has not been defined yet.", cnec.getId()));
            }
            maxLoopflowConstraintPositiveViolation.setCoefficient(cnecLoopflowViolationVariable, 1);
            maxLoopflowConstraintNegativeViolation.setCoefficient(cnecLoopflowViolationVariable, -1);
        }
    }

    private void buildObjectiveFunctionWithLoopFlowViolation() {
        for (Cnec cnec : preventiveCnecs) {
            MPVariable cnecLoopflowViolationVariable = linearRaoProblem.getLoopflowViolationVariable(cnec);
            linearRaoProblem.getObjective().setCoefficient(cnecLoopflowViolationVariable, this.loopflowViolationCost);
        }
    }

    @Override
    public void update() {
        //This should do nothing.
    }
}
