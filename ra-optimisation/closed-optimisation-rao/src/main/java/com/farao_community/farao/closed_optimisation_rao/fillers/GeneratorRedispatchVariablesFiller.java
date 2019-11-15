/*
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.*;
import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoUtil.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class GeneratorRedispatchVariablesFiller extends AbstractOptimisationProblemFiller {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorRedispatchVariablesFiller.class);

    private Map<Optional<Contingency>, List<RemedialAction>> redispatchingRemedialActions;

    @Override
    public void initFiller(Network network, CracFile cracFile, Map<String, Object> data) {
        super.initFiller(network, cracFile, data);
        this.redispatchingRemedialActions = buildRedispatchRemedialActionMap(cracFile);
    }

    @Override
    public void fillProblem(MPSolver solver) {
        LOGGER.info("Filling problem using plugin '{}'", getClass().getSimpleName());
        redispatchingRemedialActions.forEach((contingency, raList)  -> {
            raList.forEach(ra -> {
                RedispatchRemedialActionElement rrae = Objects.requireNonNull(getRedispatchElement(ra));
                double pinit = rrae.getTargetPower();
                double pmin = Math.min(pinit, rrae.getMinimumPower());
                double pmax = Math.max(pinit, rrae.getMaximumPower());

                /*
                    - if pmin < pinjt < pmax, the production of the generator must be in [pmin, pmax], therefore
                      a started generator cannot be switched off by farao

                    - if pinit < pmin, the production of the generator must be in [pinit, pmax], thus the generator
                      which are off can :
                         - stay off
                         - or be switched on by farao (note that in this case, farao can - for now - starts the generator
                           below its pmin)

                    - if pinit > pmax, the production of the generation must be in [pmin, pinit]. This case usually
                      indicates an inconsistency in the input data. Yes, to avoid any infeasibility in the optimisation
                      problem the range [pmin, pmax] is extended so as to include the initial value pinit.
                 */

                solver.makeNumVar(pmin - pinit, pmax - pinit, nameRedispatchValueVariable(contingency, ra));
            });
        });
    }

    @Override
    public List<String> variablesProvided() {
        List<String> variables = new ArrayList<>();
        redispatchingRemedialActions.forEach((contingency, raList) -> {
            variables.addAll(raList.stream()
                    .map(ra -> nameRedispatchValueVariable(contingency, ra))
                    .collect(Collectors.toList()));
        });
        return variables;
    }
}
