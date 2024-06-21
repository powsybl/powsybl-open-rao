/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerCondition;
import com.powsybl.openrao.data.cracapi.triggercondition.UsageMethod;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.jgrapht.alg.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Business object of a group of elementary remedial actions (range or network action).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public abstract class AbstractRemedialAction<I extends RemedialAction<I>> extends AbstractIdentifiable<I> implements RemedialAction<I> {
    protected String operator;
    protected Set<TriggerCondition> triggerConditions;
    protected Integer speed;
    private boolean computedUsageMethods = false;
    private Map<Pair<Instant, Contingency>, UsageMethod> usageMethodPerState;
    private Map<Instant, UsageMethod> usageMethodPerInstant;

    protected AbstractRemedialAction(String id, String name, Set<TriggerCondition> triggerConditions, String operator, Integer speed) {
        super(id, name);
        this.operator = operator;
        this.triggerConditions = triggerConditions;
        this.speed = speed;
    }

    @Override
    public String getOperator() {
        return operator;
    }

    @Override
    public Set<TriggerCondition> getTriggerConditions() {
        return triggerConditions;
    }

    @Override
    public Optional<Integer> getSpeed() {
        return Optional.ofNullable(speed);
    }

    @Override
    public UsageMethod getUsageMethod(State state) {
        if (!computedUsageMethods) {
            computeUsageMethodPerStateAndInstant();
            computedUsageMethods = true;
        }
        Instant instant = state.getInstant();
        Optional<Contingency> contingencyOpt = state.getContingency();
        if (contingencyOpt.isEmpty()) {
            return usageMethodPerInstant.getOrDefault(state.getInstant(), UsageMethod.UNDEFINED);
        }
        Contingency contingency = contingencyOpt.get();
        if (usageMethodPerState.getOrDefault(Pair.of(instant, contingency), UsageMethod.UNDEFINED).equals(usageMethodPerInstant.getOrDefault(instant, UsageMethod.UNDEFINED))) {
            return usageMethodPerInstant.getOrDefault(instant, UsageMethod.UNDEFINED);
        }
        return UsageMethod.getStrongestUsageMethod(Set.of(
            usageMethodPerState.getOrDefault(Pair.of(instant, contingency), UsageMethod.UNDEFINED),
            usageMethodPerInstant.getOrDefault(instant, UsageMethod.UNDEFINED)));
    }

    private void computeUsageMethodPerStateAndInstant() {
        usageMethodPerState = new HashMap<>();
        usageMethodPerInstant = new HashMap<>();

        for (TriggerCondition triggerCondition : triggerConditions) {
            if (triggerCondition.getInstant().isPreventive()) {
                updateMapWithValue(usageMethodPerInstant, triggerCondition.getInstant(), triggerCondition.getUsageMethod());
            } else {
                Optional<Contingency> contingency = triggerCondition.getContingency();
                Optional<Cnec<?>> cnec = triggerCondition.getCnec();
                if (cnec.isPresent()) {
                    State state = cnec.get().getState();
                    Optional<Contingency> stateContingency = state.getContingency();
                    if (triggerCondition.getInstant().equals(state.getInstant()) && stateContingency.isPresent()) {
                        updateMapWithValue(usageMethodPerState, Pair.of(state.getInstant(), stateContingency.get()), triggerCondition.getUsageMethod());
                    }
                } else if (contingency.isPresent()) {
                    updateMapWithValue(usageMethodPerState, Pair.of(triggerCondition.getInstant(), contingency.get()), triggerCondition.getUsageMethod());
                } else {
                    updateMapWithValue(usageMethodPerInstant, triggerCondition.getInstant(), triggerCondition.getUsageMethod());
                }
            }
        }
    }

    private void updateMapWithValue(Map<Instant, UsageMethod> map, Instant key, UsageMethod value) {
        if (!map.containsKey(key)) {
            map.put(key, value);
        } else if (!value.equals(map.get(key))) {
            map.put(key, UsageMethod.getStrongestUsageMethod(Set.of(map.get(key), value)));
        }
    }

    private void updateMapWithValue(Map<Pair<Instant, Contingency>, UsageMethod> map, Pair<Instant, Contingency> key, UsageMethod value) {
        if (!map.containsKey(key)) {
            map.put(key, value);
        } else if (!value.equals(map.get(key))) {
            map.put(key, UsageMethod.getStrongestUsageMethod(Set.of(map.get(key), value)));
        }
    }

    /**
     * Retrieves cnecs associated to the remedial action's trigger conditions.
     */
    // TODO: move this method to RaoUtil
    public Set<FlowCnec> getFlowCnecsConstrainingTriggerConditions(Set<FlowCnec> perimeterCnecs, Network network, State optimizedState) {
        Set<FlowCnec> flowCnecs = new HashSet<>();
        triggerConditions.stream()
            .filter(triggerCondition -> triggerCondition.getCnec().isPresent()
                && triggerCondition.getCnec().get() instanceof FlowCnec
                && triggerCondition.getCnec().get().getState().equals(optimizedState)
                || triggerCondition.getCountry().isPresent()
                && triggerCondition.getContingency().equals(optimizedState.getContingency()))
            .filter(triggerCondition -> optimizedState.getInstant().isAuto() ?
                triggerCondition.getUsageMethod().equals(UsageMethod.FORCED) :
                triggerCondition.getUsageMethod().equals(UsageMethod.AVAILABLE) || triggerCondition.getUsageMethod().equals(UsageMethod.FORCED))
            .forEach(triggerCondition -> flowCnecs.addAll(getFlowCnecsConstrainingForOneTriggerCondition(triggerCondition, perimeterCnecs, network)));
        return flowCnecs;
    }

    // TODO: move this method to RaoUtil
    public Set<FlowCnec> getFlowCnecsConstrainingForOneTriggerCondition(TriggerCondition triggerCondition, Set<FlowCnec> perimeterCnecs, Network network) {
        Optional<Cnec<?>> cnec = triggerCondition.getCnec();
        if (cnec.isPresent() && cnec.get() instanceof FlowCnec flowCnec) {
            return Set.of(flowCnec);
        } else if (triggerCondition.getCountry().isPresent()) {
            return perimeterCnecs.stream()
                .filter(flowCnec -> !flowCnec.getState().getInstant().comesBefore(triggerCondition.getInstant()))
                .filter(flowCnec -> triggerCondition.getContingency().isEmpty() || triggerCondition.getContingency().equals(flowCnec.getState().getContingency()))
                .filter(flowCnec -> isCnecInCountry(flowCnec, triggerCondition.getCountry().get(), network)).collect(Collectors.toSet());
        } else {
            throw new OpenRaoException("This method should only be used for trigger conditions having a CNEC or a country defined");
        }
    }

    private static boolean isCnecInCountry(Cnec<?> cnec, Country country, Network network) {
        return cnec.getLocation(network).stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .anyMatch(cnecCountry -> cnecCountry.equals(country));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractRemedialAction<?> remedialAction = (AbstractRemedialAction<?>) o;
        return super.equals(remedialAction)
            && new HashSet<>(triggerConditions).equals(new HashSet<>(remedialAction.getTriggerConditions()))
            && (operator != null && operator.equals(remedialAction.operator) || operator == null && remedialAction.operator == null);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
