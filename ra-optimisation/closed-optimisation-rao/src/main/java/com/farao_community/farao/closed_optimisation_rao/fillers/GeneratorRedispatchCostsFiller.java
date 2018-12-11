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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class GeneratorRedispatchCostsFiller extends AbstractOptimisationProblemFiller {
    private static final String REDISPATCH_VALUE_POSTFIX = "_redispatch_value";
    private static final String REDISPATCH_ACTIVATION_POSTFIX = "_redispatch_activation";
    private static final String REDISPATCH_COST_POSTFIX = "_redispatch_cost";
    private static final String TOTAL_REDISPATCH_COST = "total_redispatch_cost";

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
    public List<String> variablesProvided() {
        List<String> variables = new ArrayList<>();
        variables.addAll(generatorsRedispatch.stream().map(gen -> gen.getId() + REDISPATCH_ACTIVATION_POSTFIX)
                .collect(Collectors.toList()));
        variables.addAll(generatorsRedispatch.stream().map(gen -> gen.getId() + REDISPATCH_COST_POSTFIX)
                .collect(Collectors.toList()));
        variables.add(TOTAL_REDISPATCH_COST);
        return variables;
    }

    @Override
    public List<String> variablesExpected() {
        return generatorsRedispatch.stream().map(gen -> gen.getId() + REDISPATCH_VALUE_POSTFIX)
                .collect(Collectors.toList());
    }

    @Override
    public void fillProblem(MPSolver solver) {
        double infinity = MPSolver.infinity();
        // Create total redispatch cost and its equation
        MPVariable totalRedispatchCostVariable = solver.makeNumVar(-infinity, infinity, TOTAL_REDISPATCH_COST);
        MPConstraint totalRedispatchCostEquation = solver.makeConstraint(0, 0);
        totalRedispatchCostEquation.setCoefficient(totalRedispatchCostVariable, 1);

        generatorsRedispatch.forEach(gen -> {
            String genId = gen.getId();
            MPVariable redispatchValueVariable = Objects.requireNonNull(solver.lookupVariableOrNull(gen.getId() + REDISPATCH_VALUE_POSTFIX));

            // Redispatch activation variable
            MPVariable redispatchActivationVariable = solver.makeBoolVar(genId + REDISPATCH_ACTIVATION_POSTFIX);

            // Redispatch cost variable
            MPVariable redispatchCostVariable = solver.makeNumVar(-infinity, infinity, genId + REDISPATCH_COST_POSTFIX);

            // Redispatch cost equation
            MPConstraint costEquation = solver.makeConstraint(0, 0);
            costEquation.setCoefficient(redispatchCostVariable, 1);
            costEquation.setCoefficient(redispatchActivationVariable, -gen.getStartupCost());
            costEquation.setCoefficient(redispatchValueVariable, -gen.getMarginalCost());

            // Total redispatch cost participation
            totalRedispatchCostEquation.setCoefficient(redispatchCostVariable, -1);

            // Constraint for enforcing redispatch to be 0 when not activated
            MPConstraint constraintLow = solver.makeConstraint(0, infinity);
            constraintLow.setCoefficient(redispatchValueVariable, 1);
            constraintLow.setCoefficient(redispatchActivationVariable, -gen.getMinimumPower());
            MPConstraint constraintUp = solver.makeConstraint(-infinity, 0);
            constraintUp.setCoefficient(redispatchValueVariable, 1);
            constraintUp.setCoefficient(redispatchActivationVariable, -gen.getMaximumPower());
        });
    }
}
