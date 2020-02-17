/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.post_processors;

import com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoUtil;
import com.farao_community.farao.closed_optimisation_rao.OptimisationPostProcessor;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.PstElement;
import com.farao_community.farao.data.crac_file.RemedialAction;
import com.farao_community.farao.ra_optimisation.PstElementResult;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.ra_optimisation.RemedialActionResult;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.TreeMap;


import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.nameShiftValueVariable;
import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoUtil.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
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

        //make map of pst remedial actions
        Map<Optional<Contingency>, List<RemedialAction>> pstRemedialActions = new HashMap<>();
        pstRemedialActions.put(Optional.empty(), getPreventiveRemedialActions(cracFile)
                .filter(ClosedOptimisationRaoUtil::isPstRemedialAction).collect(Collectors.toList()));
        cracFile.getContingencies().forEach(contingency -> pstRemedialActions.put(Optional.of(contingency),
                getCurativeRemedialActions(cracFile, contingency).filter(ClosedOptimisationRaoUtil::isPstRemedialAction)
                        .collect(Collectors.toList())));

        pstRemedialActions.forEach((contingency, raList) -> {
            //build result list for each contingency and its associated remedial actions
            List<RemedialActionResult> resultList = raList.stream().filter(ra -> isPstRemedialActionActivated(contingency, ra, solver))
                    .map(ra -> {
                        PstElement pst = (PstElement) ra.getRemedialActionElements().get(0);
                        PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(pst.getId()).getPhaseTapChanger();
                        double initialAngle = phaseTapChanger.getCurrentStep().getAlpha();
                        int initialTapPosition = phaseTapChanger.getTapPosition();
                        MPVariable angleValue = Objects.requireNonNull(solver.lookupVariableOrNull(nameShiftValueVariable(contingency, pst)));
                        double finalAngle = initialAngle + angleValue.solutionValue();
                        int finalTapPosition = computeTapPosition(finalAngle, phaseTapChanger, network.getTwoWindingsTransformer(pst.getId()));
                        return new RemedialActionResult(
                                ra.getId(),
                                ra.getName(),
                                true,
                                Collections.singletonList(
                                        new PstElementResult(
                                                pst.getId(),
                                                initialAngle,
                                                initialTapPosition,
                                                finalAngle,
                                                finalTapPosition
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

    private boolean isPstRemedialActionActivated(Optional<Contingency> contingency, RemedialAction ra, MPSolver solver) {
        PstElement pst = (PstElement) ra.getRemedialActionElements().get(0);
        MPVariable pstValue = Objects.requireNonNull(solver.lookupVariableOrNull(nameShiftValueVariable(contingency, pst)));
        return pstValue.solutionValue() != 0;
    }
}
