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

    private boolean isLoopFlowApproximation;
    private double loopFlowConstraintAdjustmentCoefficient;

    public MaxLoopFlowFiller(boolean isLoopFlowApproximation, double loopFlowConstraintAdjustmentCoefficient) {
        this.isLoopFlowApproximation = isLoopFlowApproximation;
        this.loopFlowConstraintAdjustmentCoefficient = loopFlowConstraintAdjustmentCoefficient;
    }

    // Methods for tests
    public MaxLoopFlowFiller() {
        this(DEFAULT_LOOP_FLOW_APPROXIMATION, DEFAULT_LOOP_FLOW_CONSTRAINT_ADJUSTMENT_COEFFICIENT);
    }

    void setLoopFlowApproximation(boolean loopFlowApproximation) {
        isLoopFlowApproximation = loopFlowApproximation;
    }
    // End of methods for tests

    @Override
    public void fill(RaoData raoData, LinearProblem linearProblem) {
        buildMaxLoopFlowConstraint(raoData, linearProblem);
    }

    @Override
    public void update(RaoData raoData, LinearProblem linearProblem) {
        // do nothing
    }

    /**
     * Loopflow F_(0,all)_current = flowVariable - PTDF * NetPosition, where flowVariable is a MPVariable in linearRao
     * then max loopflow MPConstraint is: - maxLoopFlow <= currentLoopFlow <= maxLoopFlow
     * we define loopFlowShift = PTDF * NetPosition, then
     * -maxLoopFlow + loopFlowShift <= flowVariable <= maxLoopFlow + loopFlowShift,
     */
    private void buildMaxLoopFlowConstraint(RaoData raoData, LinearProblem linearProblem) {
        Map<Cnec, Double> loopFlowShifts;
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(raoData.getCrac());
        if (isLoopFlowApproximation) {
            loopFlowShifts = loopFlowComputation.buildLoopflowShiftsApproximation(raoData.getCrac());
        } else {
            loopFlowShifts = loopFlowComputation.buildZeroBalanceFlowShift(raoData.getNetwork());
        }

        for (Cnec cnec : raoData.getCrac().getCnecs(raoData.getCrac().getPreventiveState())) {
            if (Objects.isNull(cnec.getExtension(CnecLoopFlowExtension.class))) {
                continue;
            }
            double loopFlowShift = 0.0;
            double maxLoopFlowLimit = Math.abs(cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraint());
            maxLoopFlowLimit = Math.max(0.0, maxLoopFlowLimit - loopFlowConstraintAdjustmentCoefficient);
            if (loopFlowShifts.containsKey(cnec)) {
                loopFlowShift = loopFlowShifts.get(cnec);
            }
            MPConstraint maxLoopFlowConstraint = linearProblem.addMaxLoopFlowConstraint(
                    -maxLoopFlowLimit + loopFlowShift,
                    maxLoopFlowLimit + loopFlowShift,
                    cnec);
            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);
            if (Objects.isNull(flowVariable)) {
                throw new FaraoException(String.format("Flow variable on %s has not been defined yet.", cnec.getId()));
            }
            maxLoopFlowConstraint.setCoefficient(flowVariable, 1);
        }
    }
}
