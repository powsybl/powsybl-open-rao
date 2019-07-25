/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.closed_optimisation_rao.AbstractOptimisationProblemFiller;
import com.farao_community.farao.data.crac_file.*;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.fillers.FillersTools.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class GeneratorRedispatchVariablesFiller extends AbstractOptimisationProblemFiller {

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
    public void fillProblem(MPSolver solver) {
        generatorsRedispatchN.forEach(gen -> {
            double pmin = gen.getMinimumPower();
            double pmax = gen.getMaximumPower();
            double pinit = -network.getGenerator(gen.getId()).getTerminal().getP();
            solver.makeNumVar(pmin - pinit, pmax - pinit, gen.getId() + REDISPATCH_VALUE_N_POSTFIX);
        });

        cracFile.getContingencies().stream().forEach(contingency -> {
            generatorsRedispatchCurative.forEach(gen -> {
                double pmin = gen.getMinimumPower();
                double pmax = gen.getMaximumPower();
                double pinit = -network.getGenerator(gen.getId()).getTerminal().getP();
                solver.makeNumVar(pmin - pinit, pmax - pinit, contingency.getId() + BLANK_CHARACTER + gen.getId() + REDISPATCH_VALUE_CURATIVE_POSTFIX);
            });

        });
    }

    @Override
    public List<String> variablesProvided() {
        List<String> variablesList = generatorsRedispatchN.stream().map(gen -> gen.getId() + REDISPATCH_VALUE_N_POSTFIX)
                .collect(Collectors.toList());

        cracFile.getContingencies().stream().forEach(cont -> {
            variablesList.addAll(generatorsRedispatchCurative.stream().map(gen -> cont.getId() + BLANK_CHARACTER + gen.getId() + REDISPATCH_VALUE_CURATIVE_POSTFIX)
                    .collect(Collectors.toList()));
        });

        return variablesList;
    }
}
