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
import com.farao_community.farao.data.crac_file.RedispatchRemedialActionElement;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;

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
public class RedispatchEquilibriumConstraintFiller extends AbstractOptimisationProblemFiller {

    private HashMap<Optional<Contingency>, List<RedispatchRemedialActionElement>> redispatchingRemedialActions;

    @Override
    public void initFiller(Network network, CracFile cracFile, Map<String, Object> data) {
        super.initFiller(network, cracFile, data);
        this.redispatchingRemedialActions = new HashMap<>();
        // add preventive redispatching remedial actions
        this.redispatchingRemedialActions.put(Optional.empty(), getRedispatchRemedialActionElement(getPreventiveRemedialActions(cracFile)));
        // add curative redispatching remedial actions
        cracFile.getContingencies().forEach(contingency -> this.redispatchingRemedialActions.put(Optional.of(contingency),
                getRedispatchRemedialActionElement(getCurativeRemedialActions(cracFile, contingency))));
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
    public void fillProblem(MPSolver solver) {
        redispatchingRemedialActions.forEach((contingency, raList)  -> {
            MPConstraint equilibrium = solver.makeConstraint(0, 0);
            raList.forEach(rrae -> {
                MPVariable redispatchValueVariable = Objects.requireNonNull(
                        solver.lookupVariableOrNull(nameRedispatchValueVariable(contingency, rrae)));
                equilibrium.setCoefficient(redispatchValueVariable, 1);
            });
        });
    }
}
