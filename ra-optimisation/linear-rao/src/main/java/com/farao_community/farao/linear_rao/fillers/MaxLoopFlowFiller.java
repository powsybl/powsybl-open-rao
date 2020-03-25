/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.flowbased_computation.impl.LoopFlowComputation;
import com.farao_community.farao.linear_rao.AbstractProblemFiller;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.*;

/**
 * Filler of loopflow constraint in linear rao problem.
 * - Current loopflow will only be checked for preventive state cnec.
 * - This constraint is set at beginning of search tree rao. It is not updated during optimization. It could be updated by
 * re-computing loopflow's constraint bound following each network's update.
 * - NOTE: if loopflow is updated during optimization iteration, it should note that the pst tap changer positions
 * are considered as continuous variables by the solver and the updated loopflow bounds are not the exact value. This is
 * currently not a problem because we only consider the preventive state where pst tap changer positions are integers.
 *
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class MaxLoopFlowFiller extends AbstractProblemFiller {

    private Set<Cnec> preventiveCnecs; //currently we only forcus on preventive state cnec
    private CracLoopFlowExtension cracLoopFlowExtension;

    public MaxLoopFlowFiller(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData) {
        super(linearRaoProblem, linearRaoData);
        Crac crac = linearRaoData.getCrac();
        this.preventiveCnecs = crac.getCnecs(crac.getPreventiveState());
        this.cracLoopFlowExtension = crac.getExtension(CracLoopFlowExtension.class);
    }

    @Override
    public void fill() {
        buildMaxLoopFlowConstraint();
    }

    /**
     * Loopflow F_(0,all)_current = flowVariable - PTDF * NetPosition, where flowVariable is a MPVariable in linearRao
     * then max loopflow MPConstraint is: - maxLoopFlow <= currentLoopFlow <= maxLoopFlow
     * we define loopFlowShift = PTDF * NetPosition, then
     * -maxLoopFlow + loopFlowShift <= flowVariable <= maxLoopFlow + loopFlowShift,
     */
    private void buildMaxLoopFlowConstraint() {
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(linearRaoData.getCrac(), cracLoopFlowExtension);
        Map<Cnec, Double> loopFlowShifts = loopFlowComputation.buildZeroBalanceFlowShift(linearRaoData.getNetwork());

        for (Cnec cnec : preventiveCnecs) {
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

    @Override
    public void update() {
    }
}
