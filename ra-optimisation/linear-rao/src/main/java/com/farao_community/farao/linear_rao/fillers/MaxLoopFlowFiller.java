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
import com.farao_community.farao.linear_rao.AbstractProblemFiller;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Set;

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
        //build variable
        buildMaxLoopFlowVariable();
        //build constraint
        buildMaxLoopFlowConstraint();
    }

    private void buildMaxLoopFlowVariable() {
        cnecs.forEach(cnec ->  linearRaoProblem.addMaxLoopFlowVariable(-linearRaoProblem.infinity(), linearRaoProblem.infinity(), cnec));
    }

    private void buildMaxLoopFlowConstraint() {
        for (Cnec cnec : cnecs) {
            // create constraint
            CnecLoopFlowExtension cnecLoopFlowExtension = cnec.getExtension(CnecLoopFlowExtension.class);
            double maxLoopFlowLimit = cnecLoopFlowExtension.getLoopFlowConstraint();
            double currentLoopFlow = 0.0; //todo calculate currentloppflow
            MPConstraint maxLoopflowConstraint = linearRaoProblem.addMaxLoopFlowConstraint(-linearRaoProblem.infinity(), maxLoopFlowLimit, cnec);

            MPVariable maxLoopFlowVariable = linearRaoProblem.getMaxLoopFlowVariable(cnec);
            if (maxLoopFlowVariable == null) {
                throw new FaraoException(String.format("Max LoopFlow variable on %s has not been defined yet.", cnec.getId()));
            }
            maxLoopflowConstraint.setCoefficient(maxLoopFlowVariable, 1);
        }
    }

    @Override
    public void update() {
        // todo
    }
}
