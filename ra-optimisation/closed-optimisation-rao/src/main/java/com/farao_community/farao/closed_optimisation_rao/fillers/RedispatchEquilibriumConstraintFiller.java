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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.fillers.FillersTools.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class RedispatchEquilibriumConstraintFiller extends AbstractOptimisationProblemFiller {

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
    public void fillProblem(MPSolver solver) {
        MPConstraint equilibriumN = solver.makeConstraint(0, 0);
        generatorsRedispatchN.forEach(gen -> {
            MPVariable redispatchValueVariable = Objects.requireNonNull(
                    solver.lookupVariableOrNull(nameRedispatchValueVariableN(gen.getId())));
            equilibriumN.setCoefficient(redispatchValueVariable, 1);
        });

        if (!generatorsRedispatchCurative.isEmpty()) {
            cracFile.getContingencies().forEach(cont -> {
                MPConstraint equilibriumCurative = solver.makeConstraint(0, 0);
                generatorsRedispatchCurative.forEach(gen -> {
                    MPVariable redispatchValueVariable = Objects.requireNonNull(
                            solver.lookupVariableOrNull(nameRedispatchValueVariableCurative(cont.getId(), gen.getId())));
                    equilibriumCurative.setCoefficient(redispatchValueVariable, 1);
                });
            });
        }
    }
}
