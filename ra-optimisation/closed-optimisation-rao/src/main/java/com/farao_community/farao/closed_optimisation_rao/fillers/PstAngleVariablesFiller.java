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
import com.farao_community.farao.data.crac_file.TypeOfLimit;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.fillers.FillersTools.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class PstAngleVariablesFiller extends AbstractOptimisationProblemFiller {

    private List<PstElement> pstElementN;
    private List<PstElement> pstElementCurative;

    /**
     * add PST shift value variable into MPSolver with appropriate bounds
     */
    private void buildPstShitValueVariable(String varName, PstElement pst, MPSolver solver){
        PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(pst.getId()).getPhaseTapChanger();
        int lowTapPosition = pst.getTypeOfLimit() == TypeOfLimit.ABSOLUTE ? pst.getMinStepRange() : pst.getMinStepRange() + phaseTapChanger.getTapPosition();
        int highTapPosition = pst.getTypeOfLimit() == TypeOfLimit.ABSOLUTE ? pst.getMaxStepRange() : pst.getMaxStepRange() + phaseTapChanger.getTapPosition();
        // Considering alpha is always between alpha low and alpha high
        double alphaLowStep = phaseTapChanger.getStep(lowTapPosition).getAlpha();
        double alphaHighStep = phaseTapChanger.getStep(highTapPosition).getAlpha();
        double alphaMin = Math.min(alphaLowStep, alphaHighStep);
        double alphaMax = Math.max(alphaLowStep, alphaHighStep);
        double alphaInit = phaseTapChanger.getCurrentStep().getAlpha();
        solver.makeNumVar(alphaMin - alphaInit, alphaMax - alphaInit, varName );
    }

    @Override
    public void initFiller(Network network, CracFile cracFile, Map<String, Object> data) {
        super.initFiller(network, cracFile, data);
        this.pstElementN = cracFile.getRemedialActions().stream()
                .filter(ra -> isPstRemedialAction(ra))
                .filter(ra -> isRemedialActionPreventiveFreeToUse(ra))
                .flatMap(remedialAction -> remedialAction.getRemedialActionElements().stream())
                .map(remedialActionElement -> (PstElement) remedialActionElement)
                .collect(Collectors.toList());

        this.pstElementCurative = cracFile.getRemedialActions().stream()
                .filter(ra -> isPstRemedialAction(ra))
                .filter(ra -> isRemedialActionCurativeFreeToUse(ra))
                .flatMap(remedialAction -> remedialAction.getRemedialActionElements().stream())
                .map(remedialActionElement -> (PstElement) remedialActionElement)
                .collect(Collectors.toList());
    }

    @Override
    public void fillProblem(MPSolver solver) {
        // fill problem with preventive PST variables
        pstElementN.forEach(pst -> {
            buildPstShitValueVariable(nameShiftValueVariableN(pst.getId()), pst, solver);
        });

        // fill problem with curative PST variables
        cracFile.getContingencies().forEach( cont ->{
            pstElementCurative.forEach(pst -> {
                buildPstShitValueVariable(nameShiftValueVariableCurative(cont.getId(), pst.getId()), pst, solver);
            });

        });
    }

    @Override
    public List<String> variablesProvided() {
        List<String> variablesList = pstElementN.stream().map(gen -> nameShiftValueVariableN(gen.getId()))
                .collect(Collectors.toList());
        cracFile.getContingencies().forEach(cont -> {
            variablesList.addAll(pstElementCurative.stream().map(gen -> nameShiftValueVariableCurative(cont.getId(), gen.getId()))
                    .collect(Collectors.toList()));
        });
        return variablesList;
    }
}
