/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.BranchResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractMinMarginEvaluator implements CostEvaluator {
    Set<BranchCnec> cnecs;
    private final Unit unit;
    private List<BranchCnec> sortedElements = new ArrayList<>();

    public AbstractMinMarginEvaluator(Set<BranchCnec> cnecs, Unit unit) {
        this.cnecs = cnecs;
        this.unit = unit;
    }

    abstract double getMargin(BranchResult branchResult, BranchCnec branchCnec, Unit unit);

    @Override
    public List<BranchCnec> getCostlyElements(BranchResult branchResult, int numberOfElements) {
        if (sortedElements.isEmpty()) {
            sortedElements = cnecs.stream()
                    .sorted(Comparator.comparing(branchCnec -> getMargin(branchResult, branchCnec, unit)))
                    .collect(Collectors.toList());
        }

        return sortedElements.subList(0, Math.min(sortedElements.size(), numberOfElements));
    }

    public BranchCnec getMostLimitingElement(BranchResult branchResult) {
        return getCostlyElements(branchResult, 1).get(0);
    }

    @Override
    public double computeCost(BranchResult branchResult) {
        return getMargin(branchResult, getMostLimitingElement(branchResult), unit);
    }

    @Override
    public Unit getUnit() {
        return unit;
    }
}
