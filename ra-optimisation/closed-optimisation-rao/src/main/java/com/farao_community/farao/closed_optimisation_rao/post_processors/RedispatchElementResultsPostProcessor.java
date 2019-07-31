/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.post_processors;

import com.farao_community.farao.closed_optimisation_rao.OptimisationPostProcessor;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.RedispatchRemedialActionElement;
import com.farao_community.farao.data.crac_file.RemedialAction;
import com.farao_community.farao.ra_optimisation.ContingencyResult;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.ra_optimisation.RedispatchElementResult;
import com.farao_community.farao.ra_optimisation.RemedialActionResult;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.fillers.FillersTools.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(OptimisationPostProcessor.class)
public class RedispatchElementResultsPostProcessor implements OptimisationPostProcessor {

    private static final String REDISPATCH_VALUE_POSTFIX = "_redispatch_value";
    private static final String REDISPATCH_ACTIVATION_POSTFIX = "_redispatch_activation";
    private static final String REDISPATCH_COST_POSTFIX = "_redispatch_cost";

    @Override
    public Map<String, Class> dataNeeded() {
        return null;
    }

    @Override
    public void fillResults(Network network, CracFile cracFile, MPSolver solver, Map<String, Object> data, RaoComputationResult result) {

        List<RemedialActionResult> preventiveRemedialActionsResult = cracFile.getRemedialActions().stream()
                .filter(ra -> isRedispatchRemedialAction(ra))
                .filter(ra -> isRemedialActionPreventiveFreeToUse(ra))
                .filter(ra -> isPreventiveRemedialActionActivated(ra, solver))
                .map(remedialAction -> {
                    RedispatchRemedialActionElement rrae = (RedispatchRemedialActionElement) remedialAction.getRemedialActionElements().get(0);
                    double initialTargetP = network.getGenerator(rrae.getId()).getTargetP();
                    MPVariable redispatchCost = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchCostVariableN(rrae.getId())));
                    MPVariable redispatchValue = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchValueVariableN(rrae.getId())));
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
                })
                .collect(Collectors.toList());
        result.getPreContingencyResult().getRemedialActionResults().addAll(preventiveRemedialActionsResult);

        result.getContingencyResults().forEach(contingency -> {
            List<RemedialActionResult> curativeRemedialActionsResult = cracFile.getRemedialActions().stream()
                    .filter(ra -> isRedispatchRemedialAction(ra))
                    .filter(ra -> isRemedialActionCurativeFreeToUse(ra))
                    .filter(ra -> isCurativeRemedialActionActivated(contingency, ra, solver))
                    .map(remedialAction -> {
                        RedispatchRemedialActionElement rrae = (RedispatchRemedialActionElement) remedialAction.getRemedialActionElements().get(0);
                        double initialTargetP = network.getGenerator(rrae.getId()).getTargetP();
                        MPVariable redispatchCost = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchCostVariableCurative(contingency.getId(), rrae.getId())));
                        MPVariable redispatchValue = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchValueVariableCurative(contingency.getId(), rrae.getId())));
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
                    })
                    .collect(Collectors.toList());
            contingency.getRemedialActionResults().addAll(curativeRemedialActionsResult);

        });
    }

    private boolean isPreventiveRemedialActionActivated(RemedialAction remedialAction, MPSolver solver) {
        RedispatchRemedialActionElement rrae = (RedispatchRemedialActionElement) remedialAction.getRemedialActionElements().get(0);
        MPVariable redispatchActivation = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchActivationVariableN(rrae.getId())));
        return redispatchActivation.solutionValue() > 0;
    }

    private boolean isCurativeRemedialActionActivated(ContingencyResult contingency, RemedialAction remedialAction, MPSolver solver) {
        RedispatchRemedialActionElement rrae = (RedispatchRemedialActionElement) remedialAction.getRemedialActionElements().get(0);
        MPVariable redispatchActivation = Objects.requireNonNull(solver.lookupVariableOrNull(nameRedispatchActivationVariableCurative(contingency.getId(), rrae.getId())));
        return redispatchActivation.solutionValue() > 0;
    }
}
