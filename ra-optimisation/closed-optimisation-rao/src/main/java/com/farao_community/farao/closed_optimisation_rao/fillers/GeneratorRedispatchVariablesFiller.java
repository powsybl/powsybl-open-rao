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
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class GeneratorRedispatchVariablesFiller extends AbstractOptimisationProblemFiller {
    private static final String REDISPATCH_VALUE_POSTFIX = "_redispatch_value";

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
    public void fillProblem(MPSolver solver) {
        generatorsRedispatch.forEach(gen -> {
            double pmin = gen.getMinimumPower();
            double pmax = gen.getMaximumPower();
            double pinit = -network.getGenerator(gen.getId()).getTerminal().getP();
            solver.makeNumVar(pmin - pinit, pmax - pinit, gen.getId() + REDISPATCH_VALUE_POSTFIX);
        });
    }

    @Override
    public List<String> variablesProvided() {
        return generatorsRedispatch.stream().map(gen -> gen.getId() + REDISPATCH_VALUE_POSTFIX)
                .collect(Collectors.toList());
    }
}
