/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.post_processors;

import com.farao_community.farao.closed_optimisation_rao.OptimisationPostProcessor;
import com.farao_community.farao.closed_optimisation_rao.fillers.FillersTools;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.PstElement;
import com.farao_community.farao.data.crac_file.RedispatchRemedialActionElement;
import com.farao_community.farao.data.crac_file.RemedialAction;
import com.farao_community.farao.ra_optimisation.ContingencyResult;
import com.farao_community.farao.ra_optimisation.PstElementResult;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.ra_optimisation.RemedialActionResult;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.fillers.FillersTools.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(OptimisationPostProcessor.class)
public class PstElementResultsPostProcessor implements OptimisationPostProcessor {

    private static final double EPSILON = 1e-3;

    @Override
    public Map<String, Class> dataNeeded() {
        return null;
    }

    @Override
    public void fillResults(Network network, CracFile cracFile, MPSolver solver, Map<String, Object> data, RaoComputationResult result) {
        List<RemedialActionResult> preventiveRemedialActionsResult = cracFile.getRemedialActions().stream()
                .filter(this::isPstRemedialAction)
                .filter(ra -> isRemedialActionPreventiveFreeToUse(ra))
                .filter(ra -> isPreventiveRemedialActionActivated(ra, solver))
                .map(remedialAction -> {
                    PstElement prae = (PstElement) remedialAction.getRemedialActionElements().get(0);
                    PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(prae.getId()).getPhaseTapChanger();
                    double initialAngle = phaseTapChanger.getCurrentStep().getAlpha();
                    int initialTapPosition = phaseTapChanger.getTapPosition();
                    MPVariable angleValue = Objects.requireNonNull(solver.lookupVariableOrNull(nameShiftValueVariableN(prae.getId())));
                    double finalAngle = initialAngle + angleValue.solutionValue();
                    int finalTapPosition = computeTapPosition(finalAngle, phaseTapChanger, network.getTwoWindingsTransformer(prae.getId()));
                    return new RemedialActionResult(
                            remedialAction.getId(),
                            remedialAction.getName(),
                            true,
                            Collections.singletonList(
                                    new PstElementResult(
                                            prae.getId(),
                                            initialAngle,
                                            initialTapPosition,
                                            finalAngle,
                                            finalTapPosition
                                    )
                            )
                    );
                })
                .collect(Collectors.toList());
        result.getPreContingencyResult().getRemedialActionResults().addAll(preventiveRemedialActionsResult);


        result.getContingencyResults().forEach(contingency -> {
            List<RemedialActionResult> curativeRemedialActionsResult = cracFile.getRemedialActions().stream()
                    .filter(this::isPstRemedialAction)
                    .filter(ra -> isRemedialActionCurativeFreeToUse(ra))
                    .filter(ra -> isCurativeRemedialActionActivated(contingency, ra, solver))
                    .map(remedialAction -> {
                        PstElement prae = (PstElement) remedialAction.getRemedialActionElements().get(0);
                        PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(prae.getId()).getPhaseTapChanger();
                        double initialAngle = phaseTapChanger.getCurrentStep().getAlpha();
                        int initialTapPosition = phaseTapChanger.getTapPosition();
                        MPVariable angleValue = Objects.requireNonNull(solver.lookupVariableOrNull(nameShiftValueVariableCurative(contingency.getId(),prae.getId())));
                        double finalAngle = initialAngle + angleValue.solutionValue();
                        int finalTapPosition = computeTapPosition(finalAngle, phaseTapChanger, network.getTwoWindingsTransformer(prae.getId()));
                        return new RemedialActionResult(
                                remedialAction.getId(),
                                remedialAction.getName(),
                                true,
                                Collections.singletonList(
                                        new PstElementResult(
                                                prae.getId(),
                                                initialAngle,
                                                initialTapPosition,
                                                finalAngle,
                                                finalTapPosition
                                        )
                                )
                        );
                    })
                    .collect(Collectors.toList());
            contingency.getRemedialActionResults().addAll(curativeRemedialActionsResult);
        });
    }

    private int computeTapPosition(double finalAngle, PhaseTapChanger phaseTapChanger, TwoWindingsTransformer twoWindingsTransformer) {
        Map<Integer, PhaseTapChangerStep> steps = new TreeMap<>();
        for (int tapPosition = phaseTapChanger.getLowTapPosition(); tapPosition <= phaseTapChanger.getHighTapPosition(); tapPosition++) {
            steps.put(tapPosition, phaseTapChanger.getStep(tapPosition));
        }
        double minAngle = steps.values().stream().mapToDouble(PhaseTapChangerStep::getAlpha).min().orElse(Double.NaN);
        double maxAngle = steps.values().stream().mapToDouble(PhaseTapChangerStep::getAlpha).max().orElse(Double.NaN);
        if (Double.isNaN(minAngle) || Double.isNaN(maxAngle)) {
            throw new FaraoException(String.format("Phase tap changer %s steps may be invalid", twoWindingsTransformer.getId()));
        }

        // Modification of the range limitation control allowing the final angle to exceed of an EPSILON value the limitation.
        if (finalAngle < minAngle && Math.abs(finalAngle - minAngle) > EPSILON || finalAngle > maxAngle && Math.abs(finalAngle - maxAngle) > EPSILON) {
            throw new FaraoException(String.format("Angle value %.4f not is the range of minimum and maximum angle values [%.4f,%.4f] of the phase tap changer %s steps", finalAngle, minAngle, maxAngle, twoWindingsTransformer.getId()));
        }
        AtomicReference<Double> angleDifference = new AtomicReference<>(Double.MAX_VALUE);
        AtomicInteger approximatedTapPosition = new AtomicInteger(phaseTapChanger.getTapPosition());
        steps.forEach((tapPosition, step) -> {
            double diff = Math.abs(step.getAlpha() - finalAngle);
            if (diff < angleDifference.get()) {
                angleDifference.set(diff);
                approximatedTapPosition.set(tapPosition);
            }
        });
        return approximatedTapPosition.get();
    }

    private boolean isPstRemedialAction(RemedialAction remedialAction) {
        return remedialAction.getRemedialActionElements().size() == 1 &&
                remedialAction.getRemedialActionElements().get(0) instanceof PstElement;
    }


    private boolean isPreventiveRemedialActionActivated(RemedialAction remedialAction, MPSolver solver) {
        PstElement prae = (PstElement) remedialAction.getRemedialActionElements().get(0);
        MPVariable redispatchActivation = Objects.requireNonNull(solver.lookupVariableOrNull(nameShiftValueVariableN(prae.getId())));
        return redispatchActivation.solutionValue() > 0;
    }

    private boolean isCurativeRemedialActionActivated(ContingencyResult contingency, RemedialAction remedialAction, MPSolver solver) {
        PstElement prae = (PstElement) remedialAction.getRemedialActionElements().get(0);
        MPVariable redispatchActivation = Objects.requireNonNull(solver.lookupVariableOrNull(nameShiftValueVariableCurative(contingency.getId(), prae.getId())));
        return redispatchActivation.solutionValue() > 0;
    }
}
