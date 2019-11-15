/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.closed_optimisation_rao.AbstractOptimisationProblemFiller;
import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.RedispatchRemedialActionElement;
import com.farao_community.farao.data.crac_file.RemedialAction;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Objects;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.*;
import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoUtil.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class GeneratorRedispatchCostsFiller extends AbstractOptimisationProblemFiller {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorRedispatchCostsFiller.class);

    private Map<Optional<Contingency>, List<RemedialAction>> redispatchingRemedialActions;

    @Override
    public void initFiller(Network network, CracFile cracFile, Map<String, Object> data) {
        super.initFiller(network, cracFile, data);
        this.redispatchingRemedialActions = buildRedispatchRemedialActionMap(cracFile);
    }

    @Override
    public List<String> variablesProvided() {
        List<String> variables = new ArrayList<>();
        redispatchingRemedialActions.forEach((contingency, raList) -> {
            variables.addAll(raList.stream()
                    .map(ra -> nameRedispatchActivationVariable(contingency, ra))
                    .collect(Collectors.toList()));
            variables.addAll(raList.stream()
                    .map(ra -> nameRedispatchCostVariable(contingency, ra))
                    .collect(Collectors.toList()));
        });
        variables.add(TOTAL_REDISPATCH_COST);
        return variables;
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
    public void fillProblem(MPSolver solver) {
        LOGGER.info("Filling problem using plugin '{}'", getClass().getSimpleName());
        double infinity = MPSolver.infinity();
        // Create total redispatch cost and its equation
        MPVariable totalRedispatchCostVariable = solver.makeNumVar(-infinity, infinity, TOTAL_REDISPATCH_COST);
        MPConstraint totalRedispatchCostEquation = solver.makeConstraint(0, 0);
        totalRedispatchCostEquation.setCoefficient(totalRedispatchCostVariable, 1);

        redispatchingRemedialActions.forEach((contingency, raList)  -> {
            raList.forEach(ra -> {
                RedispatchRemedialActionElement rrae = Objects.requireNonNull(getRedispatchElement(ra));
                MPVariable redispatchValueVariable = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchValueVariable(contingency, ra)));

                // Redispatch activation variable
                MPVariable redispatchActivationVariable = solver.makeBoolVar(nameRedispatchActivationVariable(contingency, ra));

                // Redispatch cost variable
                MPVariable redispatchCostVariable = solver.makeNumVar(-infinity, infinity, nameRedispatchCostVariable(contingency, ra));

                // Redispatch cost equation
                MPConstraint costEquation = solver.makeConstraint(0, 0);
                costEquation.setCoefficient(redispatchCostVariable, 1);
                costEquation.setCoefficient(redispatchActivationVariable, -rrae.getStartupCost());
                costEquation.setCoefficient(redispatchValueVariable, -rrae.getMarginalCost());

                // Total redispatch cost participation
                totalRedispatchCostEquation.setCoefficient(redispatchCostVariable, -1);

                // Constraint for enforcing redispatch to be 0 when not activated
                MPConstraint constraintLow = solver.makeConstraint(0, infinity);
                constraintLow.setCoefficient(redispatchValueVariable, 1);
                constraintLow.setCoefficient(redispatchActivationVariable, -redispatchValueVariable.lb());
                MPConstraint constraintUp = solver.makeConstraint(-infinity, 0);
                constraintUp.setCoefficient(redispatchValueVariable, 1);
                constraintUp.setCoefficient(redispatchActivationVariable, -redispatchValueVariable.ub());
            });
        });

    }
}
