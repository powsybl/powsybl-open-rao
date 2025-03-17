/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.GlobalRaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.searchtreerao.marmot.MarmotUtils;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class GlobalRaoResultImpl implements GlobalRaoResult {
    private final ObjectiveFunctionResult globalObjectiveFunctionResult;
    private final TemporalData<RaoResult> raoResultPerTimestamp;

    private static final String MISSING_RAO_RESULT_ERROR_MESSAGE = "No RAO Result data found for the provided timestamp.";

    public GlobalRaoResultImpl(ObjectiveFunctionResult globalObjectiveFunctionResult, TemporalData<RaoResult> raoResultPerTimestamp) {
        this.globalObjectiveFunctionResult = globalObjectiveFunctionResult;
        this.raoResultPerTimestamp = raoResultPerTimestamp;
    }

    @Override
    public List<OffsetDateTime> getTimestamps() {
        return raoResultPerTimestamp.getTimestamps();
    }

    @Override
    public double getGlobalFunctionalCost() {
        return globalObjectiveFunctionResult.getFunctionalCost();
    }

    @Override
    public double getGlobalVirtualCost() {
        return globalObjectiveFunctionResult.getVirtualCost();
    }

    @Override
    public double getGlobalVirtualCost(String virtualCostName) {
        return globalObjectiveFunctionResult.getVirtualCost(virtualCostName);
    }

    @Override
    public double getFunctionalCost(Instant optimizedInstant, OffsetDateTime timestamp) {
        return raoResultPerTimestamp.getData(timestamp).orElseThrow(() -> new OpenRaoException(MISSING_RAO_RESULT_ERROR_MESSAGE)).getFunctionalCost(optimizedInstant);
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, OffsetDateTime timestamp) {
        return raoResultPerTimestamp.getData(timestamp).orElseThrow(() -> new OpenRaoException(MISSING_RAO_RESULT_ERROR_MESSAGE)).getVirtualCost(optimizedInstant);
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName, OffsetDateTime timestamp) {
        return raoResultPerTimestamp.getData(timestamp).orElseThrow(() -> new OpenRaoException(MISSING_RAO_RESULT_ERROR_MESSAGE)).getVirtualCost(optimizedInstant, virtualCostName);
    }

    @Override
    public boolean isSecure(Instant optimizedInstant, OffsetDateTime timestamp, PhysicalParameter... u) {
        return raoResultPerTimestamp.getData(timestamp).orElseThrow(() -> new OpenRaoException(MISSING_RAO_RESULT_ERROR_MESSAGE)).isSecure(optimizedInstant, u);
    }

    @Override
    public ComputationStatus getComputationStatus() {
        return MarmotUtils.getGlobalComputationStatus(raoResultPerTimestamp, RaoResult::getComputationStatus);
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getComputationStatus(state);
    }

    @Override
    public double getFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, flowCnec.getState()).getFlow(optimizedInstant, flowCnec, side, unit);
    }

    @Override
    public double getMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, flowCnec.getState()).getMargin(optimizedInstant, flowCnec, unit);
    }

    @Override
    public double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, flowCnec.getState()).getRelativeMargin(optimizedInstant, flowCnec, unit);
    }

    @Override
    public double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, flowCnec.getState()).getCommercialFlow(optimizedInstant, flowCnec, side, unit);
    }

    @Override
    public double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, flowCnec.getState()).getLoopFlow(optimizedInstant, flowCnec, side, unit);
    }

    @Override
    public double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, flowCnec.getState()).getPtdfZonalSum(optimizedInstant, flowCnec, side);
    }

    @Override
    public double getFunctionalCost(Instant optimizedInstant) {
        throw new OpenRaoException("Calling getGlobalFunctionalCost with an instant alone is ambiguous. For the global functional cost, use getGlobalFunctionalCost. Otherwise, please provide a timestamp.");
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant) {
        throw new OpenRaoException("Calling getVirtualCost with an instant alone is ambiguous. For the global virtual cost, use getGlobalVirtualCost. Otherwise, please provide a timestamp.");
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return globalObjectiveFunctionResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName) {
        throw new OpenRaoException("Calling getVirtualCost with an instant and a name alone is ambiguous. For the global virtual cost, use getGlobalVirtualCost. Otherwise, please provide a timestamp.");
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).wasActivatedBeforeState(state, networkAction);
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).isActivatedDuringState(state, networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getActivatedNetworkActionsDuringState(state);
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).isActivatedDuringState(state, rangeAction);
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getPreOptimizationTapOnState(state, pstRangeAction);
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getOptimizedTapOnState(state, pstRangeAction);
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getPreOptimizationSetPointOnState(state, rangeAction);
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getOptimizedSetPointOnState(state, rangeAction);
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getActivatedRangeActionsDuringState(state);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getOptimizedTapsOnState(state);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        return MarmotUtils.getDataFromState(raoResultPerTimestamp, state).getOptimizedSetPointsOnState(state);
    }

    @Override
    public String getExecutionDetails() {
        StringBuilder executionDetails = new StringBuilder();
        for (Map.Entry<OffsetDateTime, String> executionDetailsPerTimestamp : raoResultPerTimestamp.map(RaoResult::getExecutionDetails).getDataPerTimestamp().entrySet()) {
            executionDetails.append(executionDetailsPerTimestamp.getKey().format(DateTimeFormatter.ISO_DATE_TIME));
            executionDetails.append(": ");
            executionDetails.append(executionDetailsPerTimestamp.getValue());
            executionDetails.append(" - ");
        }
        return executionDetails.toString();
    }

    @Override
    public void setExecutionDetails(String executionDetails) {
        // nothing to do
    }

    @Override
    public boolean isSecure(Instant optimizedInstant, PhysicalParameter... u) {
        throw new OpenRaoException("Calling isSecure with an instant and physical parameters alone is ambiguous. Please provide a timestamp.");
    }

    @Override
    public boolean isSecure(PhysicalParameter... u) {
        return raoResultPerTimestamp.map(raoResult -> raoResult.isSecure(u)).getDataPerTimestamp().values().stream().allMatch(bool -> bool);
    }
}
