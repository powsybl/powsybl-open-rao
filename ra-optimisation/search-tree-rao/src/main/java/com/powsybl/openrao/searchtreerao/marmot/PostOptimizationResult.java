/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.Map;
import java.util.Set;

/**
 * This class concatenates all data around one individual timestamp from running Marmot:
 * <ul>
 *     <li>
 *         input data (before Marmot): RaoInput
 *     </li>
 *     <li>
 *         output data (after Marmot):
 *         <ul>
 *             <li>
 *                 RaoResult: output from initial Rao run, containing activated topological actions
 *             </li>
 *             <li>
 *                 LinearOptimizationResult: output from time-coupled MIP, containing activated range actions
 *             </li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class PostOptimizationResult extends AbstractExtendable<RaoResult> implements RaoResult {

    private final Crac crac;
    private final PrePerimeterResult initialResult;
    private final GlobalLinearOptimizationResult postMipResult;
    private final RemedialActionActivationResult remedialActionActivationResult;
    private final ObjectiveFunctionResult singleTimestampObjectiveFunctionResult;
    private String executionDetails = "RAO went through independent topological optimization and global time-coupled linear optimization";

    public PostOptimizationResult(RaoInput raoInput,
                                  PrePerimeterResult initialResult,
                                  GlobalLinearOptimizationResult postMipResult,
                                  NetworkActionsResult networkActionsResult,
                                  RaoParameters raoParameters) {
        this.initialResult = initialResult;
        this.crac = raoInput.getCrac();
        this.postMipResult = postMipResult;
        this.remedialActionActivationResult = MarmotUtils.getRemedialActionActivationResult(initialResult, postMipResult, networkActionsResult, crac);
        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(crac.getFlowCnecs(), Set.of(), initialResult, initialResult, Set.of(), raoParameters, crac.getStates());
        this.singleTimestampObjectiveFunctionResult = objectiveFunction.evaluate(postMipResult, remedialActionActivationResult);
    }

    @Override
    public ComputationStatus getComputationStatus() {
        return postMipResult.getComputationStatus();
    }

    @Override
    public ComputationStatus getComputationStatus(State state) {
        return postMipResult.getComputationStatus(state);
    }

    @Override
    public double getFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        if (optimizedInstant == null) {
            return initialResult.getFlow(flowCnec, side, unit);
        } else {
            return postMipResult.getFlow(flowCnec, side, unit);
        }
    }

    @Override
    public double getMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        if (optimizedInstant == null) {
            return initialResult.getMargin(flowCnec, unit);
        } else {
            return postMipResult.getMargin(flowCnec, unit);
        }
    }

    @Override
    public double getRelativeMargin(Instant optimizedInstant, FlowCnec flowCnec, Unit unit) {
        if (optimizedInstant == null) {
            return initialResult.getRelativeMargin(flowCnec, unit);
        } else {
            return postMipResult.getRelativeMargin(flowCnec, unit);
        }
    }

    @Override
    public double getCommercialFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        if (optimizedInstant == null) {
            return initialResult.getCommercialFlow(flowCnec, side, unit);
        } else {
            return postMipResult.getCommercialFlow(flowCnec, side, unit);
        }
    }

    @Override
    public double getLoopFlow(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side, Unit unit) {
        if (optimizedInstant == null) {
            return initialResult.getLoopFlow(flowCnec, side, unit);
        } else {
            return postMipResult.getLoopFlow(flowCnec, side, unit);
        }
    }

    @Override
    public double getPtdfZonalSum(Instant optimizedInstant, FlowCnec flowCnec, TwoSides side) {
        if (optimizedInstant == null) {
            return initialResult.getPtdfZonalSum(flowCnec, side);
        } else {
            return postMipResult.getPtdfZonalSum(flowCnec, side);
        }
    }

    @Override
    public double getFunctionalCost(Instant optimizedInstant) {
        if (optimizedInstant == null) {
            return initialResult.getFunctionalCost();
        } else {
            //TODO: someday maybe separate post PRA etc costs
            return singleTimestampObjectiveFunctionResult.getFunctionalCost();
        }
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant) {
        if (optimizedInstant == null) {
            return initialResult.getVirtualCost();
        } else {
            //TODO: someday maybe separate post PRA etc costs
            return singleTimestampObjectiveFunctionResult.getVirtualCost();
        }
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return initialResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(Instant optimizedInstant, String virtualCostName) {
        if (optimizedInstant == null) {
            return initialResult.getVirtualCost(virtualCostName);
        } else {
            //TODO: someday maybe separate post PRA etc costs
            return singleTimestampObjectiveFunctionResult.getVirtualCost(virtualCostName);
        }
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        return MarmotUtils.getPreviousStates(state, crac).stream().anyMatch(s -> isActivatedDuringState(s, networkAction));
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return getActivatedNetworkActionsDuringState(state).contains(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        return remedialActionActivationResult.getActivatedNetworkActionsPerState().getOrDefault(state, Set.of());
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction<?> rangeAction) {
        // since both preventive and curative range actions are optimized by the mip, all the activated range actions
        // are extracted from postMipResult
        return postMipResult.getActivatedRangeActions(state).contains(rangeAction);
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        return initialResult.getTap(pstRangeAction);
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        return postMipResult.getOptimizedTap(pstRangeAction, state);
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction<?> rangeAction) {
        return initialResult.getSetpoint(rangeAction);
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction<?> rangeAction) {
        return postMipResult.getOptimizedSetpoint(rangeAction, state);
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActionsDuringState(State state) {
        return postMipResult.getActivatedRangeActions(state);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return postMipResult.getOptimizedTapsOnState(state);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetPointsOnState(State state) {
        return postMipResult.getOptimizedSetpointsOnState(state);
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
        // TODO: this assumes that MARMOT is always used in costly optimization mode -> either impose or fix
        if (optimizedInstant == null) {
            return initialResult.getVirtualCost() > 1e-6;
        } else {
            return singleTimestampObjectiveFunctionResult.getVirtualCost() > 1e-6;
        }
    }

    @Override
    public boolean isSecure(PhysicalParameter... u) {
        return this.isSecure(crac.getLastInstant(), u);
    }
}
