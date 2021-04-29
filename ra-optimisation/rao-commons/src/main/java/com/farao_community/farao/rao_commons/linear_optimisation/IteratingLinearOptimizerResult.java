/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class IteratingLinearOptimizerResult implements LinearOptimizationResult {

    private LinearProblemStatus status;
    private final RangeActionResult rangeActionResult;
    private final BranchResult branchResult;
    private final SensitivityResult sensitivityResult;
    private final ObjectiveFunctionResult objectiveFunctionResult;

    public IteratingLinearOptimizerResult(LinearProblemStatus status,
                                          RangeActionResult rangeActionResult,
                                          BranchResult branchResult,
                                          ObjectiveFunctionResult objectiveFunctionResult,
                                          SensitivityResult sensitivityResult) {
        this.status = status;
        this.rangeActionResult = rangeActionResult;
        this.branchResult = branchResult;
        this.objectiveFunctionResult = objectiveFunctionResult;
        this.sensitivityResult = sensitivityResult;
    }

    public void setStatus(LinearProblemStatus status) {
        this.status = status;
    }

    public SensitivityResult getSensitivityResult() {
        return sensitivityResult;
    }

    public BranchResult getBranchResult() {
        return branchResult;
    }

    public ObjectiveFunctionResult getObjectiveFunctionResult() {
        return objectiveFunctionResult;
    }

    public RangeActionResult getRangeActionResult() {
        return rangeActionResult;
    }

    @Override
    public double getFunctionalCost() {
        return objectiveFunctionResult.getFunctionalCost();
    }

    @Override
    public List<BranchCnec> getMostLimitingElements(int number) {
        return objectiveFunctionResult.getMostLimitingElements(number);
    }

    @Override
    public double getVirtualCost() {
        return objectiveFunctionResult.getVirtualCost();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return objectiveFunctionResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return objectiveFunctionResult.getVirtualCost(virtualCostName);
    }

    @Override
    public List<BranchCnec> getCostlyElements(String virtualCostName, int number) {
        return objectiveFunctionResult.getCostlyElements(virtualCostName, number);
    }

    @Override
    public LinearProblemStatus getStatus() {
        return status;
    }

    @Override
    public double getFlow(BranchCnec branchCnec, Unit unit) {
        return branchResult.getFlow(branchCnec, unit);
    }

    @Override
    public double getCommercialFlow(BranchCnec branchCnec, Unit unit) {
        return branchResult.getCommercialFlow(branchCnec, unit);
    }

    @Override
    public double getPtdfZonalSum(BranchCnec branchCnec) {
        return branchResult.getPtdfZonalSum(branchCnec);
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        return rangeActionResult.getOptimizedTap(pstRangeAction);
    }

    @Override
    public double getOptimizedSetPoint(RangeAction rangeAction) {
        return rangeActionResult.getOptimizedSetPoint(rangeAction);
    }

    @Override
    public Set<RangeAction> getActivatedRangeActions() {
        return rangeActionResult.getActivatedRangeActions();
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        return rangeActionResult.getOptimizedTaps();
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPoints() {
        return rangeActionResult.getOptimizedSetPoints();
    }
}
