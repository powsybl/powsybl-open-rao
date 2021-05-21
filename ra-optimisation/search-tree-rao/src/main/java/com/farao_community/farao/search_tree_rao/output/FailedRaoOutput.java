package com.farao_community.farao.search_tree_rao.output;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.results.OptimizationState;
import com.farao_community.farao.rao_api.results.PerimeterResult;
import com.farao_community.farao.rao_api.results.PrePerimeterResult;
import com.farao_community.farao.rao_api.results.RaoResult;
import com.powsybl.commons.extensions.Extension;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FailedRaoOutput implements RaoResult {
    //TODO: add optimization status (failed for this implem)

    @Override
    public PerimeterResult getPerimeterResult(OptimizationState optimizationState, State state) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public PerimeterResult getPostPreventivePerimeterResult() {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public PrePerimeterResult getInitialResult() {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public double getFunctionalCost(OptimizationState optimizationState) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public List<BranchCnec> getMostLimitingElements(OptimizationState optimizationState, int number) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public Set<String> getVirtualCostNames() {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState, String virtualCostName) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public List<BranchCnec> getCostlyElements(OptimizationState optimizationState, String virtualCostName, int number) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction rangeAction) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction rangeAction) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction rangeAction) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public Set<RangeAction> getActivatedRangeActionsDuringState(State state) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPointsOnState(State state) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public void addExtension(Class aClass, Extension extension) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public Extension getExtension(Class aClass) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public Extension getExtensionByName(String s) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public boolean removeExtension(Class aClass) {
        throw new FaraoException("Should not be used: the RAO failed.");
    }

    @Override
    public Collection getExtensions() {
        throw new FaraoException("Should not be used: the RAO failed.");
    }
}
