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

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class RedispatchImpactOnBranchFlowFiller extends AbstractOptimisationProblemFiller {
    private static final String ESTIMATED_FLOW_EQUATION_POSTFIX = "_estimated_flow_equation";
    private static final String REDISPATCH_VALUE_POSTFIX = "_redispatch_value";
    private static final String GEN_SENSITIVITIES_DATA_NAME = "generators_branch_sensitivities";

    private List<RedispatchRemedialActionElement> generatorsRedispatch;

    /**
     * Check if the remedial action is a Redispatch remedial action (i.e. with only
     * one remedial action element and redispatch)
     */
    private boolean isRedispatchRemedialAction(RemedialAction remedialAction) {
        return remedialAction.getRemedialActionElements().size() == 1 &&
                remedialAction.getRemedialActionElements().get(0) instanceof RedispatchRemedialActionElement;
    }

    @Override
    public void initFiller(Network network, CracFile cracFile, Map<String, Object> data) {
        super.initFiller(network, cracFile, data);
        this.generatorsRedispatch = cracFile.getRemedialActions().stream()
                .filter(this::isRedispatchRemedialAction)
                .flatMap(remedialAction -> remedialAction.getRemedialActionElements().stream())
                .map(remedialActionElement -> (RedispatchRemedialActionElement) remedialActionElement)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> variablesExpected() {
        return generatorsRedispatch.stream().map(rae -> rae.getId() + REDISPATCH_VALUE_POSTFIX)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> constraintsExpected() {
        List<String> constraintsExpected = new ArrayList<>();
        constraintsExpected.addAll(cracFile.getPreContingency().getMonitoredBranches().stream()
                .map(branch -> branch.getId() + ESTIMATED_FLOW_EQUATION_POSTFIX).collect(Collectors.toList()));
        constraintsExpected.addAll(cracFile.getContingencies().stream()
                .flatMap(contingency -> contingency.getMonitoredBranches().stream())
                .map(branch -> branch.getId() + ESTIMATED_FLOW_EQUATION_POSTFIX).collect(Collectors.toList()));
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

        cracFile.getPreContingency().getMonitoredBranches().forEach(branch -> generatorsRedispatch.forEach(gen -> {
            MPVariable redispatchVariable = Objects.requireNonNull(solver.lookupVariableOrNull(gen.getId() + REDISPATCH_VALUE_POSTFIX));
            MPConstraint flowEquation = Objects.requireNonNull(solver.lookupConstraintOrNull(branch.getId() + ESTIMATED_FLOW_EQUATION_POSTFIX));
            double sensitivity = sensitivities.get(Pair.of(branch.getId(), gen.getId()));
            flowEquation.setCoefficient(redispatchVariable, -sensitivity);
        }));
        cracFile.getContingencies().stream()
                .flatMap(contingency -> contingency.getMonitoredBranches().stream())
                .forEach(branch -> generatorsRedispatch.forEach(gen -> {
                    MPVariable redispatchVariable = Objects.requireNonNull(solver.lookupVariableOrNull(gen.getId() + REDISPATCH_VALUE_POSTFIX));
                    MPConstraint flowEquation = Objects.requireNonNull(solver.lookupConstraintOrNull(branch.getId() + ESTIMATED_FLOW_EQUATION_POSTFIX));
                    double sensitivity = sensitivities.get(Pair.of(branch.getId(), gen.getId()));
                    flowEquation.setCoefficient(redispatchVariable, -sensitivity);
                }));
    }
}
