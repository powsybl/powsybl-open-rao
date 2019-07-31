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

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.*;
import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoUtil.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class GeneratorRedispatchCostsFiller extends AbstractOptimisationProblemFiller {

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
    public List<String> variablesProvided() {
        List<String> variables = new ArrayList<>();
        variables.addAll(generatorsRedispatchN.stream().map(gen -> nameRedispatchActivationVariableN(gen.getId()))
                .collect(Collectors.toList()));
        variables.addAll(generatorsRedispatchN.stream().map(gen -> nameRedispatchCostVariableN(gen.getId()))
                .collect(Collectors.toList()));
        cracFile.getContingencies().forEach(cont -> {
            variables.addAll(generatorsRedispatchCurative.stream().map(gen -> nameRedispatchActivationVariableCurative(cont.getId(), gen.getId()))
                    .collect(Collectors.toList()));
        });
        cracFile.getContingencies().forEach(cont -> {
            variables.addAll(generatorsRedispatchCurative.stream().map(gen -> nameRedispatchCostVariableCurative(cont.getId(), gen.getId()))
                    .collect(Collectors.toList()));
        });

        variables.add(TOTAL_REDISPATCH_COST);

        return variables;
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
    public void fillProblem(MPSolver solver) {
        double infinity = MPSolver.infinity();
        // Create total redispatch cost and its equation
        MPVariable totalRedispatchCostVariable = solver.makeNumVar(-infinity, infinity, TOTAL_REDISPATCH_COST);
        MPConstraint totalRedispatchCostEquation = solver.makeConstraint(0, 0);
        totalRedispatchCostEquation.setCoefficient(totalRedispatchCostVariable, 1);

        generatorsRedispatchN.forEach(gen -> {
            String genId = gen.getId();
            MPVariable redispatchValueVariable = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchValueVariableN(genId)));

            // Redispatch activation variable
            MPVariable redispatchActivationVariable = solver.makeBoolVar(nameRedispatchActivationVariableN(genId));

            // Redispatch cost variable
            MPVariable redispatchCostVariable = solver.makeNumVar(-infinity, infinity, nameRedispatchCostVariableN(genId));

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
            constraintLow.setCoefficient(redispatchActivationVariable, -redispatchValueVariable.lb());
            MPConstraint constraintUp = solver.makeConstraint(-infinity, 0);
            constraintUp.setCoefficient(redispatchValueVariable, 1);
            constraintUp.setCoefficient(redispatchActivationVariable, -redispatchValueVariable.ub());
        });

        cracFile.getContingencies().forEach(cont -> {
            String contId = cont.getId();
            generatorsRedispatchCurative.forEach(gen -> {

                String genId = gen.getId();
                MPVariable redispatchValueVariable = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchValueVariableCurative(contId, genId)));

                // Redispatch activation variable
                MPVariable redispatchActivationVariable = solver.makeBoolVar(nameRedispatchActivationVariableCurative(contId, genId));

                // Redispatch cost variable
                MPVariable redispatchCostVariable = solver.makeNumVar(-infinity, infinity, nameRedispatchCostVariableCurative(contId, genId));

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
                constraintLow.setCoefficient(redispatchActivationVariable, -redispatchValueVariable.lb());
                MPConstraint constraintUp = solver.makeConstraint(-infinity, 0);
                constraintUp.setCoefficient(redispatchValueVariable, 1);
                constraintUp.setCoefficient(redispatchActivationVariable, -redispatchValueVariable.ub());
            });
        });
    }
}
