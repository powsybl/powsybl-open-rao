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
import com.powsybl.openrao.data.crac.api.*;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.searchtreerao.result.api.*;

import com.powsybl.iidm.network.TwoSides;

import java.util.*;

import static com.powsybl.openrao.data.raoresult.api.ComputationStatus.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class FastRaoResultImpl implements RaoResult {
    private final PrePerimeterResult initialResult;
    private final PrePerimeterResult afterPraResult;
    private final PrePerimeterResult afterAraResult;
    private final PrePerimeterResult finalResult;
    private final RaoResult filteredRaoResult;
    private final Crac crac;
    private String executionDetails;

    public FastRaoResultImpl(PrePerimeterResult initialResult,
                             PrePerimeterResult afterPraResult,
                             PrePerimeterResult afterAraResult,
                             PrePerimeterResult finalResult,
                             RaoResult filteredRaoResult,
                             Crac crac) {
        this.initialResult = initialResult;
        this.afterPraResult = afterPraResult;
        this.afterAraResult = afterAraResult;
        this.finalResult = finalResult;
        this.filteredRaoResult = filteredRaoResult;
        this.crac = crac;
        executionDetails = filteredRaoResult.getExecutionDetails();
        removeFailingContingencies(initialResult, afterPraResult, afterAraResult, finalResult, crac);
    }

    private static void removeFailingContingencies(PrePerimeterResult initialResult, PrePerimeterResult afterPraResult, PrePerimeterResult afterAraResult, PrePerimeterResult finalResult, Crac crac) {
        Set<String> failingContingencies = new HashSet<>();
        crac.getStates().stream().filter(state -> initialResult.getComputationStatus(state) == FAILURE && !state.isPreventive())
                .forEach(state -> failingContingencies.add(state.getContingency().get().getId()));
        crac.getStates().stream().filter(state -> afterPraResult.getComputationStatus(state) == FAILURE && !state.isPreventive())
                .forEach(state -> failingContingencies.add(state.getContingency().get().getId()));
        crac.getStates().stream().filter(state -> afterAraResult.getComputationStatus(state) == FAILURE && !state.isPreventive())
                .forEach(state -> failingContingencies.add(state.getContingency().get().getId()));
        crac.getStates().stream().filter(state -> finalResult.getComputationStatus(state) == FAILURE && !state.isPreventive())
                .forEach(state -> failingContingencies.add(state.getContingency().get().getId()));
        initialResult.excludeContingencies(failingContingencies);
        afterPraResult.excludeContingencies(failingContingencies);
        afterAraResult.excludeContingencies(failingContingencies);
        finalResult.excludeContingencies(failingContingencies);
    }

    @Override
    public ComputationStatus getComputationStatus() {
        //TODO: PreventivAndCurativesRaoResult has a postContingencyResults object that we go through to evaluate the PARTIAL_FAILURE status. Understand why this object is not defined here.
        if (initialResult.getSensitivityStatus() == FAILURE) {
            return FAILURE;
        }
        if (initialResult.getSensitivityStatus() == PARTIAL_FAILURE ||
                finalResult == null || finalResult.getSensitivityStatus() != DEFAULT ||
                afterPraResult == null || afterPraResult.getSensitivityStatus() != DEFAULT ||
                afterAraResult == null || afterAraResult.getSensitivityStatus() != DEFAULT
        ) {
            return PARTIAL_FAILURE;
        }
        return DEFAULT;

    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return getAppropriateResult(state.getInstant()).getComputationStatus(state);
    }

    public PrePerimeterResult getAppropriateResult(Instant optimizedInstant) {
        if (optimizedInstant == null) {
            return initialResult;
        }
        if (optimizedInstant.isPreventive() || optimizedInstant.isOutage()) {
            return afterPraResult;
        }
        if (optimizedInstant.isAuto()) {
            return afterAraResult;
        }
        if (optimizedInstant.isCurative()) {
            return finalResult;
        }
        throw new OpenRaoException(String.format("Optimized instant %s was not recognized", optimizedInstant));
    }

    public PrePerimeterResult getAppropriateResult(Instant optimizedInstant, FlowCnec flowCnec) {
        if (Objects.isNull(optimizedInstant)) {
            return initialResult;
        }
        Instant minInstant = optimizedInstant.comesBefore(flowCnec.getState().getInstant()) ?
                optimizedInstant : flowCnec.getState().getInstant();
        return getAppropriateResult(minInstant);
    }

    @Override
    public double getMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return getAppropriateResult(optimizedInstant, flowCnec).getMargin(flowCnec, unit);
    }

    @Override
    public double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return getAppropriateResult(optimizedInstant, flowCnec).getRelativeMargin(flowCnec, unit);
    }

    @Override
    public double getFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return getAppropriateResult(optimizedInstant, flowCnec).getFlow(flowCnec, side, unit);
    }

    @Override
    public double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return getAppropriateResult(optimizedInstant, flowCnec).getCommercialFlow(flowCnec, side, unit);
    }

    @Override
    public double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return getAppropriateResult(optimizedInstant, flowCnec).getLoopFlow(flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side) {
        return getAppropriateResult(optimizedInstant, flowCnec).getPtdfZonalSum(flowCnec, side);
    }

    @Override
    public double getFunctionalCost(Instant optimizedInstant) {
        return getAppropriateResult(optimizedInstant).getFunctionalCost();
    }

    public List<FlowCnec> getMostLimitingElements(Instant optimizedInstant, int number) {
        return getAppropriateResult(optimizedInstant).getMostLimitingElements(number);
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant) {
        return getAppropriateResult(optimizedInstant).getVirtualCost();
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
        return getAppropriateResult(optimizedInstant).getVirtualCost(virtualCostName);
    }

    public List<FlowCnec> getCostlyElements(Instant optimizedInstant, String virtualCostName, int number) {
        return getAppropriateResult(optimizedInstant).getCostlyElements(virtualCostName, number);
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
    public String getExecutionDetails() {
        return executionDetails;
    }

    @Override
    public void setExecutionDetails(String executionDetails) {
        this.executionDetails = executionDetails;
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

}
