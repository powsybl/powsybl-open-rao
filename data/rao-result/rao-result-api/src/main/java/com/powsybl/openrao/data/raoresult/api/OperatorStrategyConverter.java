/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api;

import com.powsybl.action.Action;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.GeneratorActionBuilder;
import com.powsybl.action.LoadAction;
import com.powsybl.action.LoadActionBuilder;
import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.security.condition.Condition;
import com.powsybl.security.condition.TrueCondition;
import com.powsybl.security.strategy.ConditionalActions;
import com.powsybl.security.strategy.OperatorStrategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class OperatorStrategyConverter {
    private static final Condition TRUE = new TrueCondition();

    private OperatorStrategyConverter() {
    }

    public static StrategiesAndActions getOperatorStrategies(RaoResult raoResult, Crac crac, Network network) {
        Set<OperatorStrategy> operatorStrategies = new HashSet<>();
        Set<Action> actions = new HashSet<>();

        // preventive strategy
        ConditionalActions preventiveConditionalActions = getConditionalActionsForStateAndAddActionsToPool(crac.getPreventiveState(), raoResult, network, actions);
        operatorStrategies.add(new OperatorStrategy(crac.getPreventiveState().getInstant().getId(), ContingencyContext.none(), List.of(preventiveConditionalActions)));

        // post-contingency strategies
        List<Instant> postOutageInstants = crac.getSortedInstants().stream().filter(instant -> instant.isAuto() || instant.isCurative()).toList();
        for (Contingency contingency : crac.getContingencies()) {
            List<ConditionalActions> conditionalActions = new ArrayList<>();
            postOutageInstants.forEach(postOutageInstant -> conditionalActions.add(getConditionalActionsForStateAndAddActionsToPool(crac.getState(contingency.getId(), postOutageInstant), raoResult, network, actions)));
            operatorStrategies.add(new OperatorStrategy(contingency.getId(), ContingencyContext.specificContingency(contingency.getId()), conditionalActions));
        }

        return new StrategiesAndActions(operatorStrategies, actions);
    }

    private static ConditionalActions getConditionalActionsForStateAndAddActionsToPool(State state, RaoResult raoResult, Network network, Set<Action> actionsPool) {
        Set<Action> stateActions = getActivatedActionsForState(raoResult, state, network);
        actionsPool.addAll(stateActions);
        return new ConditionalActions(state.getInstant().getId(), TRUE, getActionsIds(stateActions));
    }

    private static Set<Action> getActivatedActionsForState(RaoResult raoResult, State state, Network network) {
        Set<Action> actions = new HashSet<>();
        raoResult.getActivatedNetworkActionsDuringState(state).forEach(networkAction -> actions.addAll(networkAction.getElementaryActions()));
        for (RangeAction<?> rangeAction : raoResult.getActivatedRangeActionsDuringState(state)) {
            if (rangeAction instanceof PstRangeAction pstRangeAction) {
                PhaseTapChangerTapPositionAction action = new PhaseTapChangerTapPositionAction(
                    "%s@%s".formatted(pstRangeAction.getId(), raoResult.getOptimizedTapOnState(state, pstRangeAction)),
                    pstRangeAction.getNetworkElement().getId(),
                    false,
                    raoResult.getOptimizedTapOnState(state, pstRangeAction)
                );
                actions.add(action);
            } else if (rangeAction instanceof InjectionRangeAction injectionRangeAction) {
                double setPoint = raoResult.getOptimizedSetPointOnState(state, injectionRangeAction);
                injectionRangeAction.getInjectionDistributionKeys().forEach((networkElement, distributionKey) -> {
                    Identifiable<?> identifiable = network.getIdentifiable(networkElement.getId());
                    if (identifiable instanceof Generator generator) {
                        double targetP = distributionKey * setPoint;
                        GeneratorAction action = new GeneratorActionBuilder()
                            .withId("%s::%s@%s".formatted(rangeAction.getId(), generator.getId(), targetP))
                            .withGeneratorId(generator.getId())
                            .withActivePowerValue(targetP)
                            .withActivePowerRelativeValue(false)
                            .build();
                        actions.add(action);
                    } else if (identifiable instanceof Load load) {
                        double p0 = Math.abs(distributionKey * setPoint);
                        LoadAction action = new LoadActionBuilder()
                            .withId("%s::%s@%s".formatted(rangeAction.getId(), load.getId(), p0))
                            .withLoadId(load.getId())
                            .withActivePowerValue(p0)
                            .withRelativeValue(false)
                            .build();
                        actions.add(action);
                    } else {
                        throw new OpenRaoException("Network element %s is neither a generator nor a load.".formatted(networkElement.getId()));
                    }
                });
            } else {
                OpenRaoLoggerProvider.TECHNICAL_LOGS.warn(String.format("No equivalent Action existing for remedial action of type: %s. Remedial action %s's result ignored for state %s.", rangeAction.getClass().getSimpleName(), rangeAction.getId(), state.getId()));
            }
        }
        return actions;
    }

    private static List<String> getActionsIds(Set<Action> actions) {
        return actions.stream().map(Action::getId).collect(Collectors.toList());
    }
}
