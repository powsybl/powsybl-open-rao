/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.closed_optimisation_rao.AbstractOptimisationProblemFiller;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.RedispatchRemedialActionElement;
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
import static com.farao_community.farao.closed_optimisation_rao.fillers.FillersTools.nameEstimatedFlowConstraint;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class RedispatchImpactOnBranchFlowFiller extends AbstractOptimisationProblemFiller {

    private List<RedispatchRemedialActionElement> generatorsRedispatchN;
    private List<RedispatchRemedialActionElement> generatorsRedispatchCurative;


    @Override
    public void initFiller(Network network, CracFile cracFile, Map<String, Object> data) {
        super.initFiller(network, cracFile, data);
        this.generatorsRedispatchN = cracFile.getRemedialActions().stream()
                .filter(ra -> isRedispatchRemedialAction(ra)).filter(ra -> isRemedialActionPreventiveFreeToUse(ra))
                .flatMap(remedialAction -> remedialAction.getRemedialActionElements().stream())
                .map(remedialActionElement -> (RedispatchRemedialActionElement) remedialActionElement)
                .collect(Collectors.toList());

        this.generatorsRedispatchCurative = cracFile.getRemedialActions().stream()
                .filter(ra -> isRedispatchRemedialAction(ra)).filter(ra -> isRemedialActionCurativeFreeToUse(ra))
                .flatMap(remedialAction -> remedialAction.getRemedialActionElements().stream())
                .map(remedialActionElement -> (RedispatchRemedialActionElement) remedialActionElement)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> variablesExpected() {
        List<String> variablesList = generatorsRedispatchN.stream().map(gen -> nameRedispatchValueVariableN(gen.getId()))
                .collect(Collectors.toList());

        cracFile.getContingencies().forEach(cont -> {
            variablesList.addAll(generatorsRedispatchCurative.stream().map(gen -> nameRedispatchValueVariableCurative(cont.getId(), gen.getId()))
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
        returnMap.put(GEN_SENSITIVITIES_DATA_NAME, Map.class);
        return returnMap;
    }

    @Override
    public void fillProblem(MPSolver solver) {
        Map<Pair<String, String>, Double> sensitivities = (Map<Pair<String, String>, Double>) data.get(GEN_SENSITIVITIES_DATA_NAME);

        cracFile.getPreContingency().getMonitoredBranches().forEach(branch -> generatorsRedispatchN.forEach(gen -> {
            MPVariable redispatchVariable = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchValueVariableN(gen.getId())));
            MPConstraint flowEquation = Objects.requireNonNull(solver.lookupConstraintOrNull(nameEstimatedFlowConstraint(branch.getId())));
            double sensitivity = sensitivities.get(Pair.of(branch.getId(), gen.getId()));
            flowEquation.setCoefficient(redispatchVariable, -sensitivity);
        }));

        cracFile.getContingencies().forEach( contingency -> {
            contingency.getMonitoredBranches().forEach(branch -> {
                MPConstraint flowEquation = Objects.requireNonNull(solver.lookupConstraintOrNull(nameEstimatedFlowConstraint(branch.getId())));
                generatorsRedispatchN.forEach(gen -> {
                    MPVariable redispatchVariable = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchValueVariableN(gen.getId())));
                    double sensitivity = sensitivities.get(Pair.of(branch.getId(), gen.getId()));
                    flowEquation.setCoefficient(redispatchVariable, -sensitivity);
                });
                generatorsRedispatchCurative.forEach(gen -> {
                    MPVariable redispatchVariable = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchValueVariableCurative(branch.getId(), gen.getId())));
                    double sensitivity = sensitivities.get(Pair.of(branch.getId(), gen.getId()));
                    flowEquation.setCoefficient(redispatchVariable, -sensitivity);
                });
            });
        });
    }
}
