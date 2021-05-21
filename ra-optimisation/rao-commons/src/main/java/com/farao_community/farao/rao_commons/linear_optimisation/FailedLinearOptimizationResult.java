/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.LinearOptimizationResult;
import com.farao_community.farao.rao_api.results.LinearProblemStatus;
import com.farao_community.farao.rao_api.results.SensitivityStatus;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FailedLinearOptimizationResult implements LinearOptimizationResult {

    @Override
    public LinearProblemStatus getStatus() {
        return LinearProblemStatus.ABNORMAL;
    }

    @Override
    public double getFlow(BranchCnec branchCnec, Unit unit) {
        throw new FaraoException("Should not be used");
    }

    @Override
    public double getCommercialFlow(BranchCnec branchCnec, Unit unit) {
        throw new FaraoException("Should not be used");
    }

    @Override
    public double getPtdfZonalSum(BranchCnec branchCnec) {
        throw new FaraoException("Should not be used");
    }

    @Override
    public Map<BranchCnec, Double> getPtdfZonalSums() {
        throw new FaraoException("Should not be used");
    }

    @Override
    public double getFunctionalCost() {
        throw new FaraoException("Should not be used");
    }

    @Override
    public List<BranchCnec> getMostLimitingElements(int number) {
        throw new FaraoException("Should not be used");
    }

    @Override
    public double getVirtualCost() {
        throw new FaraoException("Should not be used");
    }

    @Override
    public Set<String> getVirtualCostNames() {
        throw new FaraoException("Should not be used");
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        throw new FaraoException("Should not be used");
    }

    @Override
    public List<BranchCnec> getCostlyElements(String virtualCostName, int number) {
        throw new FaraoException("Should not be used");
    }

    @Override
    public Set<RangeAction> getRangeActions() {
        throw new FaraoException("Should not be used");
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        throw new FaraoException("Should not be used");
    }

    @Override
    public double getOptimizedSetPoint(RangeAction rangeAction) {
        throw new FaraoException("Should not be used");
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        throw new FaraoException("Should not be used");
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPoints() {
        throw new FaraoException("Should not be used");
    }

    @Override
    public SensitivityStatus getSensitivityStatus() {
        throw new FaraoException("Should not be used");
    }

    @Override
    public double getSensitivityValue(BranchCnec branchCnec, RangeAction rangeAction, Unit unit) {
        throw new FaraoException("Should not be used");
    }

    @Override
    public double getSensitivityValue(BranchCnec branchCnec, LinearGlsk linearGlsk, Unit unit) {
        throw new FaraoException("Should not be used");
    }
}
