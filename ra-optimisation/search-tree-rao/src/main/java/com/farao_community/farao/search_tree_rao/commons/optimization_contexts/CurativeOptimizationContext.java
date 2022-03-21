package com.farao_community.farao.search_tree_rao.commons.optimization_contexts;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThreshold;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.algorithm.BasecaseScenario;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CurativeOptimizationContext extends AbstractOptimizationPerimeter {

    public CurativeOptimizationContext(State curativeState,
                                       Set<FlowCnec> flowCnecs,
                                       Set<FlowCnec> looopFlowCnecs,
                                       Set<NetworkAction> availableNetworkActions,
                                       Set<RangeAction<?>> availableRangeActions) {

        super(curativeState, flowCnecs, looopFlowCnecs, availableNetworkActions, Map.of(curativeState, availableRangeActions));

        if (!curativeState.getInstant().equals(Instant.CURATIVE)) {
            throw new FaraoException("a CurativeOptimizationContext must be based on a curative state");
        }
    }

    public static CurativeOptimizationContext build(State curativeState, Crac crac, Network network, RaoParameters raoParameters, FlowResult prePerimeterFlowResult) {

        Set<FlowCnec> flowCnecs = crac.getFlowCnecs(curativeState);

        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

        Set<NetworkAction> availableNetworkActions = crac.getNetworkActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, curativeState, prePerimeterFlowResult))
            .collect(Collectors.toSet());

        Set<RangeAction<?>> availableRangeActions = crac.getRangeActions().stream()
            .filter(ra -> RaoUtil.isRemedialActionAvailable(ra, curativeState, prePerimeterFlowResult))
            .collect(Collectors.toSet());

        return new CurativeOptimizationContext(curativeState,
            flowCnecs,
            loopFlowCnecs,
            availableNetworkActions,
            availableRangeActions);
    }
}
