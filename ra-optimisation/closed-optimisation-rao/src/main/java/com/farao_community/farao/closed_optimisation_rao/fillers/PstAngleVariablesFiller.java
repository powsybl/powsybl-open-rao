/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.closed_optimisation_rao.AbstractOptimisationProblemFiller;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.PstElement;
import com.farao_community.farao.data.crac_file.RemedialAction;
import com.farao_community.farao.data.crac_file.TypeOfLimit;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class PstAngleVariablesFiller extends AbstractOptimisationProblemFiller {
    private static final String SHIFT_VALUE_POSTFIX = "_shift_value";

    private List<PstElement> pstElement;

    /**
     * Check if the remedial action is a PST remedial action (i.e. with only
     * one remedial action element and PST)
     */
    private boolean isPstRemedialAction(RemedialAction remedialAction) {
        return remedialAction.getRemedialActionElements().size() == 1 &&
                remedialAction.getRemedialActionElements().get(0) instanceof PstElement;
    }

    @Override
    public void initFiller(Network network, CracFile cracFile, Map<String, Object> data) {
        super.initFiller(network, cracFile, data);
        this.pstElement = cracFile.getRemedialActions().stream()
                .filter(this::isPstRemedialAction)
                .flatMap(remedialAction -> remedialAction.getRemedialActionElements().stream())
                .map(remedialActionElement -> (PstElement) remedialActionElement)
                .collect(Collectors.toList());
    }

    @Override
    public void fillProblem(MPSolver solver) {
        pstElement.forEach(pst -> {
            PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(pst.getId()).getPhaseTapChanger();
            int lowTapPosition = pst.getTypeOfLimit() == TypeOfLimit.ABSOLUTE ? pst.getMinStepRange() : pst.getMinStepRange() + phaseTapChanger.getTapPosition();
            int highTapPosition = pst.getTypeOfLimit() == TypeOfLimit.ABSOLUTE ? pst.getMaxStepRange() : pst.getMaxStepRange() + phaseTapChanger.getTapPosition();
            // Considering alpha is always between alpha low and alpha high
            double alphaLowStep = phaseTapChanger.getStep(lowTapPosition).getAlpha();
            double alphaHighStep = phaseTapChanger.getStep(highTapPosition).getAlpha();
            double alphaMin = Math.min(alphaLowStep, alphaHighStep);
            double alphaMax = Math.max(alphaLowStep, alphaHighStep);
            double alphaInit = phaseTapChanger.getCurrentStep().getAlpha();
            solver.makeNumVar(alphaMin - alphaInit, alphaMax - alphaInit, pst.getId() + SHIFT_VALUE_POSTFIX);
        });
    }

    @Override
    public List<String> variablesProvided() {
        return pstElement.stream().map(pst -> pst.getId() + SHIFT_VALUE_POSTFIX)
                .collect(Collectors.toList());
    }
}
