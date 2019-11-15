/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.post_processors;

import com.farao_community.farao.closed_optimisation_rao.OptimisationPostProcessor;
import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.RedispatchRemedialActionElement;
import com.farao_community.farao.data.crac_file.RemedialAction;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.ra_optimisation.RedispatchElementResult;
import com.farao_community.farao.ra_optimisation.RemedialActionResult;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Collections;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.*;
import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoUtil.*;


/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(OptimisationPostProcessor.class)
public class RedispatchElementResultsPostProcessor implements OptimisationPostProcessor {

    @Override
    public Map<String, Class> dataNeeded() {
        return null;
    }

    @Override
    public void fillResults(Network network, CracFile cracFile, MPSolver solver, Map<String, Object> data, RaoComputationResult result) {

        //make map of redispatching remedial actions
        Map<Optional<Contingency>, List<RemedialAction>> redispatchRemedialActions = buildRedispatchRemedialActionMap(cracFile);

        redispatchRemedialActions.forEach((contingency, raList) -> {
            //build result list for each contingency and its associated remedial actions
            List<RemedialActionResult> resultList = raList.stream()
                .filter(ra -> isRedispatchRemedialActionActivated(contingency, ra, solver))
                .map(remedialAction -> {
                    RedispatchRemedialActionElement rrae = (RedispatchRemedialActionElement) remedialAction.getRemedialActionElements().get(0);
                    double initialTargetP = rrae.getTargetPower();
                    MPVariable redispatchCost = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchCostVariable(contingency, remedialAction)));
                    MPVariable redispatchValue = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchValueVariable(contingency, remedialAction)));
                    return new RemedialActionResult(
                            remedialAction.getId(),
                            remedialAction.getName(),
                            true,
                            Collections.singletonList(
                                    new RedispatchElementResult(
                                            rrae.getId(),
                                            initialTargetP,
                                            initialTargetP + redispatchValue.solutionValue(),
                                            redispatchCost.solutionValue()
                                    )
                            )
                    );
                }).collect(Collectors.toList());

            // add result list into result object
            if (!contingency.isPresent()) { // preventive
                result.getPreContingencyResult().getRemedialActionResults().addAll(resultList);
            } else { //curative
                result.getContingencyResults().stream().filter(c -> c.getId().equals(contingency.get().getId()))
                        .findFirst().ifPresent(contingencyResult -> contingencyResult.getRemedialActionResults().addAll(resultList));
            }
        });
    }

    private boolean isRedispatchRemedialActionActivated(Optional<Contingency> contingency, RemedialAction remedialAction, MPSolver solver) {
        MPVariable redispatchActivation = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchActivationVariable(contingency, remedialAction)));
        return redispatchActivation.solutionValue() > 0;
    }
}
