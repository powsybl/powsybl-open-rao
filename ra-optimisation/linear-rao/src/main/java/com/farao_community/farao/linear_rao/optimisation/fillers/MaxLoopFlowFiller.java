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
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.flowbased_computation.impl.LoopFlowComputation;
import com.farao_community.farao.linear_rao.Situation;
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
public class MaxLoopFlowFiller implements ProblemFiller {

    @Override
    public void fill(Situation situation, LinearRaoProblem linearRaoProblem) {
        buildMaxLoopFlowConstraint(situation, linearRaoProblem);
    }

    @Override
    public void update(Situation situation, LinearRaoProblem linearRaoProblem) {
        // do nothing
    }

    /**
     * Loopflow F_(0,all)_current = flowVariable - PTDF * NetPosition, where flowVariable is a MPVariable in linearRao
     * then max loopflow MPConstraint is: - maxLoopFlow <= currentLoopFlow <= maxLoopFlow
     * we define loopFlowShift = PTDF * NetPosition, then
     * -maxLoopFlow + loopFlowShift <= flowVariable <= maxLoopFlow + loopFlowShift,
     */
    private void buildMaxLoopFlowConstraint(Situation situation, LinearRaoProblem linearRaoProblem) {
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(situation.getCrac(), situation.getCrac().getExtension(CracLoopFlowExtension.class));
        Map<Cnec, Double> loopFlowShifts = loopFlowComputation.buildZeroBalanceFlowShift(situation.getNetwork());

        for (Cnec cnec : situation.getCrac().getCnecs(situation.getCrac().getPreventiveState())) {
            double loopFlowShift = 0.0;
            double maxLoopFlowLimit = Math.abs(cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraint());
            if (loopFlowShifts.containsKey(cnec)) {
                loopFlowShift = loopFlowShifts.get(cnec);
            }
            MPConstraint maxLoopflowConstraint = linearRaoProblem.addMaxLoopFlowConstraint(
                    -maxLoopFlowLimit + loopFlowShift,
                    maxLoopFlowLimit + loopFlowShift,
                    cnec);
            MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec);
            if (Objects.isNull(flowVariable)) {
                throw new FaraoException(String.format("Flow variable on %s has not been defined yet.", cnec.getId()));
            }
            maxLoopflowConstraint.setCoefficient(flowVariable, 1);
        }
    }
}
