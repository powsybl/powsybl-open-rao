package com.farao_community.farao.search_tree_rao.output;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.*;
import com.farao_community.farao.search_tree_rao.PerimeterOutput;
import com.powsybl.commons.extensions.Extension;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PreventiveAndCurativesRaoOutput implements RaoResult {
    private PrePerimeterResult initialResult;
    private PerimeterResult postPreventiveResult;
    private Map<State, PerimeterResult> postCurativeResults;

    public PreventiveAndCurativesRaoOutput(PrePerimeterResult initialResult, PerimeterResult postPreventiveResult, PrePerimeterResult preCurativeResult, Map<State, OptimizationResult> postCurativeResults) {
        this.initialResult = initialResult;
        this.postPreventiveResult = postPreventiveResult;
        this.postCurativeResults = postCurativeResults.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, entry -> new PerimeterOutput(preCurativeResult, entry.getValue())));
    }

    @Override
    public PerimeterResult getPerimeterResult(OptimizationState optimizationState, State state) {
        if (optimizationState == OptimizationState.INITIAL) {
            if (state.getInstant() == Instant.PREVENTIVE) {
                return null;
            } else {
                return postPreventiveResult;
            }
        }
        if (optimizationState == OptimizationState.AFTER_PRA) {
            return postPreventiveResult;
        }
        if (optimizationState == OptimizationState.AFTER_CRA) {
            if (state.getInstant() == Instant.PREVENTIVE) {
                throw new FaraoException("Trying to access preventive results after cra is forbidden. Either get the preventive results after PRA, or the curative results after CRA.");
            } else {
                return postCurativeResults.get(state);
            }
        }
        throw new FaraoException("OptimizationState was not recognized");
    }

    @Override
    public PerimeterResult getPostPreventivePerimeterResult() {
        return postPreventiveResult;
    }

    @Override
    public PrePerimeterResult getInitialResult() {
        return initialResult;
    }

    @Override
    public double getFunctionalCost(OptimizationState optimizationState) {
        if (optimizationState == OptimizationState.INITIAL) {
            return initialResult.getFunctionalCost();
        }
        if (optimizationState == OptimizationState.AFTER_PRA) {
            return postPreventiveResult.getFunctionalCost();
        }
        double highestFunctionalCost = Double.MIN_VALUE;
        highestFunctionalCost = Math.max(highestFunctionalCost, postCurativeResults.values().stream().map(PerimeterResult::getFunctionalCost).max(Double::compareTo).get());
        return highestFunctionalCost;
    }

    @Override
    public List<BranchCnec> getMostLimitingElements(OptimizationState optimizationState, int number) {
        //TODO : store values to be able to merge easily
        return null;
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState) {
        if (optimizationState == OptimizationState.INITIAL) {
            return initialResult.getVirtualCost();
        }
        if (optimizationState == OptimizationState.AFTER_PRA) {
            return postPreventiveResult.getVirtualCost();
        }
        double virtualCostSum = 0;
        for (PerimeterResult postCurativeResult : postCurativeResults.values()) {
            virtualCostSum += postCurativeResult.getVirtualCost();
        }
        return virtualCostSum;
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return initialResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState, String virtualCostName) {
        if (optimizationState == OptimizationState.INITIAL) {
            return initialResult.getVirtualCost(virtualCostName);
        }
        if (optimizationState == OptimizationState.AFTER_PRA) {
            return postPreventiveResult.getVirtualCost(virtualCostName);
        }
        double virtualCostSum = 0;
        for (PerimeterResult postCurativeResult : postCurativeResults.values()) {
            virtualCostSum += postCurativeResult.getVirtualCost(virtualCostName);
        }
        return virtualCostSum;
    }

    @Override
    public List<BranchCnec> getCostlyElements(OptimizationState optimizationState, String virtualCostName, int number) {
        //TODO : store values to be able to merge easily
        return null;
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return false;
        } else {
            return postPreventiveResult.getActivatedNetworkActions().contains(networkAction);
        }
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return postPreventiveResult.getActivatedNetworkActions().contains(networkAction);
        } else {
            return postCurativeResults.get(state).getActivatedNetworkActions().contains(networkAction);
        }
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return postPreventiveResult.getActivatedNetworkActions();
        } else {
            return postCurativeResults.get(state).getActivatedNetworkActions();
        }
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction rangeAction) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return postPreventiveResult.getActivatedRangeActions().contains(rangeAction);
        } else {
            return postCurativeResults.get(state).getActivatedRangeActions().contains(rangeAction);
        }
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return initialResult.getOptimizedTap(pstRangeAction);
        } else {
            return postPreventiveResult.getOptimizedTap(pstRangeAction);
        }
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return postPreventiveResult.getOptimizedTap(pstRangeAction);
        } else {
            return postCurativeResults.get(state).getOptimizedTap(pstRangeAction);
        }
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction rangeAction) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return initialResult.getOptimizedSetPoint(rangeAction);
        } else {
            return postPreventiveResult.getOptimizedSetPoint(rangeAction);
        }
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction rangeAction) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return postPreventiveResult.getOptimizedSetPoint(rangeAction);
        } else {
            return postCurativeResults.get(state).getOptimizedSetPoint(rangeAction);
        }
    }

    @Override
    public Set<RangeAction> getActivatedRangeActionsDuringState(State state) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return postPreventiveResult.getActivatedRangeActions().stream().filter(rangeAction -> isActivatedDuringState(state, rangeAction)).collect(Collectors.toSet());
        } else {
            return postCurativeResults.get(state).getActivatedRangeActions().stream().filter(rangeAction -> isActivatedDuringState(state, rangeAction)).collect(Collectors.toSet());
        }
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return postPreventiveResult.getOptimizedTaps();
        } else {
            return postCurativeResults.get(state).getOptimizedTaps();
        }
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPointsOnState(State state) {
        if (state.getInstant() == Instant.PREVENTIVE) {
            return postPreventiveResult.getOptimizedSetPoints();
        } else {
            return postCurativeResults.get(state).getOptimizedSetPoints();
        }
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
