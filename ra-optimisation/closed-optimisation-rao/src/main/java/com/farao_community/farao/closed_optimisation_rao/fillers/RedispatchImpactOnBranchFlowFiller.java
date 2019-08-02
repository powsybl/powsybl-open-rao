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
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.data.crac_file.RedispatchRemedialActionElement;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.*;
import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoUtil.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class RedispatchImpactOnBranchFlowFiller extends AbstractOptimisationProblemFiller {

    private Map<Optional<Contingency>, List<RedispatchRemedialActionElement>> redispatchingRemedialActions;

    @Override
    public void initFiller(Network network, CracFile cracFile, Map<String, Object> data) {
        super.initFiller(network, cracFile, data);
        this.redispatchingRemedialActions = buildRedispatchRemedialActionMap(cracFile);
    }

    @Override
    public List<String> variablesExpected() {
        List<String> variables = new ArrayList<>();
        redispatchingRemedialActions.forEach((contingency, raList) -> {
            variables.addAll(raList.stream()
                    .map(gen -> nameRedispatchValueVariable(contingency, gen))
                    .collect(Collectors.toList()));
        });
        return variables;
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

        redispatchingRemedialActions.forEach((contingency, raList) -> {
            if (!contingency.isPresent()) {
                // impact of preventive remedial actions on preContingency flows
                cracFile.getPreContingency().getMonitoredBranches().forEach(branch -> raList.forEach(rrae -> {
                    fillImpactOfRedispatchingRemedialActionOnBranch(contingency, rrae, branch, solver, sensitivities);
                }));
                // impact of preventive remedial actions on all N-1 flows
                cracFile.getContingencies().forEach(cont -> {
                    cont.getMonitoredBranches().forEach(branch -> raList.forEach(rrae -> {
                        fillImpactOfRedispatchingRemedialActionOnBranch(contingency, rrae, branch, solver, sensitivities);
                    }));
                });
            } else {
                // impact of curative remedial actions on associated N-1 flows
                contingency.get().getMonitoredBranches().forEach(branch -> raList.forEach(rrae -> {
                    fillImpactOfRedispatchingRemedialActionOnBranch(contingency, rrae, branch, solver, sensitivities);
                }));
            }
        });
    }

    public void fillImpactOfRedispatchingRemedialActionOnBranch(Optional<Contingency> contingency, RedispatchRemedialActionElement rrae, MonitoredBranch branch, MPSolver solver, Map<Pair<String, String>, Double> sensitivities) {
        MPVariable redispatchVariable = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchValueVariable(contingency, rrae)));
        MPConstraint flowEquation = Objects.requireNonNull(solver.lookupConstraintOrNull(nameEstimatedFlowConstraint(branch.getId())));
        double sensitivity = sensitivities.get(Pair.of(branch.getId(), rrae.getId()));
        flowEquation.setCoefficient(redispatchVariable, -sensitivity);
    }
}
