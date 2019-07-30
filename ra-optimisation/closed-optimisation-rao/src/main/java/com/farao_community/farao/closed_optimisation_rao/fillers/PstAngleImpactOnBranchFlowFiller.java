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
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.fillers.FillersTools.*;
import static com.farao_community.farao.closed_optimisation_rao.fillers.FillersTools.isPstRemedialAction;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class PstAngleImpactOnBranchFlowFiller extends AbstractOptimisationProblemFiller {

    private List<PstElement> pstElementN;
    private List<PstElement> pstElementCurative;


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
    public List<String> variablesExpected() {
        List<String> variablesList = pstElementN.stream().map(gen -> nameShiftValueVariableN(gen.getId()))
                .collect(Collectors.toList());
        cracFile.getContingencies().forEach(cont -> {
            variablesList.addAll(pstElementCurative.stream().map(gen -> nameShiftValueVariableCurative(cont.getId(), gen.getId()))
                    .collect(Collectors.toList()));
        });
        return variablesList;
    }

    @Override
    public List<String> constraintsExpected() {
        List<String> constraintsExpected = new ArrayList<>();
        constraintsExpected.addAll(cracFile.getPreContingency().getMonitoredBranches().stream()
                .map(branch -> nameEstimatedFlowConstraint(branch.getId())).collect(Collectors.toList()));
        constraintsExpected.addAll(cracFile.getContingencies().stream()
                .flatMap(contingency -> contingency.getMonitoredBranches().stream())
                .map(branch -> nameEstimatedFlowConstraint(branch.getId())).collect(Collectors.toList()));
        return constraintsExpected;
    }

    @Override
    public Map<String, Class> dataExpected() {
        Map<String, Class> returnMap = new HashMap<>();
        returnMap.put(PST_SENSITIVITIES_DATA_NAME, Map.class);
        return returnMap;
    }

    @Override
    public void fillProblem(MPSolver solver) {
        Map<Pair<String, String>, Double> sensitivities = (Map<Pair<String, String>, Double>) data.get(PST_SENSITIVITIES_DATA_NAME);

        cracFile.getPreContingency().getMonitoredBranches().forEach(branch -> pstElementN.forEach(pst -> {
            MPVariable pstVariable = Objects.requireNonNull(solver.lookupVariableOrNull(nameShiftValueVariableN((pst.getId()))));
            MPConstraint flowEquation = Objects.requireNonNull(solver.lookupConstraintOrNull(nameEstimatedFlowConstraint(branch.getId())));
            double sensitivity = sensitivities.get(Pair.of(branch.getId(), pst.getId()));
            flowEquation.setCoefficient(pstVariable, -sensitivity);
        }));

        cracFile.getContingencies().forEach(contingency -> {
            contingency.getMonitoredBranches().forEach(branch -> {
                MPConstraint flowEquation = Objects.requireNonNull(solver.lookupConstraintOrNull(nameEstimatedFlowConstraint(branch.getId())));
                pstElementN.forEach(pst -> {
                    MPVariable pstVariable = Objects.requireNonNull(solver.lookupVariableOrNull(nameShiftValueVariableN(pst.getId())));
                    double sensitivity = sensitivities.get(Pair.of(branch.getId(), pst.getId()));
                    flowEquation.setCoefficient(pstVariable, -sensitivity);
                });
                pstElementCurative.forEach(pst -> {
                    MPVariable pstVariable = Objects.requireNonNull(solver.lookupVariableOrNull(nameShiftValueVariableCurative(contingency.getId(),pst.getId())));
                    double sensitivity = sensitivities.get(Pair.of(branch.getId(), pst.getId()));
                    flowEquation.setCoefficient(pstVariable, -sensitivity);
                });

            });
        });
    }
}
