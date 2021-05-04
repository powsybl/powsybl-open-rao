package com.farao_community.farao.search_tree_rao.output;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.OptimizationState;
import com.farao_community.farao.rao_api.results.PerimeterResult;
import com.farao_community.farao.rao_api.results.RaoResult;
import com.powsybl.commons.extensions.Extension;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FailedRaoOutput implements RaoResult {
    //TODO: add optimization status (failed for this implem)
    //TODO: copy syntax for failed iterating for the error thrown (and throw error for each method

    @Override
    public PerimeterResult getPerimeterResult(OptimizationState optimizationState, State state) {
        return null;
    }

    @Override
    public PerimeterResult getPreventivePerimeterResult(OptimizationState optimizationState) {
        return null;
    }

    @Override
    public double getFunctionalCost(OptimizationState optimizationState) {
        return 0;
    }

    @Override
    public List<BranchCnec> getMostLimitingElements(OptimizationState optimizationState, int number) {
        return null;
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState) {
        return 0;
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return null;
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState, String virtualCostName) {
        return 0;
    }

    @Override
    public List<BranchCnec> getCostlyElements(OptimizationState optimizationState, String virtualCostName, int number) {
        return null;
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        return false;
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return false;
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        return null;
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction rangeAction) {
        return false;
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        return 0;
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        return 0;
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction rangeAction) {
        return 0;
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction rangeAction) {
        return 0;
    }

    @Override
    public Set<RangeAction> getActivatedRangeActionsDuringState(State state) {
        return null;
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return null;
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPointsOnState(State state) {
        return null;
    }

    @Override
    public void addExtension(Class aClass, Extension extension) {

    }

    @Override
    public Extension getExtension(Class aClass) {
        return null;
    }

    @Override
    public Extension getExtensionByName(String s) {
        return null;
    }

    @Override
    public boolean removeExtension(Class aClass) {
        return false;
    }

    @Override
    public Collection getExtensions() {
        return null;
    }
}
