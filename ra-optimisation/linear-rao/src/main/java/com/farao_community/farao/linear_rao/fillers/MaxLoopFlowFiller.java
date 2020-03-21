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

import java.util.*;

/**
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
        Map<Cnec, Map<String, Double>> ptdfs = cracLoopFlowExtension.getPtdfs();
        for (Cnec cnec : preventiveCnecs) {
            double loopFlowShift = computeLoopFlowShift(ptdfs.get(cnec));
            double maxLoopFlowLimit = Math.abs(cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraint());
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
        updateMaxLoopFlowConstraint();
    }

    /**
     * update max loopflow constraint's bounds; new bounds are calculated following network update
     * Note: currently we only focus on preventive state cnec, and we use the ptdf in preventive state. In the future,
     * we can also update the ptdf and netposition during each iteration. In this case, instead of get ptdf directly
     * from cracLoopFlowExtension, we need to get GlskProvider and List countries from cracLoopFlowExtension, and then
     * launch a LoopFlowComputation to get an updated ptdfs.
     */
    private void updateMaxLoopFlowConstraint() {
        Map<Cnec, Map<String, Double>> ptdfs = cracLoopFlowExtension.getPtdfs();  //use preventive ptdf, ref javadoc
        for (Cnec cnec : preventiveCnecs) {
            double loopFlowShift = computeLoopFlowShift(ptdfs.get(cnec));
            double maxLoopFlowLimit = Math.abs(cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraint());
            MPConstraint maxLoopflowConstraint = linearRaoProblem.getMaxLoopFlowConstraint(cnec);
            if (Objects.isNull(maxLoopflowConstraint)) {
                throw new FaraoException(String.format("MaxLoopflow constraint on %s has not been defined yet.", cnec.getId()));
            }
            //reset bounds
            maxLoopflowConstraint.setLb(-maxLoopFlowLimit + loopFlowShift);
            maxLoopflowConstraint.setUb(maxLoopFlowLimit + loopFlowShift);
        }
    }

    /**
     * compute loopflow shift = PTDF * Net Position
     * @param cnecPtdf ptdf of the current Cnec
     * @return loopFlowShift
     */
    private double computeLoopFlowShift(Map<String, Double> cnecPtdf) {
        //calculate loopFlowShift = PTDF * NetPosition
        double loopFlowShift = 0.0; // PTDF * NetPosition
        Map<String, Double> referenceNetPositionByCountry = cracLoopFlowExtension.getNetPositions(); // get Net positions
        for (Map.Entry<String, Double> e : cnecPtdf.entrySet()) {
            String country = e.getKey();
            loopFlowShift += cnecPtdf.get(country) * referenceNetPositionByCountry.get(country); // PTDF * NetPosition
        }
        return loopFlowShift;
    }

}
