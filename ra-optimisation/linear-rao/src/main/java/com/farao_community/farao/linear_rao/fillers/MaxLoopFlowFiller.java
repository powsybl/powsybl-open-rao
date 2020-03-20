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
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class MaxLoopFlowFiller extends AbstractProblemFiller {

    private Crac crac;
    private Set<Cnec> cnecs;
    private CracLoopFlowExtension cracLoopFlowExtension;

    public MaxLoopFlowFiller(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData) {
        super(linearRaoProblem, linearRaoData);
        this.crac = linearRaoData.getCrac();
        this.cnecs = crac.getCnecs(crac.getPreventiveState());
        this.cracLoopFlowExtension = crac.getExtension(CracLoopFlowExtension.class);
    }

    @Override
    public void fill() {
        //build max loopflow constraint
        buildMaxLoopFlowConstraint();
    }

    /**
     * Loopflow F_(0,all)_current = flowVariable - PTDF * NetPosition, where flowVariable is a MPVariable in linearRaoProblem
     * then max loopflow MPConstraint is: - maxLoopFlow <= currentLoopFlow <= maxLoopFlow
     * we define loopFlowShift = PTDF * NetPosition, then
     * -maxLoopFlow + loopFlowShift <= flowVariable <= maxLoopFlow + loopFlowShift,
     */
    private void buildMaxLoopFlowConstraint() {
        LoopFlowComputation currentLoopFlowComputation = new LoopFlowComputation(crac, cracLoopFlowExtension);
        for (Cnec cnec : cnecs) {
            //calculate loopFlowShift = PTDF * NetPosition
            double loopFlowShift = 0.0; // PTDF * NetPosition
            Map<String, Double> cnecptdf = currentLoopFlowComputation.computePtdfOnCurrentNetwork(linearRaoData.getNetwork()).get(cnec); //get PTDF of current Cnec
            Map<String, Double> referenceNetPositionByCountry = currentLoopFlowComputation.getRefNetPositionByCountry(linearRaoData.getNetwork()); // get Net positions
            for (Map.Entry<String, Double> e : cnecptdf.entrySet()) {
                String country = e.getKey();
                loopFlowShift += cnecptdf.get(country) * referenceNetPositionByCountry.get(country); // PTDF * NetPosition
            }

            double maxLoopFlowLimit = Math.abs(cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraint()); // Math.abs, keep maxLoopFlowLimit positive
            double lb = -maxLoopFlowLimit + loopFlowShift;
            double ub = maxLoopFlowLimit + loopFlowShift;
            MPConstraint maxLoopflowConstraint = linearRaoProblem.addMaxLoopFlowConstraint(lb, ub, cnec);
            MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec);
            if (Objects.isNull(flowVariable)) {
                throw new FaraoException(String.format("Max LoopFlow variable on %s has not been defined yet.", cnec.getId()));
            }
            maxLoopflowConstraint.setCoefficient(flowVariable, 1);
        }
    }

    @Override
    public void update() {
        // todo
    }
}
