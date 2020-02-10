/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.closed_optimisation_rao.AbstractOptimisationProblemFiller;
import com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames;
import com.farao_community.farao.data.crac_file.*;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(RedispatchImpactOnBranchFlowFiller.class);

    private Map<Optional<Contingency>, List<RemedialAction>> redispatchingRemedialActions;

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
                    .map(ra -> nameRedispatchValueVariable(contingency, ra))
                    .collect(Collectors.toList()));
        });
        return variables;
    }

    @Override
    public List<String> constraintsExpected() {
        List<MonitoredBranch> monitoredBranches = getAllMonitoredBranches(cracFile);
        return monitoredBranches.stream().map(ClosedOptimisationRaoNames::nameEstimatedFlowConstraint).collect(Collectors.toList());
    }

    @Override
    public Map<String, Class> dataExpected() {
        Map<String, Class> returnMap = new HashMap<>();
        returnMap.put(GEN_SENSITIVITIES_DATA_NAME, Map.class);
        return returnMap;
    }

    @Override
    public void fillProblem(MPSolver solver) {
        LOGGER.info("Filling problem using plugin '{}'", getClass().getSimpleName());
        Map<Pair<String, String>, Double> sensitivities = (Map<Pair<String, String>, Double>) data.get(GEN_SENSITIVITIES_DATA_NAME);
        Map<String, Object> optimisationConstants = (Map<String, Object>) data.get(OPTIMISATION_CONSTANTS_DATA_NAME);
        double sensiThreshold = (Double) optimisationConstants.get(RD_SENSITIVITY_SIGNIFICANCE_THRESHOLD_NAME);

        redispatchingRemedialActions.forEach((contingency, raList) -> {
            if (!contingency.isPresent()) {
                // impact of preventive remedial actions on preContingency flows
                cracFile.getPreContingency().getMonitoredBranches().forEach(branch -> raList.forEach(ra -> {
                    fillImpactOfRedispatchingRemedialActionOnBranch(contingency, ra, branch, solver, sensitivities, sensiThreshold);
                }));
                // impact of preventive remedial actions on all N-1 flows
                cracFile.getContingencies().forEach(cont -> {
                    cont.getMonitoredBranches().forEach(branch -> raList.forEach(ra -> {
                        fillImpactOfRedispatchingRemedialActionOnBranch(contingency, ra, branch, solver, sensitivities, sensiThreshold);
                    }));
                });
            } else {
                // impact of curative remedial actions on associated N-1 flows
                contingency.get().getMonitoredBranches().forEach(branch -> raList.forEach(ra -> {
                    fillImpactOfRedispatchingRemedialActionOnBranch(contingency, ra, branch, solver, sensitivities, sensiThreshold);
                }));
            }
        });
    }

    private void fillImpactOfRedispatchingRemedialActionOnBranch(Optional<Contingency> contingency, RemedialAction ra, MonitoredBranch branch, MPSolver solver, Map<Pair<String, String>, Double> sensitivities, double sensiThreshold) {
        double sensitivity = sensitivities.getOrDefault(Pair.of(branch.getId(), Objects.requireNonNull(getRedispatchElement(ra)).getId()), 0.0);

        if (isSignificant(sensitivity, sensiThreshold)) {
            MPVariable redispatchVariable = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchValueVariable(contingency, ra)));
            MPConstraint flowEquation = Objects.requireNonNull(solver.lookupConstraintOrNull(nameEstimatedFlowConstraint(branch)));
            flowEquation.setCoefficient(redispatchVariable, -sensitivity);
        }
    }
}
