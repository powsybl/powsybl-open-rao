package com.farao_community.farao.search_tree_rao.commons.optimization_contexts;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.powsybl.iidm.network.Network;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GlobalOptimizationContext extends AbstractOptimizationPerimeter {

    public GlobalOptimizationContext(State mainOptimizationState,
                                     Set<FlowCnec> flowCnecs,
                                     Set<FlowCnec> loopFlowCnecs,
                                     Set<NetworkAction> availableNetworkActions,
                                     Map<State, Set<RangeAction<?>>> availableRangeActions) {
        super(mainOptimizationState, flowCnecs, loopFlowCnecs, availableNetworkActions, availableRangeActions);
    }

    public static GlobalOptimizationContext build(Crac crac, Network network, RaoParameters raoParameters, FlowResult prePerimeterFlowResult) {

        Set<FlowCnec> flowCnecs = crac.getFlowCnecs();
        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

        // add preventive network actions
        Set<NetworkAction> availableNetworkActions = crac.getNetworkActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, crac.getPreventiveState(), prePerimeterFlowResult))
            .collect(Collectors.toSet());

        Map<State, Set<RangeAction<?>>> availableRangeActions = new HashMap<>();
        // add preventive range actions
        availableRangeActions.put(crac.getPreventiveState(), crac.getRangeActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, crac.getPreventiveState(), prePerimeterFlowResult))
            .collect(Collectors.toSet()));

        //add curative range actions
        crac.getStates().stream()
            .filter(s -> s.getInstant().equals(Instant.CURATIVE))
            .forEach(state -> {
                Set<RangeAction<?>> availableRaForState = crac.getRangeActions().stream()
                    .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, state, prePerimeterFlowResult))
                    .collect(Collectors.toSet());
                if (!availableRaForState.isEmpty()) {
                    availableRangeActions.put(state, availableRaForState);
                }
            });

        return new GlobalOptimizationContext(crac.getPreventiveState(),
            flowCnecs,
            loopFlowCnecs,
            availableNetworkActions,
            availableRangeActions);
    }
}
