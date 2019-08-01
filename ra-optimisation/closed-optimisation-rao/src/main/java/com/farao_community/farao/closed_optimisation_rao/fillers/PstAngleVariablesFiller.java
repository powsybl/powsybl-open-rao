/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.closed_optimisation_rao.AbstractOptimisationProblemFiller;
import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.PstElement;
import com.farao_community.farao.data.crac_file.TypeOfLimit;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.*;
import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoUtil.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class PstAngleVariablesFiller extends AbstractOptimisationProblemFiller {

    private HashMap<Optional<Contingency>, List<PstElement>> pstRemedialActions;

    @Override
    public void initFiller(Network network, CracFile cracFile, Map<String, Object> data) {
        super.initFiller(network, cracFile, data);
        this.pstRemedialActions = new HashMap<>();
        // add preventive pst remedial actions
        this.pstRemedialActions.put(Optional.empty(), getPstElement(getPreventiveRemedialActions(cracFile)));
        // add curative pst remedial actions
        cracFile.getContingencies().forEach(contingency -> this.pstRemedialActions.put(Optional.of(contingency),
                getPstElement(getCurativeRemedialActions(cracFile, contingency))));
    }

    @Override
    public void fillProblem(MPSolver solver) {
        pstRemedialActions.forEach(((contingency, raList) -> {
            raList.forEach(pst -> {
                PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(pst.getId()).getPhaseTapChanger();

                int lowTapPosition = pst.getTypeOfLimit() == TypeOfLimit.ABSOLUTE ? pst.getMinStepRange() : pst.getMinStepRange() + phaseTapChanger.getTapPosition();
                int highTapPosition = pst.getTypeOfLimit() == TypeOfLimit.ABSOLUTE ? pst.getMaxStepRange() : pst.getMaxStepRange() + phaseTapChanger.getTapPosition();

                // Considering alpha is always between alpha low and alpha high
                double alphaLowStep = phaseTapChanger.getStep(lowTapPosition).getAlpha();
                double alphaHighStep = phaseTapChanger.getStep(highTapPosition).getAlpha();
                double alphaMin = Math.min(alphaLowStep, alphaHighStep);
                double alphaMax = Math.max(alphaLowStep, alphaHighStep);
                double alphaInit = phaseTapChanger.getCurrentStep().getAlpha();

                solver.makeNumVar(alphaMin - alphaInit, alphaMax - alphaInit, nameShiftValueVariable(contingency, pst));
            });
        }));
    }

    @Override
    public List<String> variablesProvided() {
        List<String> variables = new ArrayList<>();
        pstRemedialActions.forEach((contingency, raList) -> {
            variables.addAll(raList.stream()
                    .map(gen -> nameShiftValueVariable(contingency, gen))
                    .collect(Collectors.toList()));
        });
        return variables;
    }
}
