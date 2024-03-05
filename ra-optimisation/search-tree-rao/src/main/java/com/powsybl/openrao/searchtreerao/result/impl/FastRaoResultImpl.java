/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.OptimizationStepsExecuted;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.searchtreerao.result.api.*;

import java.util.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class FastRaoResultImpl implements RaoResult {
    private final PrePerimeterResult initialResult;
    private final PrePerimeterResult finalResult;
    private final RaoResult filteredRaoResult;
    private final Crac crac;
    private OptimizationStepsExecuted optimizationStepsExecuted;

    public FastRaoResultImpl(PrePerimeterResult initialResult, PrePerimeterResult finalResult, RaoResult filteredRaoResult, Crac crac) {
        this.initialResult = initialResult;
        this.finalResult = finalResult;
        this.filteredRaoResult = filteredRaoResult;
        this.crac = crac;
        optimizationStepsExecuted = filteredRaoResult.getOptimizationStepsExecuted();
    }

    @Override
    public ComputationStatus getComputationStatus() {
        if (initialResult.getSensitivityStatus() == ComputationStatus.FAILURE || finalResult.getSensitivityStatus() == ComputationStatus.FAILURE) {
            return ComputationStatus.FAILURE;
        }
        if (initialResult.getSensitivityStatus() == finalResult.getSensitivityStatus()) {
            return initialResult.getSensitivityStatus();
        }
        return ComputationStatus.DEFAULT;
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return finalResult.getSensitivityStatus(state);
    }

    private FlowResult getAppropriateResult(Instant optimizedInstant) {
        if (optimizedInstant == null) {
            return initialResult;
        } else {
            return finalResult;
        }
    }

    @Override
    public double getMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return getAppropriateResult(optimizedInstant).getMargin(flowCnec, unit);
    }

    @Override
    public double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return getAppropriateResult(optimizedInstant).getRelativeMargin(flowCnec, unit);
    }

    @Override
    public double getFlow(Instant optimizedInstant, FlowCnec flowCnec, Side side, Unit unit) {
        return getAppropriateResult(optimizedInstant).getFlow(flowCnec, side, unit);
    }

    @Override
    public double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, Side side, Unit unit) {
        return getAppropriateResult(optimizedInstant).getCommercialFlow(flowCnec, side, unit);
    }

    @Override
    public double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, Side side, Unit unit) {
        return getAppropriateResult(optimizedInstant).getLoopFlow(flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, Side side) {
        return getAppropriateResult(optimizedInstant).getPtdfZonalSum(flowCnec, side);
    }

    @Override
    public double getFunctionalCost(Instant optimizedInstant) {
        if (optimizedInstant == null) {
            return initialResult.getFunctionalCost();
        } else {
            return finalResult.getFunctionalCost();
        }
    }

    public List<FlowCnec> getMostLimitingElements(Instant optimizedInstant, int number) {
        if (optimizedInstant == null) {
            return initialResult.getMostLimitingElements(number);
        } else {
            return finalResult.getMostLimitingElements(number);
        }
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant) {
        if (optimizedInstant == null) {
            return initialResult.getVirtualCost();
        } else {
            return finalResult.getVirtualCost();
        }
    }

    @Override
    public Set<String> getVirtualCostNames() {
        Set<String> virtualCostNames = new HashSet<>();
        if (initialResult.getVirtualCostNames() != null) {
            virtualCostNames.addAll(initialResult.getVirtualCostNames());
        }
        if (finalResult.getVirtualCostNames() != null) {
            virtualCostNames.addAll(finalResult.getVirtualCostNames());
        }
        return virtualCostNames;
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName) {
        if (optimizedInstant == null) {
            return initialResult.getVirtualCost(virtualCostName);
        } else {
            return finalResult.getVirtualCost(virtualCostName);
        }
    }

    public List<FlowCnec> getCostlyElements(Instant optimizedInstant, String virtualCostName, int number) {
        if (optimizedInstant == null) {
            return initialResult.getCostlyElements(virtualCostName, number);
        } else {
            return finalResult.getCostlyElements(virtualCostName, number);
        }
    }

    @Override
    public boolean isActivatedDuringState(State state, RemedialAction<?> remedialAction) {
        if (remedialAction instanceof NetworkAction networkAction) {
            return isActivatedDuringState(state, networkAction);
        } else if (remedialAction instanceof RangeAction<?> rangeAction) {
            return isActivatedDuringState(state, rangeAction);
        } else {
            throw new OpenRaoException("Unrecognized remedial action type");
        }
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        return filteredRaoResult.wasActivatedBeforeState(state, networkAction);
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return filteredRaoResult.isActivatedDuringState(state, networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        return filteredRaoResult.getActivatedNetworkActionsDuringState(state);
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        return filteredRaoResult.isActivatedDuringState(state, rangeAction);
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        return filteredRaoResult.getPreOptimizationTapOnState(state, pstRangeAction);
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        return filteredRaoResult.getOptimizedTapOnState(state, pstRangeAction);
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        return filteredRaoResult.getPreOptimizationSetPointOnState(state, rangeAction);
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        return filteredRaoResult.getOptimizedSetPointOnState(state, rangeAction);
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        return filteredRaoResult.getActivatedRangeActionsDuringState(state);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return filteredRaoResult.getOptimizedTapsOnState(state);

    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        return filteredRaoResult.getOptimizedSetPointsOnState(state);
    }

    @Override
    public void setOptimizationStepsExecuted(OptimizationStepsExecuted optimizationStepsExecuted) {
        if (this.optimizationStepsExecuted.isOverwritePossible(optimizationStepsExecuted)) {
            this.optimizationStepsExecuted = optimizationStepsExecuted;
        } else {
            throw new OpenRaoException("The RaoResult object should not be modified outside of its usual routine");
        }
    }

    @Override
    public boolean isSecure(Instant optimizedInstant, PhysicalParameter... u) {
        if (ComputationStatus.FAILURE.equals(getComputationStatus())) {
            return false;
        }
        return getFunctionalCost(optimizedInstant) < 0;
    }

    @Override
    public boolean isSecure(PhysicalParameter... u) {
        return isSecure(crac.getLastInstant(), u);
    }

    @Override
    public OptimizationStepsExecuted getOptimizationStepsExecuted() {
        return optimizationStepsExecuted;
    }
}
