/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_commons.adapter.SystematicSensitivityResultAdapter;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.MnecParameters;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * An evaluator that computes the virtual cost resulting from the violation of
 * the MNEC minimum margin soft constraint
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MnecViolationCostEvaluator implements CostEvaluator {
    private final Set<BranchCnec> mnecs;
    private final BranchResult initialFlowResult;
    private final double mnecAcceptableMarginDiminutionInMW;
    private final double mnecViolationCostInMWPerMW;
    private List<BranchCnec> sortedElements = new ArrayList<>();

    public MnecViolationCostEvaluator(Set<BranchCnec> mnecs, BranchResult initialFlowResult, MnecParameters mnecParameters) {
        this.mnecs = mnecs;
        this.initialFlowResult = initialFlowResult;
        mnecAcceptableMarginDiminutionInMW = mnecParameters.getMnecAcceptableMarginDiminution();
        mnecViolationCostInMWPerMW = mnecParameters.getMnecViolationCost();
    }

    @Override
    public String getName() {
        return "mnec-cost";
    }

    private double computeCost(BranchResult branchResult, BranchCnec mnec) {
        double initialMargin = initialFlowResult.getMargin(mnec, MEGAWATT);
        double currentMargin = branchResult.getMargin(mnec, MEGAWATT);
        return Math.max(0, Math.min(0, initialMargin - mnecAcceptableMarginDiminutionInMW) - currentMargin);
    }

    @Override
    public double computeCost(BranchResult branchResult) {
        if (Math.abs(mnecViolationCostInMWPerMW) < 1e-10) {
            return 0;
        }
        double totalMnecMarginViolation = 0;
        for (BranchCnec mnec : mnecs) {
            if (mnec.isMonitored()) {
                totalMnecMarginViolation += computeCost(branchResult, mnec);
            }
        }
        return mnecViolationCostInMWPerMW * totalMnecMarginViolation;
    }

    @Override
    public Unit getUnit() {
        return MEGAWATT;
    }

    @Override
    public List<BranchCnec> getCostlyElements(BranchResult branchResult, int numberOfElements) {
        if (sortedElements.isEmpty()) {
            sortedElements = mnecs.stream()
                    .sorted(Comparator.comparing(mnec -> computeCost(branchResult, mnec)))
                    .collect(Collectors.toList());
        }
        Collections.reverse(sortedElements);

        return sortedElements.subList(0, Math.min(sortedElements.size(), numberOfElements));
    }
}
